package com.patch.patchcalling.javaclasses;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.patch.patchcalling.interfaces.CallStatus;
import com.patch.patchcalling.interfaces.CallType;
import com.patch.patchcalling.interfaces.PatchNotificationListener;
import com.patch.patchcalling.interfaces.NotificationResponse;
import com.patch.patchcalling.interfaces.OnSetMissedCallReceiver;
import com.patch.patchcalling.interfaces.OnSetNotificationListener;
import com.patch.patchcalling.interfaces.OutgoingCallResponse;
import com.patch.patchcalling.interfaces.PatchInitResponse;
import com.patch.patchcalling.models.IosAPF;
import com.patch.patchcalling.models.MissedCallActions;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.PatchLogger;

import org.json.JSONObject;

import java.util.List;

import io.socket.client.Socket;

/**
 * this is a singelton class mainly used to store values during a call.
 * Created by sanyamjain on 22/08/18.
 */

public class SocketInit {

    private static SocketInit instance = null;
    private Socket socket;
    private String callData;
    //private String callID;
    private String jwt, phone, cc, accountId, apikey;
    private CallStatus callStatus;
    private Activity outgoingActivity;
    private Class pendingStickyIntent;
    private String pendingNeutralActionIntent = null;
    private CountDownTimer outgoingTimer, incomingTimer;
    private AudioManager audioManager;
    private Context context, appCtx;
    private JSONObject initJsonOptions;
    private CallStatus.incomingCallStatus incomingCallStatus;
    private String callId;
    @SerializedName("outgoingCall")
    @Expose
    private OutgoingCallResponse outgoingCallResponse;
    private CallType callType;
    private String callDirection;
    private Boolean isRecording;
    private JSONObject cli;
    private IosAPF iosAPF;
    private Boolean enablePatchNotification;
    private Boolean enableNotificationUI;
    private PatchInitResponse patchInitResponse;
    private NotificationResponse outgoingNotificationResponse;
    PatchNotificationListener patchNotificationListener;
    PatchSDK.MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler;
    Boolean isClientbusyOnVoIP = false, isClientbusyOnPstn = false;
    Boolean isCalleBusyOnAnotherCall = false;
    Boolean readPhoneStatePermission = false;
    JSONObject callDetails = new JSONObject();
    String sid = "";
    String sna;
    String fromCuid, toCuid;
    String callContext;
    List<MissedCallActions> missedCallReceiverActions, missedCallInitiatorActions;

    private int stickyServiceCountdownTime = 1000;
    private Boolean isScratchCardScratched = false;

    private SocketInit(){

    }

    public static SocketInit getInstance() {
        if (instance == null) {
            instance = new SocketInit();
        }
        return instance;
    }

    public Boolean isScratchCardScratched() {
        return isScratchCardScratched;
    }

    public void setScratchCardScratched(Boolean scratchCardScratched) {
        isScratchCardScratched = scratchCardScratched;
    }

    public String getFromCuid() {
        return fromCuid;
    }

    public void setFromCuid(String fromCuid) {
        this.fromCuid = fromCuid;
    }

    public String getToCuid() {
        return toCuid;
    }

