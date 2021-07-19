package com.patch.patchcalling.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.patch.patchcalling.Constants;
import com.patch.patchcalling.R;
import com.patch.patchcalling.broadcastreciever.CallNotificationActionReceiver;
import com.patch.patchcalling.broadcastreciever.EarPieceIntentReceiver;
import com.patch.patchcalling.fcm.FcmUtil;
import com.patch.patchcalling.interfaces.CallNotificationAction;
import com.patch.patchcalling.interfaces.CallStatus;
import com.patch.patchcalling.interfaces.CallType;
import com.patch.patchcalling.javaclasses.PatchSDK;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.javaclasses.myJsInterface;
import com.patch.patchcalling.models.SelectedTemplate;
import com.patch.patchcalling.services.JobSchedulerSocketService;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.CustomHandler;
import com.patch.patchcalling.utils.NotificationHandler;
import com.patch.patchcalling.utils.OutgoingUtil;
import com.patch.patchcalling.utils.PatchLogger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.Context.POWER_SERVICE;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.CallStatus.CALL_ANSWERED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.CallStatus.CALL_DECLINED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.CallStatus.CALL_MISSED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CC_PHONE_MISSING_FOR_CUID_IN_PSTN_CALL;


public class PatchCallscreenFragment extends Fragment implements CallStatus.incomingCallStatus, SensorEventListener, CallType, CallNotificationAction {
    private static final String TAG = "PatchCallscreenFragment";
    private WebView webView;
    public static Boolean isSpeakerOn = false;
    private static ImageView iv_mute, ivHold;
    private static ImageView iv_speaker;
    private ImageView iv_hangup;
    private ImageView ivLogo;
    LinearLayout llMute, llSpeaker, llHold;
    ConstraintLayout llBackground;
    TextView tvContext, tvCallScreenLabel, tv_timer, tvPoweredBy, tvMute, tvDisconnect, tvHold, tvSpeaker, networkLatency;
    static TextView tvHoldState;
    String callDetails;
    View v;
    AudioManager audioManager;
    EarPieceIntentReceiver earPieceIntentReceiver;
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private static final int SENSOR_SENSITIVITY = 4;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private int field = 0x00000020;
    private WebView jsView;
    private Socket socket;
    JSONObject callData;
    private String callContext;
    public static boolean isMute = false, isHold = false, isHoldDueToPstn = false, isFragmentVisible = false, isPutOnHold = false;
    Chronometer chTimer;
    MediaPlayer mp;
    SharedPreferences sharedPref;
    Timer pingTimer;
    static Animation anim;
    private boolean isPstnCallAnswered = false;
    private NotificationHandler.CallNotificationHandler callNotificationHandler;
    private String callType, fromCuid, toCuid;
    private CustomHandler customHandler = CustomHandler.getInstance();
    static Vibrator vibrator;
    static PatchCallscreenFragment instance;
    private String templateType, templateUrl;
    private ImageView ivBannerImage;


    public PatchCallscreenFragment() {
        // Required empty public constructor
    }

    public static PatchCallscreenFragment getInstance() {
        return instance;
    }

