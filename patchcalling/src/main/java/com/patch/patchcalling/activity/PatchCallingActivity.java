package com.patch.patchcalling.activity;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.patch.patchcalling.R;
import com.patch.patchcalling.broadcastreciever.CallNotificationActionReceiver;
import com.patch.patchcalling.fragments.PatchCallscreenFragment;
import com.patch.patchcalling.fragments.PatchIncomingFragment;
import com.patch.patchcalling.fragments.PatchOutgoingFragment;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.NotificationHandler.CallNotificationHandler;
import com.patch.patchcalling.utils.PatchLogger;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import static com.patch.patchcalling.fragments.PatchIncomingFragment.afChangeListener;

public class PatchCallingActivity extends AppCompatActivity {
    private Fragment mFragment;
    JSONObject callDetails;
    static AudioManager audioManager;
    private CallNotificationHandler callNotificationHandler;
    private String callContext;
    static PatchCallingActivity instance;
    private static Boolean isScreenOpened = false;
    Timer pingTimer;
    private Boolean previousSpeakerState = false;
    public static PatchCallingActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        try {


           /* if(isScreenOpened == true){
                isScreenOpened = false;
                finish();
            }
            isScreenOpened = true;*/



            final String screenType = getIntent().getStringExtra(getString(R.string.screen));
            SocketInit.getInstance().setCallDirection(screenType);

            if(screenType!=null && (screenType.equals(getString(R.string.incoming)))) {
                if(SocketInit.getInstance().isClientbusyOnVoIP() == false){
                    //client is not busy on any voip it means opening this callingActivity is not valid.(due to andoid 10 issue)
                    finish();
                    return;
                }
            }

            pingTimer = new Timer();
            if(screenType!=null && (screenType.equals(getString(R.string.incoming)))) {
                pingTimer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        try {

                            if(SocketInit.getInstance().isClientbusyOnVoIP() == false){
                                //client is not busy on any voip it means opening this callingActivity is not valid.(due to andoid 10 issue)
                                finish();
                                return;
                            }
                        }catch (Exception e){

                        }
                    }
                },0,100);
            }
        }catch (Exception e){

        }
