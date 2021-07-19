package com.patch.patchcalling.fcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.patch.patchcalling.Constants;
import com.patch.patchcalling.R;
import com.patch.patchcalling.activity.PatchCallingActivity;
import com.patch.patchcalling.broadcastreciever.CallNotificationActionReceiver;
import com.patch.patchcalling.fragments.PatchCallscreenFragment;
import com.patch.patchcalling.fragments.PatchIncomingFragment;
import com.patch.patchcalling.interfaces.ApiInterface;
import com.patch.patchcalling.interfaces.CallStatus;
import com.patch.patchcalling.interfaces.PatchNotificationListener;
import com.patch.patchcalling.interfaces.OnSetNotificationListener;
import com.patch.patchcalling.interfaces.PatchInitResponse;
import com.patch.patchcalling.javaclasses.CryptLib;
import com.patch.patchcalling.javaclasses.PatchSDK;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.services.CallNotificationService;
import com.patch.patchcalling.services.ConnectivityReceiver;
import com.patch.patchcalling.services.JobSchedulerSocketService;
import com.patch.patchcalling.services.RetrofitClient;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.CustomHandler;
import com.patch.patchcalling.utils.JwtUtil;
import com.patch.patchcalling.utils.NotificationHandler;
import com.patch.patchcalling.utils.PatchLogger;
import com.patch.patchcalling.utils.SocketIOManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnSuccess.SUCCESS_PATCH_SDK_INITIALIZED;
import static com.patch.patchcalling.PatchResponseCodes.SUCCESS_SDK_CONNECTED;

/**
 * Created by Shivam Sharma on 20-04-2020.
 */
public class FcmSigsockService extends Service implements ConnectivityReceiver.ConnectivityReceiverListener {
    private Socket socket;
    private String cc, phone, accountId, apikey, cuid;
    private static String jwt = "", sna = "";
    SocketInit socketInit;
    CallStatus callStatus;
    CountDownTimer outgoingTimer;
    Handler mhandler;
    private static Boolean isUnAuthorized = false;
    ApiInterface apiService = null;
    CustomHandler customHandler = CustomHandler.getInstance();
    private ConnectivityReceiver mConnectivityReceiver;
    private static int transportDisconnectTimer = 0;
    private static CountDownTimer transportErrorCountDownTimer;
    private static Handler countDownHandler;
    private static Boolean isDisconnectDueToTransportError = false, canRetryAfterUnAuthorizedDueToTransportError = false;  //throws disconnect reason in event handler when network not avaialble
    private final int SIGSOCK_PING_TIMEOUT = 10000;
    private Timer reconnectScheduler = new Timer();
    private int reconnectingCount = 0;
    final String CHANNEL_ID = "com.patch.pushnotification";
    final String CHANNEL_NAME = "Patch Push Notifications";
    final String CHANNEL_DESC = "push notification to receive call";
    PendingIntent pendingIntent = null;
    String title = "", body = "", subText = "";
    int smallIcon, largeIcon = 0, stickyColor = 0;


    Notification.Builder notificationBuilder1;
    NotificationCompat.Builder notificationBuilder2;
    JSONObject callDetails;
    String callContext;
    //String CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC;
    private Context context;
    NotificationManager mNotificationManager;
    Map<String, Intent> actionIntentMap = new HashMap<>();

    private static FcmSigsockService instance = null;

    public static FcmSigsockService getInstance() {
        return instance;
    }

    private Intent getActionIntent(String actionType, JSONObject callDetails) {
        Intent actionIntent = null;
        try {
            actionIntent = new Intent(/*context*/ this, CallNotificationActionReceiver.class);
            actionIntent.putExtra("actionType", actionType);
            actionIntent.putExtra("callDetails", callDetails.toString());
            if (actionType.equals("Answer") && SocketInit.getInstance().getSid() != null) {
                actionIntent.putExtra("sid", SocketInit.getInstance().getSid());
            }
        } catch (Exception e) {

        }
        return actionIntent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            instance = this;
            this.context = this;
            
            apiService = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                    create(ApiInterface.class);
            
            if (context != null) {
                mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            } else {
                mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationManager.createNotificationChannel(NotificationHandler.getInstance(context).getNotficationChannel(CHANNEL_ID, CHANNEL_NAME, CHANNEL_DESC, "call"));
            }
        } catch (Exception e) {
            stopSelf();
        }