    public static void putOnHold(final Boolean hold) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (isFragmentVisible) {
                            if (hold) {
                                isPutOnHold = true;
                                PatchCallscreenFragment.getInstance().jsView.loadUrl("javascript:PatchAndroid.mute()");
                                tvHoldState.setText("Call is put on hold");
                                tvHoldState.setVisibility(View.VISIBLE);
                                tvHoldState.startAnimation(anim);
                                ivHold.setEnabled(false);
                                long[] pattern = {0, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500, 100, 500};
                                vibrator.vibrate(pattern, -1);
                            } else {
                                isPutOnHold = false;
                                ivHold.setEnabled(true);
                                tvHoldState.setVisibility(View.GONE);
                                tvHoldState.setText("");
                                tvHoldState.clearAnimation();
                                //stopVibrate();
                                vibrator.cancel();
                                PatchCallscreenFragment.getInstance().jsView.loadUrl("javascript:PatchAndroid.unmute()");
                            }
                            getInstance().restoreMuteState();
                        }
                    } catch (Exception e) {

                    }
                }
            });
        } catch (Exception e) {
        }
    }

    private void restoreMuteState() {
        try {
            if (isMute) {
                jsView.loadUrl("javascript:PatchAndroid.mute()");
            } else {
                //jsView.loadUrl("javascript:PatchAndroid.unmute()");
            }
        } catch (Exception e) {

        }
    }

    // credits to @Heath Borders at http://stackoverflow.com/questions/20228800/how-do-i-validate-an-android-net-http-sslcertificate-with-an-x509trustmanager
    private Certificate getX509Certificate(SslCertificate sslCertificate) {
        try {
            Bundle bundle = SslCertificate.saveState(sslCertificate);
            byte[] bytes = bundle.getByteArray("x509-certificate");
            if (bytes == null) {
                return null;
            } else {
                try {
                    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                    return certFactory.generateCertificate(new ByteArrayInputStream(bytes));
                } catch (Exception e) {
                    return null;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_patch_call_screenfragement, container, false);
        try {
            webView = (WebView) v.findViewById(R.id.webview);
            tvHoldState = v.findViewById(R.id.tv_hold_state);
            tvContext = (TextView) v.findViewById(R.id.tv_context);
            tvCallScreenLabel = v.findViewById(R.id.tv_callScreen_label);
            tvPoweredBy = (TextView) v.findViewById(R.id.tv_poweredBy);
            tvMute = (TextView) v.findViewById(R.id.tv_mute);
            tvDisconnect = (TextView) v.findViewById(R.id.tv_disconnect);
            tvHold = v.findViewById(R.id.tv_hold);
            tvSpeaker = (TextView) v.findViewById(R.id.tv_speaker);
            iv_hangup = (ImageView) v.findViewById(R.id.iv_hangup);
            iv_mute = (ImageView) v.findViewById(R.id.iv_mute);
            ivHold = v.findViewById(R.id.iv_hold);
            llHold = v.findViewById(R.id.ll_hold);
            iv_speaker = (ImageView) v.findViewById(R.id.iv_speaker);
            ivLogo = (ImageView) v.findViewById(R.id.iv_callScreen_logo);
            llBackground = (ConstraintLayout) v.findViewById(R.id.ll_callscreen_background);
            llMute = (LinearLayout) v.findViewById(R.id.ll_mute);
            llSpeaker = (LinearLayout) v.findViewById(R.id.ll_speaker);
            chTimer = (Chronometer) v.findViewById(R.id.tv_timer);
            networkLatency = (TextView) v.findViewById(R.id.tv_networkLatency);
            audioManager = SocketInit.getInstance().getAudioManager();
            pingTimer = new Timer();
            vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
            anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(800); //You can manage the blinking time with this parameter
//        anim.setStartOffset(20);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            tvCallScreenLabel.startAnimation(anim);

            if (audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(false);
            }
        } catch (Exception e) {

        }
        try {
            earPieceIntentReceiver = new EarPieceIntentReceiver();
        } catch (Exception e) {

        }
        try {
            instance = this;
            isFragmentVisible = true;
            isHold = false;
            isMute = false;
            isPutOnHold = false;
            // Yeah, this is hidden field.
            callNotificationHandler = NotificationHandler.CallNotificationHandler.getInstance(getActivity().getApplicationContext());
            CallNotificationActionReceiver.setCallNotificationActionListener(this);

            //field = PowerManager.class.getClass().getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);

            powerManager = (PowerManager) getContext().getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "patch:wakelocktag");
            mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);

            mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        } catch (Exception e) {

//            ignored.printStackTrace();
        }
        try {
            if (getArguments() != null && getArguments().getString("launchReason") != null &&
                    getArguments().getString("launchReason").equals("401")) {
                tvCallScreenLabel.setText("Retrying");
            }
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
        try {
            callDetails = getArguments().getString(getString(R.string.callDetails));
            SocketInit socketInit = SocketInit.getInstance();
            socketInit.setCallData(callDetails);
            try {
                callData = new JSONObject(callDetails);

                if (callData.has(getString(R.string.active_template)) && callData.get(getString(R.string.active_template)) instanceof JSONObject) {
                    Gson gson = new Gson();
                    SelectedTemplate selectedTemplate = gson.fromJson(
                            callData.getJSONObject(getString(R.string.active_template)).toString(),
                            SelectedTemplate.class);
                    if (selectedTemplate.getType() != null && selectedTemplate.getUrl() != null) {
                        templateType = selectedTemplate.getType();
                        templateUrl = selectedTemplate.getUrl();

                        if (templateType.equals(getString(R.string.template_scratchcard))) {
                            ivBannerImage = v.findViewById(R.id.iv_banner);
                            Glide.with(this)
                                    .load(templateUrl)
                                    .into(ivBannerImage);
                        }
                    }
                }
                    if (callData.has("context")) {
                        callContext = callData.getString("context");
                    }
                    if (callData.has("callType")) {
                        callType = callData.getString("callType");
                    }

                    try {
                        if (callType.equals("incoming")) {
                            //tvCallStatus.setText("Ongoing Call");
                            fromCuid = callData.getJSONObject("from").getString("cuid");
                            toCuid = callData.getJSONObject("to").getString("cuid");
                        } else if (callType.equals("outgoing")) {
                            fromCuid = callData.getJSONObject("data").getJSONObject("from").getString("cuid");
                            toCuid = callData.getJSONObject("data").getJSONObject("to").getString("cuid");
                        }
                        SocketInit.getInstance().setFromCuid(fromCuid);
                        SocketInit.getInstance().setToCuid(toCuid);
                    } catch (Exception e) {

                    }
                    if (callData.has("data")) {
                        if (callData.getJSONObject("data").has("pstn") && callData.getJSONObject("data").getBoolean("pstn")) {
                            //when a pstn call is dialling
                            tvCallScreenLabel.setText("Calling...");
                        }
                    } else {
                        if (callData.has("pstn") && callData.getBoolean("pstn")) {
                            tvCallScreenLabel.setText("Calling...");
                        }
                    }
                } catch(JSONException e){
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                }
//        checkSignalStrength();
                setBranding();

                webView.setBackgroundColor(0);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setLoadsImagesAutomatically(true);
                webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                webView.getSettings().setAppCacheEnabled(true);
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setAllowContentAccess(true);
                webView.getSettings().setDomStorageEnabled(true);
                webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
                webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
                webView.setScrollbarFadingEnabled(false);
                WebView.setWebContentsDebuggingEnabled(true);
                webView.addJavascriptInterface(new myJsInterface(getContext(), audioManager), "Android");
                webView.loadUrl(getString(R.string.jsurl));
                try {
                    mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
                } catch (Exception e) {
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                }

                // Get cert from raw resource...
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = getResources().getAssets().open("root_ca.crt"); // stored at \app\src\main\res\raw
                //Log.d("patchsharma", "caInput" + caInput);
                final Certificate certificate = cf.generateCertificate(caInput);
                caInput.close();
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        //Log.d("patchsharma", "onPageFinished");
                        try {
                            jsView = view;
                            view.loadUrl("javascript:PatchAndroid.init()");
                            initCallSock();
                            tvContext.setText(callData.getString("context"));
                        } catch (Exception e) {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                        }
                    }

                    @Override
                    public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                        // Get cert from SslError
                        //Log.d("patchsharma", "1");
                        try {
                            SslCertificate sslCertificate = error.getCertificate();
                            Certificate cert = getX509Certificate(sslCertificate);
                            if (cert != null && certificate != null) {
                                try {
                                    // Reference: https://developer.android.com/reference/java/security/cert/Certificate.html#verify(java.security.PublicKey)
                                    cert.verify(certificate.getPublicKey()); // Verify here...
                                    handler.proceed();
                                } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException e) {
                                    //super.onReceivedSslError(view, handler, error);
                                    //Log.d("patchsharma", "2");
                                    if (handler != null) handler.cancel();
                                    //e.printStackTrace();
                                }
                            } else {
                                handler.cancel();
                                //super.onReceivedSslError(view, handler, error);
                            }
                        } catch (Exception e) {
                            //Log.d("patchsharma", "3");
                        }
//                    try {
//                        switch (error.getPrimaryError()) {
//                            case SslError.SSL_UNTRUSTED:
//                                handler.proceed();
//                                break;
//                            case SslError.SSL_EXPIRED:
//                            case SslError.SSL_IDMISMATCH:
//                            case SslError.SSL_NOTYETVALID:
//                            case SslError.SSL_DATE_INVALID:
//                            case SslError.SSL_INVALID:
//                            default:
//                                handler.cancel();
//                                break;
//                        }
//                    } catch (Exception e) {
//
//                    }
                    }
                });

                pingTimer.scheduleAtFixedRate(
                        new TimerTask() {
                            public void run() {
                                try {
                                    if (getActivity() != null && getActivity().getApplicationContext() != null) {
                                        String str = PatchCommonUtil.getInstance().ping(getActivity().getApplicationContext(), "www.google.com");
                                        if (str != null && !str.isEmpty()) {
                                            if (Integer.parseInt(str) > 300 && Integer.parseInt(str) < 400) {
                                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        chTimer.startAnimation(anim);
                                                        networkLatency.setVisibility(View.VISIBLE);
                                                        networkLatency.setText("Bad Network");
                                                    }
                                                });
                                            } else if (Integer.parseInt(str) >= 400) {
                                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        chTimer.startAnimation(anim);
                                                        networkLatency.setVisibility(View.VISIBLE);
                                                        networkLatency.setText("Poor Network");
                                                    }
                                                });
                                            } else {
                                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        chTimer.clearAnimation();
                                                        networkLatency.setVisibility(View.GONE);
                                                        networkLatency.setText("");
                                                    }
                                                });

                                            }
                                        }
                                    }
                                } catch (Exception e) {

                                }
                            }
                        },
                        0,
                        5000);
                webView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onPermissionRequest(final PermissionRequest request) {
                        request.grant(request.getResources());
                    }
                });

                iv_hangup.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PatchSDK.sdkReady = true;
                        hangupCall();
                    }
                });

                iv_speaker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            //jsView.loadUrl("javascript:PatchAndroid.isOnHold()");

                            if (audioManager.isSpeakerphoneOn()) {
                                isSpeakerOn = false;
                                audioManager.setSpeakerphoneOn(false);
                                iv_speaker.setImageResource(R.drawable.speaker);
                            } else {
                                isSpeakerOn = true;
                                audioManager.setSpeakerphoneOn(true);
                                iv_speaker.setImageResource(R.drawable.speaker_active);
                            }
                        } catch (Exception e) {

                        }
                    }
                });

                iv_mute.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switchMuteState();
                    }
                });

                ivHold.setEnabled(true);
                ivHold.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            if (!isHoldDueToPstn && !isPutOnHold) {
                                if (isHold) {
                                    jsView.loadUrl("javascript:PatchAndroid.unmute()");
                                    ivHold.setImageResource(R.drawable.ic_hold_inactive);
                                    isHold = false;
                                    tvHoldState.setVisibility(View.GONE);
                                    tvHoldState.clearAnimation();
                                    emitHoldOnSigsock(false);
                                } else {
                                    jsView.loadUrl("javascript:PatchAndroid.mute()");
                                    ivHold.setImageResource(R.drawable.ic_hold);
                                    isHold = true;
                                    tvHoldState.setVisibility(View.VISIBLE);
                                    tvHoldState.setText("Call is on hold");
                                    tvHoldState.startAnimation(anim);
                                    emitHoldOnSigsock(true);
                                }
                                restoreMuteState();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {

            }
            return v;
        }

        public static void setSpeakerOff () {
            try {
                isSpeakerOn = false;
                iv_speaker.setImageResource(R.drawable.speaker);
            } catch (Exception e) {

            }
        }

        public void emitHoldOnSigsock (Boolean hold){
            try {
                if (getActivity() != null) {
                    if (callType.equals("incoming")) {
                        JobSchedulerSocketService.getInstance(getActivity()).emitHoldOnSigSock(hold, fromCuid);
                    } else if (callType.equals("outgoing")) {
                        JobSchedulerSocketService.getInstance(getActivity()).emitHoldOnSigSock(hold, toCuid);
                    }
                }
            } catch (Exception e) {

            }
        }

        public static void switchMuteState () {
            try {
                if (!/*isHold*/isHoldDueToPstn && !isPutOnHold && !isHold) {
                    if (isMute) {
                        isMute = false;
                        PatchCallscreenFragment.getInstance().jsView.loadUrl("javascript:PatchAndroid.unmute()");
                        iv_mute.setImageResource(R.drawable.mute);
                    } else {
                        isMute = true;
                        PatchCallscreenFragment.getInstance().jsView.loadUrl("javascript:PatchAndroid.mute()");
                        iv_mute.setImageResource(R.drawable.mute_active);
                    }
                }
            } catch (Exception e) {
            }
        }

        public static void switchHoldState () {
            try {
                if (isHoldDueToPstn) {
                    isHoldDueToPstn = false;
                    PatchCallscreenFragment.getInstance().jsView.loadUrl("javascript:PatchAndroid.unmute()");
                    ivHold.setImageResource(R.drawable.hold);
                    tvHoldState.setVisibility(View.GONE);
                    tvHoldState.clearAnimation();
                    getInstance().emitHoldOnSigsock(false);
                } else {
                    isHoldDueToPstn = true;
                    PatchCallscreenFragment.getInstance().jsView.loadUrl("javascript:PatchAndroid.mute()");
                    ivHold.setImageResource(R.drawable.hold_active);
                    tvHoldState.setVisibility(View.VISIBLE);
                    tvHoldState.setText("Call is on hold");
                    tvHoldState.startAnimation(anim);
                    getInstance().emitHoldOnSigsock(true);
                }
            } catch (Exception e) {

            }
        }

        public void closeCallScreen () {
            try {
                callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                OutgoingUtil.getInstance().releaseMediaPlayer(getActivity().getApplicationContext());
                            getActivity().finishAndRemoveTask();
                            SocketInit.getInstance().setClientbusyOnVoIP(false);
                        } catch (Exception e) {

                        }
                    }
                });
            } catch (Exception e) {

            }

        }

        private void hangupCall () {
            try {

                callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

                if (callData.has("data") && callData.getJSONObject("data").getBoolean("pstn") && !isPstnCallAnswered) {
                    JSONObject tocall = callData.getJSONObject("to");
                    socket.emit("cancel_pstn", callData.getJSONObject("to"), new Ack() {
                        @Override
                        public void call(Object... args) {
                            try {
                                JSONObject pstnCallback = (JSONObject) args[0];
                                boolean status = pstnCallback.getBoolean("status");
                                if (status) {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                if (getActivity() != null && getActivity().getApplicationContext() != null)
                                                    OutgoingUtil.getInstance().releaseMediaPlayer(getActivity().getApplicationContext());
                                                getActivity().finishAndRemoveTask();
                                                SocketInit.getInstance().setClientbusyOnVoIP(false);
                                            } catch (Exception e) {

                                            }
                                        }
                                    });
                                }
                            } catch (Exception e) {
                                if (getActivity() != null && getActivity().getApplicationContext() != null)
                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                            }
                        }
                    });
                } else {
                    jsView.loadUrl("javascript:PatchAndroid.hangup()");
                    SocketInit.getInstance().setClientbusyOnVoIP(false);
                    try {
                        PatchCommonUtil.getInstance().sendBroadcast(getActivity().getApplicationContext(), Constants.ACTION_CALL_OVER);
                    } catch (Exception e) {

                    }
                }
            } catch (Exception e) {
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            }
        }

        /**
         * set the branding of the view according to the data set by the owner on the dashboard.
         */
        private void setBranding () {
            Map<String, View> brandingViewParams = new HashMap<>();
            brandingViewParams.put("ivLogo", ivLogo);
            brandingViewParams.put("llBackground", llBackground);
            brandingViewParams.put("tvContext", tvContext);
            brandingViewParams.put("tvMute", tvMute);
            brandingViewParams.put("tvDisconnect", tvDisconnect);
            brandingViewParams.put("tvHold", tvHold);
            brandingViewParams.put("tvSpeaker", tvSpeaker);
            brandingViewParams.put("tvPoweredBy", tvPoweredBy);
            brandingViewParams.put("chTimer", chTimer);
            brandingViewParams.put("tvCallScreenLabel", tvCallScreenLabel);
            brandingViewParams.put("tvNetworkLatency", networkLatency);
            brandingViewParams.put("tvHoldState", tvHoldState);

            OutgoingUtil.getInstance().setBranding(getContext(), brandingViewParams, TAG);
        }

        /**
         * initialize the callsock.
         *
         * @throws JSONException
         */

        private void initCallSock () throws JSONException {
            try {
                SocketInit.getInstance().setCallType(PatchCallscreenFragment.this);
                IO.Options opts = new IO.Options();
                opts.transports = new String[]{getString(R.string.websocket)};
                opts.forceNew = true;
                opts.reconnection = true;
                opts.reconnectionAttempts = 10;
                opts.reconnectionDelay = 1500;
                opts.reconnectionDelayMax = 6000;
                opts.timeout = 30000;
                opts.forceNew = false;
                opts.query = getContext().getString(R.string.jwt) + callData.getString("token");
                String url = "https://" + callData.getString("host") + ":3001";
                socket = IO.socket(url, opts);
                setUpEventHandlers();
            } catch (Exception e) {
                //e.printStackTrace();
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            }
        }

        /**
         * registers all the events/hnadlers of callsock.
         *
         * @throws JSONException
         */
        private void setUpEventHandlers () throws JSONException {
            try {
                final JSONObject authData = new JSONObject();
                authData.put("platform", "android");
                authData.put("accountId", callData.getString("accountId"));
                authData.put("apikey", callData.getString("apikey"));
                authData.put("cc", callData.getString("cc"));
                authData.put("phone", callData.getString("phone"));
                authData.put("callId", callData.getString("call"));
                authData.put("cuid", callData.getString("cuid"));
                authData.put("cli", SocketInit.getInstance().getCli());
                socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {

                    }
                })
                        .on(getContext().getString(R.string.sconnect), new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                try {
                                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                                        socket.emit(getActivity().getApplicationContext().getString(R.string.sauthentication), authData);
                                } catch (Exception e) {
                                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                                }
                            }
                        })
                        .on(getContext().getString(R.string.sauthenticated), new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                            }
                        })
                        .on("disconnect", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                            }
                        })
                        .on("socket-timeout", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                try {

                                } catch (Exception e) {

                                }
                            }
                        })
                        .on("endpoint_ready", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                try {
                                    if (callData.has("data")) {
                                        if (callData.getJSONObject("data").getBoolean("pstn")) {
                                            callPstn();
                                        } else {
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    jsView.loadUrl("javascript:PatchAndroid.initJSSIP()");

                                                }
                                            });
                                        }
                                    } else {
                                        if (callData.getBoolean("pstn")) {
                                            callPstn();
                                        } else {
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    jsView.loadUrl("javascript:PatchAndroid.initJSSIP()");
                                                }
                                            });
                                        }
                                    }

                                } catch (Exception e) {
                                    //e.printStackTrace();
                                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                                }
                            }
                        })
                        .on("call_status", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                try {
                                    JSONObject callStatus = (JSONObject) args[0];
                                    if (callStatus.has("alone") && callStatus.getBoolean("alone")) {
                                        overCallOnSip();
                                    } else if (callStatus.has("left")) {
                                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                                        String leftEndpoint = callStatus.getJSONObject("left").has("endpoint") ?
                                                callStatus.getJSONObject("left").getString("endpoint") : "";
                                        if (leftEndpoint.equals(sharedPref.getString("patch_cuid", ""))) {
                                            overCallOnSip();
                                        }
                                    }
                                } catch (Exception e) {
                                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                                }
                            }
                        })
                        .on(getContext().getString(R.string.sconnection_timeout), new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
//                        Log.d(tvContext.getClass().getSimpleName(), args.toString());
                            }
                        })
                        .on(Socket.EVENT_RECONNECT, new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {

                            }
                        })
                        .on(Socket.EVENT_RECONNECT_ATTEMPT, new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                try {
                                    if ((Integer) args[0] > 5 && socket != null) {
                                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    jsView.loadUrl("javascript:PatchAndroid.hangup()");
                                               /* try {
                                                    AppUtil.getInstance().sendBroadcast(getActivity().getApplicationContext(), Constants.ACTION_CALL_OVER);
                                                }catch (Exception e){

                                                }*/
                                                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                                                } catch (Exception e) {

                                                }

                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                                }
                            }
                        })
                        .on(Socket.EVENT_RECONNECTING, new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {

                            }
                        })
                        .on(Socket.EVENT_RECONNECT_FAILED, new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                try {
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            jsView.loadUrl("javascript:PatchAndroid.hangup()");
                                        }
                                    });
                                    try {
                                        PatchCommonUtil.getInstance().sendBroadcast(getActivity().getApplicationContext(), Constants.ACTION_CALL_OVER);
                                    } catch (Exception e) {

                                    }
                                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                                } catch (Exception e) {
                                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                                }
                            }
                        })
                        .on(Socket.EVENT_RECONNECT_ERROR, new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
//                            try {
////                                new Handler(Looper.getMainLooper()).post(new Runnable() {
////                                    @Override
////                                    public void run() {
////                                        jsView.loadUrl("javascript:PatchAndroid.hangup()");
////                                        /*try {
////                                            AppUtil.getInstance().sendBroadcast(getActivity().getApplicationContext(), Constants.ACTION_CALL_OVER);
////                                        }catch (Exception e){
////
////                                        }*/
////                                    }
////                                });
//                                //callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
//                            } catch (Exception e) {
//                                if (getActivity() != null && getActivity().getApplicationContext() != null)
//                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
//                            }
                            }
                        })
                        .on(getContext().getString(R.string.sreconnect_failed), new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                            }
                        });
                if (!socket.connected()) {
                    socket.connect();
                }
            } catch (Exception e) {

            }
        }

        /**
         * check if all the permissions are given by the user before making a pstn call..
         */
        private void callPstn () {
            try {
                int PERMISSION_ALL = 1;
                String[] PERMISSIONS = {
                        android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                        android.Manifest.permission.RECORD_AUDIO
                };
                if (PatchCommonUtil.getInstance().hasPermissions(getContext(), PERMISSIONS)) {
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(PERMISSIONS, PERMISSION_ALL);
                }*/
                    startPSTN();
                } else {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            getActivity().finishAndRemoveTask();
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                try {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            getActivity().finishAndRemoveTask();
                        }
                    });
                } catch (Exception e1) {
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(e1.getMessage(), Log.getStackTraceString(e1), getActivity().getApplicationContext());
                }
            }
        }

        /**
         * starts the outgoing tone when user is trying to make a outgoing call, and emits a call_pstn on callsock to make
         * a PSTN call to the receiver.
         *
         * @throws JSONException
         */
        private void startPSTN () throws JSONException {
            JSONObject tocall = callData.getJSONObject("to");
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        OutgoingUtil.getInstance().setOutgoingRingtone(getContext());
                    }
                });
            } catch (Exception e) {
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            }
            if (socket != null)
                socket.emit("call_pstn", tocall, new Ack() {
                    @Override
                    public void call(Object... args) {
                        try {
                            JSONObject pstnCallback = (JSONObject) args[0];
                            boolean status = pstnCallback.getBoolean("status");
                            if (status) {

                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvCallScreenLabel.setText("Ongoing Call");
                                    }
                                });
                                callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);

                                if (callContext != null) {
                                    callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext)
                                            , NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                                }


                                //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALL_ANSWERED);
                                customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALL_ANSWERED);

                                isPstnCallAnswered = true;
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
//                                Log.d("Patch", "got pstn callback");
                                        if (getActivity() != null && getActivity().getApplicationContext() != null)
                                            OutgoingUtil.getInstance().releaseMediaPlayer(getActivity().getApplicationContext());
                                        jsView.loadUrl("javascript:PatchAndroid.initJSSIP()");
                                    }
                                });
                            } else {
                                String error = pstnCallback.getString("error");
//                        if (error.equals("declined") || error.equals("missed")) {
                                //Log.d("PatchPSTNCALLBACK", error);
                                if (error.equals("declined")) {
                                    PatchSDK.sdkReady = true;
                                    //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALL_DECLINED);
                                    customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALL_DECLINED);
                                } else if (error.equals("missed")) {
                                    PatchSDK.sdkReady = true;
                                    //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALL_MISSED);
                                    customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALL_MISSED);
                                } else if (error.equals("Invalid or missing cc/phone")) {
                                    PatchSDK.sdkReady = true;
                                    //SocketInit.getInstance().getOutgoingCallResponse().onFailure(ERR_CC_PHONE_MISSING_FOR_CUID_IN_PSTN_CALL);
                                    customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.ON_FAILURE, ERR_CC_PHONE_MISSING_FOR_CUID_IN_PSTN_CALL);
                                }
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
//                                Log.d("Patch", "exiting webview");
                                        try {
                                            if (getActivity() != null && getActivity().getApplicationContext() != null) {
                                                if (callNotificationHandler.getCallNotificationStatus().equals(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL))
                                                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                                                OutgoingUtil.getInstance().releaseMediaPlayer(getActivity().getApplicationContext());
                                                getActivity().finishAndRemoveTask();
                                            }
                                        } catch (Exception e) {
                                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                                        }
                                    }
                                });
                            }