//
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (getApplicationContext() != null ) {
//                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                    //removing patch_never_ask_again from prefs if permission is granted somehow from permission settings.
//                    if(AppUtil.getInstance().hasPermissions(getApplicationContext(), new String[]{Manifest.permission.RECORD_AUDIO})){
//                        SharedPreferences.Editor editor = sharedPref.edit();
//                        editor.remove("patch_never_ask_again");
//                        editor.apply();
//                    }
//                    Boolean isNeverAskAgain = sharedPref.getBoolean("patch_never_ask_again", false);
//                    //user has opted 'never ask again', so finishing call-screen right after emitting decline
//                    if (isNeverAskAgain /*|| SocketInit.getInstance().isClientbusyOnVoIP() || SocketInit.getInstance().isClientbusyOnPstn()*/) {
//                        String callData = getIntent().getStringExtra(getString(R.string.callDetails));
//                        callDetails = new JSONObject(callData);
//                        String id = callDetails.getJSONObject("from").getString(getString(R.string.id));
//                        String accountId = callDetails.getString(getString(R.string.accountId));
//                        String call = callDetails.getString(getString(R.string.scall));
//                        String sid = callDetails.has(getString(R.string.sid)) ? callDetails.getString(getString(R.string.sid)) : "";
//
//                        JSONObject data = new JSONObject();
//                        data.put("responseSid", id + "_" + accountId);
//                        data.put("callId", call);
//                        data.put(getString(R.string.sid), sid);
//                        if(isNeverAskAgain){
//                            data.put("reason", getString(R.string.microphone_permission_not_granted));
//                            data.put("reasonCode", 401);
//                        }
//                        if (SocketIOManager.getSocket() != null) {
//                            SocketIOManager.getSocket().emit(getApplication().getString(R.string.sdecline), data, new Ack() {
//                                @Override
//                                public void call(Object... args) {
//                                }
//                            });
//                        }
//                        finish(); //finish activity
//                        return;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            try{
//                if (getApplicationContext() != null)
//                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
//            }catch (Exception e1){
//
//            }
//        }

        try {
            setContentView(R.layout.activity_patch_outgoing);

            //------------------Setting channel is busy due to incoming call screen---------------------
            String screen = getIntent().getStringExtra(getString(R.string.screen));

            if(/*screen!=null && (screen.equals(getString(R.string.incoming)) || */screen.equals(getString(R.string.outgoing))){
                SocketInit.getInstance().setClientbusyOnVoIP(true);
            }

            final Window win = getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            previousSpeakerState = audioManager.isSpeakerphoneOn();

            callNotificationHandler = CallNotificationHandler.getInstance(this);

            SocketInit.getInstance().setAudioManager(audioManager);
            String callData = getIntent().getStringExtra(getString(R.string.callDetails));
            callContext = new JSONObject(callData).getString("context");
            SocketInit.getInstance().setCallContext(callContext);
            String sid = getIntent().hasExtra(getString(R.string.sid)) ? getIntent().getStringExtra(getString(R.string.sid)) : "";
            SocketInit.getInstance().setOutgoingActivity(PatchCallingActivity.this);
            if (screen.equals(getString(R.string.outgoing))) {
                callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext),
                        CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

                try {
                    if (audioManager.isSpeakerphoneOn()) {
                        audioManager.setSpeakerphoneOn(false);
                    }
                }catch (Exception e){

                }
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                mFragment = new PatchOutgoingFragment();
                Bundle args = new Bundle();
                args.putString(getString(R.string.callDetails), callData);
                mFragment.setArguments(args);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, mFragment, getString(R.string.outgoing_fragment_tag));
                ft.commit();
            } else if (screen.equals(getString(R.string.incoming))) {
                //callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext), CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);

                mFragment = new PatchIncomingFragment();
                Bundle args = new Bundle();
                args.putString(getString(R.string.callDetails), callData);
                args.putString(getString(R.string.sid), sid);
                try {
                    //this is the case when PatchIncomingFragment is not yet launched but user aswered the call via call-notification
                    if(getIntent()!=null && getIntent().hasExtra("call_answer")){
                        args.putString(getString(R.string.call_answer), getString(R.string.call_answer));
                    }
                }catch (Exception e){

                }
                mFragment.setArguments(args);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, mFragment, getString(R.string.incoming_fragment_tag));
                ft.commit();
            } else {

                try {
                    if (audioManager.isSpeakerphoneOn()) {
                        audioManager.setSpeakerphoneOn(false);
                    }
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

                }catch (Exception e){

                }
                callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext),
                        CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

                mFragment = new PatchCallscreenFragment();
                Bundle args = new Bundle();

                if(getIntent()!= null && getIntent().hasExtra("launchReason")
                        && getIntent().getStringExtra("launchReason").equals("401")){

                    args.putString("launchReason", "401");
                }
                args.putString(getString(R.string.callDetails), callData);
                mFragment.setArguments(args);
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, mFragment, getString(R.string.callScreen_fragment_tag));
                ft.commit();
            }
        } catch (Exception e) {
            try {
                if (getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            }catch (Exception e1){

            }
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            //isScreenOpened = false;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                if(CallNotificationActionReceiver.isAnswerClickEnabled == true){
                    CallNotificationActionReceiver.isAnswerClickEnabled = false;
                }
            }
            instance = null;
            SocketInit.getInstance().setClientbusyOnVoIP(false);


            try{
                if(pingTimer!=null){
                    pingTimer.cancel();
                }
                if (audioManager != null) {
                    if (audioManager.getMode() != AudioManager.MODE_NORMAL) {
                        this.audioManager.setMode(AudioManager.MODE_NORMAL);
                    }
                }
            }catch (Exception e){

            }
            try {
                Vibrator vib = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                vib.cancel();
            }catch (Exception e){
            }

            try {
                PatchCommonUtil.getInstance().stopAudio(getApplicationContext());
            }catch (Exception e){

            }
        } catch (Exception e) {
            try{
                if (getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
            }catch (Exception e1){

            }
        }
        try {
            if (audioManager != null) {
                if (audioManager.isSpeakerphoneOn() == previousSpeakerState) {
                    audioManager.setSpeakerphoneOn(previousSpeakerState);
                }
            }

            //audioManager.setStreamVolume(AudioManager.STREAM_MUSIC);
            releaseAudio();
        }catch (Exception e){

        }
        try{
            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                PatchCommonUtil.getInstance().dismissCallNotificationService(getApplicationContext());
            }else{
                callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            }
        }catch (Exception e){

        }

        /*try {
            AppUtil.getInstance().sendBroadcast(getApplicationContext(), Constants.ACTION_CALL_OVER);
        }catch (Exception e){

        }*/
    }

    private void releaseAudio() {
        try {

           /* if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                this.audioManager.abandonAudioFocusRequest(this.mAFRequest);
            } else {
                this.audioManager.abandonAudioFocus(afChangeListener);
            }*/
            if(this.audioManager != null){

             /*   if(audioManager.isSpeakerphoneOn()){
                    audioManager.setSpeakerphoneOn(false);
                }
                audioManager.setMode(AudioManager.MODE_NORMAL);*/
                this.audioManager.abandonAudioFocus(afChangeListener);
            }
        } catch (Exception e) {
           /* if (getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getApplicationContext());
  */      }
    }
}


