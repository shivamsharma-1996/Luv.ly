package com.patch.patchcalling.services;

import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.patch.patchcalling.Constants;
import com.patch.patchcalling.R;
import com.patch.patchcalling.activity.PatchCallingActivity;
import com.patch.patchcalling.fcm.FcmCacheManger;
import com.patch.patchcalling.fcm.FcmUtil;
import com.patch.patchcalling.fragments.PatchCallscreenFragment;
import com.patch.patchcalling.fragments.PatchIncomingFragment;
import com.patch.patchcalling.interfaces.ApiInterface;
import com.patch.patchcalling.interfaces.CallStatus;
import com.patch.patchcalling.interfaces.IosApfInterface;
import com.patch.patchcalling.interfaces.PatchNotificationListener;
import com.patch.patchcalling.interfaces.NotificationResponse;
import com.patch.patchcalling.interfaces.OnSetNotificationListener;
import com.patch.patchcalling.interfaces.OutgoingCallResponse;
import com.patch.patchcalling.interfaces.OutgoingMessageResponse;
import com.patch.patchcalling.javaclasses.CryptLib;
import com.patch.patchcalling.javaclasses.MessageInit;
import com.patch.patchcalling.javaclasses.PatchSDK;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.models.IosAPF;
import com.patch.patchcalling.models.PendingSentiment;
import com.patch.patchcalling.retrofitresponse.resolvesentimentresponse.ResolvedSentimentResponse;
import com.patch.patchcalling.utils.AckWithTimeOut;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.CustomHandler;
import com.patch.patchcalling.utils.JwtUtil;
import com.patch.patchcalling.utils.NotificationHandler;
import com.patch.patchcalling.utils.PatchLogger;
import com.patch.patchcalling.utils.PendingSentimentHandler;
import com.patch.patchcalling.utils.SocketIOManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.patch.patchcalling.PatchResponseCodes.NotificationCallback.OnFailure.ERR_NOTIFICATION_FAILED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CONTACT_NOT_REACHABLE;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CUID_ALREADY_CONNECTED_ELSEWHERE;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_INVALID_CALL_TOKEN;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_MICROPHONE_PERMISSION_NOT_GRANTED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_NUMBER_NOT_EXISTS;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_WHILE_MAKING_VOIP_CALL;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.FAILURE_INTERNET_LOST_AT_RECEIVER_END;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnSuccess.CALL_PLACED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingMessageCallback.OnFailure.ERR_MESSAGE_FAILED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingMessageCallback.OnFailure.ERR_WRONG_THREAD_EXCEPTION_TO_UPDATE_UI;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingMessageCallback.OnResponse.SUCCESS_MESSAGING_SENT;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnSuccess.SUCCESS_PATCH_SDK_INITIALIZED;
import static com.patch.patchcalling.PatchResponseCodes.SUCCESS_SDK_CONNECTED;

/**
 * Created by sanyamjain on 18/12/18.
 */

public class JobSchedulerSocketService extends JobService implements ConnectivityReceiver.ConnectivityReceiverListener {
    private Socket socket;
    private String cc, phone, accountId, apikey, cuid;
    private static String jwt = "", sna = "";
    private static SocketInit socketInit;
    static Context context;
    CallStatus callStatus;
    CountDownTimer outgoingTimer;
    Handler mhandler;
    private static Boolean isUnAuthorized = false;
    private static ApiInterface apiService = null;
    CustomHandler customHandler = CustomHandler.getInstance();
    private ConnectivityReceiver mConnectivityReceiver;
    private static JobSchedulerSocketService instance;
    private static int transportDisconnectTimer = 0;
    private static CountDownTimer transportErrorCountDownTimer;
    private static Handler countDownHandler;
    private static Boolean isDisconnectDueToTransportError = false, canRetryAfterUnAuthorizedDueToTransportError = false;  //throws disconnect reason in event handler when network not avaialble
    private final int SIGSOCK_PING_TIMEOUT = 10000;
    private Timer reconnectScheduler = new Timer();
    private int reconnectingCount = 0;

    public JobSchedulerSocketService() {
    }