    public void setToCuid(String toCuid) {
        this.toCuid = toCuid;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public JSONObject getCallDetails() {
        return callDetails;
    }

    public void setCallDetails(JSONObject callDetails) {
        this.callDetails = callDetails;
    }

    public IosAPF getIosAPF() {
        return iosAPF;
    }

    public void setIosAPF(IosAPF iosAPF) {
        this.iosAPF = iosAPF;
    }

    public JSONObject getCli() {
        return cli;
    }

    public void setCli(JSONObject cli) {
        this.cli = cli;
    }

    public Boolean getRecording() {
        return isRecording;
    }

    public void setRecording(Boolean recording) {
        isRecording = recording;
    }

    public CallType getCallType() {
        return callType;
    }

    public void setCallType(CallType callType) {
        this.callType = callType;
    }

    public OutgoingCallResponse getOutgoingCallResponse() {
        return outgoingCallResponse;
    }

    public void setOutgoingCallResponse(OutgoingCallResponse outgoingCallResponse) {
        this.outgoingCallResponse = outgoingCallResponse;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public CallStatus.incomingCallStatus getIncomingCallStatus() {
        return incomingCallStatus;
    }

    public void setIncomingCallStatus(CallStatus.incomingCallStatus incomingCallStatus) {
        this.incomingCallStatus = incomingCallStatus;
    }

    public Context getContext() {
        return context;
    }

    public Context getAppContext(){
        return appCtx;
    }

    public void setAppContext(Context appCtx) {
        this.appCtx = appCtx;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    public void setAudioManager(AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public CountDownTimer getTimer() {
        return outgoingTimer;
    }

    public void setTimer(CountDownTimer timer) {
        this.outgoingTimer = timer;
    }

    public CountDownTimer getIncomingTimer() {
        return incomingTimer;
    }

    public void setIncomingTimer(CountDownTimer incomingTimer) {
        this.incomingTimer = incomingTimer;
    }

    public Activity getOutgoingActivity() {
        return outgoingActivity;
    }

    public void setOutgoingActivity(Activity outgoingActivity) {
        this.outgoingActivity = outgoingActivity;
    }

    public CallStatus getCallStatus() {
        return callStatus;
    }

    public NotificationResponse getOutgoingNotificationResponse() {
        return outgoingNotificationResponse;
    }

    public void setOutgoingNotificationResponse(NotificationResponse outgoingNotificationResponse) {
        this.outgoingNotificationResponse = outgoingNotificationResponse;
    }

    /*public SentimentReciever getSentimentReceiver() {
        return sentimentReciever;
    }*/

    public PatchSDK.MissedCallNotificationOpenedHandler getMissedCallNotificationOpenedHandler() {
        return missedCallNotificationOpenedHandler;
    }

    /*public void setSentimentReceiver(SentimentReciever sentimentReciever) {
        this.sentimentReciever = sentimentReciever;
    }*/
    public PatchNotificationListener getNotificationListener() {
        return patchNotificationListener;
    }

    /* public void setNotificationReciever(NotificationInterface.NotificationReceiver notificationReciever) {
        this.notificationReciever = notificationReciever;
    }*/

    public void setNotificationListenerHost(final String notificationRecieverPackage, final OnSetNotificationListener onSetNotificationListener) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Object notificationListenerClass = null;
                    String className = notificationRecieverPackage;  // Change here
                    notificationListenerClass = Class.forName(className).newInstance();
                    patchNotificationListener = (PatchNotificationListener) notificationListenerClass;
                    onSetNotificationListener.onSetNotificationListener(patchNotificationListener);
                } catch (Exception e) {
                    if (context != null)
                        PatchLogger.createLog(e.getMessage() , Log.getStackTraceString(e), context);
                }
            }
        });
    }

    public void setMissedCallInitiatorHost(final String missedCallInitiatorHostPackage, final OnSetMissedCallReceiver onSetMissedCallReceiver) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Object missedCallHostClass = null;
                    String className = missedCallInitiatorHostPackage;  // Change here

                    missedCallHostClass = Class.forName(className).newInstance();

                    missedCallNotificationOpenedHandler = (PatchSDK.MissedCallNotificationOpenedHandler) missedCallHostClass;
                    onSetMissedCallReceiver.onSetMissedCallReceiver(missedCallNotificationOpenedHandler);
                } catch (Exception e) {
                    try {
                        onSetMissedCallReceiver.onSetMissedCallReceiver(null);

                    }catch (Exception e1){

                    }
                    if (context != null)
                        PatchLogger.createLog(e.getMessage() , Log.getStackTraceString(e), context);
                }
            }
        });
    }

    public void setMissedCallReceiverHost(final String missedCallReceiverHostPackage, final OnSetMissedCallReceiver onSetMissedCallReceiver) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    Object missedCallHostClass = null;
                    String className = missedCallReceiverHostPackage;  // Change here

                    missedCallHostClass = Class.forName(className).newInstance();

                    missedCallNotificationOpenedHandler = (PatchSDK.MissedCallNotificationOpenedHandler) missedCallHostClass;
                    onSetMissedCallReceiver.onSetMissedCallReceiver(missedCallNotificationOpenedHandler);
                } catch (Exception e) {
                    try {
                        onSetMissedCallReceiver.onSetMissedCallReceiver(null);

                    }catch (Exception e1){

                    }
                    if (context != null)
                        PatchLogger.createLog(e.getMessage() , Log.getStackTraceString(e), context);
                }
            }
        });
    }

    public void setCallStatus(CallStatus callStatus) {
        this.callStatus = callStatus;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public  void setCallData(String callData) {
        this.callData = callData;
    }

    public String getCallData() {
        return this.callData;
    }

    public JSONObject getInitJsonOptions() {
        return initJsonOptions;
    }

    public void setInitJsonOptions(JSONObject initJsonOptions) {
        this.initJsonOptions = initJsonOptions;
    }

    public Boolean isPatchNotificationEnable() {
        return enablePatchNotification;
    }

    public void setEnablePatchNotification(Boolean enablePatchNotification) {
        this.enablePatchNotification = enablePatchNotification;
    }

    public Boolean isNotificationUIEnable() {
        return enableNotificationUI;
    }

    public void setEnableNotificationUI(Boolean enableNotificationUI) {
        this.enableNotificationUI = enableNotificationUI;
    }

    public PatchInitResponse getPatchInitResponse() {
        return patchInitResponse;
    }

    public void setPatchInitResponse(PatchInitResponse patchInitResponse) {
        this.patchInitResponse = patchInitResponse;
    }

    public Boolean isClientbusyOnVoIP() {
        return isClientbusyOnVoIP;
    }

    public void setClientbusyOnVoIP(Boolean clientbusyOnCall) {
        isClientbusyOnVoIP = clientbusyOnCall;
    }

    public Boolean isClientbusyOnPstn() {
        return isClientbusyOnPstn;
    }

    public void setClientbusyOnPstn(Boolean clientbusyOnPstn) {
        isClientbusyOnPstn = clientbusyOnPstn;
    }

    public Boolean isCalleBusyOnAnotherCall() {
        return isCalleBusyOnAnotherCall;
    }

    public void setCalleBusyOnAnotherCall(Boolean calleBusyOnAnotherCall) {
        isCalleBusyOnAnotherCall = calleBusyOnAnotherCall;
    }

    public String getSna() {
        return sna;
    }

    public void setSna(String sna) {
        this.sna = sna;
    }

    public Boolean getReadPhoneStatePermission() {
        return readPhoneStatePermission;
    }

    public void setReadPhoneStatePermission(Boolean readPhoneStatePermission) {
        this.readPhoneStatePermission = readPhoneStatePermission;
    }

    public Class getPendingNeutralActionIntent(Context context) {
        try {
            return Class.forName(PatchCommonUtil.getInstance().getPendingNeutralIntent(context)).newInstance().getClass();
        }catch (Exception e){
            return null;
        }
    }

    public void setMissedCallReceiverActions(List<MissedCallActions> missedCallActions) {
        this.missedCallReceiverActions = missedCallActions;
    }

    public List<MissedCallActions> getMissedCallReceiverActions(){
        return missedCallReceiverActions;
    }

    public List<MissedCallActions> getMissedCallInitiatorActions() {
        return missedCallInitiatorActions;
    }

    public void setMissedCallInitiatorActions(List<MissedCallActions> missedCallInitiatorActions) {
        this.missedCallInitiatorActions = missedCallInitiatorActions;
    }

    public String getCallDirection() {
        return callDirection;
    }

    public void setCallDirection(String callDirection) {
        this.callDirection = callDirection;
    }

    public String getCallContext() {
        return callContext;
    }

    public void setCallContext(String callContext) {
        this.callContext = callContext;
    }
}