        try {
            if(SocketInit.getInstance().getContext()!=null){
                context = SocketInit.getInstance().getContext();
            }else {
                context = getApplicationContext();
            }
            mConnectivityReceiver = new ConnectivityReceiver(this);
        } catch (Exception e) {

        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (SocketInit.getInstance().getContext() != null) {
                context = SocketInit.getInstance().getContext();
            } else {
                context = getApplicationContext();
            }
           /* new Thread(new Runnable() {
                @Override
                public void run() {
                    AppUtil.getInstance().startServiceAndSigsock(context);
                }
            }).start();*/
            //Log.d("patchsharma", "fcmservice onStartCommand" );
            String action = intent.getAction();
            // TODO - Put validation for payload before starting the service
            // TODO - Also Put validation for callId inside payload if cancel-notification is received
            JSONObject incomingData = new JSONObject(intent.getStringExtra(Constants.FCM_NOTIFICATION_PAYLOAD));
            switch (action) {
                case Constants.ACTION_INCOMING_CALL:
                    handleIncomingCall(context, incomingData);
                    break;
                case Constants.ACTION_CANCEL_CALL:
                    handleCancelledCall(context, incomingData.has("callId") ?
                            incomingData.getString("callId") : null);
                    break;
                case Constants.ACTION_FCM_NOTIFICATION_RECEIVED:
                    JobSchedulerSocketService.generateNotification(context, incomingData);
                    final String notificationId = incomingData.has("notificationId") ? incomingData.getString("notificationId") : "";
                    FcmUtil.getInstance(context).setFcmNotificationID(notificationId);
                    if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                        PatchCommonUtil.getInstance().markNotificationDelivered(context);
                        Log.d("PatchNotification", "markNotificationDelivered  fcmsigsockservice");
                    }else{
                        FcmCacheManger.getInstance().setShouldMarkNotificationStatusDelivered(true);
                        Log.d("PatchNotification", "setShouldMarkNotificationStatusDelivered  true");
                        //Following setter will help to get notificationID while marking fcmNotification status to delivered
                    }
                    break;
                default:

                    break;
            }
            //do heavy work on a background thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        countDownHandler = new Handler(Looper.getMainLooper());
                        JwtUtil.getInstance(getApplicationContext()).verifyTokenForSigsockService(context, new PatchInitResponse() {
                            @Override
                            public void onSuccess(int response) {
                                try {
                                    if(response == 1){
                                       /* if(AppUtil.getInstance().getRunningTaskCount(getApplicationContext()) > 0) {

                                        }*/
                                        if(SocketIOManager.getSocket()!=null) {

                                            if(!SocketIOManager.isSocketConnected()) {
                                                SocketIOManager.getSocket().connect();
                                            }else {
                                                return;
                                            }
                                        }else{
                                            JwtUtil.getInstance(getApplicationContext()).startServiceAndSigsock(getApplicationContext());
/*
                                            registerReceiver(mConnectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
*/
                                        }
                                        //preloading brand__logo
                                        if (getApplicationContext() != null) {
                                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                            String logoUrl = sharedPref.getString("patch_logo", null);
                                            PatchCommonUtil.getInstance().preloadBrandLogo(getApplicationContext(), logoUrl);
                                        }
                                    }

                                }catch (Exception e){
                                    endForeground();
                                }
                            }

                            @Override
                            public void onFailure(int failure) {
                                try {
                                    if (failure == 0) {
                                        /*if(SocketInit.getInstance().getPatchStickyServiceResponse()!= null) {
                                            customHandler.sendInitAnnotations(SocketInit.getInstance().getPatchStickyServiceResponse(),
                                                    CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_STICKY_SERVICE_FINISHED);
                                        }*/


                                        stopSelf();
                                        return;
                                    }
                                }catch (Exception e){

                                }
                            }
                        });
                    }catch (Exception e){

                    }
                }
            }).start();

        } catch (Exception e) {
            //stop service here
            try {
                endForeground();
                stopSelf();
            }catch (Exception e1){

            }
        }
        return START_NOT_STICKY;
    }

    private void handleIncomingCall(Context context, JSONObject incomingData) {
        setCallInProgressNotification(incomingData);
        SocketInit.getInstance().setCallDirection("incoming");
        FcmUtil.getInstance(context).onIncomingCall(context, incomingData);
    }

    private void setCallInProgressNotification(JSONObject incomingData) {
        try {
            callDetails = incomingData;
            callContext = callDetails.getString("context");

            //startservice in foreground fot oreo and onwards other show only notication
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(10001, createNotification());
            }else {
                createNotification();
            }
        } catch (Exception e) {

        }
    }

    private Notification createNotification() {
        try {
            if (PatchIncomingFragment.getInstance() != null) {
                actionIntentMap.put("Answer", getActionIntent("Answer", callDetails));
            } else {
                actionIntentMap.put("Answer", getActionIntent("Answer", new JSONObject(callDetails.getString(this.getString(R.string.callDetails)))));
            }
            actionIntentMap.put("Decline", getActionIntent("Decline", callDetails));

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String logoUrl = sharedPref.getString("patch_logo", null);
            RequestOptions requestOptions = RequestOptions
                    .diskCacheStrategyOf(DiskCacheStrategy.ALL);
            Glide.with(this).asBitmap().
                    load(logoUrl).
                    listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            try {
                                Log.d("pachsharma", "onLoadFailed");
                                generateNotfication(null);
                                //stopSelf();
                            } catch (Exception e1) {

                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            try {
                                generateNotfication(resource);
                            } catch (Exception e) {

                                stopSelf();
                            }
                            return false;
                        }
                    })
                    .apply(requestOptions)
                    .submit();
        } catch (Exception e) {

        }
        return null;
    }

    private void handleCancelledCall(Context context, String callId) {
        FcmUtil.getInstance(context).onCancelCall(context, callId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            //Log.d("sharma", "exampleonDestroy");
            instance = null;
            try {
                unregisterReceiver(mConnectivityReceiver);
            }catch (Exception e){

            }
        } catch (Exception e) {
            if (getApplicationContext() != null) {
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
     //   endForeground();
    }

    private void endForeground() {
        try {
            stopForeground(true);
        }catch (Exception e){

        }
    }
    public Notification generateNotfication(Bitmap resource) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder1 = new Notification.Builder(context, CHANNEL_ID)
                        .setContentTitle(callContext)
                        .setContentText("Incoming voice call")
                        .setShowWhen(true)
                        .setSmallIcon(R.drawable.ic_patch_lightening)
                        .setStyle(new Notification.BigTextStyle().bigText("Incoming voice call"))
                        //.setLargeIcon(notificationHandler.getBitmapFromUrl(context, logoUrl))
                        .setAutoCancel(false);

                if (resource != null) {
                    notificationBuilder1.setLargeIcon(resource);
                }

                Intent intent = new Intent(context, PatchCallingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                          /*  if(notiType.equals(INCOMING_CALL)){
                                Bundle bundle = new Bundle();
                                bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                                bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                                bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                                intent.putExtras(bundle);
                            }*/
                Bundle bundle = new Bundle();
                bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                intent.putExtras(bundle);
                notificationBuilder1.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);

                notificationBuilder1.setOngoing(true)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));


                for (String key : actionIntentMap.keySet()) {
                    switch (key) {
                        case "Answer":
                            notificationBuilder1.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        case "Decline":
                            notificationBuilder1.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        default:
                            notificationBuilder1.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                    }
                }
                return notificationBuilder1.build();
                //startForeground(199, notificationBuilder1.build());
            } else {
                notificationBuilder2 = new NotificationCompat.Builder(context)
                        .setContentTitle(callContext)
                        .setContentText("Incoming voice call")
                        .setShowWhen(true)
                        .setSmallIcon(R.drawable.ic_patch_lightening)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .setBigContentTitle(callContext)
                                .bigText("Incoming voice call"))
                        //.setLargeIcon(notificationHandler.getBitmapFromUrl(context, logoUrl))
                        .setAutoCancel(false);

                if (resource != null) {
                    notificationBuilder2.setLargeIcon(resource);
                }

                Intent intent = new Intent(context, PatchCallingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                          /*  if(notiType.equals(INCOMING_CALL)){
                                Bundle bundle = new Bundle();
                                bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                                bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                                bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                                intent.putExtras(bundle);
                            }*/
                Bundle bundle = new Bundle();
                bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                intent.putExtras(bundle);
                notificationBuilder2.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);

                notificationBuilder2.setOngoing(true)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));


                for (String key : actionIntentMap.keySet()) {
                    switch (key) {
                        case "Answer":
                            notificationBuilder2.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        case "Decline":
                            notificationBuilder2.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                        default:
                            notificationBuilder2.addAction(0, key, NotificationHandler.getInstance(context).getPendingIntent(context, actionIntentMap.get(key)));
                            break;
                    }
                }
                return notificationBuilder2.build();
                //startForeground(199, notificationBuilder2.build());
            }

        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        try {
            if (isConnected) {
                try {
                    canRetryAfterUnAuthorizedDueToTransportError = false;
                    //updatePendingSentiment();                   // sending pending sentiment in a PATCH request to update sentiment against notifications
                    if (isDisconnectDueToTransportError == true && transportDisconnectTimer < 10) {
                        countDownHandler.
                                post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            transportErrorCountDownTimer.cancel();

                                            new CountDownTimer(SIGSOCK_PING_TIMEOUT - (transportDisconnectTimer * 1000), 1000) {
                                                public void onTick(long millisUntilFinished) {
                                                }

                                                public void onFinish() {
                                                    try {
                                                        startSigsock();
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }.start();
                                        } catch (Exception e) {

                                        }
                                    }
                                });
                    } else {
                        startSigsock();
                    }
                } catch (Exception e) {
                    if (getApplicationContext() != null) {
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                    }
                }

            } else {
                Log.d("Patch", "Sorry! Not connected to internet");
            }
        }catch (Exception e){

        }
    }

    /**
     * fetch the jwt token and initialize and start the sigsock.
     */
    public void startSigsock() {
        try {
            //this condition is important becasue os throw multiple networkchange broadcast
            if (SocketIOManager.isSocketConnected()){
                return;
            }

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            phone = sharedPref.getString("patch_userPhone", "");
            cc = sharedPref.getString("patch_userCC", "");
            accountId = sharedPref.getString("patch_accountId", null);
            apikey = sharedPref.getString("patch_apikey", null);
            cuid = sharedPref.getString("patch_cuid", "");
            jwt = sharedPref.getString(this.getApplicationContext().getResources().getString(R.string.patch_token), "");
            sna = sharedPref.getString(this.getApplicationContext().getResources().getString(R.string.sna), "");

            try {
                if(jwt!=null && jwt.isEmpty()){
                    PatchCommonUtil.getInstance().removeSession(this.getApplicationContext());
                    JwtUtil.getInstance(getApplicationContext()).stopServiceAndSigsock(this.getApplicationContext());
                    if (SocketInit.getInstance() != null && SocketInit.getInstance().getPatchInitResponse() != null) {
                        customHandler.sendInitAnnotations(SocketInit.getInstance().getPatchInitResponse(), CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                    }
                    return;
                }
            }catch (Exception e){
                if(jwt!=null && jwt.isEmpty()) {
                    PatchCommonUtil.getInstance().removeSession(this.getApplicationContext());
                    JwtUtil.getInstance(getApplicationContext()).stopServiceAndSigsock(this.getApplicationContext());
                    return;
                }
            }

            IO.Options opts = new IO.Options();
            opts.transports = new String[]{getString(R.string.websocket)};
            opts.forceNew = true;
            opts.reconnection = true;
            opts.reconnectionAttempts = 5;
            opts.reconnectionDelay = 1500;
            opts.reconnectionDelayMax = 6000;
            opts.timeout = 300000;
            opts.query = getApplicationContext().getString(R.string.jwt) + jwt;

            String url = "https://" + sna;
            try {
                socket = SocketIOManager.getSocket(opts, url);
                SocketIOManager.setSocket(socket);
                setUpEventHandlers();
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
        }
    }

    /**
     * registers all the events/handlers of sigsock and take actions accordingly on receiving those events.
     *
     * @throws JSONException
     */
    private void setUpEventHandlers() throws Exception {
        final JSONObject authArray = new JSONObject();
        authArray.put(getApplicationContext().getString(R.string.platform), getApplicationContext().getString(R.string.android));
        authArray.put(getApplicationContext().getString(R.string.accountId), accountId);
        authArray.put(getApplicationContext().getString(R.string.apikey), apikey);
        authArray.put(getApplicationContext().getString(R.string.cc), cc);
        authArray.put(getApplicationContext().getString(R.string.phone), phone);
        authArray.put("cuid", cuid);

        if (SocketIOManager.getIsNewInstance()) {
            socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                    try {
                        if (args[0] instanceof String) {
                            String reason = (String) args[0];
                            //Log.d("Patch", "reason" + reason);
                            //Log.d("sharma",  "disconnect "  + reason);

                            if (reason != null)
                                switch (reason) {
                                    case "transport error":
                                        isDisconnectDueToTransportError = true;
                                        canRetryAfterUnAuthorizedDueToTransportError = true;
                                        socket.io().reconnection(false); //this is important to remove unauthirized case
                                        transportDisconnectTimer = 0;
                                        countDownHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    transportErrorCountDownTimer = new CountDownTimer(SIGSOCK_PING_TIMEOUT, 1000) {

                                                        public void onTick(long millisUntilFinished) {
                                                            transportDisconnectTimer += 1;
                                                        }

                                                        public void onFinish() {
                                                            try {
                                                                socket.io().reconnection(true);
                                                                if (!socket.connected()) {
                                                                    socket.connect();
                                                                }
                                                            }catch (Exception e){

                                                            }
                                                        }
                                                    };
                                                    transportErrorCountDownTimer.start();
                                                }catch (Exception e){

                                                }
                                            }
                                        });
                                        break;
                                    case "io server disconnect":
                                        //call /jwt/verify wala flow
                                        try {
                                            if (!isUnAuthorized) {
                                                //Log.d("sharma", "verifytoken in server io");
                                                JwtUtil.getInstance(getApplicationContext()).verifyToken(getApplicationContext(), false, SocketInit.getInstance().getPatchInitResponse());
                                            }
                                        }catch (Exception e){

                                        }
                                        break;
                                    /*default:
                                        socket.io().reconnection(true);
                                        if (!isUnAuthorized) {
                                            if (!socket.connected()) {
                                                socket.connect();
                                            }
                                        }
                                        break;*/
                                }
                        }
                    } catch (Exception e) {
                    }
                }
            })
                    .on(getApplication().getString(R.string.sconnect), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                //Log.d("sharma",  "sconnect");

                                isDisconnectDueToTransportError = false;
                                socket.io().reconnection(true);
                                socket.emit(getApplicationContext().getString(R.string.sauthentication), authArray);
                            } catch (Exception e) {

                            }
                        }
                    })

                    .on(getApplication().getString(R.string.sauthenticated), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                PatchSDK.sdkReady = true;
                                //Log.d("sharma",  "sauthenticated");

                                isDisconnectDueToTransportError = false;
                                isUnAuthorized = false;
                                SocketIOManager.setIsUnAuthorized(false);
                                socketInit = SocketInit.getInstance();
                                socketInit.setSocket(socket);

                                Log.d("patchsharmafcm", "sigsock2 " +SocketIOManager.isSocketConnected());
                                if(FcmCacheManger.getInstance().isAnswered()){
                                    if(PatchIncomingFragment.getInstance()!=null){
                                        PatchIncomingFragment.getInstance().callAccepted();
                                    }
                                }else if(FcmCacheManger.getInstance().isRejected()){
                                    /*if(PatchIncomingFragment.getInstance()!=null){

                                    }*/
                                    //jan bujhkar declineReason set to null kia hai. cz it will be the rightmost decision.
                                    Log.d("patchsharmafcm", "sauthenticated calldeclined3" );;
                                    //PatchIncomingFragment.fcmCallDecline(getApplicationContext());
                                    PatchCommonUtil.getInstance().fcmCallDecline(getApplicationContext());
                                }
                                //bottom line of resetting cache is required
                                FcmCacheManger.getInstance().resetFcmCache();

                                if (SocketInit.getInstance() != null && SocketInit.getInstance().getPatchInitResponse() != null) {
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                if (SocketIOManager.getIsUnAuthorized()) {
                                                    customHandler.sendInitAnnotations(SocketInit.getInstance().getPatchInitResponse(), CustomHandler.Init.ON_FAILURE, ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE);
                                                } else {
                                                    customHandler.sendInitAnnotations(SocketInit.getInstance().getPatchInitResponse(), CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_SDK_INITIALIZED);
                                                }
                                                SocketInit.getInstance().setPatchInitResponse(null);
                                            } catch (Exception e) {

                                            }
                                        }
                                    }, 1200);
                                } else {
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                if (!SocketIOManager.getIsUnAuthorized()) {
                                                    Intent intent = new Intent("com.patch.CONNECTION_STATUS");
                                                    intent.putExtra("status", SUCCESS_SDK_CONNECTED);
                                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                                                }
                                            } catch (Exception e) {

                                            }
                                            /*else{
                                        intent.putExtra("status", ERR_CONNECTION_FAILED);
                                    }*/
                                        }
                                    }, 1000);
                                }
                            } catch (Exception e) {
                                try {
                                    if (getApplicationContext() != null) {
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                    }
                                } catch (Exception e1) {

                                }

                            }
                        }
                    })
                    .on(getApplication().getString(R.string.sunauthorized), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                //Log.d("sharma",  "sunauthorized");

                                JSONObject response = (JSONObject) args[0];
                                //Log.d("sharma",  "sunauthorized" + response);

                                isUnAuthorized = true;
                                SocketIOManager.setIsUnAuthorized(true);

                                /*if(SocketInit.getInstance()!= null && SocketInit.getInstance().getPatchInitResponse()!=null){
                                    customHandler.sendInitAnnotations(SocketInit.getInstance().getPatchInitResponse(), CustomHandler.Init.ON_FAILURE, ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE);
                                }*/

                                //if (canRetryAfterUnAuthorizedDueToTransportError) {
                                if (reconnectScheduler != null && reconnectingCount == 0) {
                                    reconnectScheduler.scheduleAtFixedRate(new TimerTask() {
                                        @Override
                                        public void run() {
                                            try {
                                                if (reconnectingCount <= 2) {
                                                    if (socket != null && !socket.connected()) {
                                                        socket.connect();
                                                    } else {
                                                        //matlab socket connect ho chuka hai, so reconnectingCount ko reset karo
                                                        reconnectingCount = 0;
                                                        reconnectScheduler.cancel();
                                                        return;
                                                    }
                                                } else {
                                                    reconnectingCount = 0;
                                                    if (socket != null) {
                                                        socket.close();
                                                        socket.off();
                                                    }
                                                    reconnectScheduler.cancel();
                                                    Intent intent = new Intent(getApplicationContext(), FcmSigsockService.class);
                                                    stopService(intent);
                                                    /*JobScheduler jobScheduler = (JobScheduler) getApplicationContext().
                                                            getSystemService(Context.JOB_SCHEDULER_SERVICE);
                                                    jobScheduler.cancel(10);*/
                                                    return;
                                                }
                                                reconnectingCount += 1;
                                            } catch (Exception e) {
                                                try {
                                                    reconnectScheduler.cancel();
                                                }catch (Exception e1){

                                                }
                                            }
                                        }
                                    }, 5000, 5000);
                                }

                                /*JobScheduler jobScheduler = (JobScheduler) getApplicationContext().
                                        getSystemService(Context.JOB_SCHEDULER_SERVICE);
                                jobScheduler.cancel(10);*/

                            } catch (Exception e) {
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    .on("sigsockError", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                        }
                    })

                    .on("makecall", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                        }
                    })
                    .on("makecall_forward", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                        }
                    })
                    .on(getApplication().getString(R.string.scancel), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                String cancelCall = (String) args[0];
                                String currentCall = socketInit.getCallId();
                                if (currentCall == null) {
                                    Ack ack = (Ack) args[args.length - 1];
                                    ack.call("noactivecall");
                                } else if (!(currentCall.equals(cancelCall))) {
                                    Ack ack = (Ack) args[args.length - 1];
                                    ack.call("othercall");
                                } else {
                                    JSONObject status = new JSONObject();
                                    status.put("status", true);
                                    Ack ack = (Ack) args[args.length - 1];
                                    ack.call(status);
                                    //AppUtil.getInstance().dismissCallNotificationService(getApplicationContext());
                                    if (PatchIncomingFragment.getInstance() != null) {
                                        CallStatus.incomingCallStatus incomingCallStatus = socketInit.getIncomingCallStatus();
                                        incomingCallStatus.onCancel();
                                    } else {
                                        PatchCommonUtil.getInstance().callCancelled(getApplicationContext());
                                    }
                                }
                            } catch (Exception e) {
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    .on(getApplication().getString(R.string.sanswer), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                JSONObject status = new JSONObject();
                                status.put("status", true);
                                Ack ack = (Ack) args[args.length - 1];
                                ack.call(status);
                                callStatus = socketInit.getCallStatus();
                                callStatus.onAnswer();
                                socketInit.getTimer().cancel();
                            } catch (Exception e) {
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    .on(getApplication().getString(R.string.sdecline), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                JSONObject status = new JSONObject();
                                status.put("status", true);
                                Ack ack = (Ack) args[args.length - 1];
                                ack.call(status);

                                JSONObject data = (JSONObject) args[0];
                                if (data.has("reasonCode") && data.get("reasonCode") instanceof Integer && data.getInt("reasonCode") == 401) {
                                    /*if (calleeCC.length() > 0 && calleePhone.length() > 0) {
                                        callOptions.put("pstn", true);
                                        callOptions.remove("autoFallback");
                                        makeCall(calleeCC, calleePhone, "", callContext, callOptions, outgoingCallResponse, "401");
                                    } else {
                                        callOptions.put("pstn", true);
                                        callOptions.remove("autoFallback");
                                        makeCall("", "", calleeCuid, callContext, callOptions, outgoingCallResponse, "401");
                                    }*/
                                    return;
                                } else {

                                    if (data.has("reasonCode") && data.get("reasonCode") instanceof Integer && data.getInt("reasonCode") == 503) {
                                        SocketInit.getInstance().setCalleBusyOnAnotherCall(true);

                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                callStatus = socketInit.getCallStatus();
                                                callStatus.onDecline();
                                                socketInit.getTimer().cancel();
                                            }
                                        }, 3000);
                                    } else {
                                        callStatus = socketInit.getCallStatus();
                                        callStatus.onDecline();
                                        socketInit.getTimer().cancel();
                                    }
                                }
                            } catch (Exception e) {
                                //e.printStackTrace();
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    .on(getApplication().getString(R.string.smiss), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                JSONObject status = new JSONObject();
                                status.put("status", true);
                                Ack ack = (Ack) args[args.length - 1];
                                ack.call(status);
                                callStatus = socketInit.getCallStatus();
                                callStatus.onMiss();
                                socketInit.getTimer().cancel();
                            } catch (Exception e) {
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    .on("message", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                JSONObject status = new JSONObject();
                                status.put("status", true);
                                Ack ack = (Ack) args[args.length - 1];
                                ack.call(status);
                                JSONObject data = (JSONObject) args[0];
                                JSONObject messageObj = data.has("message") ? data.getJSONObject("message") : null;
                                String encryptedMessage = messageObj.has("body") ? messageObj.getString("body") : null;
                                String conversationId = messageObj.has("convoId") ? messageObj.getString("convoId") : null;
                                String senderCuid = messageObj.has("senderCuid") ? messageObj.getString("senderCuid") : null;
                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                String accoutnId = sharedPref.getString("patch_accountId", null);

                                CryptLib cryptLib = new CryptLib();
                                if (encryptedMessage != null) {
                                    String decryptedString = cryptLib.decryptCipherTextWithRandomIV(encryptedMessage, accoutnId);
                                    Intent intent = new Intent("message");
                                    intent.putExtra("conversationId", conversationId);
                                    intent.putExtra("senderCuid", senderCuid);
                                    intent.putExtra("message", decryptedString);
                                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                                }
                            } catch (Exception e) {
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    .on(getApplication().getString(R.string.sincoming_call), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                FcmUtil.getInstance(getApplicationContext()).resetExpiredCallId(null); //this is important to reset the expiredCallId on every new call

                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                                SocketInit.getInstance().setAppContext(getApplicationContext());
                                JSONObject status = new JSONObject();
                                status.put("status", true);
                                Ack ack = (Ack) args[args.length - 1];
                                ack.call(status);
                                JSONObject incomingdata = (JSONObject) args[0];
                                String fromId = incomingdata.getJSONObject("from").getString(getString(R.string.id));
                                String accountId = sharedPref.getString("patch_accountId", "");
                                String call = incomingdata.getString("call");
                                String sid = incomingdata.has("sid")? incomingdata.getString("sid"):"";
                                incomingdata.put(getApplication().getString(R.string.accountId), accountId);
                                incomingdata.put(getApplication().getString(R.string.apikey), apikey);
                                incomingdata.put(getApplication().getString(R.string.phone), phone);
                                incomingdata.put(getApplication().getString(R.string.cc), cc);
                                incomingdata.put(getApplication().getString(R.string.token), jwt);
                                incomingdata.put("cuid", cuid);
                                incomingdata.put(getApplication().getString(R.string.callType), "incoming");
                                if (cuid.length() > 0) {
                                    incomingdata.put("initiatorData", cuid);
                                } else {
                                    incomingdata.put("initiatorData", cc + phone);
                                }

                                if(getApplicationContext() != null){
                                    PatchCommonUtil.getInstance().removeNeverAskAgain(getApplicationContext());
                                    Boolean isNeverAskAgain = sharedPref.getBoolean("patch_never_ask_again", false);
                                    if(isNeverAskAgain){
                                        JSONObject data = new JSONObject();
                                        data.put("responseSid", fromId + "_" + accountId);
                                        data.put("callId", call);
                                        data.put(getString(R.string.sid), sid);
                                        if(isNeverAskAgain){
                                            data.put("reason", getString(R.string.microphone_permission_not_granted));
                                            data.put("reasonCode", 401);
                                        }
                                        if (SocketIOManager.getSocket() != null) {
                                            SocketIOManager.getSocket().emit(getApplication().getString(R.string.sdecline), data, new Ack() {
                                                @Override
                                                public void call(Object... args) {
                                                }
                                            });
                                        }
                                        return;
                                    }
                                }

                                //this user is busy on other voip or pstn call so decline with reasonCode-503
                                if (SocketInit.getInstance().isClientbusyOnVoIP() || SocketInit.getInstance().isClientbusyOnPstn()) {
                                    incomingdata.put("responseSid", fromId + "_" + accountId);
                                    incomingdata.put("callId", call);
                                    incomingdata.put(getString(R.string.sid), sid);
                                    incomingdata.put("reason", getString(R.string.client_busy));
                                    incomingdata.put("reasonCode", 503);
                                    if (SocketIOManager.getSocket() != null) {
                                        SocketIOManager.getSocket().emit(getApplication().getString(R.string.sdecline), incomingdata, new Ack() {
                                            @Override
                                            public void call(Object... args) {

                                            }
                                        });
                                    }
                                } else {
                                    SocketInit.getInstance().setClientbusyOnVoIP(true);
                                    SocketInit.getInstance().setCallDetails(incomingdata);
                                    SocketInit.getInstance().setSid(sid);

                                    PatchCommonUtil.getInstance().lightUpScreen(getApplicationContext());
                                    JSONObject incomingNotificationData = new JSONObject();
                                    incomingNotificationData.put(getApplicationContext().getString(R.string.callDetails), incomingdata.toString());
                                    //incomingNotificationData.put(getApplicationContext().getString(R.string.screen), getApplicationContext().getString(R.string.incoming));
                                    incomingNotificationData.put(getApplicationContext().getString(R.string.sid), sid);
                                    incomingNotificationData.put("context", incomingdata.getString("context"));

                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                        NotificationHandler.CallNotificationHandler.getInstance(getApplicationContext()).
                                                showCallNotification(incomingNotificationData,
                                                        NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                                        PatchCommonUtil.getInstance().startAudio(getApplicationContext());
                                        PatchCommonUtil.getInstance().startTimer(getApplicationContext(), incomingdata);
                                    }else {
                                        Intent incomingCallNotificationIntent = new Intent(context, CallNotificationService.class);
                                        incomingCallNotificationIntent.putExtra("incomingNotificationData", incomingNotificationData.toString());
                                        PatchCommonUtil.getInstance().startstickyIncomingNotificationService(getApplicationContext(), incomingCallNotificationIntent);
                                        PatchCommonUtil.getInstance().startTimer(getApplicationContext(), incomingdata);
                                    }
                                    /*NotificationHandler.CallNotificationHandler.getInstance(getApplicationContext()).
                                            showCallNotification(incomingNotificationData,
                                                    NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);

                                    AppUtil.getInstance().startAudio(getApplicationContext());
                                    AppUtil.getInstance().startTimer(getApplicationContext(), incomingdata);*/

                                    try {
                                        PatchCommonUtil.getInstance().sendBroadcast(getApplicationContext(), Constants.ACTION_INCOMING_CALL);
                                    }catch (Exception e){

                                    }
                                    //Log.d("patchsharma", "incoming call " + args[0]);
                                    socketInit.setCallId(call);
                                    socketInit.setRecording(true);
                                    Intent i = new Intent(getApplicationContext(), PatchCallingActivity.class);
                                    i.putExtra(getApplicationContext().getString(R.string.callDetails), incomingdata.toString());
                                    i.putExtra(getApplicationContext().getString(R.string.screen), getApplicationContext().getString(R.string.incoming));
                                    i.putExtra(getApplicationContext().getString(R.string.sid), sid);
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                    startActivity(i);

                                }
                            } catch (Exception e) {
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    .on(getApplication().getString(R.string.hold_unhold), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            //blink "call is put on hold" and lock hold button , vibrate for a time
                            try {
                                JSONObject jsonObject = (JSONObject) args[0];
                                if (jsonObject.has("hold")) {
                                    PatchCallscreenFragment.putOnHold(jsonObject.getBoolean("hold"));
                                }
                            } catch (Exception e) {
                            }
                        }
                    })
                    .on(getApplication().getString(R.string.snotificationRespsonse), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {

                            try {
                                if (socketInit != null) {
                                    Context context = getApplicationContext();

                                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                    Boolean patchNotificaion = sharedPref.getBoolean(context.getString(R.string.patch_notification), false);
                                    if (patchNotificaion == true) {

                                        JSONObject status = new JSONObject();
                                        status.put("status", true);
                                        Ack ack = (Ack) args[args.length - 1];
                                        ack.call(status);
                                        final JSONObject data = (JSONObject) args[0];

                                        String sentimentListenerHost = sharedPref.getString(context.getString(R.string.patch_notificationlistener), null);
                                        /*socketInit.setSentientReciever(sentimentListenerHost, new OnSetSentimentReceiver() {
                                            @Override
                                            public void onSetSentimentReceiver(SentimentReciever sentimentRecieverOp) {
                                                if (socketInit.getSentimentReceiver() != null) {
                                                    if (data.has("btnSentiment")) {
                                                        try {
                                                            if (getApplicationContext() != null)
                                                                sentimentRecieverOp.onSentimentRecieved(getApplicationContext(), data.getBoolean("btnSentiment"), data.getString("notificationId"));
                                                        } catch (JSONException e) {
                                                            if (getApplicationContext() != null) {
                                                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        });*/
                                        socketInit.setNotificationListenerHost(sentimentListenerHost, new OnSetNotificationListener() {
                                            @Override
                                            public void onSetNotificationListener(PatchNotificationListener patchNotificationListener) {
                                                if (socketInit.getNotificationListener() != null) {
                                                    try {
                                                        if (data.has("btnSentiment") && data.get("btnSentiment") instanceof Integer) {
                                                            if (getApplicationContext() != null)
                                                                patchNotificationListener.onSentimentReceived(getApplicationContext(),
                                                                        data.getInt("btnSentiment"), data.getString("notificationId"));
                                                        }
                                                    } catch (Exception e) {
                                                        if (getApplicationContext() != null) {
                                                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                                        }
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }

                            } catch (Exception e) {
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    .on(getApplication().getString(R.string.snotification), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            try {
                                //Check for if ui is required or not
                                if (socketInit != null) {
                                    Context context = getApplicationContext();
                                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                    Boolean patchNotificaion = sharedPref.getBoolean(context.getString(R.string.patch_notification), false);
                                    if (patchNotificaion == true) {
                                        JSONObject status = new JSONObject();
                                        status.put("status", true);
                                        Ack ack = (Ack) args[args.length - 1];
                                        ack.call(status);

                                        final JSONObject data = (JSONObject) args[0];
                                        if (!data.has("payload")) {
                                            return;
                                        }
                                        JSONObject payload = data.getJSONObject("payload");
                                        if (!payload.has("title") || !payload.has("body") || !payload.has("picture"))
                                            return;
                                        final String title = payload.getString("title");
                                        final String body = payload.getString("body");
                                        final String picture = payload.getString("picture");
                                        final String notificationId = data.has("notificationId") ? data.getString("notificationId") : null;
                                        String senderCuid = data.has("senderCuid") ? data.getString("senderCuid") : null;
                                        String sentAt = data.has("sentAt") ? data.getString("sentAt") : PatchCommonUtil.getInstance().getCurrentIsoDateTime();

                                        final JSONObject notificationDataToHandler = new JSONObject();
                                        final JSONObject notificationDataToReceiver = new JSONObject();
                                        notificationDataToHandler.put("title", title);
                                        notificationDataToHandler.put("body", body);
                                        notificationDataToHandler.put("picture", picture);
                                        notificationDataToHandler.put("sentAt", sentAt);

                                        notificationDataToReceiver.put("title", title);
                                        notificationDataToReceiver.put("body", body);
                                        notificationDataToReceiver.put("picture", picture);

                                        if (notificationId != null) {
                                            notificationDataToHandler.put("notificationId", notificationId);
                                        }
                                        if (senderCuid != null) {
                                            notificationDataToHandler.put("senderCuid", senderCuid);
                                        }
                                        if (data.has("buttons")) {
                                            if (data.get("buttons") instanceof JSONObject) {
                                                JSONObject btnObjects = data.getJSONObject("buttons");
                                                if (btnObjects.has("buttonPositive") && btnObjects.has("buttonNegative")) {
                                                    notificationDataToHandler.put("buttons", btnObjects);
                                                    notificationDataToReceiver.put("buttons", btnObjects);
                                                }
                                            }
                                        }

                                        Boolean patchNotificaionUI = sharedPref.getBoolean(context.getString(R.string.patch_notificationUI), false);
                                        String notificaionReciever = sharedPref.getString(context.getString(R.string.patch_notificationlistener), null);
                                        String sentimentReciever = sharedPref.getString(context.getString(R.string.patch_sentimentReciever), null);
                                        socketInit.setEnablePatchNotification(patchNotificaion);
                                        socketInit.setEnableNotificationUI(patchNotificaionUI);
                                        /*socketInit.setSentientReciever(sentimentReciever, new OnSetSentimentReceiver() {
                                            @Override
                                            public void onSetSentimentReceiver(SentimentReciever sentimentReciever) {

                                            }
                                        });*/

                                        socketInit.setNotificationListenerHost(notificaionReciever, new OnSetNotificationListener() {
                                            @Override
                                            public void onSetNotificationListener(PatchNotificationListener patchNotificationListenerOp) {
                                                if (socketInit.isPatchNotificationEnable() != null && socketInit.isPatchNotificationEnable() == true) {
                                                    if (socketInit.isNotificationUIEnable() != null && patchNotificationListenerOp != null) {
                                                        //both notificationUiEnable or notificationReciever are passed , so call callback event
                                                        if (getApplicationContext() != null) {
                                                            patchNotificationListenerOp.onNotificationReceived(getApplicationContext(), notificationDataToReceiver);

                                                        }

                                                        //if notificationUI is eneable then only show local notification
                                                        if (socketInit.isNotificationUIEnable() == true) {
                                                            if (getApplicationContext() != null)
                                                                NotificationHandler.getInstance(getApplicationContext()).showNotification(getApplicationContext(), notificationDataToHandler);
                                                        }
                                                    } else {
                                                        //ignoring recieved notification
                                                        //if either notificationUiEnable or notificationReciever is not passed then return
                                                        return;
                                                    }
                                                }
                                            }
                                        });
                                    }
                                }

                            } catch (Exception e) {
                                if (getApplicationContext() != null) {
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                }
                            }
                        }
                    })
                    /*.on(getApplication().getString(R.string.sconnection_timeout), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.d("Patch", "sconnection_timeout");
                        }
                    })
                    .on(getApplication().getString(R.string.sconnection_timeout), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.d("Patch", "sconnection_timeout");
                        }
                    })
                    .on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.d("Patch", "EVENT_RECONNECT_ATTEMPT");
                        }
                    })
                    .on(getApplication().getString(R.string.sreconnect_failed), new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.d("Patch", "sreconnect_failed");
                        }
                    })
                    .on(Socket.EVENT_RECONNECT_ERROR, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.d("Patch", "EVENT_RECONNECT_ERROR");
                        }
                    })
                    .on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.d("Patch", "EVENT_RECONNECTING");
                        }
                    })
                    .on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.d("Patch", "EVENT_RECONNECT");
                        }
                    })*/;
            ;
        }
        try {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!socket.connected()) {
                        socket.connect();
                    }
                }
            }, 1400);
        } catch (Exception e) {

        }
    }
}
