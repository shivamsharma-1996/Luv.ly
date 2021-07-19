package com.patch.patchcalling.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.gson.Gson;
import com.patch.patchcalling.Constants;
import com.patch.patchcalling.PatchResponseCodes;
import com.patch.patchcalling.R;
import com.patch.patchcalling.activity.PatchCallingActivity;
import com.patch.patchcalling.broadcastreciever.CallNotificationActionReceiver;
import com.patch.patchcalling.broadcastreciever.NotificationActionReceiver;
import com.patch.patchcalling.fragments.PatchIncomingFragment;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.models.MissedCallActions;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static com.patch.patchcalling.utils.NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL;
import static com.patch.patchcalling.utils.NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL;
import static com.patch.patchcalling.utils.NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL;
import static com.patch.patchcalling.utils.NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL;

/**
 * Created by Shivam Sharma on 19-05-2019.
 */

/**
 * Helper class to manage notification channels, and create notifications.
 */
public class NotificationHandler {

    private static NotificationHandler instance;
    private static NotificationManager mNotificationManager;

    public NotificationHandler() {
    }


    @IntDef({NotificationActions.ACTION_BUTTON_1, NotificationActions.ACTION_BUTTON_2, NotificationActions.ACTION_BUTTON_3})
    @Retention(RetentionPolicy.SOURCE)
    @interface NotificationActions {
        int ACTION_BUTTON_NONE = 0;  //clicked on notification
        int ACTION_BUTTON_1 = 1;
        int ACTION_BUTTON_2 = 2;
        int ACTION_BUTTON_3 = 3;
    }
    /**
     * Singleton pattern implementation
     *
     * @return
     */
    public static NotificationHandler getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationHandler();
            mNotificationManager =
                    (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return instance;
    }

    Bitmap largeIconBitmap = null, bannerBitmap;
    String buttonNegativeLabel = null, buttonNeutralLabel = null, buttonPositiveLabel = null;
    public void showNotification(final Context context, final JSONObject notificationData) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String CHANNEL_ID = context.getString(R.string.primary_channel_id);
                    final String CHANNEL_NAME = context.getString(R.string.primary_channel_name);
                    final String CHANNEL_DESC = context.getString(R.string.primary_channel_desc);

                    JSONObject buttons;

