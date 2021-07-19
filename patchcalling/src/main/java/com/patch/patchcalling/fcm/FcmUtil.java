package com.patch.patchcalling.fcm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.patch.patchcalling.Constants;
import com.patch.patchcalling.R;
import com.patch.patchcalling.activity.PatchCallingActivity;
import com.patch.patchcalling.fragments.PatchCallscreenFragment;
import com.patch.patchcalling.fragments.PatchIncomingFragment;
import com.patch.patchcalling.interfaces.ApiInterface;
import com.patch.patchcalling.interfaces.PatchInitResponse;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.models.VoiceCallStatus;
import com.patch.patchcalling.services.CallNotificationService;
import com.patch.patchcalling.services.RetrofitClient;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.JwtUtil;
import com.patch.patchcalling.utils.NotificationHandler;
import com.patch.patchcalling.utils.SocketIOManager;

import org.json.JSONObject;

import io.socket.client.Ack;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP;

/**
 * Created by Shivam Sharma on 14-04-2020.
 */
public class FcmUtil {
    private static FcmUtil ourInstance;
    String cc, phone, accountId, apikey, currentCuid;
    String jwt = "", sna = "";
    static ApiInterface apiService = null;

    private String expiredCallId = null, fcmNotificationID = null;

    public static FcmUtil getInstance(Context context) {
        if(ourInstance==null){
           ourInstance = new FcmUtil();
            apiService = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                    create(ApiInterface.class);
        }
        return ourInstance;
    }

    public String getExpiredCallId() {
        return expiredCallId;
    }

    public void resetExpiredCallId(String expiredCallId) {
        this.expiredCallId = expiredCallId;
    }

    public String getFcmNotificationID() {
        return fcmNotificationID;
    }

    public void setFcmNotificationID(String fcmNotificationID) {
        this.fcmNotificationID = fcmNotificationID;
    }

    private FcmUtil() {
    }
    
