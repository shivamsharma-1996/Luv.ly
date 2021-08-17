package com.patch.patchcalling.fragments;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.ui.StyledPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.patch.patchcalling.R;
import com.patch.patchcalling.broadcastreciever.CallNotificationActionReceiver;
import com.patch.patchcalling.custom.PatchPinView;
import com.patch.patchcalling.custom.ScratchCard;
import com.patch.patchcalling.fcm.FcmCacheManger;
import com.patch.patchcalling.fcm.FcmUtil;
import com.patch.patchcalling.interfaces.CallNotificationAction;
import com.patch.patchcalling.interfaces.CallStatus;
import com.patch.patchcalling.javaclasses.PatchSDK;
import com.patch.patchcalling.javaclasses.PatchTemplates;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.models.SelectedTemplate;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.NotificationHandler;
import com.patch.patchcalling.utils.OutgoingUtil;
import com.patch.patchcalling.utils.PatchLogger;
import com.patch.patchcalling.utils.PatchObservable;
import com.patch.patchcalling.utils.SocketIOManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.socket.client.Ack;

//import com.patch.patchcalling.javaclasses.PatchTemplate;

public class PatchIncomingFragment extends Fragment implements CallStatus.incomingCallStatus, ActivityCompat.OnRequestPermissionsResultCallback, CallNotificationAction, Player.EventListener {
    View v;
    private static final String TAG = "PatchIncomingFragment";

    private SimpleExoPlayer simpleExoplayer;
    private long playbackPosition = 0;
    private String mp4Url = "https://html5demos.com/assets/dizzy.mp4";

    private ImageView ivDecline, ivLogo, ivAccept;
    static JSONObject callDetails;
    String callContext, fromCuid, toCuid, fromUserName, fromUserImageUrl;
    TextView tvContext, tvCallScreenLabel, tvDecline, tvAccept, tvPoweredBy;
    private ConstraintLayout llBackground;
    private AudioManager audioManager;
    private AudioFocusRequest mAFRequest;
    private MediaPlayer ringtonePlayer;
    CountDownTimer timer;
    public static AudioManager.OnAudioFocusChangeListener afChangeListener;
    private NotificationHandler.CallNotificationHandler callNotificationHandler;
    public int pinviewItemCount;
    public static final int DEFAULT_PINVIEW_ITEM_COUNT = 4;

    private Boolean isAnswered = false;
    static PatchIncomingFragment instance;
    View bannerPlaceholder;
    //PatchTemplate.Pinview pinviewConfig;
    PatchPinView patchPinView;
    private TextView tvPinLabel;
    VideoView videoView;
    
    private Group mScratchOuterLayer;
    private TextView tvScratchHeader, tvScratchFooter, tvScratchInner, tvUserName;
    private ImageView /*ivOuterOverlay,*/ ivScratchInner;
    private ScratchCard scratchView;
    private View clScratchCardView, viewInnerOverlay;

    public static PatchIncomingFragment getInstance() {
        return instance;
    }

    String templateType, templateUrl;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            templateType = getString(R.string.template_default);
            int resLayout = R.layout.fragment_patch_incoming_banner;
            resetTemplateSettings();
            callDetails = new JSONObject(getArguments().getString(getString(R.string.callDetails)));
            callContext = callDetails.getString(getString(R.string.scontext));
            String[] separated = callContext.split("<>");
            callContext = separated[0];
            fromUserName = separated[1];
            fromUserImageUrl = separated[2];

            try {
                if (callDetails.has(getString(R.string.active_template)) && callDetails.get(getString(R.string.active_template)) instanceof JSONObject) {
                    Gson gson = new Gson();
                    SelectedTemplate selectedTemplate = gson.fromJson(
                            callDetails.getJSONObject(getString(R.string.active_template)).toString(),
                            SelectedTemplate.class);
                    if (selectedTemplate.getType() != null && selectedTemplate.getUrl() != null) {
                        templateType = selectedTemplate.getType();
                        templateUrl = selectedTemplate.getUrl();
                    }
                    if (templateType.equals(getString(R.string.template_default))) {
                        resLayout = R.layout.fragment_patch_incoming_banner;
                    } else if (templateType.equals(getString(R.string.template_full_image))) {
                        resLayout = R.layout.fragment_patch_incoming_full_image;
                    } else if (templateType.equals(getString(R.string.template_video)) ||
                            templateType.equals(getString(R.string.template_banner_image)) ||
                            templateType.equals(getString(R.string.template_gif)) ||
                            templateType.equals(getString(R.string.template_scratchcard))) {
                        resLayout = R.layout.fragment_patch_incoming_banner;
                    }
                }
            }catch (Exception e){

            }
            v = inflater.inflate(resLayout, container, false);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            String profilePicUrl = sharedPref.getString(getContext().getString(R.string.prefs_profileUrl), null);

