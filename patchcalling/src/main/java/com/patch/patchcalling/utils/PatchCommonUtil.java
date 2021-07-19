package com.patch.patchcalling.utils;

import android.Manifest;
import android.app.ActivityManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.patch.patchcalling.R;
import com.patch.patchcalling.fcm.FcmCacheManger;
import com.patch.patchcalling.fcm.FcmUtil;
import com.patch.patchcalling.fragments.PatchIncomingFragment;
import com.patch.patchcalling.javaclasses.PatchTemplates;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.models.MissedCallActions;
import com.patch.patchcalling.services.CallNotificationService;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import io.socket.client.Ack;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.AUDIO_SERVICE;
import static com.patch.patchcalling.fragments.PatchIncomingFragment.afChangeListener;

//import com.patch.patchcalling.javaclasses.PatchTemplate;

public class PatchCommonUtil {

    private static PatchCommonUtil instance = null;
    private static final String TAG = "AppUtil";
    private static MediaPlayer ringtonePlayer;
    private static Vibrator vib;

    public static PatchCommonUtil getInstance() {
        if (instance == null) {
            instance = new PatchCommonUtil();
        }
        return instance;
    }

    private PatchCommonUtil() {
    }


    public String getCurrentIsoDateTime() {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
        return nowAsISO;
    }

    /**
     * check if the user has active internet connection or not.
     *
     * @param context: context of the activity from which initSdk is called.
     * @return reutrns true if internet connection is available else false.
     * @throws IOException
     */
    public boolean hasInternetAccess(Context context) throws Exception {
        if (isNetworkAvailable(context)) {
            try {
                HttpURLConnection urlc = (HttpURLConnection)
                        (new URL("https://clients3.google.com/generate_204")
                                .openConnection());
                urlc.setRequestProperty("User-Agent", "Android");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(1500);
                urlc.connect();
                return (urlc.getResponseCode() == 204);
            } catch (Exception e) {
                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            }
        }
        return false;
    }