    public static JobSchedulerSocketService getInstance(Context context) {
        if (instance == null) {
            instance = new JobSchedulerSocketService();
            instance.context = context;
            apiService = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                    create(ApiInterface.class);
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mConnectivityReceiver = new ConnectivityReceiver(this);

            try {
                registerReceiver(mConnectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

                countDownHandler = new Handler(Looper.getMainLooper());

                //preloading brand__logo
                if (getApplicationContext() != null) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String logoUrl = sharedPref.getString("patch_logo", null);
                    PatchCommonUtil.getInstance().preloadBrandLogo(getApplicationContext(), logoUrl);
                }
            } catch (Exception e) {
                if (getApplicationContext() != null) {
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //Log.i("Patch", "Service destroyed"  + counter++);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    /**
     * fetch the jwt token and initialize and start the sigsock.
     */
    public void startSigsock() {
        try {
        /*    if(SocketIOManager.getSocket()!=null && !SocketIOManager.getIsNewInstance()){
                SocketIOManager.getSocket().connect();
                Log.d("patchsharma", "startSigsock me se jobs");
                return;
            }*/
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
            phone = sharedPref.getString("patch_userPhone", "");
            cc = sharedPref.getString("patch_userCC", "");
            accountId = sharedPref.getString("patch_accountId", null);
            apikey = sharedPref.getString("patch_apikey", null);
            cuid = sharedPref.getString("patch_cuid", "");
            jwt = sharedPref.getString(this.getApplicationContext().getResources().getString(R.string.patch_token), "");
            sna = sharedPref.getString(this.getApplicationContext().getResources().getString(R.string.sna), "");

            try {
                if (jwt != null && jwt.isEmpty()) {
                    PatchCommonUtil.getInstance().removeSession(this.getApplicationContext());
                    JwtUtil.getInstance(getApplicationContext()).stopServiceAndSigsock(this.getApplicationContext());
                    customHandler.sendInitAnnotations(SocketInit.getInstance().getPatchInitResponse(), CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                    return;
                }
            } catch (Exception e) {
                if (jwt != null && jwt.isEmpty()) {
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
            //Log.d("sharma", "jwt of service is : "  + jwt);

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
//            getToken(cc, phone, accountId, cuid, new JwtTokenResponse() {
//                @Override
//                public void onSuccess(String response) {
//                    IO.Options opts = new IO.Options();
//                    opts.transports = new String[]{getString(R.string.websocket)};
//                    opts.forceNew = true;
//                    opts.reconnection = true;
//                    opts.reconnectionAttempts = 15;
//                    opts.reconnectionDelay = 1500;
//                    opts.reconnectionDelayMax = 6000;
//                    opts.timeout = 300000;
//                    opts.query = getApplicationContext().getString(R.string.jwt) + jwt;
//                    String url = "https://" + sna;
//
//                    try {
//                        socket = SocketIOManager.getSocket(opts, url);
//                        SocketIOManager.setSocket(socket);
//                        setUpEventHandlers();pa
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    } catch (URISyntaxException e) {
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void onFailure(String failure) {
//                }
//            });
        } catch (Exception e) {
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        return true;
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
                                                            } catch (Exception e) {

                                                            }
                                                        }
                                                    };
                                                    transportErrorCountDownTimer.start();
                                                } catch (Exception e) {

                                                }
                                            }
                                        });
                                        break;
                                    case "io server disconnect":
                                        //call /jwt/verify wala flow
                                        try {
                                            if (!isUnAuthorized) {
                                                //Log.d("sharma", "verifytoken in server io");
                                                //JwtUtil.getInstance().verifyToken(getApplicationContext(), false,SocketInit.getInstance().getPatchInitResponse());
                                                //disconnecting socket, logging out, stopping service
                                                if (socket != null) {
                                                    socket.close();
                                                    socket.off();
                                                }
                                                PatchSDK.getInstance().logout(context);
                                                JobScheduler jobScheduler = (JobScheduler) getApplicationContext().
                                                        getSystemService(Context.JOB_SCHEDULER_SERVICE);
                                                jobScheduler.cancel(10);
                                                reconnectScheduler.cancel();
                                            }
                                        } catch (Exception e) {

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
//                                Log.d("patchsharma", "sauthenticated in jobs");

                                try {
                                    JSONObject status = new JSONObject();
                                    status.put("status", true);
                                    Object ackObj = args[args.length - 1];
                                    if (ackObj instanceof Ack) {
                                        Ack ack = (Ack) ackObj;
                                        ack.call(status);
                                    }
                                } catch (Exception e) {

                                }

                                //;

                                PatchSDK.sdkReady = true;
                                //Log.d("sharma",  "sauthenticated");

                                isDisconnectDueToTransportError = false;
                                isUnAuthorized = false;
                                SocketIOManager.setIsUnAuthorized(false);
                                socketInit = SocketInit.getInstance();
                                socketInit.setSocket(socket);

                                //Log.d("patchsharma", "jobs " +SocketIOManager.isSocketConnected());
                                if (FcmCacheManger.getInstance().isAnswered()) {
                                    if (PatchIncomingFragment.getInstance() != null) {
                                        PatchIncomingFragment.getInstance().callAccepted();
                                    }
                                } else if (FcmCacheManger.getInstance().isRejected()) {
                                    /*if(PatchIncomingFragment.getInstance()!=null){

                                    }*/
                                    //jan bujhkar declineReason set to null kia hai. cz it will be the rightmost decision.
                                    //Should I apply isBusy() condition here so can pass in declineReason, is this case possible in viaFcm call???
                                    //PatchIncomingFragment.fcmCallDecline(getApplicationContext());
                                    PatchCommonUtil.getInstance().fcmCallDecline(getApplicationContext());
                                } else if (FcmCacheManger.getInstance().getShouldMarkNotificationStatusDelivered()) {
                                    Log.d("PatchNotification", "getShouldMarkNotificationStatusDelivered");
                                    PatchCommonUtil.getInstance().markNotificationDelivered(getApplicationContext());
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
                                //Log.d("patchsharma",  "sunauthorized");

                                JSONObject response = (JSONObject) args[0];

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
                                                    JobScheduler jobScheduler = (JobScheduler) getApplicationContext().
                                                            getSystemService(Context.JOB_SCHEDULER_SERVICE);
                                                    jobScheduler.cancel(10);
                                                    reconnectScheduler.cancel();
                                                    return;
                                                }
                                                reconnectingCount += 1;
                                            } catch (Exception e) {

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
                                    callStatus = socketInit.getCallStatus();
                                    callStatus.onDecline();
                                    socketInit.getTimer().cancel();
                                    //following code is the retrying flow to PSTN
                                   /* if (calleeCC.length() > 0 && calleePhone.length() > 0) {
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
                                        PatchCommonUtil.getInstance().delayedHandler(2500,
                                                () -> {
                                                    callStatus = socketInit.getCallStatus();
                                                    if(callStatus!=null){
                                                        callStatus.onDecline();
                                                    }
                                                    if(socketInit.getTimer()!=null){
                                                        socketInit.getTimer().cancel();
                                                    }
                                        });
                                    } else {
                                        callStatus = socketInit.getCallStatus();
                                        if(callStatus!=null){
                                            callStatus.onDecline();
                                        }
                                        if(socketInit.getTimer()!=null){
                                            socketInit.getTimer().cancel();
                                        }
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

                                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (PatchCommonUtil.getInstance().getMissedCallInitiatorActionsPref(context) != null) {
                                                Log.d("patchsharma", "callDetails : " + SocketInit.getInstance().getToCuid() + SocketInit.getInstance().getFromCuid());

                                                if (SocketInit.getInstance().getCallContext() != null)
                                                    NotificationHandler.CallNotificationHandler.getInstance(getApplicationContext().getApplicationContext()).showCallNotification(
                                                            new JSONObject().
                                                                    put("context", SocketInit.getInstance().getCallContext()).
                                                            put("toCuid", SocketInit.getInstance().getToCuid()!=null? SocketInit.getInstance().getToCuid():null).
                                                            put("fromCuid", SocketInit.getInstance().getFromCuid()!=null? SocketInit.getInstance().getFromCuid():null),
                                                            NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);

                                            }
                                        } catch (Exception e) {

                                        }
                                    }
                                }, 1500);
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
                                //FcmCacheManger.getInstance().resetFcmCache();
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
                                String sid = incomingdata.has("sid") ? incomingdata.getString("sid") : "";
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

                                if (getApplicationContext() != null) {
                                    PatchCommonUtil.getInstance().removeNeverAskAgain(getApplicationContext());
                                    Boolean isNeverAskAgain = sharedPref.getBoolean("patch_never_ask_again", false);
                                    if (isNeverAskAgain) {
                                        JSONObject data = new JSONObject();
                                        data.put("responseSid", fromId + "_" + accountId);
                                        data.put("callId", call);
                                        data.put(getString(R.string.sid), sid);
                                        if (isNeverAskAgain) {
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
                                    } else {
                                        Intent incomingCallNotificationIntent;
                                        if (context != null) {
                                            incomingCallNotificationIntent = new Intent(context, CallNotificationService.class);
                                        } else {
                                            incomingCallNotificationIntent = new Intent(getApplicationContext(), CallNotificationService.class);
                                        }
                                        incomingCallNotificationIntent.putExtra("incomingNotificationData", incomingNotificationData.toString());
                                        PatchCommonUtil.getInstance().startstickyIncomingNotificationService(getApplicationContext(), incomingCallNotificationIntent);
                                        PatchCommonUtil.getInstance().startTimer(getApplicationContext(), incomingdata);
                                    }

                                    try {
                                        PatchCommonUtil.getInstance().sendBroadcast(getApplicationContext(), Constants.ACTION_INCOMING_CALL);
                                    } catch (Exception e) {

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
                                        /*socketInit.setSentientReciever(sentimentReciever, new OnSetSentimentReceiver() {
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
                                JSONObject status = new JSONObject();
                                status.put("status", true);
                                Ack ack = (Ack) args[args.length - 1];
                                ack.call(status);
                                final JSONObject data = (JSONObject) args[0];
                                Log.d("PatchNotification", " data : " + data);
                                generateNotification(getApplicationContext(), data);
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

    public static void generateNotification(final Context context, JSONObject data) {
        try {
            socketInit = SocketInit.getInstance();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            Boolean patchNotificaion = sharedPref.getBoolean(context.getString(R.string.patch_notification), false);
            if (patchNotificaion == true) {

                if (!data.has("payload")) {
                    return;
                }
                JSONObject payload = data.getJSONObject("payload");
                if (!payload.has("title") || !payload.has("body") /*|| !payload.has("picture")*/)
                    return;
                final String title = payload.getString("title");
                final String body = payload.getString("body");
                final String picture = payload.has("picture") ? payload.getString("picture") : null;
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

                        /*if (btnObjects.has("1") && btnObjects.has("2")) {
                            notificationDataToHandler.put("buttons", btnObjects);
                            notificationDataToReceiver.put("buttons", btnObjects);
                        }*/
                        notificationDataToHandler.put("buttons", btnObjects);
                        notificationDataToReceiver.put("buttons", btnObjects);
                    }
                }

                Boolean patchNotificaionUI = sharedPref.getBoolean(context.getString(R.string.patch_notificationUI), false);
                String notificaionReciever = sharedPref.getString(context.getString(R.string.patch_notificationlistener), null);
                String sentimentReciever = sharedPref.getString(context.getString(R.string.patch_sentimentReciever), null);
                socketInit.setEnablePatchNotification(patchNotificaion);
                socketInit.setEnableNotificationUI(patchNotificaionUI);
               /* socketInit.setSentientReciever(sentimentReciever, new OnSetSentimentReceiver() {
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
                                if (context != null) {
                                    patchNotificationListenerOp.onNotificationReceived(context, notificationDataToReceiver);
                                }

                                //if notificationUI is eneable then only show local notification
                                if (socketInit.isNotificationUIEnable() == true) {
                                    if (context != null)
                                        NotificationHandler.getInstance(context).showNotification(context, notificationDataToHandler);

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        try {
            unregisterReceiver(mConnectivityReceiver);
        } catch (Exception e) {
            if (getApplicationContext() != null) {
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            }
        }
        return true;
    }


    /**
     * called when the user's network connectivity is changed.
     *
     * @param isConnected:- true if coneected else false.
     */
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        if (isConnected) {
            try {
/*                if(SocketIOManager.getSocket()!=null && !SocketIOManager.getIsUnAuthorized() && !SocketIOManager.getSocket().connected()){
                    SocketIOManager.getSocket().connect();
                    Log.d("patchsharma", "onNetworkConnectionChanged me se return jobs");
                    return;
                }else {*/
                canRetryAfterUnAuthorizedDueToTransportError = false;
                updatePendingSentiment();                   // sending pending sentiment in a PATCH request to update sentiment against notifications
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
                //  }
            } catch (Exception e) {
                if (getApplicationContext() != null) {
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                }
            }

        } else {
            Log.d("Patch", "Sorry! Not connected to internet");
        }
    }

