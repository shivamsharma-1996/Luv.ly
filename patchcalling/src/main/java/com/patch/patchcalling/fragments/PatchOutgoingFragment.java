package com.patch.patchcalling.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.patch.patchcalling.R;
import com.patch.patchcalling.broadcastreciever.CallNotificationActionReceiver;
import com.patch.patchcalling.interfaces.CallNotificationAction;
import com.patch.patchcalling.interfaces.CallStatus;
import com.patch.patchcalling.interfaces.IosApfInterface;
import com.patch.patchcalling.javaclasses.PatchSDK;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.CustomHandler;
import com.patch.patchcalling.utils.NotificationHandler;
import com.patch.patchcalling.utils.NotificationHandler.CallNotificationHandler;
import com.patch.patchcalling.utils.OutgoingUtil;
import com.patch.patchcalling.utils.PatchLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.socket.client.Ack;

import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.CallStatus.CALLEE_BUSY_ON_ANOTHER_CALL;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.CallStatus.CALL_ANSWERED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.CallStatus.CALL_DECLINED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.CallStatus.CALL_MISSED;
import static com.patch.patchcalling.javaclasses.PatchSDK.sdkReady;


//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class PatchOutgoingFragment extends Fragment implements CallStatus, ActivityCompat.OnRequestPermissionsResultCallback, CallNotificationAction {
    private static final String TAG = "PatchOutgoingFragment";
    View v;
    RelativeLayout llBackground;
    ImageView endCall, ivLogo;
    TextView tvContext, tvCallStatus, tvCallScreenLabel, tvPoweredBy, tvCalleeBusy;
    MediaPlayer mp;
    JSONObject callDetails;
    private String callContext;
    private CallNotificationHandler callNotificationHandler;
    Animation anim;
    private CustomHandler customHandler = CustomHandler.getInstance();
    //    CountDownTimer timer;
    Vibrator vibrator;
    private String fromCuid, toCuid;

    public PatchOutgoingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_patch_outgoing, container, false);
        endCall = v.findViewById(R.id.iv_decline);
        tvContext = v.findViewById(R.id.tv_context);
        tvCallStatus = v.findViewById(R.id.tv_callStatus);
        llBackground = v.findViewById(R.id.ll_background);
        tvCallScreenLabel = v.findViewById(R.id.tv_callScreen_label);
        tvCalleeBusy = v.findViewById(R.id.tv_busy_state);
        ivLogo = v.findViewById(R.id.iv_logo);
        tvPoweredBy = v.findViewById(R.id.tv_poweredBy);
        //registering this recieiver for future use
        try {
            vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

            anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(800);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);

            if (getActivity() != null)
                LocalBroadcastManager.getInstance(getActivity()).registerReceiver(ActivityReceiver,
                        new IntentFilter("com.patch.PatchOutgoingFragment"));
        } catch (Exception e) {

        }

        try {
            callNotificationHandler = CallNotificationHandler.getInstance(getActivity().getApplicationContext());
            CallNotificationActionReceiver.setCallNotificationActionListener(this);

            callDetails = new JSONObject(getArguments().getString("callDetails"));
            callContext = callDetails.getJSONObject("data").getString("context");
            tvContext.setText(callDetails.getJSONObject("data").getString("context"));
            fromCuid = callDetails.getJSONObject("data").getJSONObject("from").getString("cuid");
            toCuid = callDetails.getJSONObject("data").getJSONObject("to").getString("cuid");
            SocketInit.getInstance().setToCuid(toCuid);
            SocketInit.getInstance().setFromCuid(fromCuid);

        } catch (Exception e) {
            //e.printStackTrace()
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }

        try {
            SocketInit.getInstance().setCallStatus(PatchOutgoingFragment.this);

            setBranding();
            OutgoingUtil.getInstance().setOutgoingRingtone(getContext());
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }


        endCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sdkReady = true;
                hangupCall();

            }
        });
        return v;
    }

    public void hangupCall() {
        try {
            //Removing outgoing notification
            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

            SocketInit.getInstance().getTimer().cancel();
            SocketInit socketInit = SocketInit.getInstance();
            final String call = callDetails.getString(getString(R.string.scall));
            socketInit.getSocket().emit(getString(R.string.scancel), call, new Ack() {
                @Override
                public void call(Object... args) {
                    //Log.d("Patch", "Call Canceled");
                }
            });
            //mp.stop();
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                OutgoingUtil.getInstance().stopMediaPlayer(getActivity().getApplicationContext());

            getActivity().finish();
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            //e.printStackTrace()
        }
    }

    /**
     * set the branding of the view according to the data set by the owner on the dashboard.
     */
    private void setBranding() {
        Map<String, View> brandingViewParams = new HashMap<>();
        brandingViewParams.put("ivLogo", ivLogo);
        brandingViewParams.put("llBackground", llBackground);
        brandingViewParams.put("tvContext", tvContext);
        brandingViewParams.put("tvCallStatus", tvCallStatus);
        brandingViewParams.put("tvCallScreenLabel", tvCallScreenLabel);
        brandingViewParams.put("tvPoweredBy", tvPoweredBy);

        OutgoingUtil.getInstance().setBranding(getContext(), brandingViewParams, TAG);
    }

    /**
     * sets branding of account to the callDetails object to be sent to callScreen
     *
     * @param key:-   key of the object
     * @param value:- value of the object.
     */
    private void branding(String key, String value) {
        try {
            callDetails.put(key, value);
        } catch (JSONException e) {
            //e.printStackTrace()
        }
    }

    /**
     * starts the countdown timer of 2 seconds when user receiving an incoming call either declines or misses the call
     * in order to display the mentioned events to the caller.
     */
    private void startEndTimer(final int countDownTime) {
        new CountDownTimer(countDownTime, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
//                Log.d("Patch", "timer in patch outgoing screen " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                try {
                    sdkReady = true;
                    if (countDownTime == 10000) {
                        //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALLEE_BUSY_ON_ANOTHER_CALL);
                        //customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALLEE_BUSY_ON_ANOTHER_CALL);
                        //tvCalleeBusy.setVisibility(View.GONE);
                        tvCalleeBusy.clearAnimation();
                        SocketInit.getInstance().setCalleBusyOnAnotherCall(false);
                        //Removing outgoing notification
                        callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                        getActivity().finish();
                        if(vibrator!=null)
                            vibrator.cancel();
                    } else {
                        //Removing outgoing notification
                        callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                        getActivity().finish();
                    }
                } catch (Exception e) {
                    sdkReady = true;
                    //e.printStackTrace()
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                    try {
                        callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                    } catch (Exception e1) {

                    }
                }
            }
        }.start();
    }

    /**
     * called when incoming call is answered by the user. if answered, check whether the permission are given by the user
     * and goes to callScreenFragment.
     */
    @Override
    public void onAnswer() {
        try {
            String[] PERMISSIONS = {
                    android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    android.Manifest.permission.RECORD_AUDIO
            };
            if (getActivity() != null && getActivity().getApplicationContext() != null && PatchCommonUtil.getInstance().hasPermissions(getActivity().getApplicationContext(), PERMISSIONS)) {
                //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALL_ANSWERED);
                customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALL_ANSWERED);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //mp.stop();
                        try {
                            //Replacing incoming notification from ongoing notification
                            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                            if (callContext != null) {
                                callNotificationHandler.showCallNotification(
                                        new JSONObject().put("context", callContext),
                                        NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                            }

                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                OutgoingUtil.getInstance().stopMediaPlayer(getActivity().getApplicationContext());

//                            timer.cancel();
                            PatchCallscreenFragment callscreenFragment = new PatchCallscreenFragment();
                            Bundle args = new Bundle();
                            args.putString("callDetails", callDetails.toString());
                            callscreenFragment.setArguments(args);
                            FragmentManager fm = getFragmentManager();
                            FragmentTransaction ft = fm.beginTransaction();
                            ft.replace(R.id.fragment_container, callscreenFragment).addToBackStack(null);
                            ft.remove(PatchOutgoingFragment.this);
                            ft.commitAllowingStateLoss();
                        } catch (Exception e) {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                        }

                    }
                });
            } else {
                try {
                    SocketInit.getInstance().getTimer().cancel();
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        OutgoingUtil.getInstance().stopMediaPlayer(getActivity().getApplicationContext());
                    if (getActivity() != null) {
                        getActivity().finish();
                        try {
                            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                        } catch (Exception e) {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                        }
                    }
                } catch (Exception c) {
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(c.getMessage(), Log.getStackTraceString(c), getActivity().getApplicationContext());
                }
            }
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    public void iosAPF(String callId, final IosApfInterface iosApfInterface) {
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
                    //e.printStackTrace()
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                OutgoingUtil.getInstance().releaseMediaPlayer(getActivity().getApplicationContext());

            SocketInit.getInstance().setClientbusyOnVoIP(false);
            if (vibrator != null)
                vibrator.cancel();

        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (vibrator != null)
                vibrator.cancel();
        } catch (Exception e) {

        }
    }

    /**
     * called when incoming call is declined by the user. if declined, close the current view after 2 seconds
     */
    @Override
    public void onDecline() {
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (getActivity() != null && getActivity().getApplicationContext() != null)
                            OutgoingUtil.getInstance().stopMediaPlayer(getActivity().getApplicationContext());

                        if (SocketInit.getInstance().isCalleBusyOnAnotherCall()) {
                            tvCalleeBusy.setVisibility(View.VISIBLE);
                            tvCalleeBusy.setAnimation(anim);
                            startEndTimer(10000);
                            //AppUtil.getInstance().vibratePhone(getActivity());
                            long[] pattern = {0, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500};
                            if (vibrator != null)
                                vibrator.vibrate(pattern, 0);
                            customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALLEE_BUSY_ON_ANOTHER_CALL);
                        } else {
                            //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALL_DECLINED);
                            customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALL_DECLINED);
                            tvCallStatus.setText("Declined");
                            startEndTimer(2000);
                        }
                    }catch (Exception e){

                    }
                }
            });
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }

    }

    /**
     * called when incoming call is missed by the user. if missed, close the cureent view after 2 seconds.
     */
    @Override
    public void onMiss() {
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //mp.stop();
                    try {
                        if (getActivity() != null && getActivity().getApplicationContext() != null) {
                            OutgoingUtil.getInstance().stopMediaPlayer(getActivity().getApplicationContext());
                            //callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                        }
//                timer.cancel();
                        PatchSDK.sdkReady = true;
                        //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALL_MISSED);
                        customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALL_MISSED);
                        tvCallStatus.setText("Missed");
                        startEndTimer(2000);
                    }catch (Exception e){

                    }
                }
            });
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }

    }

    /**
     * called when trying to make pstn call if receiver is ios and autofallback is true.
     *
     * @param data
     */
    @Override
    public void onIosApf(String data) {
        //var yha notiification lagana hai;
        try {
            PatchCallscreenFragment callscreenFragment = new PatchCallscreenFragment();
            Bundle args = new Bundle();
            args.putString("callDetails", data);
            callscreenFragment.setArguments(args);
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.replace(R.id.fragment_container, callscreenFragment).addToBackStack(null);
            ft.remove(PatchOutgoingFragment.this);
            ft.commitAllowingStateLoss();
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            //e.printStackTrace()
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {

            try {
                if (getActivity() != null)
                    LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(ActivityReceiver);
            } catch (Exception e) {

            }
            if (callNotificationHandler.getCallNotificationStatus() != null &&
                    callNotificationHandler.getCallNotificationStatus().equals(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL))
                callNotificationHandler.rebuildNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    @Override
    public void onActionClick(String action) {
        try {
            if (action.equals("Hangup_Outgoing")) {
                sdkReady = true;
                hangupCall();
            }
        } catch (Exception e) {

        }
    }

    private BroadcastReceiver ActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            // Log.d("ActivityReceiver", "replaceFragment");

            try {
                String replaceAction = intent.getStringExtra("action");
                if (replaceAction != null && replaceAction.equals("finish")) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (getActivity() != null && getActivity().getApplicationContext() != null)
                                    OutgoingUtil.getInstance().stopMediaPlayer(getActivity().getApplicationContext());

                                sdkReady = true;
                                //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALL_DECLINED);
                                customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALL_DECLINED);
                                //tvCallStatus.setText("Falling back to PSTN call");
                                closeOutgoingScreen();
                                //startEndTimer();

                            }catch (Exception e){

                            }
                        }
                    });
                }
            } catch (Exception e) {
                //e.printStackTrace()
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());

            }
        }
    };

    private void closeOutgoingScreen() {
        try {
            callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
            getActivity().finish();
        } catch (Exception e) {
            //e.printStackTrace()
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            try {
                callNotificationHandler.removeNotification(CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
            } catch (Exception e1) {

            }
        }
    }
}
