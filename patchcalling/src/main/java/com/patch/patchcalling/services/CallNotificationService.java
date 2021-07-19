package com.patch.patchcalling.services;

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
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.patch.patchcalling.R;
import com.patch.patchcalling.activity.PatchCallingActivity;
import com.patch.patchcalling.broadcastreciever.CallNotificationActionReceiver;
import com.patch.patchcalling.fragments.PatchIncomingFragment;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.NotificationHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CallNotificationService extends Service {

    Notification.Builder notificationBuilder1;
    NotificationCompat.Builder notificationBuilder2;
    JSONObject callDetails;
    String callContext;
    String CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC;
    private Context context;
    NotificationManager mNotificationManager;
    Map<String, Intent> actionIntentMap = new HashMap<>();

    private static CallNotificationService instance = null;

    public static CallNotificationService getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            instance = this;
            this.context = this;
            if(context!=null){
                mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }else {
                mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            }
            this.CALL_CHANNEL_ID = getString(R.string.secondary_channel_id);
            this.CALL_CHANNEL_NAME = getString(R.string.secondary_channel_name);
            this.CALL_CHANNEL_DESC = getString(R.string.secondary_channel_desc);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mNotificationManager.createNotificationChannel(NotificationHandler.getInstance(context).getNotficationChannel(CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC, "call"));
            }
        }catch (Exception e){
            stopSelf();
        }
    }

    private Intent getActionIntent(String actionType, JSONObject callDetails) {
        Intent actionIntent = null;
        try {
            actionIntent = new Intent(/*context*/ this, CallNotificationActionReceiver.class);
            actionIntent.putExtra("actionType", actionType);
            actionIntent.putExtra("callDetails", callDetails.toString());
            if(actionType.equals("Answer") && SocketInit.getInstance().getSid()!=null){
                actionIntent.putExtra("sid", SocketInit.getInstance().getSid());
            }
        }catch (Exception e){

        }
        return actionIntent;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            if(intent.hasExtra("incomingNotificationData")) {
                callDetails = new JSONObject(intent.getStringExtra("incomingNotificationData"));
                callContext = callDetails.getString("context");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            stopSelf();
        }

        try {

            if(PatchIncomingFragment.getInstance()!=null){
                actionIntentMap.put("Answer", getActionIntent("Answer", callDetails));
            }else {
                actionIntentMap.put("Answer", getActionIntent("Answer", new JSONObject(callDetails.getString(this.getString(R.string.callDetails)))));
            }
            actionIntentMap.put("Decline", getActionIntent("Decline", callDetails));
        }catch (Exception e){

        }
        try {
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
                                //Log.d("pachsharma","onLoadFailed");
                                generateNotfication(null);
                                //stopSelf();
                            }catch (Exception e1){
                                try {
                                    stopSelf();
                                }catch (Exception e2){

                                }
                            }
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            try {
                                generateNotfication(resource);
                            }catch (Exception e){
                                try {
                                    stopSelf();
                                }catch (Exception e1){

                                }
                            }
                            return false;
                        }
                    })
                    .apply(requestOptions)
                    .submit();

        }catch (Exception e){

        }


        //do heavy work on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    PatchCommonUtil.getInstance().startAudio(getApplicationContext());
                } catch (Exception e) {
                    try {
                        stopSelf();
                    }catch (Exception e1){

                    }
                }
            }
        }).start();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        try {
            Intent intent = new Intent(context, CallNotificationService.class);
            context.stopService(intent);
        }catch (Exception e){

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        try {
            Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vib.cancel();
        }catch (Exception e){
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void generateNotfication(Bitmap resource){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationBuilder1 = new Notification.Builder(context, CALL_CHANNEL_ID)
                        .setContentTitle(callContext)
                        .setContentText("Incoming voice call")
                        .setShowWhen(true)
                        .setSmallIcon(R.drawable.ic_patch_lightening)
                        .setStyle(new Notification.BigTextStyle().bigText("Incoming voice call"))
                        //.setLargeIcon(notificationHandler.getBitmapFromUrl(context, logoUrl))
                        .setAutoCancel(false);

                if(resource!=null){
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

                startForeground(199, notificationBuilder1.build());
            }else {
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

                if(resource!=null){
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

                startForeground(199, notificationBuilder2.build());
            }

        }catch (Exception e){
            try {
                stopSelf();
            }catch (Exception e1){

            }
        }
    }
}