//                    }
                        } catch (Exception e) {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                        }
                    }
                });
        }

        /**
         * starts the call timer to be displayed on the UI.
         */
        private void startTimer () {
            try {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            chTimer.setBase(SystemClock.elapsedRealtime());
                            chTimer.start();
                        } catch (Exception e) {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                        }
                    }
                });
            } catch (Exception e) {

            }
        }

        @Override
        public void onCancel () {
            try {
                if (getActivity() != null) {
                    if (webView != null) {
                        webView.destroy();
                        webView = null;

                    }
                    getActivity().finishAndRemoveTask();
                    SocketInit.getInstance().setClientbusyOnVoIP(false);
                }
            } catch (Exception e) {
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            }

        }

        @Override
        public void onPause () {
            super.onPause();
            try {
                vibrator.cancel();
            } catch (Exception e) {

            }
        }

        @Override
        public void onDestroyView () {
            super.onDestroyView();
            try {
                //releaseAudio();
            } catch (Exception e) {

            }
            try {
                vibrator.cancel();
            } catch (Exception e) {

            }
            try {
                isFragmentVisible = false;

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            chTimer.stop();
                            chTimer.setText("00:00");
                        } catch (Exception e) {

                        }
                    }
                });
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    OutgoingUtil.getInstance().releaseMediaPlayer(getActivity().getApplicationContext());
                pingTimer.cancel();
                SocketInit.getInstance().setRecording(false);
                //SocketInit.getInstance().setCallId("");
                socket.disconnect();
                socket.close();
                mSensorManager.unregisterListener(this);
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }

//            webView.clearCache(true);
                webView.removeAllViews();
                webView.destroyDrawingCache();
                webView.destroy();
                webView = null;
            } catch (Exception e) {
                try {
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                } catch (Exception e1) {

                }
            }
        }


        @Override
        public void onSensorChanged (SensorEvent event){
            try {
                if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    if (event.values[0] >= -SENSOR_SENSITIVITY && event.values[0] <= SENSOR_SENSITIVITY) {
                        //near
                        if (!wakeLock.isHeld()) {
                            wakeLock.acquire();
                        }
                    } else {
                        //far
                        if (wakeLock.isHeld()) {
                            wakeLock.release();
                        }
                    }
                }
            } catch (Exception e) {
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            }
        }

        @Override
        public void onAccuracyChanged (Sensor sensor,int accuracy){

        }

        /**
         * emits call_voip on callsock once the user is registered on asterisk to get a incoming call from asterisk.
         */
        @Override
        public void callVoip () {
            if (socket != null) {
                socket.emit("call_voip", "", new Ack() {
                    @Override
                    public void call(Object... args) {
                        try {
                            JSONObject data = (JSONObject) args[0];
                            Boolean status = data.getBoolean("status");
                            if (status) {
                                if (SocketInit.getInstance().getRecording() != null && SocketInit.getInstance().getRecording()) {
                                    socket.emit("record_call", callData.getString("call"), new Ack() {
                                        @Override
                                        public void call(Object... args) {
                                        }
                                    });
                                }
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            tvCallScreenLabel.clearAnimation();
                                            tvCallScreenLabel.setText("Ongoing Call");
                                            llMute.setVisibility(View.VISIBLE);
                                            llSpeaker.setVisibility(View.VISIBLE);
                                            llHold.setVisibility(View.VISIBLE);
                                            chTimer.setVisibility(View.VISIBLE);
                                            if (templateType!=null &&
                                                    templateType.equals(getString(R.string.template_scratchcard))
                                            && SocketInit.getInstance().isScratchCardScratched()) {
                                                ivBannerImage.setVisibility(View.VISIBLE);
                                            }
                                            if (callContext != null) {
                                                callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext), NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                                            }
                                        } catch (Exception e) {

                                        }
                                    }
                                });
                                chTimer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
                                    @Override
                                    public void onChronometerTick(Chronometer chronometer) {
                                        //chTimer = chronometer;
                                    }
                                });

                                startTimer();
                            } else {
                                overCallOnSip();
                            }
                        } catch (Exception e) {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                        }
                    }
                });
            }
        }

        /*Each time as the app goes to background,it makes popup ongoing notification*/
        @Override
        public void onStop () {
            super.onStop();
            try {
                if (callNotificationHandler.getCallNotificationStatus() != null) {
                    if (callNotificationHandler.getCallNotificationStatus().equals(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL)) {
                        callNotificationHandler.rebuildNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                    } else if (callNotificationHandler.getCallNotificationStatus().equals(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL)) {
                        callNotificationHandler.rebuildNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.OUTGOING_CALL);
                    }
                }

            } catch (Exception e) {
                if (getActivity() != null && getActivity().getApplicationContext() != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
            }
        }


        @Override
        public void onResume () {
            super.onResume();
            try {
                if (FcmUtil.getInstance(getContext()).getExpiredCallId() != null &&
                        FcmUtil.getInstance(getContext()).getExpiredCallId().equals(callData.getString("call"))) {
                    closeCallScreen();
                    if (getActivity() != null) {
                        FcmUtil.getInstance(getContext()).updateCallStatus(getActivity().getApplicationContext(), FcmUtil.getInstance(getContext()).getExpiredCallId());
                    }
                }
            } catch (Exception e) {

            }
            try {
                IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
                if (getActivity() != null) {
                    getActivity().registerReceiver(earPieceIntentReceiver, filter);
                }
            } catch (Exception e) {

            }

        }

        @Override
        public void onDestroy () {
            super.onDestroy();
            try {
                if (getActivity() != null) {
                    getActivity().unregisterReceiver(earPieceIntentReceiver);
                }
            } catch (Exception e) {

            }
        }

        @Override
        public void onActionClick (String action){
            try {
                PatchSDK.sdkReady = true;
                hangupCall();
            } catch (Exception e) {

            }
        }


   /* public static void startVibrate(int time){

    }
    public static void stopVibrate(){
        vibrator.cancel();
    }*/


        public void overCallOnSip () {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    PatchSDK.sdkReady = true;
                    jsView.loadUrl("javascript:PatchAndroid.hangup()");
                    try {
                        PatchCommonUtil.getInstance().sendBroadcast(getActivity().getApplicationContext(), Constants.ACTION_CALL_OVER);
                    } catch (Exception e) {

                    }
                    try {
                        callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                    } catch (Exception e) {

                    }
                }
            });
        }
    }