    /**
     * emit make call on the sigsock to make an outgoing call.
     *
     * @param caller:-               cc of the user whom user is trying to call.
     * @param callee:-               phone number of the user whom user is trying to make a call.
     *                               // * @param calleeCuid:- cuid of the user to whom user is trying to make a call.
     * @param callOptions:-          jsonObject containing call options passed at the time of making a call.
     * @param outgoingCallResponse:- callback statuses to be returned to the user on making a call.
     * @throws JSONException
     */
    public void makeCall(final JSONObject caller, final JSONObject callee, final JSONObject callOptions, final OutgoingCallResponse outgoingCallResponse) throws JSONException {
        String[] PERMISSIONS = {
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.RECORD_AUDIO
        };
        try {
            if (PatchCommonUtil.getInstance().hasPermissions(context, PERMISSIONS)) {
                final SocketInit socketInit = SocketInit.getInstance();

                final JSONObject jsonObject = new JSONObject();
                jsonObject.put(context.getString(R.string.from), caller);
                jsonObject.put(context.getString(R.string.to), callee);
                if (SocketInit.getInstance().getCli() != null) {
                    jsonObject.put("cli", SocketInit.getInstance().getCli());
                }
                PatchSDK.sdkReady = true;
                socketInit.getSocket().emit(context.getString(R.string.spure_pstn), jsonObject, new Ack() {
                    @Override
                    public void call(Object... args) {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            if (data.getBoolean("status")) {
                                //outgoingCallResponse.callStatus(CALL_PLACED);
                                //outgoingCallResponse.onSuccess(SUCCESS_CALL_PLACED);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_SUCCESS, CALL_PLACED);
                                PatchSDK.sdkReady = true;
                            } else {
                                PatchSDK.sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_CONTACT_NOT_REACHABLE);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CONTACT_NOT_REACHABLE);
                            }
                        } catch (Exception e) {
                            PatchSDK.sdkReady = true;
                            //outgoingCallResponse.onFailure(ERR_CONTACT_NOT_REACHABLE);
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CONTACT_NOT_REACHABLE);
                        }
                    }
                });
            } else {
                PatchSDK.sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_MICROPHONE_PERMISSION_NOT_GRANTED);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MICROPHONE_PERMISSION_NOT_GRANTED);
            }
        } catch (Exception e) {
            PatchSDK.sdkReady = true;
            if (getApplicationContext() != null) {
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            }
        }
    }

    public void emitHoldOnSigSock(Boolean isHold, String otherCuid) {
        try {
            if (SocketIOManager.getSocket() != null && SocketIOManager.getSocket().connected()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("hold", isHold);
                jsonObject.put("cuid", otherCuid);
                SocketIOManager.getSocket().emit("hold-unhold", jsonObject, new Ack() {
                    @Override
                    public void call(Object... args) {
                        try {
                            JSONObject holdCallBack = (JSONObject) args[0];
                            boolean status = holdCallBack.getBoolean("status");
                        } catch (Exception e) {
                            try {

                            } catch (Exception e1) {

                            }
                        }
                    }
                });
            }
        } catch (Exception e) {

        }
    }

    public void emitDebug(String message, String callId) {
        try {
            if (SocketIOManager.getSocket() != null && SocketIOManager.getSocket().connected()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("hold", message);
                jsonObject.put("cuid", callId);
                SocketIOManager.getSocket().emit("debug", jsonObject, new Ack() {
                    @Override
                    public void call(Object... args) {
                        try {
                            //JSONObject holdCallBack = (JSONObject) args[0];
                            //boolean status = holdCallBack.getBoolean("status");
                            Log.d("patchsharma", "status of debugger" + args[0]);
                        } catch (Exception e) {
                            try {

                            } catch (Exception e1) {

                            }
                        }
                    }
                });
            }
        } catch (Exception e) {

        }
    }


    /**
     * emit make call on the sigsock to make an outgoing call.
     *
     * @param calleeCC:-             cc of the user whom user is trying to call.
     * @param calleePhone:-          phone number of the user whom user is trying to make a call.
     * @param calleeCuid:-           cuid of the user to whom user is trying to make a call.
     * @param callContext:-          context of the call.
     * @param callOptions:-          jsonObject containing call options passed at the time of making a call.
     * @param outgoingCallResponse:- callback statuses to be returned to the user on making a call.
     * @throws JSONException
     */
    static String calleeCC, calleePhone, calleeCuid, callContext;
    static JSONObject callOptions;
    static OutgoingCallResponse outgoingCallResponse;

    public void makeCall(final String calleeCC, final String calleePhone, final String calleeCuid, final String callContext, final JSONObject callOptions, final OutgoingCallResponse outgoingCallResponse, final String unAuthorizedFlag) throws JSONException {
        String[] PERMISSIONS = {
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.RECORD_AUDIO
        };
        try {
            this.calleeCC = calleeCC;
            this.calleePhone = calleePhone;
            this.calleeCuid = calleeCuid;
            this.callContext = callContext;
            this.callOptions = callOptions;
            this.outgoingCallResponse = outgoingCallResponse;
            if (PatchCommonUtil.getInstance().hasPermissions(context, PERMISSIONS)) {

                if (SocketIOManager.getIsUnAuthorized()) {
                    PatchSDK.sdkReady = true;
                    //outgoingCallResponse.onFailure(ERR_SOMETHING_WENT_WRONG);
                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CUID_ALREADY_CONNECTED_ELSEWHERE);
                    return;
                }
                final SocketInit socketInit = SocketInit.getInstance();

                JSONArray tagsarray = null;
                if (callOptions.has("tags") && callOptions.get("tags") instanceof JSONArray) {
                    tagsarray = callOptions.getJSONArray("tags");
                }
                final Boolean isPstn = callOptions.getBoolean("pstn");
                if (callOptions.getBoolean("recording")) {
                    socketInit.setRecording(true);
                } else {
                    socketInit.setRecording(false);
                }

                final JSONObject jsonObject = new JSONObject();
                jsonObject.put(context.getString(R.string.cc), calleeCC);
                jsonObject.put(context.getString(R.string.phone), calleePhone);
                jsonObject.put(context.getString(R.string.callContext), callContext);
                jsonObject.put("cuid", calleeCuid);
                jsonObject.put(context.getString(R.string.pstn), callOptions.getBoolean("pstn"));
                jsonObject.put(context.getString(R.string.recording), callOptions.getBoolean("recording"));
                jsonObject.put(context.getString(R.string.webhook), callOptions.getString("webhook"));
                jsonObject.put(context.getString(R.string.var1), callOptions.getString("var1"));
                jsonObject.put(context.getString(R.string.var2), callOptions.getString("var2"));
                jsonObject.put(context.getString(R.string.var3), callOptions.getString("var3"));
                jsonObject.put(context.getString(R.string.var4), callOptions.getString("var4"));
                jsonObject.put(context.getString(R.string.var5), callOptions.getString("var5"));
                jsonObject.put(context.getString(R.string.tags), tagsarray);


                if (callOptions.has("callToken") && callOptions.getString("callToken") != null
                        && callOptions.getString("callToken") != "") {
                    jsonObject.put("callToken", callOptions.getString("callToken"));
                }
                if (callOptions.has("autoFallback")) {
                    if (callOptions.getBoolean("autoFallback")) {
                        jsonObject.put("apf", true);
                    }
                }

                //to prevent it remaining false when caller is trying autofallback and callee is just offline so server will not send make_call response.
                /*new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        PatchSDK.isGoodToGo = true;
                    }
                }, 2500);*/

                socketInit.getSocket().emit(context.getString(R.string.smakecall), jsonObject, new AckWithTimeOut(10000) {
                    @Override
                    public void call(Object... args) {
                        try {
                            if (args != null) {
                                if (args[0].toString().equalsIgnoreCase("No Ack")) {
                                    Log.d("patchsharma1234", "AckWithTimeOut : " + args[0].toString());
                                    PatchSDK.sdkReady = true;
                                    //outgoingCallResponse.onFailure(ERR_INVALID_CALL_TOKEN);
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, FAILURE_INTERNET_LOST_AT_RECEIVER_END);
                                    return;
                                } else {
                                    cancelTimer(); //cancel timer if emit ACK return true
                                    Log.d("patchsharma1234", "AckWithTimeOut : " + args[0].toString());
                                    try {
                                        JSONObject data = (JSONObject) args[0];
                                        if (data.getBoolean("status")) {
                                            final String call = data.getJSONObject("data").getString("call");
                                            String host = data.getJSONObject("data").getString("host");
                                            String callingContext = data.getJSONObject("data").getString("context");
                                            if (!call.equals("")) {
                                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                                phone = sharedPref.getString("patch_userPhone", "");
                                                cc = sharedPref.getString("patch_userCC", "");
                                                accountId = sharedPref.getString("patch_accountId", null);
                                                apikey = sharedPref.getString("patch_apikey", null);
                                                jwt = sharedPref.getString("patch_token", null);
                                                cuid = sharedPref.getString("patch_cuid", "");
                                                data.put(context.getString(R.string.accountId), accountId);
                                                data.put(context.getString(R.string.apikey), apikey);
                                                data.put(context.getString(R.string.phone), phone);
                                                data.put(context.getString(R.string.cc), cc);
                                                data.put(context.getString(R.string.token), jwt);
                                                data.put("cuid", cuid);
                                                data.put(context.getString(R.string.scontext), callingContext);
                                                data.put(context.getString(R.string.scall), call);
                                                data.put(context.getString(R.string.shost), host);
                                                if (cuid.length() > 0) {
                                                    data.put("initiatorData", cuid);
                                                } else {
                                                    data.put("initiatorData", cc + phone);
                                                }
                                                if (data.getJSONObject("data").getJSONObject("to").has("platform")) {
                                                    if (data.getJSONObject("data").getJSONObject("to").getString("platform").equals("ios")
                                                            && !isPstn && callOptions.has("autoFallback") && callOptions.getBoolean("autoFallback")) {
                                                        SocketInit.getInstance().setIosAPF(new IosAPF(calleeCC, calleePhone, calleeCuid, callContext, callOptions, outgoingCallResponse));
                                                    } else {
                                                        SocketInit.getInstance().setIosAPF(new IosAPF("",
                                                                "",
                                                                "",
                                                                "",
                                                                new JSONObject(), null));
                                                    }
                                                }
                                                if (data.getJSONObject("data").has("fcmEnabled")) {
                                                    if (!isPstn && callOptions.has("autoFallback") && callOptions.getBoolean("autoFallback")) {
                                                        SocketInit.getInstance().setIosAPF(new IosAPF(calleeCC, calleePhone, calleeCuid, callContext, callOptions, outgoingCallResponse));
                                                    } else {
                                                        SocketInit.getInstance().setIosAPF(new IosAPF("",
                                                                "",
                                                                "",
                                                                "",
                                                                new JSONObject(), null));
                                                    }
                                                }
//                        data.put("callOptions", callOptions);
                                                data.put("callType", "outgoing");
                                                //outgoingCallResponse.onSuccess(SUCCESS_CALL_PLACED);
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_SUCCESS, CALL_PLACED);
                                                if (isPstn) {
                                     /*SharedPreferences branding = PreferenceManager.getDefaultSharedPreferences(context);
                                        String color = sharedPref.getString("patch_fontColor", null);
                                        String backgrounColor = sharedPref.getString("patch_bgColor", null);
                                        String logoUrl = sharedPref.getString("patch_logo", null);*/
                                                    if (data.getJSONObject("data").getJSONObject("to").getString("cc").equals("false") ||
                                                            data.getJSONObject("data").getJSONObject("to").getString("cc").equals("false")) {
                                                        PatchSDK.sdkReady = true;
                                                        //outgoingCallResponse.onFailure(ERR_NUMBER_NOT_EXISTS);
                                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_NUMBER_NOT_EXISTS);
                                                        return;
                                                    }
                                                    data.put("to", data.getJSONObject("data").getJSONObject("to"));
                                                    if (callOptions.has("ios_apf") && callOptions.getBoolean("ios_apf")) {
                                                        socketInit.setCallId(call);
                                                        callStatus = socketInit.getCallStatus();
//                                    SocketInit.getInstance().getTimer().cancel();
                                                        callStatus.onIosApf(data.toString());
                                                    } else {
                                                        //THIS conditons is True for Never_Ask_Again flow

                                                        final Intent i = new Intent(context, PatchCallingActivity.class);
                                                        i.putExtra(context.getString(R.string.screen), context.getString(R.string.callScreen));
                                                        i.putExtra(context.getString(R.string.callDetails), data.toString());
                                                        socketInit.setCallId(call);
                                                        if (unAuthorizedFlag != null && unAuthorizedFlag.equals("401")) {
                                                            socketInit.getTimer().cancel();  //this is important here to do as done in decline handler?

                                                            Intent broadcastIntent = new Intent("com.patch.PatchOutgoingFragment");
                                                            broadcastIntent.putExtra("action", "finish");
                                                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                                                            countDownHandler.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    new CountDownTimer(750, 750) {

                                                                        @Override
                                                                        public void onTick(long millisUntilFinished) {
                                                                        }

                                                                        @Override
                                                                        public void onFinish() {
                                                                            i.putExtra("launchReason", "401");
                                                                            context.startActivity(i);
                                                                        }
                                                                    }.start();
                                                                }
                                                            });
                                                            //return;
                                                        } else {
                                                            context.startActivity(i);
                                                        }
                                                    }
                                                } else {
                                                    Intent i = new Intent(context, PatchCallingActivity.class);
                                                    i.putExtra(context.getString(R.string.screen), context.getString(R.string.outgoing));
                                                    i.putExtra(context.getString(R.string.callDetails), data.toString());
                                                    socketInit.setCallId(call);
                                                    i.addFlags(FLAG_ACTIVITY_NEW_TASK);
                                                    context.startActivity(i);
                                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            outgoingTimer = new CountDownTimer(35000, 1000) {
                                                                @Override
                                                                public void onTick(long millisUntilFinished) {

                                                                }

                                                                @Override
                                                                public void onFinish() {
                                                                    final IosAPF iosAPF = SocketInit.getInstance().getIosAPF();
                                                                    if (iosAPF.getContext().isEmpty() || iosAPF.getContext() == null) {
                                                                        socketInit.getOutgoingActivity().finish();
                                                                        PatchSDK.sdkReady = true;

                                                                        if (outgoingTimer != null) {
                                                                            outgoingTimer.cancel();
                                                                        }
                                                                        socketInit.getSocket().emit("cancel", call, new Ack() {
                                                                            @Override
                                                                            public void call(Object... args) {

                                                                            }
                                                                        });
                                                                        SocketInit.getInstance().setTimer(outgoingTimer);
                                                                    } else {
                                                                        iosAPF(call, new IosApfInterface() {
                                                                            @Override
                                                                            public void onSuccess() {
                                                                                try {
                                                                                    iosAPF.getCallOptions().put("pstn", true);
                                                                                    iosAPF.getCallOptions().put("ios_apf", true);
                                                                                    makeCall(iosAPF.getCalleeCC(),
                                                                                            iosAPF.getCalleePhone(),
                                                                                            iosAPF.getCuid(),
                                                                                            iosAPF.getContext(),
                                                                                            iosAPF.getCallOptions(),
                                                                                            outgoingCallResponse, null);
                                                                                    SocketInit.getInstance().setIosAPF(new IosAPF("",
                                                                                            "",
                                                                                            "", "", new JSONObject(), null));
                                                                                    outgoingTimer.cancel();
//                                                                outgoingTimer.start();
//                                                                socketInit.setTimer(outgoingTimer);

                                                                                } catch (Exception e) {
                                                                                    if (getApplicationContext() != null)
                                                                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                                                                                }
                                                                            }

                                                                            @Override
                                                                            public void onFailure() {

                                                                            }
                                                                        });

                                                                    }
                                                                }
                                                            };
                                                            outgoingTimer.start();
                                                            socketInit.setTimer(outgoingTimer);
                                                        }
                                                    });
                                                }
                                            }
                                        } else {

                                            if (data.getJSONObject("error") != null && data.getJSONObject("error").has("message") &&
                                                    data.getJSONObject("error").getString("message") != null &&
                                                    data.getJSONObject("error").getString("message").equals("Invalid Call Token")) {
                                                PatchSDK.sdkReady = true;
                                                //outgoingCallResponse.onFailure(ERR_INVALID_CALL_TOKEN);
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_CALL_TOKEN);
                                                return;
                                            }
                                            if (data.getJSONObject("error") != null && data.getJSONObject("error").has("code") &&
                                                    data.getJSONObject("error").getInt("code") == 404 && callOptions.has("autoFallback")
                                                    && callOptions.getBoolean("autoFallback")) {
                                                if (calleeCC.length() > 0 && calleePhone.length() > 0) {
                                                    callOptions.put("pstn", true);
                                                    callOptions.remove("autoFallback");
                                                    makeCall(calleeCC, calleePhone, "", callContext, callOptions, outgoingCallResponse, null);
                                                } else {
                                                    callOptions.put("pstn", true);
                                                    callOptions.remove("autoFallback");
                                                    makeCall("", "", calleeCuid, callContext, callOptions, outgoingCallResponse, null);
                                                }
                                            } else if (data.getJSONObject("error") != null && data.getJSONObject("error").has("code") &&
                                                    data.getJSONObject("error").getInt("code") == 400 &&
                                                    data.getJSONObject("error").has("message") &&
                                                    data.getJSONObject("error").getString("message") != null &&
                                                    data.getJSONObject("error").getString("message").equals("Missing CC/Phone to make a PSTN call")) {
                                                PatchSDK.sdkReady = true;
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MISSING_CC_PHONE_TO_MAKE_PSTN_CALL);
                                                if (PatchCallingActivity.getInstance() != null) {
                                                    PatchCallingActivity.getInstance().finish();
                                                }
                                            } else if (data.getJSONObject("error") != null && data.getJSONObject("error").has("code") &&
                                                    data.getJSONObject("error").getInt("code") == 404) {
                                                PatchSDK.sdkReady = true;
                                                //outgoingCallResponse.onFailure(ERR_CONTACT_NOT_REACHABLE);
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CONTACT_NOT_REACHABLE);
                                            } else if (data.getJSONObject("error").has("message") &&
                                                    data.getJSONObject("error").getString("message") != null) {
                                                PatchSDK.sdkReady = true;
                                                if (data.getJSONObject("error").getString("message").equals("jwt malformed")) {
                                                    Log.d("Patch", "Unregistered CallToken!");
                                                } else
                                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_WHILE_MAKING_VOIP_CALL);
                                            } else {
                                                PatchSDK.sdkReady = true;
                                                //outgoingCallResponse.onFailure(ERR_CONTACT_NOT_REACHABLE);
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_WHILE_MAKING_VOIP_CALL);
                                            }
                                        }
                                    } catch (Exception e) {
                                        PatchSDK.sdkReady = true;
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_WHILE_MAKING_VOIP_CALL);
                                    }
                                }
                            }

                        } catch (Exception e) {
                            PatchSDK.sdkReady = true;
                        }
                    }
                });
                /*socketInit.getSocket().emit(context.getString(R.string.smakecall), jsonObject, new Ack() {
                    @Override
                    public void call(Object... args) {

                    }
                });*/
            } else {
                PatchSDK.sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_MICROPHONE_PERMISSION_NOT_GRANTED);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MICROPHONE_PERMISSION_NOT_GRANTED);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            PatchSDK.sdkReady = true;
            if (getApplicationContext() != null) {
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            }
        }
    }

    /**
     * emits ios_apf on sigsock before making a pstn call to he receiver who is on ios.
     *
     * @param callId:-          id of the call got in the response of makecall.
     * @param iosApfInterface:- fire the iosApf functoin on the outgoingFragment.
     */
    public void iosAPF(String callId, final IosApfInterface iosApfInterface) {
        if (SocketInit.getInstance().getSocket() != null) {
            SocketInit.getInstance().getSocket().emit("ios_apf", callId, new Ack() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject iosApfStatus = (JSONObject) args[0];
                        if (iosApfStatus.getBoolean("status")) {
                            iosApfInterface.onSuccess();
                        } else {
                            iosApfInterface.onFailure();
                        }
                    } catch (Exception e) {
                        if (getApplicationContext() != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                    }
                }
            });
        }
    }

    /**
     * message to be send on sigsock. encrypts the message by 256 bit encryption and then sends a message
     *
     * @param messageOptions:-          cc of the user whom user wants to send a message.
     *                                  phone of the user whom user wants to send a message.
     *                                  message to be send.
     * @param outgoingMessageResponse:- callback statuses of the messages i.e. either success or failure.w
     */
    public void sendMessage(final JSONObject messageOptions, final OutgoingMessageResponse outgoingMessageResponse) throws Exception {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String accoutnId = sharedPref.getString("patch_accountId", null);
            CryptLib cryptLib = new CryptLib();
            JSONObject jsonObject = new JSONObject();

            if (messageOptions.has("cuid") && messageOptions.getString("cuid") != null) {
                jsonObject.put("cuid", messageOptions.getString("cuid"));
            }
            /*else if (messageOptions.has("cc") && messageOptions.has("phone") &&
                    messageOptions.getString("cc") != null && messageOptions.getString("phone") != null) {
                jsonObject.put("cc", messageOptions.getString("cc"));
                jsonObject.put("phone", messageOptions.getString("phone"));
            }*/
            if (sharedPref.getString("patch_cuid", "") != null) {
                jsonObject.put("senderCuid", sharedPref.getString("patch_cuid", ""));
            }
            if (MessageInit.getInstance().getConversationId() == null) {
                /*MessageInit.getInstance().setConversationId(
                        AppUtil.getInstance().getConversationId(sharedPref.getString("patch_cuid", ""),
                                messageOptions.has("cuid") ? messageOptions.getString("cuid"):""));*/
                String convoId = PatchCommonUtil.getInstance().getConversationId(sharedPref.getString("patch_cuid", ""),
                        messageOptions.has("cuid") ? messageOptions.getString("cuid") : "");
                jsonObject.put("conversationId", convoId);
                MessageInit.getInstance().setConversationId(convoId);  //setting so that next time the same covoId get
            } else {
                jsonObject.put("conversationId", MessageInit.getInstance().getConversationId());
            }
            if (MessageInit.getInstance().getMerchant() !=null && MessageInit.getInstance().getMerchant()) {
                jsonObject.put("merchant", true);
            }
            if (messageOptions.has("message") && messageOptions.getString("message") != null) {
                jsonObject.put("body", cryptLib.encryptPlainTextWithRandomIV(messageOptions.getString("message"), accoutnId));
            }

            SocketInit.getInstance().getSocket().emit("message", jsonObject, new Ack() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject jsonObject = (JSONObject) args[0];
                        Boolean status = jsonObject.getBoolean("status");
                        if (status) {
                            customHandler.sendOutMessAnnotations(outgoingMessageResponse, CustomHandler.OutMess.ON_SUCCESS, SUCCESS_MESSAGING_SENT);
                        } else {
                            customHandler.sendOutMessAnnotations(outgoingMessageResponse, CustomHandler.OutMess.ON_FAILURE, ERR_MESSAGE_FAILED);
                        }
                    } catch (Exception e) {
                        customHandler.sendOutMessAnnotations(outgoingMessageResponse, CustomHandler.OutMess.ON_FAILURE, ERR_WRONG_THREAD_EXCEPTION_TO_UPDATE_UI);
                    }
                }
            });
        } catch (Exception e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
        }
    }

    public void sendNotification(JSONObject notifyDataToBeEmit, final NotificationResponse notificationResponse) {
        try {
            final SocketInit socketInit = SocketInit.getInstance();
            if (socketInit != null)
                socketInit.getSocket().emit(context.getString(R.string.snotify), notifyDataToBeEmit, new Ack() {
                    @Override
                    public void call(Object... args) {
                        try {
                            JSONObject response = (JSONObject) args[0];
                            if (response.has("data")) {
                                JSONObject data = response.getJSONObject("data");
                                if (response.getBoolean("status")) {
                                    customHandler.sendNotiAnnotations(notificationResponse, CustomHandler.Noti.ON_SUCCESS,0);
                                    //notificationResponse.onResponse(true, data.getString("notificationId"));
                                }else {
                                    customHandler.sendNotiAnnotations(notificationResponse, CustomHandler.Noti.ON_FAILURE, ERR_NOTIFICATION_FAILED);
                                }
                            } else if (response.has("error")) {
                                customHandler.sendNotiAnnotations(notificationResponse, CustomHandler.Noti.ON_FAILURE, ERR_NOTIFICATION_FAILED);
                            } else {
                                customHandler.sendNotiAnnotations(notificationResponse, CustomHandler.Noti.ON_FAILURE, ERR_NOTIFICATION_FAILED);
                            }
                        } catch (Exception e) {
                            //notificationResponse.onFailure(ERR_NOTIFICATION_FAILED);
                            customHandler.sendNotiAnnotations(notificationResponse, CustomHandler.Noti.ON_FAILURE, ERR_NOTIFICATION_FAILED);
                            if (getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                        }
                    }
                });
        } catch (Exception e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
        }
    }

    JSONArray sortedJsonArrayOnScheduledTime;    //{"sentiment" : true/false , "notificationId" : "5cfa6f2570d9fc21814b0357"}
    JSONObject jsonObject;

    public void updatePendingSentiment() {
        try {
            sortedJsonArrayOnScheduledTime = null;
            jsonObject = null;
            if (getApplicationContext() != null) {
                sortedJsonArrayOnScheduledTime = getSortedArrayonScheduledTime();
                if (sortedJsonArrayOnScheduledTime != null) {
                    try {
                        for (int index = 0; index < sortedJsonArrayOnScheduledTime.length(); index++) {
                            jsonObject = sortedJsonArrayOnScheduledTime.getJSONObject(index);
                            PendingSentiment patchData = new PendingSentiment();
                            List<Integer> list = new ArrayList<>();
                            list.add(jsonObject.getInt("btnSentiment"));
                            patchData.setSentiment(list);
                            if(jsonObject.has("callId")){
                                patchData.setCallId(jsonObject.getString("callId"));
                            }
                            String authToken = PatchCommonUtil.getInstance().getAccessToken(context);
                            Call<ResolvedSentimentResponse> call = apiService.resolvePendingSentiment(jsonObject.getString("notificationId"), patchData, authToken);
                            call.enqueue(new Callback<ResolvedSentimentResponse>() {
                                @Override
                                public void onResponse(Call<ResolvedSentimentResponse> call, Response<ResolvedSentimentResponse> response) {
                                    if (response.isSuccessful()) {
                                        ResolvedSentimentResponse notificationObject = response.body();
                                        recreateNewPendingSentimentFile(notificationObject.getNotificationId());
                                    } else {
                                        if (response.code() == 401) {
                                            PatchCommonUtil.getInstance().removeDevToken(context);
                                        }
                                    }
                                }

                                @Override
                                public void onFailure(Call<ResolvedSentimentResponse> call, Throwable t) {
                                }
                            });
                        }

                    } catch (Exception e) {
                        if (getApplicationContext() != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                    }
                }
            }
        } catch (Exception e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
        }

    }

    private JSONArray getSortedArrayonScheduledTime() {

        List<JSONObject> jsonValues = new ArrayList<>();
        try {
            JSONArray unSortedJsonArray = PendingSentimentHandler.getInstance(getApplicationContext()).readFromFile();
            if (unSortedJsonArray != null) {
                try {
                    for (int index = 0; index < unSortedJsonArray.length(); index++)
                        jsonValues.add(unSortedJsonArray.getJSONObject(index));
                    Collections.sort(jsonValues, new Comparator<JSONObject>() {
                        @Override
                        public int compare(JSONObject o1, JSONObject o2) {
                            //valA and valB could be any simple type, such as number, string, whatever
                            String valA = null, valB = null;
                            try {
                                valA = o1.getString("notificationScheduledTime");
                                valB = o2.getString("notificationScheduledTime");
                            } catch (JSONException e) {
                                if (getApplicationContext() != null)
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                            }
                            return valA.compareTo(valB);
                        }
                    });
                } catch (JSONException e) {
                    if (getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                }
            }
        } catch (Exception e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
        }

        return new JSONArray(jsonValues);
    }

    private void recreateNewPendingSentimentFile(final String resolvedNotificationId) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    JSONArray jsonArray = PendingSentimentHandler.getInstance(getApplicationContext()).readFromFile();
                    for (int index = 0; index < jsonArray.length(); index++) {
                        try {
                            if (resolvedNotificationId.equals(jsonArray.getJSONObject(index).getString("notificationId"))) {
                                jsonArray.remove(index);   //removing resolved sentiment from jsonArray
                            }
                        } catch (JSONException e) {
                            if (getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                        }
                    }
                    try {
                        PendingSentimentHandler.getInstance(getApplicationContext()).deleteFile();           //deleting existing file
                        PendingSentimentHandler.getInstance(getApplicationContext()).writeToFile(jsonArray); //write updated jsonArray to new file
                    } catch (Exception e) {
                        if (getApplicationContext() != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
                    }
                }
            }).start();
        } catch (Exception e) {
            if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
        }
    }


}