    /**
     * check whether user is connected to data or wifi.
     *
     * @param context context of the activity from which initSdk is called.
     * @return
     */
    private boolean isNetworkAvailable(Context context) throws Exception {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public boolean hasPermissions(Context context, String... permissions) throws Exception {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * gives real time latency of user network during a call.
     *
     * @param url:- url to be send to calculate the network latency
     * @return
     */
    public String ping(Context context, String url) {
        String str = "";
        try {
            Process process = Runtime.getRuntime().exec(
                    "ping -c 1 " + url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            int i;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            String op[] = new String[64];
            String delay[] = new String[8];
            while ((i = reader.read(buffer)) > 0)
                output.append(buffer, 0, i);
            reader.close();
            op = output.toString().split("\n");
            if (op.length > 1) {
                delay = op[1].split("time=");
            }
            // body.append(output.toString()+"\n");

            if (delay.length > 1) {
                str = delay[1];
                if (str != null && !str.isEmpty()) {
                    if (str.indexOf('.') >= 0) {
                        str = str.substring(0, str.indexOf('.'));
                    } else {
                        str = str.substring(0, str.indexOf(' '));
                    }
                }
                Log.i("Pinger", "Ping: " + delay[1]);
            }

        } catch (IOException e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }

        return str;
    }

    public boolean isJobIdRunning(Context context, int JobId) {
        try {
            final JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
                if (jobInfo.getId() == JobId) {
                    return true;
                }
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }

        return false;
    }


    public void preloadBrandLogo(Context context, String url) {
        try {
           /* RequestOptions requestOptions =  new RequestOptions()
                    .diskCacheStrategyOf(DiskCacheStrategy.ALL);*/
            Glide.with(context).asBitmap()
                    .load(url)
                    .preload(500, 500);
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public String getConversationId(String cuid, String receiverCuid) {
        String hash = null;
        String target = cuid + receiverCuid + getCurrentIsoDateTime();
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(target.getBytes(), 0, target.length());
            hash = new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hash;
    }

    public boolean isNetworkBandwidthGood(Context activityContext) {
        final String str = PatchCommonUtil.getInstance().ping(activityContext, "www.google.com");
        if (str != null && !str.isEmpty()) {
            if (Integer.parseInt(str) >= 300) {
                //Log.d("Patch", "Bad Network");
                return false;
            } else {
                //Log.d("Patch", "good Network");
                return true;
            }
        } else {
            return false;
        }
    }

    public void startAudio(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(true);

            vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

            int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                ringtonePlayer = MediaPlayer.create(context, uri);
                ringtonePlayer.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .build());
                //ringtonePlayer.setAudioStreamType(AudioManager.STREAM_RING);
                int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
                ringtonePlayer.setVolume(currentVolume, currentVolume);
                ringtonePlayer.setLooping(true);
                ringtonePlayer.start();
                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                    long[] VIBRATE_PATTERN = { 500, 500 };
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // API 26 and above
                        vib.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, 0));
                    } else {
                        // Below API 26
                        vib.vibrate(VIBRATE_PATTERN , 0);
                    }
                }

            } else {
//                    Log.d(PatchIncomingActivity.class.getSimpleName(), "Failed to get audio focus");
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public void stopAudio(Context context) {
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            //MediaPlayer ringtonePlayer = MediaPlayer.create(context, uri);
            AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            audioManager.setSpeakerphoneOn(false);

            try {
                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
                    vib.cancel();
                }else {
                    vib.cancel();
                }
            }catch (Exception e){

            }

            ringtonePlayer.stop();
            ringtonePlayer.release();
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public void startTimer(final Context context, final JSONObject callDetails) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        CountDownTimer timer;
                        timer = new CountDownTimer(30000, 1000) {

                            @Override
                            public void onTick(long millisUntilFinished) {
                            }

                            @Override
                            public void onFinish() {
                                try {
                                    stopAudio(context);
                                    releaseAudio(context);
                                    callMissed(context, callDetails);
                                    if (context != null) {
                                        PatchIncomingFragment.getInstance().getActivity().finishAndRemoveTask();
                                    }
                                } catch (Exception e) {
                                    if (context != null)
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                                }

                            }
                        }.start();
                        SocketInit.getInstance().setIncomingTimer(timer);
                    } catch (Exception e) {

                    }
                }
            });
        } catch (Exception e) {

        }
    }

    public void releaseAudio(Context context) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.abandonAudioFocus(afChangeListener);
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public void callMissed(final Context context, JSONObject callDetails) {
        try {
//            try {
//                if(PatchCallingActivity.getInstance()!=null){
//                    PatchCallingActivity.getInstance().finish();
//                }
//            }catch (Exception e){
//
//            }
            String fromCuid = callDetails.getJSONObject("from").getString("cuid");
            String toCuid = callDetails.getJSONObject("to").getString("cuid");
            String callContext = callDetails.getString(context.getString(R.string.scontext));

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                NotificationHandler.CallNotificationHandler.getInstance(context).removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            }else {
                PatchCommonUtil.getInstance().dismissCallNotificationService(context);
            }

            if (fromCuid != null)
                NotificationHandler.CallNotificationHandler.getInstance(context).showCallNotification(
                        new JSONObject().
                                put("context", callContext).
                                put("fromCuid", fromCuid).
                                put("toCuid", toCuid),
                        NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);

            SocketInit socketInit = SocketInit.getInstance();
            String id = callDetails.getJSONObject("from").getString(context.getString(R.string.id));
            String accountId = callDetails.getString(context.getString(R.string.accountId));
            String call = callDetails.getString(context.getString(R.string.scall));
            String sid = callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "";

            JSONObject data = new JSONObject();
            data.put("responseSid", id + "_" + accountId);
            data.put("callId", call);
            data.put(context.getString(R.string.sid), sid);

            socketInit.getSocket().emit(context.getString(R.string.smiss), data, new Ack() {
                @Override
                public void call(Object... args) {
                    /*try {
                        AppUtil.getInstance().sendBroadcast(context, Constants.ACTION_CALL_MISSED);
                    }catch (Exception e){

                    }*/
                }
            });

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SocketInit.getInstance().setClientbusyOnVoIP(false);
                }
            }, 1000);
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public void callCancelled(Context context) {
        try {
            if (context != null) {
                PatchCommonUtil.getInstance().stopAudio(context);
                PatchCommonUtil.getInstance().releaseAudio(context);
                SocketInit.getInstance().getIncomingTimer().cancel();

            /*if (context != null) {
                PatchIncomingFragment.getInstance().getActivity().finishAndRemoveTask();
            }*/
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    NotificationHandler.CallNotificationHandler.getInstance(context).
                            removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                }else {
                    PatchCommonUtil.getInstance().dismissCallNotificationService(context);
                }
                try {
                    if (SocketInit.getInstance().getCallDetails() != null) {
                        //callMissed(context, SocketInit.getInstance().getCallDetails());
                        String fromCuid = SocketInit.getInstance().getCallDetails().getJSONObject("from").getString("cuid");
                        String toCuid = SocketInit.getInstance().getCallDetails().getJSONObject("to").getString("cuid");
                        String callContext = SocketInit.getInstance().getCallDetails().getString(context.getString(R.string.scontext));

                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            NotificationHandler.CallNotificationHandler.getInstance(context).removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                        }

                        if (fromCuid != null)
                            NotificationHandler.CallNotificationHandler.getInstance(context).showCallNotification(
                                    new JSONObject().
                                            put("context", callContext).
                                            put("fromCuid", fromCuid).
                                            put("toCuid", toCuid),
                                    NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);
                    }
                }catch (Exception e){

                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SocketInit.getInstance().setClientbusyOnVoIP(false);
                        } catch (Exception e) {
                        }
                    }
                }, 1000);
            }
        } catch (Exception e) {

        }

    }
    public void fcmCallDecline(Context context){
        try {
//            Log.d("patchsharma", "fcmCallDecline" );;
            SocketInit socketInit = SocketInit.getInstance();

            if (socketInit.getCallDetails() != null) {
                JSONObject callDetails = socketInit.getCallDetails();
                String id = callDetails.getJSONObject("from").getString(context.getString(R.string.id));
                String accountId = callDetails.getString(context.getString(R.string.accountId));
                String call = callDetails.getString(context.getString(R.string.scall));
                String sid = callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "";

                JSONObject data = new JSONObject();
                data.put("responseSid", id + "_" + accountId);
                data.put("callId", call);
                data.put(context.getString(R.string.sid), sid);
                data.put("reason", FcmCacheManger.getInstance().getRejectedReason());
                data.put("reasonCode", FcmCacheManger.getInstance().getRejectedReasonCode());
//                Log.d("patchsharma", "Call Declined via FCM\n" + data);

                socketInit.getSocket().emit(context.getString(R.string.sdecline), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                    }
                });
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        SocketInit.getInstance().setClientbusyOnVoIP(false);
                    } catch (Exception e) {

                    }
                }
            }, 1000);

        }catch (Exception e){

        }
    }
    public void calldeclined(final Context context) {
        try {
            if (context == null) {
                return;
            }
            //if incoming call is routed via FCM so it can be a chance that callee answered the call before getting authenticate that time
            // role of FcmCacheManger comes into play
            if(!SocketIOManager.isSocketConnected()){
                FcmCacheManger.getInstance().setRejected(true);
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                NotificationHandler.CallNotificationHandler.getInstance(context).
                        removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            }else{
                dismissCallNotificationService(context);
            }

            //AppUtil.getInstance().stopAudio(getActivity());;
            PatchCommonUtil.getInstance().stopAudio(context);
            PatchCommonUtil.getInstance().releaseAudio(context);
            SocketInit.getInstance().getIncomingTimer().cancel();

            if (context != null && PatchIncomingFragment.getInstance() != null) {
                PatchIncomingFragment.getInstance().getActivity().finishAndRemoveTask();
            }

            SocketInit socketInit = SocketInit.getInstance();
            if (socketInit.getCallDetails() != null) {
                JSONObject callDetails = socketInit.getCallDetails();
                String id = callDetails.getJSONObject("from").getString(context.getString(R.string.id));
                String accountId = callDetails.getString(context.getString(R.string.accountId));
                String call = callDetails.getString(context.getString(R.string.scall));
                String sid =  callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "";
                JSONObject data = new JSONObject();
                data.put("responseSid", id + "_" + accountId);
                data.put("callId", call);
                data.put(context.getString(R.string.sid), sid);

                socketInit.getSocket().emit(context.getString(R.string.sdecline), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                    }
                });

                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SocketInit.getInstance().setClientbusyOnVoIP(false);
                        } catch (Exception e) {

                        }
                    }
                }, 1000);
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context.getApplicationContext());
        }
    }


    public void lightUpScreen(Context context) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isScreenOn = pm.isScreenOn();
            Log.e("screen on.......", "" + isScreenOn);
            if (isScreenOn == false) {
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "Patch:MyLock");
                if (!wl.isHeld()) {
                    wl.acquire(30000);
                }
                PowerManager.WakeLock wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Patch:MyCpuLock");
                if (!wl_cpu.isHeld()) {
                    wl_cpu.acquire(30000);
                }
            }
        } catch (Exception e) {

        }
    }

    public void removeSession(Context context){
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("patch_session");
            editor.apply();
            editor.commit();
        }catch (Exception e){

        }
    }

    public int compareVersionNames(String oldVersionName, String newVersionName) {
        int res = 0;

        try {
            String[] oldNumbers = oldVersionName.split("\\.");
            String[] newNumbers = newVersionName.split("\\.");

            // To avoid IndexOutOfBounds
            int maxIndex = Math.min(oldNumbers.length, newNumbers.length);

            for (int i = 0; i < maxIndex; i ++) {
                int oldVersionPart = Integer.valueOf(oldNumbers[i]);
                int newVersionPart = Integer.valueOf(newNumbers[i]);

                if (oldVersionPart < newVersionPart) {
                    res = -1;
                    break;
                } else if (oldVersionPart > newVersionPart) {
                    res = 1;
                    break;
                }
            }

            // If versions are the same so far, but they have different length...
            if (res == 0 && oldNumbers.length != newNumbers.length) {
                res = (oldNumbers.length > newNumbers.length)?1:-1;
            }

            return res;
        }catch (Exception e){
            return res;

        }
    }

    public void removeNeverAskAgain(Context context){
        try {
            if (context != null){
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                //removing patch_never_ask_again from prefs if permission is granted somehow from permission settings.
                if(PatchCommonUtil.getInstance().hasPermissions(context, new String[]{Manifest.permission.RECORD_AUDIO})){
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.remove("patch_never_ask_again");
                    editor.apply();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public int getRunningTaskCount(Context context){
        try {
            ActivityManager mngr = (ActivityManager) context.getSystemService( ACTIVITY_SERVICE );
            return mngr.getAppTasks().size();
        }catch (Exception e){
            return -1;
        }
    }


    public Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, drawableId);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = (DrawableCompat.wrap(drawable)).mutate();
            }

            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }catch (Exception e){
            //Log.d("patchsharma", "ex : " + e.getMessage());
            return null;
        }
    }

    public void startstickyIncomingNotificationService(Context context, Intent incomingCallNotificationIntent) {
        try {
            //this code execute only for Android Q(10)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                context.startForegroundService(incomingCallNotificationIntent);
            } /*else {
            context.startService(incomingCallNotificationIntent);
        }*/
        }catch (Exception e){

        }
    }

    public void dismissCallNotificationService(Context context){
        try {
            if(CallNotificationService.getInstance()!=null){
//                try {
//                    CallNotificationService.getInstance().dismissNotification();
//                }catch (Exception e){
//
//                }
                Intent intent = new Intent(context, CallNotificationService.class);
                context.stopService(intent);
            }
        }catch (Exception e)
        {
        }
    }

    public void sendBroadcast(Context context, String action){
        try {
            Intent intent = new Intent(action);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }catch (Exception e){

        }
    }

    public void savePendingNeutralIntent(Context context, String intentPkg){
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("patch_neutralIntent", intentPkg);
            editor.apply();
            editor.commit();
        }catch (Exception e){

        }
    }
    public String getPendingNeutralIntent(Context context){
        SharedPreferences sharedPreferences;
        try {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPreferences.getString("patch_neutralIntent", null);
        }catch (Exception e){
            return null;
        }
    }
    public String getAccessToken(Context context){
        SharedPreferences sharedPreferences;
        try {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPreferences.getString("patch_accessToken", null);
        }catch (Exception e){
            return null;
        }
    }

    public String getDevToken(Context context){
        SharedPreferences sharedPreferences;
        try {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPreferences.getString("patch_devToken", null);
        }catch (Exception e){
            return null;
        }
    }
    public String getFcmToken(Context context){
        SharedPreferences sharedPreferences;
        try {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPreferences.getString("patch_fcmToken", null);
        }catch (Exception e){
            return null;
        }
    }
    public String getBaseUrl(Context context){
        SharedPreferences sharedPreferences;
        try {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            return sharedPreferences.getString(context.getString(R.string.prefs_baseurl), null);
        }catch (Exception e){
            return null;
        }
    }

    public void setFcmToken(Context context, String token){
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("patch_fcmToken", token);
            editor.apply();
            editor.commit();
        }catch (Exception e){
        }
    }
    public void removeDevToken(Context context){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("patch_devToken");
        editor.apply();
        editor.commit();
    }

    public void markNotificationDelivered(Context context) {
        try {
            SocketInit socketInit = SocketInit.getInstance();

            if (socketInit.getCallDetails() != null) {
                String fcmnNotificationId = FcmUtil.getInstance(context).getFcmNotificationID();
                JSONObject data = new JSONObject();
                data.put("notificationId", fcmnNotificationId);
                Log.d("PatchNotification", "emitting fcm_notification_ack :" + data);
                socketInit.getSocket().emit(context.getString(R.string.fcm_notification_ack), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                        Log.d("PatchNotification", "args are : "+ args[0]);
                    }
                });
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        SocketInit.getInstance().setClientbusyOnVoIP(false);
                    } catch (Exception e) {

                    }
                }
            }, 1000);

        }catch (Exception e){

        }
    }

    public List<MissedCallActions> getMissedCallReceiverActionsPref(Context context){
        List<MissedCallActions> missedCallActionsList = null;
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String missedCallActionsJson = sharedPref.getString(context.getString(R.string.patch_missedCallReceiverActions), null);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<MissedCallActions>>() {}.getType();
            missedCallActionsList = gson.fromJson(missedCallActionsJson, listType);
        }catch (Exception e){

        }
        return missedCallActionsList;
    }
    public List<MissedCallActions> getMissedCallInitiatorActionsPref(Context context){
        List<MissedCallActions> missedCallActionsList = null;
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String missedCallActionsJson = sharedPref.getString(context.getString(R.string.patch_missedCallInitiatorActions), null);
            Gson gson = new Gson();
            Type listType = new TypeToken<List<MissedCallActions>>() {}.getType();
            missedCallActionsList = gson.fromJson(missedCallActionsJson, listType);
        }catch (Exception e){

        }
        return missedCallActionsList;
    }

    public PatchTemplates.Pinview getPinviewConig(Context context){
        PatchTemplates.Pinview pinview = null;
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String json = sharedPref.getString(context.getString(R.string.patch_template_pinview_config), null);
            pinview = new Gson().fromJson(json, PatchTemplates.Pinview.class);
        }catch (Exception e){

        }
        return pinview;
    }

    public PatchTemplates.ScratchCard getScratchCardConfig(Context context){
        PatchTemplates.ScratchCard scratchCard = null;
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String json = sharedPref.getString(context.getString(R.string.patch_template_scratchcard_config), null);
            scratchCard = new Gson().fromJson(json, PatchTemplates.ScratchCard.class);
        }catch (Exception e){

        }
        return scratchCard;
    }


    public float dipToPx(Context context, float dipValue) {
        float density = context.getResources().getDisplayMetrics().density;
        return dipValue * density;
    }

    public void delayedHandler(int delay, Runnable runnable) {
        new Handler(Looper.getMainLooper()).postDelayed(runnable, delay);
    }
}