    public void onIncomingCall(final Context context, final JSONObject incomingdata){
        JwtUtil.getInstance(context).verifyToken(context, true, new PatchInitResponse() {
            @Override
            public void onSuccess(int response) {
                try {
                    if(response!= 121212){
                        return;
                    }
//                    Log.d("patchsharma", "verifyToken aya" + incomingdata);

                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                    phone = sharedPref.getString("patch_userPhone", "");
                    cc = sharedPref.getString("patch_userCC", "");
                    accountId = sharedPref.getString("patch_accountId", null);
                    apikey = sharedPref.getString("patch_apikey", null);
                    currentCuid = sharedPref.getString("patch_cuid", "");
                    jwt = sharedPref.getString(context.getResources().getString(R.string.patch_token), "");
                    String accountId = sharedPref.getString("patch_accountId", "");


                    incomingdata.put(context.getString(R.string.accountId), accountId);
                    incomingdata.put(context.getString(R.string.apikey), sharedPref.getString("patch_apikey", null));
                    incomingdata.put(context.getString(R.string.phone), sharedPref.getString("patch_userPhone", null));
                    incomingdata.put(context.getString(R.string.cc), sharedPref.getString("patch_userCC", null));
                    incomingdata.put(context.getString(R.string.token), jwt);
                    incomingdata.put("cuid", currentCuid);
                    incomingdata.put(context.getString(R.string.callType), "incoming");
                    if (currentCuid.length() > 0) {
                        incomingdata.put("initiatorData", currentCuid);
                    } else {
                        incomingdata.put("initiatorData", cc + phone);
                    }

                    String call = incomingdata.getString("call");
                    String sid = incomingdata.has("sid")? incomingdata.getString("sid"):"";

                    //settting calldetails, callId, sid to sigleton for every call
                    SocketInit.getInstance().setCallDetails(incomingdata);
                    SocketInit.getInstance().setSid(sid);
                    SocketInit.getInstance().setCallId(call);

                    SocketInit.getInstance().setAppContext(context);


                    String incomingCuid = incomingdata.getJSONObject("to").getString("cuid");
                    String fromId = incomingdata.getJSONObject("from").getString(context.getString(R.string.id));

                    //Log.d("patchsharma", "verifyToken" + incomingCuid + currentCuid);
                    if(!incomingCuid.equals(currentCuid)){
                       // return;
                        incomingdata.put("responseSid", fromId + "_" + accountId);
                        incomingdata.put("callId", call);
                        incomingdata.put(context.getString(R.string.sid), sid);
                        incomingdata.put("reason", context.getString(R.string.invalid_cuid));
                        incomingdata.put("reasonCode", 401);
                        if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                            SocketIOManager.getSocket().emit(context.getString(R.string.sdecline), incomingdata, new Ack() {
                                @Override
                                public void call(Object... args) {

                                }
                            });
                        }else {
                            FcmCacheManger.getInstance().setRejected(true);
                            FcmCacheManger.getInstance().setRejectedReason(context.getString(R.string.invalid_cuid));
                            FcmCacheManger.getInstance().setRejectedReasonCode(401);
                        }
                        return;
                    }

                    if(context != null){
                        PatchCommonUtil.getInstance().removeNeverAskAgain(context);
                        Boolean isNeverAskAgain = sharedPref.getBoolean("patch_never_ask_again", false);
                        if(isNeverAskAgain){
                            JSONObject data = new JSONObject();
                            data.put("responseSid", fromId + "_" + accountId);
                            data.put("callId", call);
                            data.put(context.getString(R.string.sid), sid);
                            if(isNeverAskAgain){
                                data.put("reason", context.getString(R.string.microphone_permission_not_granted));
                                data.put("reasonCode", 401);
                            }
                            if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                                SocketIOManager.getSocket().emit(context.getString(R.string.sdecline), data, new Ack() {
                                    @Override
                                    public void call(Object... args) {
                                    }
                                });
                            }else {
                                FcmCacheManger.getInstance().setRejected(true);
                                FcmCacheManger.getInstance().setRejectedReason(context.getString(R.string.microphone_permission_not_granted));
                                FcmCacheManger.getInstance().setRejectedReasonCode(401);
                            }
                            return;
                        }
                    }

                    //this user is busy on other voip or pstn call so decline with reasonCode-503
                    if (SocketInit.getInstance().isClientbusyOnVoIP() || SocketInit.getInstance().isClientbusyOnPstn()) {
                        incomingdata.put("responseSid", fromId + "_" + accountId);
                        incomingdata.put("callId", call);
                        incomingdata.put(context.getString(R.string.sid), sid);
                        incomingdata.put("reason", context.getString(R.string.client_busy));
                        incomingdata.put("reasonCode", 503);
                        if (SocketIOManager.getSocket() != null && SocketIOManager.isSocketConnected()) {
                            SocketIOManager.getSocket().emit(context.getString(R.string.sdecline), incomingdata, new Ack() {
                                @Override
                                public void call(Object... args) {

                                }
                            });
                        }else {
                            FcmCacheManger.getInstance().setRejected(true);
                            FcmCacheManger.getInstance().setRejectedReason(context.getString(R.string.client_busy));
                            FcmCacheManger.getInstance().setRejectedReasonCode(503);
                        }
                    } else {
                        SocketInit.getInstance().setClientbusyOnVoIP(true);
//                        SocketInit.getInstance().setCallDetails(incomingdata);
//                        SocketInit.getInstance().setSid(sid);

                        PatchCommonUtil.getInstance().lightUpScreen(context);
                        JSONObject incomingNotificationData = new JSONObject();
                        incomingNotificationData.put(context.getString(R.string.callDetails), incomingdata.toString());
                        //incomingNotificationData.put(context.getString(R.string.screen), context.getString(R.string.incoming));
                        incomingNotificationData.put(context.getString(R.string.sid), sid);
                        incomingNotificationData.put("context", incomingdata.getString("context"));

                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                            NotificationHandler.CallNotificationHandler.getInstance(context).
                                    showCallNotification(incomingNotificationData,
                                            NotificationHandler.CallNotificationHandler.CallNotificationTypes.INCOMING_CALL);
                            PatchCommonUtil.getInstance().startAudio(context);
                            PatchCommonUtil.getInstance().startTimer(context, incomingdata);
                        } else {
                            Intent incomingCallNotificationIntent;
                            if (context != null) {
                                incomingCallNotificationIntent = new Intent(context, CallNotificationService.class);
                            } else {
                                incomingCallNotificationIntent = new Intent(context, CallNotificationService.class);
                            }
                            incomingCallNotificationIntent.putExtra("incomingNotificationData", incomingNotificationData.toString());
                            PatchCommonUtil.getInstance().startstickyIncomingNotificationService(context, incomingCallNotificationIntent);
                            PatchCommonUtil.getInstance().startTimer(context, incomingdata);
                        }

                        try {
                            PatchCommonUtil.getInstance().sendBroadcast(context, Constants.ACTION_INCOMING_CALL);
                        } catch (Exception e) {

                        }

                        //Log.d("patchsharma", "incoming call " + args[0]);
//                        SocketInit.getInstance().setCallId(call);
                        SocketInit.getInstance().setRecording(true);
                        Intent i = new Intent(context, PatchCallingActivity.class);
                        i.putExtra(context.getString(R.string.callDetails), incomingdata.toString());
                        i.putExtra(context.getString(R.string.screen), context.getString(R.string.incoming));
                        i.putExtra(context.getString(R.string.sid), sid);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context.startActivity(i);
                    }
                }catch (Exception e){

                }

            }

            @Override
            public void onFailure(int failure) {
                if(failure == ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP){
                    //do nothing or can return invalid (false) value to primary notification Handler

                }
            }
        });

    }

    public void onCancelCall(Context context, String callId){
        if(PatchIncomingFragment.getInstance()!=null){
            PatchIncomingFragment.getInstance().onCancel();
        }else if(PatchCallscreenFragment.getInstance()!=null){
            PatchCallscreenFragment.getInstance().closeCallScreen();
            updateCallStatus(context, callId);
        }
        expiredCallId = callId;
    }

    public void updateCallStatus(Context context, String callId) {
        try {
            VoiceCallStatus voiceCallStatus = new VoiceCallStatus();
            voiceCallStatus.setCallStatus("cancelled");
            Call<Object> call = apiService.resolveCallStatus(callId, voiceCallStatus, PatchCommonUtil.getInstance().getAccessToken(context));
            call.enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if(response.isSuccessful()){
                    }
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {

                }
            });
        }catch (Exception e){
        }
    }
}