            ImageView ivUserPic = v.findViewById(R.id.iv_user_pic);
            if(fromUserImageUrl!=null)
            Glide.with(this)
                    .load(fromUserImageUrl)
                    .into(ivUserPic);
            initializePlayer(v);
        } catch (Exception e) {
            Log.d("patchahaad1", Log.getStackTraceString(e));

        }

        //PRELOAD the image like brand logo in incoming_event
        try {
            if (templateType.equals(getString(R.string.template_full_image))) {
                ImageView ivBg = v.findViewById(R.id.iv_bg);
                Glide.with(this)
                        .load(templateUrl)
                        .into(ivBg);
            } else if (templateType.equals(getString(R.string.template_banner_image)) || templateType.equals(getString(R.string.template_gif))) {
                bannerPlaceholder = v.findViewById(R.id.banner_placeholder);
                ImageView ivBg = v.findViewById(R.id.banner_image_gif);
                bannerPlaceholder.setVisibility(View.VISIBLE);
                ivBg.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(templateUrl)
                        .into(ivBg);
            } else if (templateType.equals(getString(R.string.template_video))) {
                bannerPlaceholder = v.findViewById(R.id.banner_placeholder);
                videoView = v.findViewById(R.id.banner_video);
                bannerPlaceholder.setVisibility(View.VISIBLE);
                videoView.setVisibility(View.VISIBLE);
                videoView.setZOrderOnTop(true);
                videoView.setVideoPath(templateUrl);
                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        if (videoView != null)
                            videoView.start();
                        mp.setLooping(true);
                    }
                });
                videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
                        if (i == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                            //first frame was bufered - do your stuff here
                            if (videoView != null)
                                videoView.setZOrderOnTop(false);
                            return true;
                        }
                        return false;
                    }
                });
                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        if (videoView != null)
                            videoView.setVisibility(View.INVISIBLE);
                        return true;
                    }
                });
            }else if (templateType.equals(getString(R.string.template_pinview))) {
                patchPinView = v.findViewById(R.id.patch_pin_view);
                tvPinLabel = v.findViewById(R.id.tv_pin_label);
//                patchPinView.setVisibility(View.VISIBLE);
//                tvPinLabel.setVisibility(View.VISIBLE);

                PatchTemplates.Pinview pinviewConfig = PatchCommonUtil.getInstance().getPinviewConig(getContext());
                pinviewItemCount = pinviewConfig.getItemCount();
                if(pinviewItemCount!=0){
                    patchPinView.setItemCount(pinviewConfig.getItemCount());
                }else {
                    pinviewItemCount = DEFAULT_PINVIEW_ITEM_COUNT;
                }

                patchPinView.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        try {
                            patchPinView.setError(null);
                            if(charSequence.length() == pinviewItemCount){
                                if(PatchObservable.getInstance().getPatchPinviewTextObserver() != null){
                                    PatchObservable.getInstance().getPatchPinviewTextObserver()
                                            .onPinTextCompleted(String.valueOf(charSequence), new PatchSDK.PinTextVerificationHandler() {
                                                @Override
                                                public void onSuccess() {
                                                    Log.d("Patchsharma", "pin : onSuccess");

                                                    onCallPicked();
                                                }

                                                @Override
                                                public void onFailure() {
                                                    Log.d("Patchsharma", "pin : onFailure");
                                                    patchPinView.setError("Wrong Pin");
                                                    //calldeclined(null);
                                                }
                                            });
                                }
                            }
                        }catch (Exception e){

                        }
                    }
                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
            }else if(templateType.equals(getString(R.string.template_scratchcard))){
                clScratchCardView = v.findViewById(R.id.cl_scratch_card);
                scratchView = v.findViewById(R.id.scratch_view);
                mScratchOuterLayer = v.findViewById(R.id.scratch_outer_layer);
                tvScratchHeader = v.findViewById(R.id.tv_scratch_header);
                tvScratchFooter = v.findViewById(R.id.tv_scratch_footer);
                viewInnerOverlay = v.findViewById(R.id.view_inner_overlay);
                tvScratchInner = v.findViewById(R.id.tv_scratch_inner);
                ivScratchInner = v.findViewById(R.id.iv_scratch_inner);
                tvUserName = v.findViewById(R.id.tv_user_name);

                PatchTemplates.ScratchCard scratchCardConfig = PatchCommonUtil.getInstance().getScratchCardConfig(getContext());
                if(scratchCardConfig!=null){
                    if(scratchCardConfig.getOuterDrawableRes()!=0 &&
                            scratchCardConfig.getInnerText()!=null && !scratchCardConfig.getInnerText().equals("") &&
                            scratchCardConfig.getInnerDrawableRes()!=0){
                        clScratchCardView.setVisibility(View.VISIBLE);
                        scratchView.setOuterScratchDrawable(scratchCardConfig.getOuterDrawableRes()); //Outer overlay
                        tvScratchHeader.setText(scratchCardConfig.getOuterTextHeader()); //Outer text header
                        tvScratchFooter.setText(scratchCardConfig.getOuterTextFooter()); //Outer text footer
                        viewInnerOverlay.setBackgroundResource(scratchCardConfig.getInnerBackgroundColorRes()); //Inner Background Color
                        tvScratchInner.setText(scratchCardConfig.getInnerText()); //Inner text
                        ivScratchInner.setImageResource(scratchCardConfig.getInnerDrawableRes()); //Inner image

                        scratchView.setOnScratchListener(new ScratchCard.OnScratchListener() {
                            @Override
                            public void onScratch(ScratchCard scratchCard, float visiblePercent) {
                                if (visiblePercent > 0.4) {
                                    setScratchVisibility(true);
                                    mScratchOuterLayer.setVisibility(View.GONE);
                                    SocketInit.getInstance().setScratchCardScratched(true);
                                }
                            }
                        });
                    }
                }
            }
            else {
                //this is default template so no handling required
            }
        } catch (Exception e) {
           //Log.d("patchahaad2", Log.getStackTraceString(e));
        }
        try {
            ivDecline = v.findViewById(R.id.iv_decline);
            ivAccept = v.findViewById(R.id.iv_accept);
            tvContext = v.findViewById(R.id.tv_driverName);
            ivLogo = v.findViewById(R.id.iv_logo);
            tvCallScreenLabel = v.findViewById(R.id.tv_callScreen_label);
            tvDecline = v.findViewById(R.id.tv_decline);
            tvAccept = v.findViewById(R.id.tv_accept);
            llBackground = v.findViewById(R.id.incoming_fragment_container);
            tvPoweredBy = v.findViewById(R.id.tv_poweredBy);
            instance = this;
            isAnswered = false;

            callNotificationHandler = NotificationHandler.CallNotificationHandler.getInstance(getActivity().getApplicationContext());
            CallNotificationActionReceiver.setCallNotificationActionListener(this);
            if (callDetails.has("from") && callDetails.getJSONObject("from").getString("cuid") != null) {
                fromCuid = callDetails.getJSONObject("from").getString("cuid");
            }
            if (callDetails.has("to") && callDetails.getJSONObject("to").getString("cuid") != null) {
                toCuid = callDetails.getJSONObject("to").getString("cuid");
            }
            tvContext.setText(callContext);
        } catch (JSONException e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }

        // starting audio and timer
        try {
            this.setBranding();

            final Animation animShake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
            // starting animations of buttons
            ivDecline.startAnimation(animShake);
            ivAccept.startAnimation(animShake);

            SocketInit.getInstance().setIncomingCallStatus(PatchIncomingFragment.this);
            audioManager = SocketInit.getInstance().getAudioManager();
            //startAudio();
            //startTimer();
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }

        ivAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    onAcceptClick();
                }catch (Exception e){

                }
            }
        });
        ivDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    calldeclined(null);
                } catch (Exception e) {
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                }

            }
        });
        try {
            if (getArguments() != null && getArguments().getString(getActivity().getString(R.string.call_answer)) != null) {
                onAcceptClick();
            }

        } catch (Exception e) {
            e.getStackTrace();
        }
        return v;
    }

    private void resetTemplateSettings() {
        if(SocketInit.getInstance()!=null)
        SocketInit.getInstance().setScratchCardScratched(false);
    }

    private void setScratchVisibility(boolean isScratched) {
        if (isScratched) {
            scratchView.setVisibility(View.INVISIBLE);
        } else {
            scratchView.setVisibility(View.VISIBLE);
        }
    }

    private void onCallPicked() {
        int PERMISSION_ALL = 1;
        try {
            List<String> permissionList = new ArrayList<>();
            if (SocketInit.getInstance().getReadPhoneStatePermission()) {
                permissionList.add(Manifest.permission.READ_PHONE_STATE);
            } else {
                permissionList.remove(Manifest.permission.READ_PHONE_STATE);
            }
            permissionList.add(android.Manifest.permission.RECORD_AUDIO);

            String[] permissions = new String[permissionList.size()];
            permissions = permissionList.toArray(permissions);

            if (!PatchCommonUtil.getInstance().hasPermissions(getContext(), permissions)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permissions, PERMISSION_ALL);
                }
            } else {
                try {
                    SocketInit.getInstance().getIncomingTimer().cancel();
                    PatchCommonUtil.getInstance().stopAudio(getActivity());
                    PatchCommonUtil.getInstance().releaseAudio(getActivity());
                    callAccepted();
                    //removing incoming notification
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                    } else {
                        PatchCommonUtil.getInstance().dismissCallNotificationService(getActivity());
                    }
                    PatchCallscreenFragment callscreenFragment = new PatchCallscreenFragment();
                    Bundle args = new Bundle();
                    args.putString(getString(R.string.callDetails), callDetails.toString());
                    callscreenFragment.setArguments(args);
                    FragmentManager fm = getFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(R.id.fragment_container, callscreenFragment).addToBackStack(null);
                    ft.remove(PatchIncomingFragment.this);
                    ft.commitAllowingStateLoss();
                } catch (Exception e) {
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                }
            }
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    private void setBranding() {
        Map<String, View> brandingViewParams = new HashMap<>();
        brandingViewParams.put("ivLogo", ivLogo);
        brandingViewParams.put("llBackground", llBackground);
        brandingViewParams.put("tvContext", tvContext);
        brandingViewParams.put("tvPoweredBy", tvPoweredBy);
        //brandingViewParams.put("tvAccept", tvAccept);
        //brandingViewParams.put("tvDecline", tvDecline);
        brandingViewParams.put("tvCallScreenLabel", tvCallScreenLabel);
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
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case 1: {
                try {
                    // If request is cancelled, the result arrays are empty.
                    for (int index = 0; index < permissions.length; index++) {
                        String permission = permissions[index];
                        if (permission.equals(android.Manifest.permission.RECORD_AUDIO)) {
                            if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                                try {
                                    SocketInit.getInstance().getIncomingTimer().cancel();
                                    ;
                                    PatchCommonUtil.getInstance().stopAudio(getActivity());
                                    ;
                                    callAccepted();
                                    //removing incoming notification
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                        callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                                    } else {
                                        PatchCommonUtil.getInstance().dismissCallNotificationService(getActivity());
                                    }
                                    PatchCallscreenFragment callscreenFragment = new PatchCallscreenFragment();
                                    Bundle args = new Bundle();
                                    args.putString(getString(R.string.callDetails), callDetails.toString());
                                    callscreenFragment.setArguments(args);

                                    FragmentManager fm = getFragmentManager();
                                    FragmentTransaction ft = fm.beginTransaction();
                                    ft.replace(R.id.fragment_container, callscreenFragment).addToBackStack(null);
                                    ft.remove(PatchIncomingFragment.this);
                                    ft.commitAllowingStateLoss();
                                } catch (Exception e) {
                                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());

                                }
                            } else {
                                if (grantResults[index] == PackageManager.PERMISSION_DENIED && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                                    //user opted never ask again, so storing it to prefs
                                    if (getActivity() != null && getActivity().getApplicationContext() != null) {
                                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putBoolean("patch_never_ask_again", true);
                                        editor.commit();
                                        editor.apply();
                                    }
                                }
//                        AppUtil.getInstance().stopAudio(getActivity());;
//                        SocketInit.getInstance().getIncomingTimer().cancel();;
                                calldeclined(getString(R.string.microphone_permission_not_granted));
                                // permission denied, boo! Disable the
                                // functionality that depends on this permission.
                            }

                        }
                        continue;
                    }
                } catch (Exception e) {
                    if (getActivity() != null && getActivity().getApplicationContext() != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                }
                return;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (FcmUtil.getInstance(getContext()).getExpiredCallId() != null &&
                    FcmUtil.getInstance(getContext()).getExpiredCallId().equals(callDetails.getString(getString(R.string.scall)))) {
                onCancel();
            }
        } catch (Exception e) {

        }
    }

    /**
     * called when the user has answered the incoming call.
     */
    public void callAccepted() {
        try {
            try {
                //if incoming call is routed via FCM so it can be a chance that callee answered the call before getting authenticate that time
                // role of FcmCacheManger comes into play
                if (!SocketIOManager.isSocketConnected()) {
                    FcmCacheManger.getInstance().setAnswered(true);
                }
            } catch (Exception e) {

            }

            SocketInit socketInit = SocketInit.getInstance();
            String id = callDetails.getJSONObject("from").getString(getString(R.string.id));
            String accountId = callDetails.getString(getString(R.string.accountId));
            String call = callDetails.getString(getString(R.string.scall));
//            String rsid = callDetails.getString(getString("rsid"));
            String sid = callDetails.has(getString(R.string.sid)) ? callDetails.getString(getString(R.string.sid)) : "";

            JSONObject data = new JSONObject();
            data.put("responseSid", id + "_" + accountId);
            data.put("callId", call);
            data.put(getString(R.string.sid), sid);
            if (SocketIOManager.getSocket() != null)
                SocketIOManager.getSocket().emit(getString(R.string.sanswer), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                        try {
                            //adding ongoing notification
                       /* if (callContext != null) {
                            callNotificationHandler.showCallNotification(new JSONObject().put("context", callContext), NotificationHandler.CallNotificationHandler.CallNotificationTypes.ONGOING_CALL);
                        }*/
                        } catch (Exception e) {
                            if (getActivity() != null && getActivity().getApplicationContext() != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                        }
                    }
                });
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }

    }

 /*   public static void fcmCallDecline(Context context){
        try {
            Log.d("patchsharma", "fcmCallDecline" );;
            SocketInit socketInit = SocketInit.getInstance();
            String id = callDetails.getJSONObject("from").getString(context.getString(R.string.id));
            String accountId = callDetails.getString(context.getString(R.string.accountId));
            String call = callDetails.getString(context.getString(R.string.scall));
            String sid = callDetails.has(context.getString(R.string.sid)) ? callDetails.getString(context.getString(R.string.sid)) : "";

            JSONObject data = new JSONObject();
            data.put("responseSid", id + "_" + accountId);
            data.put("callId", call);
            data.put(context.getString(R.string.sid), sid);

            //Log.d("Patch", "Call Declined" + data);

            socketInit.getSocket().emit(context.getString(R.string.sdecline), data, new Ack() {
                @Override
                public void call(Object... args) {
                }
            });
        }catch (Exception e){

        }
    }*/

    /**
     * called when user has declined the incoming call
     *
     * @param declineReason
     */
    public void calldeclined(String declineReason) {
        try {
            try {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);

                } else {
                    PatchCommonUtil.getInstance().dismissCallNotificationService(getActivity());
                }
            } catch (Exception e) {

            }
            //AppUtil.getInstance().stopAudio(getActivity());;
            PatchCommonUtil.getInstance().stopAudio(getActivity());
            PatchCommonUtil.getInstance().releaseAudio(getActivity());
            SocketInit.getInstance().getIncomingTimer().cancel();

            if (getActivity() != null) {
                getActivity().finishAndRemoveTask();
            }

            SocketInit socketInit = SocketInit.getInstance();
            String id = callDetails.getJSONObject("from").getString(getString(R.string.id));
            String accountId = callDetails.getString(getString(R.string.accountId));
            String call = callDetails.getString(getString(R.string.scall));
            String sid = callDetails.has(getString(R.string.sid)) ? callDetails.getString(getString(R.string.sid)) : "";

            JSONObject data = new JSONObject();
            data.put("responseSid", id + "_" + accountId);
            data.put("callId", call);
            data.put(getString(R.string.sid), sid);
            if (declineReason != null) {
                data.put("reason", declineReason);
                data.put("reasonCode", 401);
            }

            //Log.d("Patch", "Call Declined" + data);
            if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                socketInit.getSocket().emit(getString(R.string.sdecline), data, new Ack() {
                    @Override
                    public void call(Object... args) {
                    }
                });
            } else {
                //if incoming call is routed via FCM so it can be a chance that callee answered the call before getting authenticate that time
                // role of FcmCacheManger comes into play
                try {
                    FcmCacheManger.getInstance().setRejected(true);
                    if (declineReason != null) {
                        FcmCacheManger.getInstance().setRejectedReason(declineReason);
                        FcmCacheManger.getInstance().setRejectedReasonCode(401);
                    }
                } catch (Exception e) {

                }
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SocketInit.getInstance().setClientbusyOnVoIP(false);
                }
            }, 1000);
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    /**
     * called when user has missed an incoming call.
     */
    public void callMissed() {
        try {
            callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            if (fromCuid != null)
                callNotificationHandler.showCallNotification(
                        new JSONObject().
                                put("context", callContext).
                                put("fromCuid", fromCuid),
                        NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);

            SocketInit socketInit = SocketInit.getInstance();
            String id = callDetails.getJSONObject("from").getString(getString(R.string.id));
            String accountId = callDetails.getString(getString(R.string.accountId));
            String call = callDetails.getString(getString(R.string.scall));
            String sid = callDetails.has(getString(R.string.sid)) ? callDetails.getString(getString(R.string.sid)) : "";

            JSONObject data = new JSONObject();
            data.put("responseSid", id + "_" + accountId);
            data.put("callId", call);
            data.put(getString(R.string.sid), sid);

            socketInit.getSocket().emit(getString(R.string.smiss), data, new Ack() {
                @Override
                public void call(Object... args) {
//                    Log.d("Patch", "Call Missed");
                }
            });

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SocketInit.getInstance().setClientbusyOnVoIP(false);
                }
            }, 1000);
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    /**
     * used to start the default ringtone of the user on receiving an incoming call.
     */
    private void startAudio() {
        try {
            this.audioManager.setSpeakerphoneOn(true);
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                this.mAFRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
//                        .setAcceptsDelayedFocusGain(false)
//                        .setOnAudioFocusChangeListener((AudioManager.OnAudioFocusChangeListener) getActivity())
//                        .setWillPauseWhenDucked(true)
//                        .build();
//                int res = this.audioManager.requestAudioFocus(mAFRequest);
//                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
//                ringtonePlayer = MediaPlayer.create(getContext(), uri);
//                ringtonePlayer.setLooping(true);
//                ringtonePlayer.start();
//                final Object mFocusLock = new Object();
//                synchronized (mFocusLock) {
//                    if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
////                        Log.d(PatchIncomingActivity.class.getSimpleName(), "Failed to get audio focus");
//                    } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
////                        Log.d(PatchIncomingActivity.class.getSimpleName(), "Success");
//                    } else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
////                        Log.d(PatchIncomingActivity.class.getSimpleName(), "Delay in getting audio focus");
//                    }
//                }
//            } else {
            int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_RING, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                ringtonePlayer = MediaPlayer.create(getContext(), uri);
                ringtonePlayer.setLooping(true);
                ringtonePlayer.start();
            } else {
//                    Log.d(PatchIncomingActivity.class.getSimpleName(), "Failed to get audio focus");
            }
//        }
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    /**
     * used to stop the ringtone statrted before leaving the view associated with this fragment.
     */
    private void stopAudio() {
        try {
            audioManager.setSpeakerphoneOn(false);
            ringtonePlayer.stop();
            ringtonePlayer.release();
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    /**
     * releases audio before leaving the view associated with this fragment.
     */
    private void releaseAudio() {
        try {
           /* if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                this.audioManager.abandonAudioFocusRequest(this.mAFRequest);
            } else {
                this.audioManager.abandonAudioFocus(afChangeListener);
            }*/
            if (this.audioManager != null) {
                this.audioManager.abandonAudioFocus(afChangeListener);
            }
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    /**
     * starts the countdown timer of 30 seconds when user receives an incoming call.
     */
    private void startTimer() {
        try {
            timer = new CountDownTimer(30000, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
//                Log.d("Patch", "seconds remaining: " + millisUntilFinished / 1000);
                }

                @Override
                public void onFinish() {
                    try {
                        PatchCommonUtil.getInstance().stopAudio(getActivity());
                        ;
                        releaseAudio();
                        callMissed();
                        if (getActivity() != null) {
                            getActivity().finishAndRemoveTask();
                        }
                    } catch (Exception e) {
                        if (getActivity() != null && getActivity().getApplicationContext() != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
                    }

                }
            }.start();

        } catch (Exception e) {

        }
    }

    /**
     * called when an outgoing call is cancelled by the caller.
     */
    @Override
    public void onCancel() {
        try {
            if (getActivity() != null) {
                PatchCommonUtil.getInstance().stopAudio(getActivity());
                PatchCommonUtil.getInstance().releaseAudio(getActivity());
                SocketInit.getInstance().getIncomingTimer().cancel();
                getActivity().finishAndRemoveTask();

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                } else {
                    PatchCommonUtil.getInstance().dismissCallNotificationService(getActivity());
                }

                try {
                    if (fromCuid != null)
                        callNotificationHandler.showCallNotification(
                                new JSONObject().
                                        put("context", callContext).
                                        put("fromCuid", fromCuid).
                                put("toCuid", toCuid),
                                NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);
                } catch (Exception e) {

                }
                //callMissed();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SocketInit.getInstance().setClientbusyOnVoIP(false);
                    }
                }, 1000);
            }
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }

        //Log.d("Patch", "call is cancelled");
        /*try {
            if (getActivity() != null) {
                AppUtil.getInstance().stopAudio(getActivity());
                AppUtil.getInstance().releaseAudio(getActivity());
                SocketInit.getInstance().getIncomingTimer().cancel();
                getActivity().finishAndRemoveTask();
                callNotificationHandler.removeNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);

                callMissed();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SocketInit.getInstance().setClientbusyOnVoIP(false);
                    }
                }, 1000);
            }
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            releasePlayer();

            //instance = null;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (callNotificationHandler.getCallNotificationStatus() != null &&
                        callNotificationHandler.getCallNotificationStatus().equals(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL)
                        && callContext != null)
                    callNotificationHandler.rebuildNotification(NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
            }
        } catch (Exception e) {
            if (getActivity() != null && getActivity().getApplicationContext() != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), getActivity().getApplicationContext());
        }
    }

    @Override
    public void onActionClick(String action) {
        try {
            switch (action) {
                case "Answer":
//                    onAceptClick();
                    onAcceptClick();
                    break;
                case "Decline":
                    calldeclined(null);
                    break;
            }
        } catch (Exception e) {

        }
    }

    private void onAcceptClick() {
        if (templateType.equals(getString(R.string.template_pinview))){
            if(patchPinView!=null)
                patchPinView.setVisibility(View.VISIBLE);
            if(tvPinLabel!=null)
                tvPinLabel.setVisibility(View.VISIBLE);
        }else if(templateType.equals(getString(R.string.template_video))){
            if(videoView!=null){
                videoView.stopPlayback();
                videoView.setVisibility(View.GONE);
            }
            onCallPicked();
        }
        else {
            onCallPicked();
        }
    }

    private void initializePlayer(View view) {
        Log.d(TAG, "initializePlayer: "+  callContext.toString());
        simpleExoplayer = new SimpleExoPlayer.Builder(getContext()).build();
        preparePlayer(callContext, "default");
        StyledPlayerView styledPlayerView = view.findViewById(R.id.exoPlayerView);
        styledPlayerView.setPlayer(simpleExoplayer);
        simpleExoplayer.seekTo(playbackPosition);
        simpleExoplayer.setPlayWhenReady(true);
        simpleExoplayer.setRepeatMode(SimpleExoPlayer.REPEAT_MODE_ONE);
        simpleExoplayer.addListener(this);
    }

    private MediaSource buildMediaSource(Uri uri, String type) {
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getContext(), "exoplayer-sample");
        if (type == "dash") {
            return  new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(uri);
        } else {
            return  new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(uri);
        }
    }

    private void preparePlayer(String videoUrl, String type) {
        Uri uri = Uri.parse(videoUrl);
        MediaSource mediaSource = buildMediaSource(uri, type);
        simpleExoplayer.prepare(mediaSource);
    }

    private void  releasePlayer() {
        playbackPosition = simpleExoplayer.getCurrentPosition();
        simpleExoplayer.release();
    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.d("patchsharma", "onPlayerError");
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        Log.d("patchsharma", "onPlayWhenReadyChanged");

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.d("patchsharma", "onPlayerStateChanged");
    }

}