                    final String title, body, picture, bannerImage, notificationId, senderCuid, sentAt;
                    buttonNegativeLabel = null;
                    buttonNeutralLabel = null;
                    buttonPositiveLabel = null;
                    final int LOCAL_NOTIFICATION_ID = getRandomNotificationId();
                    try {
                        if (!notificationData.has("title") || !notificationData.has("body") || !notificationData.has("picture"))
                            return;
                   /* if (!notificationData.has("notificationId")) {
                        return;
                    }*/
                  /*  if (!notificationData.has("senderCuid")) {
                        return;
                    }*/

                        title = notificationData.getString("title");
                        body = notificationData.getString("body");
                        picture = notificationData.getString("picture");
                        bannerImage = /*notificationData.getString("picture")*/ null;
                        sentAt = notificationData.getString("sentAt");
                        notificationId = notificationData.has("notificationId") ? notificationData.getString("notificationId") : null;
                        senderCuid = notificationData.has("senderCuid") ? notificationData.getString("senderCuid") : null;
                        if (notificationData.has("buttons")) {
                            buttons = notificationData.getJSONObject("buttons");
                            buttonPositiveLabel = buttons.has("1") ? buttons.getString("1") : null;
                            buttonNegativeLabel = buttons.has("2") ? buttons.getString("2") : null;
                            buttonNeutralLabel = buttons.has("3") ? buttons.getString("3") : null;
                        }

                        final Bundle bundle = new Bundle();
                        if (notificationId != null) {
                            bundle.putString("notificationId", notificationId);
                        }
                        if (senderCuid != null) {
                            bundle.putString("senderCuid", senderCuid);
                        }
                        if (sentAt != null) {
                            bundle.putString("sentAt", sentAt);
                        }
                        if(SocketInit.getInstance().getCallId()!=null){
                            bundle.putString("callId", SocketInit.getInstance().getCallId());
                        }

                        bundle.putInt("localNotificationId", LOCAL_NOTIFICATION_ID);

                        if (picture != null && !"".equals(picture)) {
                            largeIconBitmap = getBitmapFromUrl(context, picture);
                        }

                        if (bannerImage != null && !"".equals(bannerImage)) {
                            try {
                                FutureTarget<Bitmap> futureTarget= Glide.with(context).asBitmap().load(picture).submit();
                                bannerBitmap = futureTarget.get();
                            }catch (ExecutionException e){
                                Log.d("patchException", "Exception" + Log.getStackTraceString(e));
                            }catch (Exception e){

                            }
                        }

                        new Handler(Looper.getMainLooper()).post(new Runnable() {

                            @Override
                            public void run() {
                                try {
//                                    Class targetIntent = SocketInit.getInstance().getPendingNeutralActionIntent(context);

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Notification.Builder notificationBuilder1 = new Notification.Builder(context, CHANNEL_ID)
                                                .setContentTitle(title)
                                                .setContentText(body)
                                                .setShowWhen(true)
                                                .setSmallIcon(R.drawable.ic_patch_lightening)
                                                .setStyle(new Notification.BigTextStyle().bigText(body))
                                                //.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                                                .setLargeIcon(largeIconBitmap)
                                                .setAutoCancel(true);

                                        if(bannerImage!=null)
                                        notificationBuilder1.setStyle(new Notification.BigPictureStyle().bigPicture(bannerBitmap));

                                        notificationBuilder1.setContentIntent(getPendingIntent(context, getNotificationActionIntent(context,
                                                bundle, NotificationActions.ACTION_BUTTON_NONE)));

                                        if (buttonPositiveLabel != null) {
                                            notificationBuilder1.addAction(0, buttonPositiveLabel, getPendingIntent(context, getNotificationActionIntent(context,
                                                    bundle, NotificationActions.ACTION_BUTTON_1)));
                                        }
                                        if(buttonNegativeLabel != null){
                                            notificationBuilder1.addAction(0, buttonNegativeLabel, getPendingIntent(context, getNotificationActionIntent(context,
                                                    bundle, NotificationActions.ACTION_BUTTON_2)));
                                        }
                                        if(buttonNeutralLabel!= null){
                                            notificationBuilder1.addAction(0, buttonNeutralLabel, getPendingIntent(context, getNotificationActionIntent(context,
                                                    bundle, NotificationActions.ACTION_BUTTON_3)));
                                        }

                                        assert mNotificationManager != null;
                                        mNotificationManager.createNotificationChannel(getNotficationChannel(CHANNEL_ID, CHANNEL_NAME, CHANNEL_DESC, "notification"));
                                        assert mNotificationManager != null;
                                        mNotificationManager.notify(LOCAL_NOTIFICATION_ID, notificationBuilder1.build());
                                    } else {
                                        NotificationCompat.Builder notificationBuilder2 = new NotificationCompat.Builder(context);
                                        notificationBuilder2.setContentTitle(title)
                                                .setStyle(new NotificationCompat.BigTextStyle()
                                                        .setBigContentTitle(title)
                                                        .bigText(body))
                                                .setShowWhen(true)
                                                .setContentText(body)
                                                .setAutoCancel(true)
                                                .setPriority(NotificationManager.IMPORTANCE_HIGH)
//                                                .setContentIntent(getNotificationTouchPIntent(context, notificationIntent))
                                                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                                                //.setStyle(new NotificationCompat.BigPictureStyle().bigLargeIcon(bannerBitmap))
                                                .setLargeIcon(largeIconBitmap)
                                                .setDefaults(Notification.DEFAULT_VIBRATE)
                                                .setSmallIcon(R.drawable.ic_patch_lightening);

                                        if(bannerImage!=null)
                                            notificationBuilder2.setStyle(new NotificationCompat.BigPictureStyle().bigLargeIcon(bannerBitmap));

                                        notificationBuilder2.setContentIntent(getPendingIntent(context, getNotificationActionIntent(context,
                                                bundle, NotificationActions.ACTION_BUTTON_NONE)));

                                        if (buttonPositiveLabel != null) {
                                            notificationBuilder2.addAction(0, buttonPositiveLabel, getPendingIntent(context, getNotificationActionIntent(context,
                                                    bundle, NotificationActions.ACTION_BUTTON_1)));
                                        }
                                        if(buttonNegativeLabel != null){
                                            notificationBuilder2.addAction(0, buttonNegativeLabel, getPendingIntent(context, getNotificationActionIntent(context,
                                                    bundle, NotificationActions.ACTION_BUTTON_2)));
                                        }
                                        if(buttonNeutralLabel!= null){
                                            notificationBuilder2.addAction(0, buttonNeutralLabel, getPendingIntent(context, getNotificationActionIntent(context,
                                                    bundle, NotificationActions.ACTION_BUTTON_3)));
                                        }
                                        assert mNotificationManager != null;
                                        mNotificationManager.notify(LOCAL_NOTIFICATION_ID, notificationBuilder2.build());
                                    }
                                }catch (Exception e){

                                }
                            }
                        });
                    } catch (Exception e) {
                        if (context != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                    }
                }
            }).start();
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public NotificationChannel getNotficationChannel(String CHANNEL_ID, String CHANNEL_NAME, String CHANNEL_DESC, String channelCause) throws Exception {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(CHANNEL_DESC);
        channel.setShowBadge(true);
        channel.canShowBadge();
        channel.enableLights(true);
        channel.setLightColor(Color.GREEN);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        if(channelCause.equals("call")){
            channel.enableVibration(false);
            channel.setSound(null, null);
            channel.setVibrationPattern(new long[]{0});
        }else {
            channel.enableVibration(true);
            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build());
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
        }
        return channel;
    }

    public PendingIntent getPendingIntent(Context context, Intent intent) {
        return PendingIntent.getBroadcast(context, getRandomNotificationId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public Bitmap getBitmapFromUrl(Context context, String bitmapUrl) {
        try {
            URL url = new URL(bitmapUrl);
            return BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            return null;
        }
    }

    public PendingIntent getNotificationTouchPIntent(Context context, Intent intent) {
        return PendingIntent.getActivity(context, getRandomNotificationId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public int getRandomNotificationId() {
        return ThreadLocalRandom.current().nextInt(0, 9999);
    }

    public Intent getNotificationActionIntent(Context context, Bundle bundle, int actionButtonKey){
        Intent nActionIntent = new Intent(context, NotificationActionReceiver.class);
        nActionIntent.putExtra("sentiment", actionButtonKey);
        nActionIntent.putExtra("bundle", bundle);
        return nActionIntent;
    }

    public static class CallNotificationHandler {
        private Context context;
        private static CallNotificationHandler callNotificationInstance;
        private NotificationHandler notificationHandler;
        private String CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC;
        private String callNotificationStatus;

        public interface CallNotificationTypes {
            String OUTGOING_CALL = "outgoing";
            String INCOMING_CALL = "incoming";
            String ONGOING_CALL = "ongoing";
            String MISSED_CALL = "missed";
        }

        public String getCallNotificationStatus() {
            return callNotificationStatus;
        }

        public void setCallNotificationStatus(String callNotificationStatus) {
            this.callNotificationStatus = callNotificationStatus;
        }

        private CallNotificationHandler(Context context) {
            this.context = context;
            notificationHandler = NotificationHandler.getInstance(context);
            this.CALL_CHANNEL_ID = context.getString(R.string.secondary_channel_id);
            this.CALL_CHANNEL_NAME = context.getString(R.string.secondary_channel_name);
            this.CALL_CHANNEL_DESC = context.getString(R.string.secondary_channel_desc);
        }

        public static CallNotificationHandler getInstance(Context context) {
            if (callNotificationInstance == null) {
                callNotificationInstance = new CallNotificationHandler(context);
                mNotificationManager =
                        (NotificationManager) context.getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            }
            return callNotificationInstance;
        }

        public void showCallNotification(final JSONObject callDetails, final String type) {
            try {
                Thread imageLoaderThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setCallNotificationStatus(type);   //setting call-notification type

                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                            String logoUrl = sharedPref.getString("patch_logo", null);

                            RequestOptions requestOptions = RequestOptions
                                    .diskCacheStrategyOf(DiskCacheStrategy.ALL);

                            Glide.
                                    with(context).asBitmap().
                                    load(logoUrl).
                                    listener(new RequestListener<Bitmap>() {
                                        @Override
                                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                            onNotificationLargeIconLoaded(callDetails, type, null);
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                            onNotificationLargeIconLoaded(callDetails, type, resource);
                                            return false;
                                        }
                                    })
                                    .apply(requestOptions)
                                    .submit();

                        }catch (Exception e) {
                            if (context != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                        }
                    }
                });
                imageLoaderThread.start();}catch (Exception e){

            }
        }

        private void onNotificationLargeIconLoaded(JSONObject callDetails, String type, Bitmap largeIcon){
            try {
                String callContext = callDetails.has("context")? callDetails.getString("context"):null;
                String fromCuid = callDetails.has("fromCuid") ? callDetails.getString("fromCuid") : null;
                String toCuid = callDetails.has("toCuid") ? callDetails.getString("toCuid") : null;
                Map<String, Intent> actionIntentMap = new HashMap<>();
                switch (type) {
                    case CallNotificationTypes.INCOMING_CALL:
                        try {
                            if(PatchIncomingFragment.getInstance()!=null){
                                actionIntentMap.put("Answer", getCallActionIntent("Answer", callDetails));
                            }else {
                                actionIntentMap.put("Answer", getCallActionIntent("Answer", new JSONObject(callDetails.getString(context.getString(R.string.callDetails)))));
                            }
                            actionIntentMap.put("Decline", getCallActionIntent("Decline", callDetails));
                            generateNotification(callContext, callDetails, "Incoming voice call", actionIntentMap, CallNotificationTypes.INCOMING_CALL, largeIcon);
                        }catch (Exception e){

                        }
                        break;
                    case OUTGOING_CALL:
                        actionIntentMap.put("Hangup", getCallActionIntent("Hangup_Outgoing", callDetails));
                        generateNotification(callContext, callDetails,"Ringing...", actionIntentMap, OUTGOING_CALL,largeIcon);
                        break;
                    case CallNotificationTypes.ONGOING_CALL:
                        actionIntentMap.put("Hangup", getCallActionIntent("Hangup_Ongoing", callDetails));
                        generateNotification(callContext, callDetails,"Ongoing voice call", actionIntentMap, CallNotificationTypes.ONGOING_CALL, largeIcon);
                        break;
                    case CallNotificationTypes.MISSED_CALL:
                        List<MissedCallActions> missedCallActions = null;
                        Log.d("patchsharma1223", "direction :- " + SocketInit.getInstance().getCallDirection());
                        String callDirection = SocketInit.getInstance().getCallDirection();
                        switch (callDirection){
                            case "outgoing":
                                missedCallActions = PatchCommonUtil.getInstance().getMissedCallInitiatorActionsPref(context);
                                break;
                            case "incoming":
                                missedCallActions = PatchCommonUtil.getInstance().getMissedCallReceiverActionsPref(context);
                                break;
                        }

                        if(missedCallActions!=null && missedCallActions.size()>0){
                            for(int i = 0 ; i < missedCallActions.size(); i++){
                                if(i!=3){
                                    actionIntentMap.put(missedCallActions.get(i).getActionLabel(),getMissedActionIntent
                                            ("Missed", callDirection, fromCuid, toCuid, callContext, missedCallActions.get(i)));
                                }else {
                                    break;
                                }
                                }
                            }
                        //actionIntentMap.put("Call back", getActionIntent("Callback", callDetails));
                        generateNotification("Missed voice call",callDetails, callContext, actionIntentMap, CallNotificationTypes.MISSED_CALL, largeIcon);
                        break;
                }
            }catch (Exception e){
                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            }
        }

        private Intent getCallActionIntent(String actionType, JSONObject callDetails) {
            Intent actionIntent = new Intent(context, CallNotificationActionReceiver.class);
                try {
                    actionIntent.putExtra("actionType", actionType);
                    actionIntent.putExtra("callDetails", callDetails.toString());
                    if(actionType.equals(INCOMING_CALL) && SocketInit.getInstance().getSid()!=null){
                        actionIntent.putExtra("sid", SocketInit.getInstance().getSid());
                    }
                }catch (Exception e){

            }
            return actionIntent;
        }

        private Intent getMissedActionIntent(String actionType, String callDirection, String fromCuid, String toCuid, String callCtx,MissedCallActions missedCallActions) {
            Intent actionIntent = new Intent(context, CallNotificationActionReceiver.class);
            actionIntent.putExtra("actionType", actionType);
            actionIntent.putExtra("callDirection", callDirection);
            actionIntent.putExtra("callContext", callCtx);
            actionIntent.putExtra("callerCuid", fromCuid);
            actionIntent.putExtra("calleeCuid", toCuid);

            try {
                actionIntent.putExtra("missedCallActions", new Gson().toJson(missedCallActions));

            }catch (Exception e){

            }
//            if(!actionType.equals("Missed")){
//                actionIntent.putExtra("callDetails", callDetails.toString());
//                try {
//                    if(actionType.equals(INCOMING_CALL) && SocketInit.getInstance().getSid()!=null){
//                        actionIntent.putExtra("sid", SocketInit.getInstance().getSid());
//                    }
//                }catch (Exception e){
//
//                }
//            }
            return actionIntent;
        }
        Notification.Builder notificationBuilder1;
        NotificationCompat.Builder notificationBuilder2;

        private void generateNotification(final String title, final JSONObject callDetails, final String body, final Map<String, Intent> actionIntentMap, final String notiType, final Bitmap largeIcon) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationBuilder1 = new Notification.Builder(context, CALL_CHANNEL_ID)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setShowWhen(true)
                            .setSmallIcon(R.drawable.ic_patch_lightening)
                            .setStyle(new Notification.BigTextStyle().bigText(body))
                            //.setLargeIcon(notificationHandler.getBitmapFromUrl(context, logoUrl))
                            .setAutoCancel(false);

                    if(largeIcon!=null){
                        notificationBuilder1.setLargeIcon(largeIcon);
                    }
                    if (notiType.equals(CallNotificationTypes.ONGOING_CALL))
                        notificationBuilder1.setUsesChronometer(true);
                    if (!notiType.equals(CallNotificationTypes.MISSED_CALL)) {
                        Intent intent = new Intent(context, PatchCallingActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        if(notiType.equals(INCOMING_CALL)){
                            Bundle bundle = new Bundle();
                            bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                            bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                            bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                            intent.putExtras(bundle);
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                notificationBuilder1.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);
                            }
                        }
                                  /*  if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                                        notificationBuilder1.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);
                                    }*/

                                  /*  if(!(AppUtil.getInstance().getRunningTaskCount(context) > 0)) {
                                        notificationBuilder1.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), false);
                                    }*/
                        //notificationBuilder1.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);

                                  /*  if(ProcessLifecycleOwner.get().getLifecycle().getCurrentState() == Lifecycle.State.CREATED){
                                        Log.d("patchsharma", "bg");
                                        notificationBuilder1.setFullScreenIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT), true);
                                    }
                                    if(ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED)){
                                        Log.d("patchsharma", "fg");

                                    }*/

                        notificationBuilder1.setOngoing(true)
                                .setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                    } else {
                        notificationBuilder1.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                                .setAutoCancel(true);
                    }

                    for (String key : actionIntentMap.keySet()) {
                        switch (key) {
                            case "Answer":
//                                            actionTitle = "<font color='#FF64DD17'>" + key + "</font>";
//                                            notificationBuilder1.addAction(0, Html.fromHtml(actionTitle), notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                notificationBuilder1.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                            case "Decline":
//                                            actionTitle = "<font color='#FFD50000'>" + key + "</font>";
//                                            notificationBuilder1.addAction(0, Html.fromHtml(actionTitle), notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                notificationBuilder1.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                            default:
                                notificationBuilder1.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                        }
                    }
                    assert mNotificationManager != null;
                    mNotificationManager.createNotificationChannel(notificationHandler.getNotficationChannel(CALL_CHANNEL_ID, CALL_CHANNEL_NAME, CALL_CHANNEL_DESC, "call"));

                    switch (notiType) {
                        case OUTGOING_CALL:
                            mNotificationManager.notify(OUTGOING_CALL, 101, notificationBuilder1.build());
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.notify(ONGOING_CALL, 102, notificationBuilder1.build());
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.notify(INCOMING_CALL, 103, notificationBuilder1.build());
                            break;
                        case MISSED_CALL:
                            mNotificationManager.notify(MISSED_CALL, 104, notificationBuilder1.build());
                            break;
                    }
                } else {
                    notificationBuilder2 = new NotificationCompat.Builder(context);
                    notificationBuilder2.setContentTitle(title)
                            .setStyle(new NotificationCompat.BigTextStyle()
                                    .setBigContentTitle(title)
                                    .bigText(body))
                            .setShowWhen(true)
                            .setContentText(body)
                            .setAutoCancel(false)
                            .setPriority(NotificationManager.IMPORTANCE_HIGH)
                            .setSound(null)
                            .setVibrate(new long[]{0})
                            //.setLargeIcon(notificationHandler.getBitmapFromUrl(context, logoUrl))
                            //.setDefaults(Notification.DEFAULT_ALL)
                            .setSmallIcon(R.drawable.ic_patch_lightening);
                    if(largeIcon!=null){
                        notificationBuilder2.setLargeIcon(largeIcon);
                    }
                    if (notiType.equals(CallNotificationTypes.ONGOING_CALL))
                        notificationBuilder2.setUsesChronometer(true);
                    if (!notiType.equals(CallNotificationTypes.MISSED_CALL)) {
                        Intent intent1 = new Intent(context, PatchCallingActivity.class);
                        intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        if(notiType.equals(INCOMING_CALL)){
                            Bundle bundle = new Bundle();
                            bundle.putString(context.getString(R.string.callDetails), callDetails.getString(context.getString(R.string.callDetails)));
                            bundle.putString(context.getString(R.string.screen), context.getString(R.string.incoming));
                            bundle.putString(context.getString(R.string.sid), callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "");
                            intent1.putExtras(bundle);
                        }
                        notificationBuilder2.setOngoing(true)
                                .setContentIntent(PendingIntent.getActivity(context, 0, intent1, PendingIntent.FLAG_UPDATE_CURRENT));
                    } else {
                        notificationBuilder2.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(), 0))
                                .setAutoCancel(true);
                    }


                    for (String key : actionIntentMap.keySet()) {
                        switch (key) {
                            case "Answer":
//                                            actionTitle = "<font color='#FF64DD17'>" + key + "</font>";
//                                            notificationBuilder1.addAction(0, Html.fromHtml(actionTitle), notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                notificationBuilder2.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                            case "Decline":
//                                            actionTitle = "<font color='#FFD50000'>" + key + "</font>";
//                                            notificationBuilder1.addAction(0, Html.fromHtml(actionTitle), notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                notificationBuilder2.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                            default:
                                notificationBuilder2.addAction(0, key, notificationHandler.getPendingIntent(context, actionIntentMap.get(key)));
                                break;
                        }
                    }
                    assert mNotificationManager != null;
                    switch (notiType) {
                        case OUTGOING_CALL:
                            mNotificationManager.notify(OUTGOING_CALL, 101, notificationBuilder2.build());
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.notify(ONGOING_CALL, 102, notificationBuilder2.build());
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.notify(INCOMING_CALL, 103, notificationBuilder2.build());
                            break;
                        case MISSED_CALL:
                            mNotificationManager.notify(MISSED_CALL, 104, notificationBuilder2.build());
                            break;
                    }
                }
            } catch (Exception e) {
                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            }
            /*try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {


                    }
                }).start();
            } catch (Exception e) {
                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            }*/
        }

        public void removeNotification(String tag) {
            try {
                setCallNotificationStatus(null);

                if (mNotificationManager != null) {
                    switch (tag) {
                        case OUTGOING_CALL:
                            mNotificationManager.cancel(tag, 101);
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.cancel(tag, 102);
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.cancel(tag, 103);
                            break;
                        case MISSED_CALL:
                            mNotificationManager.cancel(tag, 104);
                            break;
                    }
                }
            } catch (Exception e) {
                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            }
        }

        public void rebuildNotification(String type) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    switch (type) {
                        case OUTGOING_CALL:
                            mNotificationManager.notify(type, 101, notificationBuilder1.build());
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.notify(type, 102, notificationBuilder1.build());
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.notify(type, 103, notificationBuilder1.build());
                            break;
                    }
                } else {
                    switch (type) {
                        case OUTGOING_CALL:
                            mNotificationManager.notify(type, 101, notificationBuilder2.build());
                            break;
                        case ONGOING_CALL:
                            mNotificationManager.notify(type, 102, notificationBuilder2.build());
                            break;
                        case INCOMING_CALL:
                            mNotificationManager.notify(type, 103, notificationBuilder2.build());
                            break;
                    }
                }
            } catch (Exception e) {
                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            }

        }
    }
}


