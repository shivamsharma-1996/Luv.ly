package com.patch.patchcalling.javaclasses;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import com.patch.patchcalling.BuildConfig;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.patch.patchcalling.Constants;
import com.patch.patchcalling.PatchResponseCodes;
import com.patch.patchcalling.R;
import com.patch.patchcalling.fcm.FcmCacheManger;
import com.patch.patchcalling.fcm.FcmSigsockService;
import com.patch.patchcalling.fcm.FcmUtil;
import com.patch.patchcalling.fcm.PatchFcmResponse;
import com.patch.patchcalling.interfaces.ApiInterface;
import com.patch.patchcalling.interfaces.BaseInterface;
import com.patch.patchcalling.interfaces.MessageInitResponse;
import com.patch.patchcalling.interfaces.PatchNotificationListener;
import com.patch.patchcalling.interfaces.NotificationResponse;
import com.patch.patchcalling.interfaces.OnSetMissedCallReceiver;
import com.patch.patchcalling.interfaces.OnSetNotificationListener;
import com.patch.patchcalling.interfaces.OutgoingCallResponse;
import com.patch.patchcalling.interfaces.OutgoingMessageResponse;
import com.patch.patchcalling.interfaces.PatchInitResponse;
import com.patch.patchcalling.interfaces.TokenResponse;
import com.patch.patchcalling.interfaces.messaging.FetchCallLogsCallback;
import com.patch.patchcalling.interfaces.messaging.FetchChatMessageCallback;
import com.patch.patchcalling.interfaces.messaging.FetchConversationCallback;
import com.patch.patchcalling.interfaces.messaging.FetchUnreadMessageCallback;
import com.patch.patchcalling.interfaces.messaging.MarkMessageSeenCallback;
import com.patch.patchcalling.interfaces.tagging.TagListener;
import com.patch.patchcalling.models.ApiUrlRequest;
import com.patch.patchcalling.models.ContactDeviceId;
import com.patch.patchcalling.models.CreateContact;
import com.patch.patchcalling.models.MerchantSigninResponse;
import com.patch.patchcalling.models.MissedCallActions;
import com.patch.patchcalling.retrofitresponse.createcontact.Cli;
import com.patch.patchcalling.retrofitresponse.createcontact.CreateContactResponse;
import com.patch.patchcalling.retrofitresponse.messaging.calllogs.CallLogsResponse;
import com.patch.patchcalling.retrofitresponse.messaging.calllogs.FetchParticipantIdResponse;
import com.patch.patchcalling.retrofitresponse.messaging.conversations.FetchConversationResponse;
import com.patch.patchcalling.retrofitresponse.messaging.messages.ChatMessages;
import com.patch.patchcalling.retrofitresponse.messaging.messages.UnreadCount;
import com.patch.patchcalling.services.JobSchedulerSocketService;
import com.patch.patchcalling.services.RetrofitClient;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.CustomHandler;
import com.patch.patchcalling.utils.JwtUtil;
import com.patch.patchcalling.utils.PatchLogger;
import com.patch.patchcalling.utils.PatchObservable;
import com.patch.patchcalling.utils.Preconditions;
import com.patch.patchcalling.utils.SocketIOManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.patch.patchcalling.Constants.ACTION_CANCEL_CALL;
import static com.patch.patchcalling.Constants.ACTION_FCM_NOTIFICATION_RECEIVED;
import static com.patch.patchcalling.Constants.ACTION_INCOMING_CALL;
import static com.patch.patchcalling.PatchResponseCodes.ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM;
import static com.patch.patchcalling.PatchResponseCodes.ERR_MESSAGING_MUST_BE_INITIALIZED;
import static com.patch.patchcalling.PatchResponseCodes.ERR_NETWORK_NOT_AVAILABLE;
import static com.patch.patchcalling.PatchResponseCodes.FAILURE_NO_ACTIVE_PATCH_SESSION;
import static com.patch.patchcalling.PatchResponseCodes.FetchCallLogsCallback.OnFailure.ERR_FETCHING_CALL_LOG;
import static com.patch.patchcalling.PatchResponseCodes.FetchChatMessageCallback.OnFailure.ERR_FETCHING_CHAT_MESSAGES;
import static com.patch.patchcalling.PatchResponseCodes.FetchConversationCallback.OnFailure.ERR_FETCHING_CONVERSATION;
import static com.patch.patchcalling.PatchResponseCodes.FetchUnreadMessageCallback.OnFailure.ERR_FETCHING_UNREAD_COUNT;
import static com.patch.patchcalling.PatchResponseCodes.MarkMessageSeenCallback.OnFailure.ERR_MARK_MESSAGES_SEEN;
import static com.patch.patchcalling.PatchResponseCodes.MarkMessageSeenCallback.OnSuccess.SUCCESS_MESSAGES_MARKED_SEEN;
import static com.patch.patchcalling.PatchResponseCodes.MessageInitCallback.OnFailure.ERR_INVALID_RECEIVER_CUID;
import static com.patch.patchcalling.PatchResponseCodes.MessageInitCallback.OnFailure.ERR_MESSAGING_NOT_INITIALIZED;
import static com.patch.patchcalling.PatchResponseCodes.MessageInitCallback.OnFailure.ERR_MISSING_MERCHANT;
import static com.patch.patchcalling.PatchResponseCodes.MessageInitCallback.OnFailure.ERR_MISSING_RECEIVER_CUID;
import static com.patch.patchcalling.PatchResponseCodes.MessageInitCallback.OnResponse.SUCCESS_MESSAGING_INITIALIZED;
import static com.patch.patchcalling.PatchResponseCodes.NotificationCallback.OnFailure.ERR_INCORRECT_NOTIFICATION_PARAMS_IN_SDK_INITIALIZATION;
import static com.patch.patchcalling.PatchResponseCodes.NotificationCallback.OnFailure.ERR_NOTIFICATION_INITIALIZATION_REQUIRED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_BAD_NETWORK;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_BOTH_CC_AND_PHONE_REQUIRED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CALLEE_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CALLER_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CALL_OPTIONS_REQUIRED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CUID_ALREADY_CONNECTED_ELSEWHERE;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_EMPTY_VERIFIED_CLI_LIST;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_INVALID_CC_LENGTH;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_INVALID_FORMAT_OF_WEBHOOK;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_INVALID_PHONE_NUMBER_LENGTH;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_MICROPHONE_PERMISSION_NOT_GRANTED;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_MISSING_CC_OR_PHONE_IN_CLI;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_TAGS_COUNT_EXCEEDED_BY_10;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_TAG_LENGTH_EXCEEDED_BY_32;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_UNAUTHORIZED_CLI;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_VAR_LENGTH_EXCEEDED_BY_128;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingMessageCallback.OnFailure.ERR_MISSING_CUID;
import static com.patch.patchcalling.PatchResponseCodes.OutgoingMessageCallback.OnFailure.ERR_MISSING_MESSAGE;
import static com.patch.patchcalling.PatchResponseCodes.PatchFcmResponse.OnFailure.ERR_FAILED_TO_UPDATE_FCM_TOKEN;
import static com.patch.patchcalling.PatchResponseCodes.PatchFcmResponse.OnFailure.ERR_INTERNAL_SERVER_ERROR;
import static com.patch.patchcalling.PatchResponseCodes.PatchFcmResponse.OnSuccess.SUCCESS_FCM_TOKEN_UPDATED;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_BOTH_ACCOUNTID_AND_APIKEY_REQUIRED;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_CUID_CAN_NOT_HAVE_SPECIAL_CHARS_BETWEEN_NUMBERS;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_EITHER_CC_AND_PHONE_OR_CUID_NEEDED;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_INCORRECT_PARAMS_IN_SDK_INITIALIZATION;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_INVALID_ACCOUNTID_OR_APIKEY;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_INVALID_ACTIVITY_CONTEXT;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_INVALID_CC;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_INVALID_FCM_TOKEN;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_INVALID_LENGH_CUID;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_INVALID_PHONE_NUMBER;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_NAME_LENGTH_EXCEEDED_BY_25;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_PATCH_SDK_NOT_INITIALIZED;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP;
import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE;
import static com.patch.patchcalling.PatchResponseCodes.TagsCallback.OnFailure.FAILURE_SESSION_EXPIRED_RESTART_THE_APP;
import static com.patch.patchcalling.PatchResponseCodes.TagsCallback.OnFailure.FAILURE_WHIILE_ADDING_TAG;
import static com.patch.patchcalling.PatchResponseCodes.TagsCallback.OnFailure.FAILURE_WHIILE_REMOVING_TAG;

/**
 * Created by shivamsharma on 20/08/18.
 */

public class PatchSDK {

    static PatchSDK instance = null;
    ApiInterface apiService = RetrofitClient.getClient().create(ApiInterface.class);
    ApiInterface routeApiService = null;
    //    private Context context;
    //SocketService socketService;
    Gson gson = new Gson();
    static JobInfo myJob;
    static JobScheduler jobScheduler;
    static JobSchedulerSocketService jobSchedulerSocketService;
    private static final int ERR_CONTACT_NOT_REGISTERED = 10001;
    private static final int SUCCESS_CONTACT_REGISTERED = 10002;
    private static final int ERR_EXPECT_CALL_TOKEN_TO_MAKE_CALL = 10003;
    public static Boolean sdkReady = true;
    private CustomHandler customHandler = CustomHandler.getInstance();

    /**
     * Creates a new instance of this class if not created and returns the instance.
     *
     * @return
     */
    public static PatchSDK getInstance() {
        if (instance == null) {
            instance = new PatchSDK();
        }
        return instance;
    }

    public static Boolean isGoodToGo() {
        return sdkReady;
    }

    /**
     * functoin to register a contact of the user with rest API
     *
     * @param context:           context of the application from which the function is called
     * @param cc:                country code of the user
     * @param phone:             phone number of the user
     * @param name:              name of the user
     * @param patchInitResponse: for success or failure responses
     */
    private void registerContact(final Context context, final String fcmToken, final String appId, final String name, final String cc, final String phone, final String accountId, final String apiKey, final String ringtone, final String cuid, final PatchInitResponse patchInitResponse) {
        try {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean isInternetConnected = PatchCommonUtil.getInstance().hasInternetAccess(context);
                        final String plateform = context.getString(R.string.platform_android);
                        if (isInternetConnected) {
                            //final CreateContact createContact = new CreateContact(name, cc, phone, plateform, accountId, apiKey, cuid);
                            final CreateContact createContact = new CreateContact();
                            //Log.d("sharma", "appId : " + appId);
                            if (appId != null && !appId.isEmpty()) {
                                createContact.setAppId(appId);
                            }
                            if (name != null) {
                                createContact.setName(name);
                            }
                            if (cc != null) {
                                createContact.setCc(cc);
                            }
                            if (phone != null) {
                                createContact.setPhone(phone);
                            }
                            if (plateform != null) {
                                createContact.setPlatform(plateform);
                            }
                            if (accountId != null) {
                                createContact.setAccountId(accountId);
                            }
                            if (apiKey != null) {
                                createContact.setApikey(apiKey);
                            }
                            if (cuid != null) {
                                createContact.setCuid(cuid);
                            }
                            if (fcmToken != null) {
                                createContact.setDeviceId(fcmToken);
                            }
                            createContact.setSdkVersion(Constants.VERSION_NAME);
                            routeApiService = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                                    create(ApiInterface.class);
                            Call<CreateContactResponse> createContactCall = routeApiService.createContact(accountId, createContact/*, authToken*/);
                            createContactCall.enqueue(new Callback<CreateContactResponse>() {
                                @Override
                                public void onResponse(Call<CreateContactResponse> call, Response<CreateContactResponse> response) {
                                    try {
                                        if (response.isSuccessful()) {
                                            CreateContactResponse contactResponse = response.body();

                                            //Log.d("patchsharma", "ss :" + contactResponse.getAccessToken());
                                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            //editor.putString("patch_session", "true");
                                            editor.putString("patch_accessToken", contactResponse.getAccessToken());
                                            editor.putString("patch_contactId", contactResponse.getContactId());
                                            editor.putString("patch_userName", contactResponse.getName());
                                            editor.putString("patch_userCC", contactResponse.getCc());
                                            editor.putString("patch_fcmToken", fcmToken);
                                            editor.putString("patch_userPhone", contactResponse.getPhone());
                                            editor.putString("patch_accountId", accountId);
                                            editor.putString("patch_apikey", apiKey);
                                            editor.putString("patch_sdkVersion", Constants.VERSION_NAME);
                                            editor.putString("patch_fontColor", contactResponse.getBranding().getColor());
                                            editor.putString("patch_bgColor", contactResponse.getBranding().getBgColor());
                                            editor.putString("patch_logo", contactResponse.getBranding().getLogo());
                                            if (contactResponse.getEcta() != null) {
                                                editor.putString("patch_ecta", contactResponse.getEcta());
                                            }
                                            if (ringtone != null && ringtone != "") {
                                                editor.putString("patch_ringtone", ringtone);
                                            }
                                            if (cuid != null) {
                                                editor.putString("patch_cuid", cuid);
                                            }
                                            if (contactResponse.getCliList() != null) {
                                                editor.putString("patch_cliList", gson.toJson(contactResponse.getCliList()));
                                            }
                                            editor.commit();
                                            editor.apply();

                                            //patchInitResponse.onSuccess(SUCCESS_CONTACT_REGISTERED);
                                            PatchCommonUtil.getInstance().preloadBrandLogo(context, contactResponse.getBranding().getLogo());
                                            customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, SUCCESS_CONTACT_REGISTERED);
                                            //Log.d("sharma", "registerContact called");
                                            JwtUtil.getInstance(context).getToken(context, cc, phone, accountId, cuid, new TokenResponse() {
                                                @Override
                                                public void onSuccess(String response) {
                                                    if (response.equals("200")) {
                                                        jobSchedulerSocketService = JobSchedulerSocketService.getInstance(context);
                                                        myJob = new JobInfo.Builder(10, new ComponentName(context, jobSchedulerSocketService.getClass()))
//                                                .setPeriodic(1800000)
                                                                .setBackoffCriteria(4000, JobInfo.BACKOFF_POLICY_LINEAR)
                                                                .setPersisted(true)
                                                                .setMinimumLatency(1)
                                                                .setOverrideDeadline(1)
                                                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                                                .build();

                                                        jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                                                        if (jobScheduler != null) {
                                                            jobScheduler.schedule(myJob);
                                                        }
                                                    }
                                                }

                                                @Override
                                                public void onFailure(String failure) {
                                                    customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                                                }
                                            });
                                        } else {
                                            if (response.code() == 401) {
                                                sdkReady = false;
                                                //patchInitResponse.onFailure(ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE);
                                                customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE);
                                            } else {
                                                //patchInitResponse.onFailure(ERR_CONTACT_NOT_REGISTERED);
                                                customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED);
                                            }
                                            if (context != null)
                                                PatchLogger.createLog("Contact Registration onResponse", String.valueOf(response), context);
                                        }
                                    } catch (Exception e) {
                                        //patchInitResponse.onFailure(ERR_CONTACT_NOT_REGISTERED);
                                        customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED);
                                        if (context != null)
                                            PatchLogger.createLog("Contact Registration onFailure", e.toString(), context);
                                    }
                                }

                                @Override
                                public void onFailure(Call<CreateContactResponse> call, Throwable t) {
                                    try {
                                        //Log.d("PatchonFailure", String.valueOf(t.toString()));
                                        //patchInitResponse.onFailure(ERR_CONTACT_NOT_REGISTERED);
                                        customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_CONTACT_NOT_REGISTERED);
                                        if (context != null)
                                            PatchLogger.createLog("Contact Registration onFailure", t.toString(), context);
                                    } catch (Exception e) {

                                    }
                                }
                            });
                        } else {
                            //patchInitResponse.onFailure(ERR_NETWORK_NOT_AVAILABLE); // 100 for no internet connection
                            customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_NETWORK_NOT_AVAILABLE);
                        }
                    } catch (Exception e) {
                        //e.printStackTrace();
                        if (context != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);

            //e.printStackTrace();
        }

    }

    /**
     * function to initialize the sdk
     *
     * @param context:       context of the application from which the function is called
     * @param initJson       JsonObject with all the fields required for initialization
     * @param patchInitResponse: success or failure response
     */

    SocketInit socketInit = SocketInit.getInstance();

    private void init(final Context context, final JSONObject initJson, final PatchInitResponse patchInitResponse, final Boolean readPhoneStatePermission) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        try {
                            FcmCacheManger.getInstance().resetFcmCache();
                        } catch (Exception e) {

                        }
                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                        try {
                            String sdkVersion = sharedPref.getString("patch_sdkVersion", null);
                            if (sdkVersion == null || PatchCommonUtil.getInstance().compareVersionNames(sdkVersion, Constants.VERSION_NAME) != 0) {
                                //delete stored session here
                                PatchCommonUtil.getInstance().removeSession(context);
                                JwtUtil.getInstance(context).stopServiceAndSigsock(context);
                            }
                        } catch (Exception e) {

                        }

                        if (PatchCommonUtil.getInstance().hasInternetAccess(context)) {
                            socketInit.setContext(context);
                            socketInit.setInitJsonOptions(initJson);
                            socketInit.setPatchInitResponse(patchInitResponse);
                            socketInit.setReadPhoneStatePermission(readPhoneStatePermission);
                            removeNotificationPreferences(context);

                            jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                            if (SocketIOManager.isSocketConnected()) {
                                if (SocketIOManager.getSocket() != null) {
                                    SocketIOManager.setSocketInstanceNull();
                                    SocketIOManager.setIsUnAuthorized(false);
                                    //Log.d("sharma", "socket disconnected");
                                }
                                //customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_SDK_INITIALIZED);
                                //return;
                            }
                            String session = sharedPref.getString("patch_session", null);
                            if (session != null && session.equals("true")) {
                                //Hit /jwt/verify
                                JwtUtil.getInstance(context).verifyToken(context, false, patchInitResponse);
                                try {
                                    String latestToken = initJson.has("fcmToken") ? initJson.getString("fcmToken") : null;
                                    if (latestToken != null && !latestToken.equals("")
                                            && !latestToken.equals(PatchCommonUtil.getInstance().getFcmToken(context))) {
                                        //It means fetched latestToken & stored token do not exist,
                                        // so updating new deviceToken in contact model
                                        registerNewFcmToken(context, latestToken, new PatchFcmResponse() {
                                            @Override
                                            public void onSuccess(int response) {
                                                //Log.d("patchsharma", "resp" + response);
                                            }

                                            @Override
                                            public void onFailure(int failure) {
                                                //Log.d("patchsharma", "fail" + failure);
                                            }
                                        });
                                    }
                                } catch (Exception e) {

                                }
                                //customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_SDK_INITIALIZED);
                            } else {
                                initializeSdk();
                            }
//            if (!AppUtil.getInstance().isJobIdRunning(context, 10) || !SocketIOManager.isSocketConnected()) {
//                initializeSdk();
//            } else {
//                isGoodToGo = true;
//                //patchInitResponse.onSuccess(SUCCESS_PATCH_SDK_INITIALIZED);
//                customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_SDK_INITIALIZED);
//                jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
//                jobSchedulerSocketService = JobSchedulerSocketService.getInstance(context);
//            }
                        } else {
                            customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_NETWORK_NOT_AVAILABLE);
                        }
                    } catch (Exception e) {

                    }

                }
            }).start();

        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    private void init(final Context context, final JSONObject initJson, final PatchInitResponse patchInitResponse, final Boolean readPhoneStatePermission, final boolean enablePatchNotification, final boolean enableNotificationUI, final String notificationListenerHost,
                      final List<MissedCallActions> missedCallReceiverActions, final String missedCallReceiverHost,
                      final List<MissedCallActions> missedCallInitiatorActions, final String missedCallInitiatorHost, final PatchInitOptions initOptions) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FcmCacheManger.getInstance().resetFcmCache();

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

                        SharedPreferences.Editor editor = sharedPref.edit();
                        //it will remove config first every time then insert newly passed one
                        editor.remove(context.getString(R.string.patch_template_pinview_config));
                        editor.commit();
                        editor.apply();
                        if (initOptions.getPinviewConfig() != null) {
                            editor.putString(context.getString(R.string.patch_template_pinview_config), new Gson().toJson(initOptions.getPinviewConfig()));
                        }
                        if (initOptions.getScratchCardConfig() != null) {
                            editor.putString(context.getString(R.string.patch_template_scratchcard_config), new Gson().toJson(initOptions.getScratchCardConfig()));
                        }
                        /*else {
                            editor.remove(context.getString(R.string.patch_template_pinview_config));
                        }*/
                        editor.commit();
                        editor.apply();


                        try {
                            String sdkVersion = sharedPref.getString("patch_sdkVersion", null);

                            if (sdkVersion == null || PatchCommonUtil.getInstance().compareVersionNames(sdkVersion, Constants.VERSION_NAME) != 0) {

                                //delete stored session here
                                PatchCommonUtil.getInstance().removeSession(context);
                                JwtUtil.getInstance(context).stopServiceAndSigsock(context);
                            }
                        } catch (Exception e) {

                        }

                        boolean isInternetConnected = PatchCommonUtil.getInstance().hasInternetAccess(context);
                        if (!isInternetConnected) {
                            customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_NETWORK_NOT_AVAILABLE);
                            return;
                        }
                        //socketInit.setPendingNeutralActionIntent(context, notificationLauncher);
                        socketInit.setContext(context);
                        socketInit.setInitJsonOptions(initJson);
                        socketInit.setPatchInitResponse(patchInitResponse);
                        socketInit.setReadPhoneStatePermission(readPhoneStatePermission);
                        socketInit.setEnablePatchNotification(enablePatchNotification);
                        socketInit.setEnableNotificationUI(enableNotificationUI);
                        socketInit.setMissedCallInitiatorActions(missedCallInitiatorActions);
                        socketInit.setMissedCallReceiverActions(missedCallReceiverActions);
                        socketInit.setMissedCallInitiatorHost(missedCallInitiatorHost, new OnSetMissedCallReceiver() {
                            @Override
                            public void onSetMissedCallReceiver(MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler) {

                            }
                        });
                        socketInit.setMissedCallReceiverHost(missedCallReceiverHost, new OnSetMissedCallReceiver() {
                            @Override
                            public void onSetMissedCallReceiver(MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler) {

                            }
                        });

                        socketInit.setNotificationListenerHost(notificationListenerHost, new OnSetNotificationListener() {
                            @Override
                            public void onSetNotificationListener(PatchNotificationListener patchNotificationListener) {
                            }
                        });
//                        socketInit.setSentientReciever(sentimentReceiver, new OnSetSentimentReceiver() {
//                            @Override
//                            public void onSetSentimentReceiver(SentimentReciever sentimentReciever) {
//
//                            }
//                        });
                        storePreferences(context, enablePatchNotification, enableNotificationUI, notificationListenerHost, /*sentimentReceiver,*/
                                missedCallReceiverHost, missedCallInitiatorHost);

                        jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                        if (SocketIOManager.isSocketConnected()) {
                            if (SocketIOManager.getSocket() != null) {
                                SocketIOManager.setSocketInstanceNull();
                                SocketIOManager.setIsUnAuthorized(false);
                                //Log.d("sharma", "socket disconnected");
                            }
                            //customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_SDK_INITIALIZED);
                            //return;
                        }
                        String session = sharedPref.getString("patch_session", null);
                        if (session != null && session.equals("true")) {
                            //Hit /jwt/verify
                            JwtUtil.getInstance(context).verifyToken(context, false, patchInitResponse);
                            try {
                                String latestToken = initJson.has("fcmToken") ? initJson.getString("fcmToken") : null;
                                if (latestToken != null && !latestToken.equals("")
                                        && !latestToken.equals(PatchCommonUtil.getInstance().getFcmToken(context))) {
                                    //It means fetched latestToken & stored token do not exist,
                                    // so updating new deviceToken in contact model
                                    registerNewFcmToken(context, latestToken, new PatchFcmResponse() {
                                        @Override
                                        public void onSuccess(int response) {
                                            //Log.d("patchsharma", "resp" + response);
                                        }

                                        @Override
                                        public void onFailure(int failure) {
                                            //Log.d("patchsharma", "fail" + failure);
                                        }
                                    });
                                }
                            } catch (Exception e) {

                            }
                        } else {
                            initializeSdk();
                        }
//                        if(SocketIOManager.isSocketConnected()){
//                            customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_SDK_INITIALIZED);
//                            return;
//                        }
//
//                        if (!AppUtil.getInstance().isJobIdRunning(context, 10) || !SocketIOManager.isSocketConnected()) {
//                            initializeSdk();
//                        } else {
//                            isGoodToGo = true;
//                            //patchInitResponse.onSuccess(SUCCESS_PATCH_SDK_INITIALIZED);
//                            //customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_SDK_INITIALIZED);
//                            jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
//                            jobSchedulerSocketService = JobSchedulerSocketService.getInstance(context);
//                        }
                    } catch (Exception e) {

                    }
                }
            }).start();
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public void initMessage(Context context, JSONObject initOptions, MessageInitResponse messageInitResponse) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            Boolean merchant = initOptions.has("merchant") ? initOptions.getBoolean("merchant") : null;
            //Boolean isCallEnable = initOptions.has("enableCall") ? initOptions.getBoolean("enableCall") : null;
            String converionsationId = initOptions.has("conversationId") ? initOptions.getString("conversationId") : null;
            String receiverCuid = initOptions.has("receiverCuid") ? initOptions.getString("receiverCuid") : null;
            String cuid = sharedPref.getString("patch_cuid", null);
            String accountId = sharedPref.getString("patch_accountId", null);
            //String apiKey = sharedPref.getString("apiKey", null);

            if (context != null) {
                if (merchant == null) {
                    //messageInitResponse.onFailure(ERR_MISSING_MERCHANT);
                    customHandler.sendMessAnnotations(messageInitResponse, CustomHandler.Mess.ON_FAILURE, ERR_MISSING_MERCHANT);
                    return;
                } /*else if (isCallEnable) {
                    messageInitResponse.onFailure(ERR_MISSING_ENABLE_CALL);
                    return;
                }*/
                if (merchant == false) {
                    //its for customer
                    if (receiverCuid == null) {
                        //messageInitResponse.onFailure(ERR_MISSING_RECEIVER_CUID);
                        customHandler.sendMessAnnotations(messageInitResponse, CustomHandler.Mess.ON_FAILURE, ERR_MISSING_RECEIVER_CUID);
                        return;
                    } else if (receiverCuid.isEmpty()) {
                        //messageInitResponse.onFailure(ERR_INVALID_RECEIVER_CUID);
                        customHandler.sendMessAnnotations(messageInitResponse, CustomHandler.Mess.ON_FAILURE, ERR_INVALID_RECEIVER_CUID);
                        return;
                    } else {
                        MessageInit.getInstance().setReceiverCuid(receiverCuid);
                    }
                } /*else if (converionsationId == null) {
                    if (merchant == false) {
                        converionsationId = AppUtil.getInstance().getConversationId(cuid, receiverCuid); //for customer,conversionId required so generating from SDK
                    }
                    return;
                }*/
                MessageInit.getInstance().setMerchant(merchant);
                //MessageInit.getInstance().setEnableCall(isCallEnable);
                MessageInit.getInstance().setConversationId(converionsationId);
                MessageInit.getInstance().setCuid(cuid);

                //if merchant is true go for merchant-signin
                if (merchant != null) {
                    {
                        if (merchant == true) {
                            merchantSignin(context, cuid, accountId, messageInitResponse);
                        } else if (merchant == false) {
                            //messageInitResponse.onSuccess(SUCCESS_MESSAGING_INITIALIZED);
                            customHandler.sendMessAnnotations(messageInitResponse, CustomHandler.Mess.ON_SUCCESS, SUCCESS_MESSAGING_INITIALIZED);
                            editor.putString("messaging", "true");
                            editor.apply();
                            editor.commit();
                        }
                    }
                } else {
                    editor.remove("messaging");
                    editor.apply();
                    editor.commit();
                }
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }


    private void merchantSignin(final Context activityContext, final String cuid, final String accountId, final MessageInitResponse messageInitResponse) {
        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activityContext);
            final SharedPreferences.Editor editor = sharedPref.edit();
            ApiInterface apiInterface = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                    create(ApiInterface.class);
            String authToken = PatchCommonUtil.getInstance().getAccessToken(activityContext);
            Call<MerchantSigninResponse> call = apiInterface.merchantSignin(accountId, cuid, authToken);
            call.enqueue(new Callback<MerchantSigninResponse>() {
                @Override
                public void onResponse(Call<MerchantSigninResponse> call, Response<MerchantSigninResponse> response) {
                    if (response.isSuccessful()) {
                        MerchantSigninResponse merchantSigninResponse = response.body();
                        editor.putString("messaging", "true");
                        editor.putString("patch_devToken", merchantSigninResponse.getAccessToken());
                        editor.apply();
                        editor.commit();
                        //messageInitResponse.onSuccess(SUCCESS_MESSAGING_INITIALIZED);
                        customHandler.sendMessAnnotations(messageInitResponse, CustomHandler.Mess.ON_SUCCESS, SUCCESS_MESSAGING_INITIALIZED);
                    } else {
                        if (response.code() == 401) {
                            PatchCommonUtil.getInstance().removeSession(activityContext);
                            customHandler.sendMessAnnotations(messageInitResponse, CustomHandler.Mess.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                            //merchantSignin(activityContext,cuid,accountId, messageInitResponse);
                        }
                    }
                }

                @Override
                public void onFailure(Call<MerchantSigninResponse> call, Throwable t) {
                    //messageInitResponse.onSuccess(ERR_MESSAGING_NOT_INITIALIZED);
                    customHandler.sendMessAnnotations(messageInitResponse, CustomHandler.Mess.ON_SUCCESS, ERR_MESSAGING_NOT_INITIALIZED);
                    editor.remove("messaging");
                    editor.apply();
                    editor.commit();
                }
            });
        } catch (Exception e) {

        }
    }

    public void fetchConversations(final Context context, final String merchantCuid, final FetchConversationCallback fetchConversationCallback) {
        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String messaging = sharedPref.getString("messaging", null);
            if (messaging != null) {
                //for whom these functionality should be enable merchat?, visitor? or both?
                String query =
                        "{\"where\":{\"merchantCuid\":\"" +
                                merchantCuid +
                                "\"},\"include\": {\"relation\":\"messages\", \"scope\": {\"fields\": [\"status\", \"body\",\"senderCuid\"],\"limit\": 1,\"order\": \"createdAt DESC\" }},\"fields\": [\"customerCuid\", \"convoId\"], \"order\":\"createdAt DESC\"}";
                ApiInterface apiInterface = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                        create(ApiInterface.class);
                String chatToken = PatchCommonUtil.getInstance().getDevToken(context);
                Call<List<FetchConversationResponse>> call = apiInterface.fetchMerchantConversions(query, chatToken);
                call.enqueue(new Callback<List<FetchConversationResponse>>() {
                    @Override
                    public void onResponse(Call<List<FetchConversationResponse>> call, Response<List<FetchConversationResponse>> response) {
                        try {
                            if (response.isSuccessful()) {
                                List<FetchConversationResponse> conversationsList = response.body();
                                fetchConversationCallback.onConversationsResponse(conversationsList);
                            } else {
                                if (response.code() == 401) {
                                    PatchCommonUtil.getInstance().removeDevToken(context);
                                    fetchConversationCallback.onFailure(ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                                } else {
                                    fetchConversationCallback.onFailure(ERR_FETCHING_CONVERSATION);
                                }
                            }
                        } catch (Exception e) {
                            fetchConversationCallback.onFailure(ERR_FETCHING_CONVERSATION);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<FetchConversationResponse>> call, Throwable t) {
                        fetchConversationCallback.onFailure(ERR_FETCHING_CONVERSATION);
                    }
                });
            } else {
                fetchConversationCallback.onFailure(ERR_MESSAGING_MUST_BE_INITIALIZED);
            }
        } catch (Exception e) {
        }
    }

    public void fetchChatMessages(final Context context, final String conversationId, final int limit, final int skip, final FetchChatMessageCallback fetchChatMessageCallback) {
        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String messaging = sharedPref.getString("messaging", null);
            if (messaging != null) {
                try {
                    String query =
                            "{\"where\":{\"convoId\":\"" +
                                    conversationId +
                                    "\"},\"limit\":\"" +
                                    limit +
                                    "\", \"skip\":\"" +
                                    skip +
                                    "\", \"order\": \"createdAt DESC\"}";
                    ApiInterface apiInterface = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                            create(ApiInterface.class);
                    String chatToken = PatchCommonUtil.getInstance().getDevToken(context);
                    Call<List<ChatMessages>> call = apiInterface.fetchChatMessages(query, chatToken);
                    call.enqueue(new Callback<List<ChatMessages>>() {
                        @Override
                        public void onResponse(Call<List<ChatMessages>> call, Response<List<ChatMessages>> response) {
                            try {
                                if (response.isSuccessful()) {
                                    List<ChatMessages> chatMessagesList = response.body();
                                    fetchChatMessageCallback.onChatMessagesResponse(chatMessagesList);
                                } else {
                                    if (response.code() == 401) {
                                        PatchCommonUtil.getInstance().removeDevToken(context);
                                        fetchChatMessageCallback.onFailure(ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                                    } else {
                                        fetchChatMessageCallback.onFailure(ERR_FETCHING_CHAT_MESSAGES);
                                    }
                                }
                            } catch (Exception e) {
                                fetchChatMessageCallback.onFailure(ERR_FETCHING_CHAT_MESSAGES);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<ChatMessages>> call, Throwable t) {
                            //Log.d("Patch", String.valueOf(t.toString()));
                            fetchChatMessageCallback.onFailure(ERR_FETCHING_CHAT_MESSAGES);
                        }
                    });
                } catch (Exception e) {

                }
            } else {
                fetchChatMessageCallback.onFailure(ERR_MESSAGING_MUST_BE_INITIALIZED);
            }

        } catch (Exception e) {

        }
    }

    public void fetchUnreadMessageCount(final Context context, final String conversationId, final String receiverCuid, final FetchUnreadMessageCallback fetchUnreadMessageCallback) {
        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String messaging = sharedPref.getString("messaging", null);
            if (messaging != null) {
                String query = "{\"convoId\":\"" +
                        conversationId +
                        "\", \"receiverCuid\":\"" +
                        receiverCuid +
                        "\",\"status\":\"unsent\", \"order\": \"createdAt DESC\"}";
                ApiInterface apiInterface = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                        create(ApiInterface.class);
                String chatToken = PatchCommonUtil.getInstance().getDevToken(context);
                Call<UnreadCount> call = apiInterface.fetchUnreadCount(query, chatToken);
                call.enqueue(new Callback<UnreadCount>() {
                    @Override
                    public void onResponse(Call<UnreadCount> call, Response<UnreadCount> response) {
                        try {
                            if (response.isSuccessful()) {
                                UnreadCount unreadCount = response.body();
                                fetchUnreadMessageCallback.onUnreadCountResponse(unreadCount.getCount());
                            } else {
                                if (response.code() == 401) {
                                    PatchCommonUtil.getInstance().removeDevToken(context);
                                    fetchUnreadMessageCallback.onFailure(ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                                } else {
                                    fetchUnreadMessageCallback.onFailure(ERR_FETCHING_UNREAD_COUNT);
                                }
                            }
                        } catch (Exception e) {
                            fetchUnreadMessageCallback.onFailure(ERR_FETCHING_UNREAD_COUNT);
                        }
                    }

                    @Override
                    public void onFailure(Call<UnreadCount> call, Throwable t) {
                        fetchUnreadMessageCallback.onFailure(ERR_FETCHING_UNREAD_COUNT);
                    }
                });
            } else {
                fetchUnreadMessageCallback.onFailure(ERR_MESSAGING_MUST_BE_INITIALIZED);

            }
        } catch (Exception e) {

        }
    }

    public void markMessageSeen(final Context context, final String conversationId, final MarkMessageSeenCallback markMessageSeenCallback) {
        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String messaging = sharedPref.getString("messaging", null);
            if (messaging != null) {
                ApiInterface apiInterface = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                        create(ApiInterface.class);
                String chatToken = PatchCommonUtil.getInstance().getAccessToken(context);
                Call<Response<Void>> call = apiInterface.markMessagesSeen(conversationId, chatToken);
                call.enqueue(new Callback<Response<Void>>() {
                    @Override
                    public void onResponse(Call<Response<Void>> call, Response<Response<Void>> response) {
                        if (response.isSuccessful()) {
                            markMessageSeenCallback.onMarkMessagesSeenSuccess(SUCCESS_MESSAGES_MARKED_SEEN);
                        } else {
                            if (response.code() == 401) {
                                PatchCommonUtil.getInstance().removeDevToken(context);
                                markMessageSeenCallback.onFailure(ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                            } else {
                                markMessageSeenCallback.onFailure(ERR_MARK_MESSAGES_SEEN);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Response<Void>> call, Throwable t) {
                        markMessageSeenCallback.onFailure(ERR_MARK_MESSAGES_SEEN);
                    }
                });
            } else {
                markMessageSeenCallback.onFailure(ERR_MESSAGING_MUST_BE_INITIALIZED);
            }
        } catch (Exception e) {

        }
    }

    public void fetchCallLogs(final Context context, final String targetCuid, final FetchCallLogsCallback fetchCallLogsCallback) {
        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String messaging = sharedPref.getString("messaging", null);
            if (messaging != null) {
                final String participantQuery =
                        "{\"where\":{\"cuid\":\"" +
                                targetCuid +
                                "\"}, \"fields\": [\"id\"]}";
                final ApiInterface apiInterface = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                        create(ApiInterface.class);
                final String chatToken = PatchCommonUtil.getInstance().getDevToken(context);
                Call<List<FetchParticipantIdResponse>> call1 = apiInterface.fetchParticipantId(participantQuery, chatToken);
                call1.enqueue(new Callback<List<FetchParticipantIdResponse>>() {
                    @Override
                    public void onResponse(Call<List<FetchParticipantIdResponse>> call, Response<List<FetchParticipantIdResponse>> response) {
                        try {
                            if (response.isSuccessful()) {
                                String incomingCallLogQuery =
                                        "{\"where\":{\"participants\":\"" +
                                                response.body().get(0).getId() +
                                                "\",\"inititatorId\":{\"neq\":\"" +
                                                response.body().get(0).getId() +
                                                "\"}},\"include\": {\"relation\":\"initiator\", \"scope\": {\"fields\": [\"name\"]}},\"fields\": [\"createdAt\", \"status\",\"initiatorId\"],\"order\": \"createdAt DESC\"}";

                                final Call<List<CallLogsResponse>> call2 = apiInterface.fetchCallLogs(incomingCallLogQuery, chatToken);
                                call2.enqueue(new Callback<List<CallLogsResponse>>() {
                                    @Override
                                    public void onResponse(Call<List<CallLogsResponse>> call, Response<List<CallLogsResponse>> response) {
                                        try {
                                            if (response.isSuccessful()) {
                                                fetchCallLogsCallback.onCallLogsResponse(response.body());
                                            } else {
                                                if (response.code() == 401) {
                                                    PatchCommonUtil.getInstance().removeDevToken(context);
                                                    fetchCallLogsCallback.onFailure(ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                                                } else {
                                                    fetchCallLogsCallback.onFailure(ERR_FETCHING_CALL_LOG);
                                                }
                                            }
                                        } catch (Exception e) {
                                            fetchCallLogsCallback.onFailure(ERR_FETCHING_CALL_LOG);
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<List<CallLogsResponse>> call, Throwable t) {
                                        fetchCallLogsCallback.onFailure(ERR_FETCHING_CALL_LOG);
                                    }
                                });
                            } else {
                                if (response.code() == 401) {
                                    PatchCommonUtil.getInstance().removeDevToken(context);
                                    fetchCallLogsCallback.onFailure(ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                                } else {
                                    fetchCallLogsCallback.onFailure(ERR_FETCHING_CALL_LOG);
                                }
                            }
                        } catch (Exception e) {

                        }
                    }

                    @Override
                    public void onFailure(Call<List<FetchParticipantIdResponse>> call, Throwable t) {
                        fetchCallLogsCallback.onFailure(ERR_FETCHING_CALL_LOG);
                    }
                });
            } else {
                fetchCallLogsCallback.onFailure(ERR_MESSAGING_MUST_BE_INITIALIZED);
            }
        } catch (Exception e) {

        }
    }

    private void storePreferences(Context context, Boolean enablePatchNotification, Boolean enableNotificationUI, String notificationReciever, /*String sentimentReciever,*/
                                  String missedCallActionReceiverHost, String missedCallActionInitiatorHost) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(context.getString(R.string.patch_notification), enablePatchNotification);
            editor.putBoolean(context.getString(R.string.patch_notificationUI), enableNotificationUI);
            editor.putString(context.getString(R.string.patch_notificationlistener), String.valueOf(notificationReciever));
            //editor.putString(context.getString(R.string.patch_sentimentReciever), String.valueOf(sentimentReciever));
            editor.putString(context.getString(R.string.patch_missedCallInitiatorHost), String.valueOf(missedCallActionInitiatorHost));
            editor.putString(context.getString(R.string.patch_missedCallReceiverHost), String.valueOf(missedCallActionReceiverHost));
            editor.commit();
            editor.apply();
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    private void removeNotificationPreferences(Context context) {
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(context.getString(R.string.patch_notification));
            editor.remove(context.getString(R.string.patch_notificationUI));
            editor.remove(context.getString(R.string.patch_notificationlistener));
            //  editor.remove(context.getString(R.string.patch_sentimentReciever));
            editor.remove(context.getString(R.string.patch_missedCallReceiverHost));
            editor.remove(context.getString(R.string.patch_missedCallInitiatorHost));
            editor.apply();
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    Context context;
    JSONObject initJson;
    PatchInitResponse patchInitResponse;

    private void initializeSdk() {
        try {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        socketInit = SocketInit.getInstance();
                        if (socketInit == null) {
                            return;
                        }
                        if (socketInit.getContext() == null || socketInit.getInitJsonOptions() == null || socketInit.getPatchInitResponse() == null) {
                            return;
                        }
                        context = socketInit.getContext();
                        initJson = socketInit.getInitJsonOptions();
                        patchInitResponse = socketInit.getPatchInitResponse();

                        boolean isInternetConnected = PatchCommonUtil.getInstance().hasInternetAccess(context);
                        if (isInternetConnected) {
                            String numberRegex = "\\d+";
                            String cuid;
                            String malformedCuidRegex = "^(\\d{0,}(?:[-,.\"'*+:@!#$%^&*()_=~<>|\\}{])\\d{0,})*$";
                            String parsedCuidRegex = "^[a-zA-Z0-9 \\@._-]*$";
                            if (initJson.has("name") && initJson.getString("name").length() > 25) {
                                sdkReady = true;
                                //patchInitResponse.onFailure(ERR_NAME_LENGTH_EXCEEDED_BY_25);
                                customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_NAME_LENGTH_EXCEEDED_BY_25);
                                return;
                            }
                            /*if (!initJson.has("fcmToken")) {
                                isGoodToGo = false;
                                customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_FCM_TOKEN_REQUIRED);
                                return;
                            }*/
                            //if fcmToken filed is present inside initJson then only this condition will be passed
                            if (initJson.has("fcmToken")) {
                                if (initJson.getString("fcmToken") == null || initJson.getString("fcmToken").equals("")) {
                                    sdkReady = true;
                                    customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INVALID_FCM_TOKEN);
                                    return;
                                }
                            }
                            if ((!initJson.has("accountID") || initJson.getString("accountID") == "") ||
                                    (!initJson.has("apikey") || initJson.getString("apikey") == "")) {
                                sdkReady = true;
                                //patchInitResponse.onFailure(ERR_INVALID_ACCOUNTID_OR_APIKEY);
                                customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_BOTH_ACCOUNTID_AND_APIKEY_REQUIRED);
                                return;
                            }
                            if (initJson.has("cuid") && initJson.getString("cuid").length() > 0) {
                                if (initJson.getString("cuid").trim().length() < 5 || initJson.getString("cuid").trim().length() > 50) {
                                    sdkReady = true;
                                    //patchInitResponse.onFailure(ERR_INVALID_CUID);
                                    customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INVALID_LENGH_CUID);
                                    return;
                                } else if (!(Pattern.compile((parsedCuidRegex)).matcher(initJson.getString("cuid")).matches())
                                        ||
                                        (Pattern.compile((malformedCuidRegex)).matcher(initJson.getString("cuid")).matches())) {
                                    sdkReady = true;
                                    customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_CUID_CAN_NOT_HAVE_SPECIAL_CHARS_BETWEEN_NUMBERS);
                                    return;
                                } else if ((initJson.has("phone") && initJson.getString("phone").length() > 0) ||
                                        (initJson.has("cc") && initJson.getString("cc").length() > 0)) {
                                    if (initJson.getString("phone").length() < 6 || initJson.getString("phone").length() > 15 || !(initJson.getString("phone").matches(numberRegex))) {
                                        sdkReady = true;
                                        //patchInitResponse.onFailure(ERR_INVALID_PHONE_NUMBER);
                                        customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INVALID_PHONE_NUMBER);
                                        return;
                                    }
                                    if (initJson.getString("cc").length() < 1 || initJson.getString("cc").length() > 4 || !(initJson.getString("cc").matches(numberRegex))) {
                                        sdkReady = true;
                                        //patchInitResponse.onFailure(ERR_INVALID_CC);
                                        customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INVALID_CC);
                                        return;
                                    }
                                }
                            } else if (initJson.has("phone") && initJson.getString("phone").length() > 0
                                    && initJson.has("cc") && initJson.getString("cc").length() > 0) {
                                if (initJson.getString("phone").length() < 6 || initJson.getString("phone").length() > 15 || !(initJson.getString("phone").matches(numberRegex))) {
                                    sdkReady = true;
                                    //patchInitResponse.onFailure(ERR_INVALID_PHONE_NUMBER);
                                    customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INVALID_PHONE_NUMBER);
                                    return;
                                }
                                if (initJson.getString("cc").length() < 1 || initJson.getString("cc").length() > 4 || !(initJson.getString("cc").matches(numberRegex))) {
                                    sdkReady = true;
                                    //patchInitResponse.onFailure(ERR_INVALID_CC);
                                    customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INVALID_CC);
                                    return;
                                }
                                if (initJson.has("cuid") && initJson.getString("cuid").trim().length() == 0) {
                                    initJson.remove("cuid");
                                }
                            } else {
                                sdkReady = true;
                                //patchInitResponse.onFailure(ERR_EITHER_CC_AND_PHONE_OR_CUID_NEEDED);
                                customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_EITHER_CC_AND_PHONE_OR_CUID_NEEDED);
                                return;
                            }

                            if ((context == null)) {
                                sdkReady = true;
                                //patchInitResponse.onFailure(ERR_INVALID_APP_CONTEXT);
                                customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INVALID_ACTIVITY_CONTEXT);
                                return;
                            }

                            getBaseUrl(context, initJson.getString("accountID"), initJson.getString("apikey"), patchInitResponse);
                        } else {
                            sdkReady = true;
                            //patchInitResponse.onFailure(ERR_NETWORK_NOT_AVAILABLE);
                            customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_NETWORK_NOT_AVAILABLE);
                        }
                    } catch (Exception e) {
                        sdkReady = true;
                        //patchInitResponse.onFailure(ERR_INCORRECT_PARAMS_IN_SDK_INITIALIZATION);
                        customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INCORRECT_PARAMS_IN_SDK_INITIALIZATION);
                        if (context != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            sdkReady = true;
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    private void getBaseUrl(final Context context, final String accountID, final String apikey, final PatchInitResponse patchInitResponse) {
        try {
            ApiUrlRequest apiUrlRequest = new ApiUrlRequest();
            apiUrlRequest.setApiKey(apikey);
            Call<Object> call = apiService.getBaseUrl(accountID, apiUrlRequest);
            call.enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    try {
                        Log.d("patchsharma", String.valueOf(response.code()));
                        if (response.isSuccessful()) {
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor editor = sharedPref.edit();
                            editor.putString(context.getString(R.string.prefs_baseurl),
                                    response.body().toString());
                            editor.apply();
                            editor.commit();

                            signin();
                        } else {
                                        /*SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putString(context.getString(R.string.prefs_baseurl),
                                                *//*response.body().toString()*//*
                                         "https://papigcp-demo.patchus.in/api/v2/");
                                        editor.apply();
                                        editor.commit();
                                        signin();*/
                            //when 401, 400,404 error code received
                            if (response.code() == 400) {
                                customHandler.sendInitAnnotations(PatchSDK.this.patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_INVALID_ACCOUNTID_OR_APIKEY);
                            } else {
                                customHandler.sendInitAnnotations(PatchSDK.this.patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED);
                            }
                        }
                    } catch (Exception e) {
                        try {
                            customHandler.sendInitAnnotations(PatchSDK.this.patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED);
                        } catch (Exception e1) {

                        }
                    }
                }

                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    try {
                        customHandler.sendInitAnnotations(PatchSDK.this.patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED);
                    } catch (Exception e) {

                    }
                }
            });
        } catch (Exception e) {

        }

    }

    private void signin() {
        try {
            registerContact(context, initJson.has("fcmToken") ? initJson.getString("fcmToken") : null
                    , initJson.has("appId") ? initJson.getString("appId") : "",
                    initJson.has("name") ? initJson.getString("name") : "",
                    initJson.has("cc") ? initJson.getString("cc") : "",
                    initJson.has("phone") ? initJson.getString("phone") : "",
                    initJson.getString("accountID"),
                    initJson.getString("apikey"),
                    initJson.has("ringtone") ? initJson.getString("ringtone") : null,
                    initJson.has("cuid") ? initJson.getString("cuid") : null, new PatchInitResponse() {
                        @Override
                        public void onSuccess(int success) {
                            //patchInitResponse.onSuccess(SUCCESS_PATCH_SDK_INITIALIZED);
                            //customHandler.sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, SUCCESS_PATCH_SDK_INITIALIZED);
                            sdkReady = true;
                        }

                        @Override
                        public void onFailure(int failure) {
                            if (failure == ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE) {
                                sdkReady = true;
                                //patchInitResponse.onFailure(ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE);
                                customHandler.sendInitAnnotations(PatchSDK.this.patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_SDK_NOT_INITIALIZED_DUE_TO_CUID_ALREADY_CONNECTED_ELSEWHERE);
                            } else {
                                sdkReady = true;
                                //patchInitResponse.onFailure(ERR_PATCH_SDK_NOT_INITIALIZED);
                                customHandler.sendInitAnnotations(PatchSDK.this.patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED);
                            }
                        }
                    });
        } catch (Exception e) {

        }
    }

    /**
     * clears all the values stored in sharedPreferences by the patch sdk.
     *
     * @param logoutContext:- context of the activity from which this function is called.
     */

    public void logout(Context logoutContext) {
        try {
            if (SocketIOManager.getSocket() != null) {
                SocketIOManager.setSocketInstanceNull();
                SocketIOManager.setIsUnAuthorized(false);
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(logoutContext);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("patch_session");
            editor.remove(logoutContext.getString(R.string.patch_missedCallReceiverActions));
            editor.remove(logoutContext.getString(R.string.patch_missedCallInitiatorActions));
            editor.remove("patch_accessToken");
            editor.remove("patch_baseurl");
            editor.remove("patch_devToken");
            editor.remove("patch_contactId");
            editor.remove(logoutContext.getResources().getString(R.string.sna));
            editor.remove(logoutContext.getResources().getString(R.string.patch_token));
            editor.remove("patch_userName");
            editor.remove("patch_userCC");
            editor.remove("message");
            editor.remove("patch_userPhone");
            editor.remove("patch_accountId");
            editor.remove("patch_apikey");
            editor.remove("patch_fontColor");
            editor.remove("patch_bgColor");
            editor.remove("patch_logo");
            editor.remove("patch_ringtone");
            editor.remove("patch_cuid");
            editor.remove("patch_token");
            editor.remove("messaging");
            editor.remove("patch_neutralIntent");
            editor.remove(logoutContext.getResources().getString(R.string.patch_template_scratchcard_config));
            editor.remove(logoutContext.getResources().getString(R.string.patch_template_pinview_config));
            editor.apply();
            editor.commit();

            try {
                jobScheduler = (JobScheduler) logoutContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (jobScheduler != null) {
                    jobScheduler.cancelAll();
                }
            } catch (Exception e) {

            }
            try {
                removeNotificationPreferences(logoutContext);
            } catch (Exception e) {

            }
        } catch (Exception e) {
            if (logoutContext != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), logoutContext);
        }
    }

    /**
     * called when the user is trying to make a call using phone and cc.
     *
     * @param activityContext:-      context of the activity from which this method is called.
     * @param callee:-               jsonObject containing phone and cc.
     * @param callOptions:-          jsonObject containig call options passed while making a call.
     * @param outgoingCallResponse:- statuses of the call to be returned while making a call i.e. success or failure.
     * @throws JSONException
     */
    public void pstnToPstnCall(final Context activityContext, final JSONObject callee, final JSONObject callOptions, final OutgoingCallResponse outgoingCallResponse) {

        try {
            sdkReady = false;
            final SocketInit socketInit = SocketInit.getInstance();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (PatchCommonUtil.getInstance().hasInternetAccess(activityContext)) {
                            if (socketInit.getSocket() == null) {
                                sdkReady = true;
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                //outgoingCallResponse.onFailure(ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                return;
                            }
                            if (SocketIOManager.getIsUnAuthorized()) {
                                sdkReady = true;
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CUID_ALREADY_CONNECTED_ELSEWHERE);
                                return;
                            }
                            String numberRegex = "\\d+";
                            if (!callee.has("cc") || !callee.has("phone")) {
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_CALLEE_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CALLEE_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN);
                                return;
                            }
                            if (callee.getString("phone").length() < 6 || callee.getString("phone").length() > 20 /*|| !(callee.getString("phone").matches(numberRegex))*/) {
                                //Log.d("PATCH", "Please pass a valid phone number");
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_INVALID_PHONE_NUMBER_LENGTH);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_PHONE_NUMBER_LENGTH);
                                return;
                            }
                            if (callee.getString("cc").length() < 1 || callee.getString("cc").length() > 4  /*!(callee.getString("cc").matches(numberRegex))*/) {
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_INVALID_CC_LENGTH);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_CC_LENGTH);
                                return;
                            }

                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activityContext);
                            if (sharedPref.getString("patch_ecta", null) == "true") {
                                if (callOptions.has("callToken") && callOptions.getString("callToken") != null && callOptions.getString("callToken") != "") {
                                } else {
                                    sdkReady = true;
                                    //outgoingCallResponse.onFailure(ERR_EXPECT_CALL_TOKEN_TO_MAKE_CALL);
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_EXPECT_CALL_TOKEN_TO_MAKE_CALL);
                                    // Log.d("Patch", "CallToken required to place call");
                                    return;
                                }
                            }
                            String cc = sharedPref.getString("patch_userCC", "");
                            String phone = sharedPref.getString("patch_userPhone", "");
                            if (cc.length() == 0 || phone.length() == 0) {
                                //cc phone reuired to place pstn to pstn call
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_CALLER_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CALLER_CC_PHONE_NEEDED_TO_MAKE_PSTN_TO_PSTN);
                                return;
                            } else {
                                if (!(getVerifiedNumbers(activityContext).size() > 0)) {
                                    //Log.d("Patch", "no verified clis");
                                    sdkReady = true;
                                    //outgoingCallResponse.onFailure(ERR_EMPTY_VERIFIED_CLI_LIST);
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_EMPTY_VERIFIED_CLI_LIST);
                                    return;
                                } else if (callOptions.has("cli")) {
                                    JSONObject cliJson = callOptions.getJSONObject("cli");
                                    if (!cliJson.has("cc") || !cliJson.has("phone")) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_MISSING_CC_OR_PHONE_IN_CLI);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MISSING_CC_OR_PHONE_IN_CLI);
                                        return;
                                    }
                                    String cliCC = cliJson.getString("cc");
                                    String cliPhone = cliJson.getString("phone");
                                    if (cliCC.length() > 0 && cliPhone.length() > 0) {
                                        if (isCliPassedAuthorized(activityContext, cliJson)) {
                                            //Log.d("Patch", "cli passed is authorized");
                                            SocketInit.getInstance().setCli(cliJson);

                                        } else {
                                            // Log.d("Patch", "cli passed is unauthorized");
                                            sdkReady = true;
                                            //outgoingCallResponse.onFailure(ERR_UNAUTHORIZED_CLI);
                                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_UNAUTHORIZED_CLI);
                                            return;
                                        }
                                    } else {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                                        return;
                                    }
                                } else {
                                    SocketInit.getInstance().setCli(getCliFromList(activityContext, 0));
                                }
//                if (callOptions.has("cli")) {
//                    JSONObject cliJson = callOptions.getJSONObject("cli");
//                    if (cliJson.has("cc") || cliJson.has("phone")) {
//                        if (cliJson.getString("cc").length() > 0 && cliJson.getString("phone").length() > 0) {
//                            SocketInit.getInstance().setCli(cliJson);
//                        }
//                    }
//                }
                            }
                            //  }
                            JSONObject caller = new JSONObject();
                            caller.put("cc", sharedPref.getString("patch_userCC", ""));
                            caller.put("phone", sharedPref.getString("patch_userPhone", ""));

                            socketInit.setOutgoingCallResponse(outgoingCallResponse);
                            jobSchedulerSocketService = JobSchedulerSocketService.getInstance(activityContext);
                            if (SocketIOManager.isSocketConnected()) {
                                jobSchedulerSocketService.makeCall(caller, callee, callOptions, outgoingCallResponse);
                            } else {
                                //outgoingCallResponse.onFailure(ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                sdkReady = true;
                            }
                        } else {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_NETWORK_NOT_AVAILABLE);
                        }

                    } catch (Exception e) {
                        sdkReady = true;
                        if (context != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                    }
                }
            }).start();
        } catch (Exception e) {
            sdkReady = true;
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }

    }

    public void notify(final Context activityContext, final JSONObject notifyOptions, final NotificationResponse outgoingNotificationResponse) {
        try {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (PatchCommonUtil.getInstance().hasInternetAccess(activityContext)) {
                            SocketInit socketInit = SocketInit.getInstance();
                            if (socketInit.isPatchNotificationEnable() != null && socketInit.isPatchNotificationEnable() == true
                                    && socketInit.getNotificationListener() != null
                                /*&& socketInit.getSentimentReceiver() != null*/) {

                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activityContext);
                                try {
                                    if (socketInit.getSocket() == null) {
                                        //outgoingNotificationResponse.onFailure(PatchResponseCodes.NotificationResponse.OnFailure.ERR_SOMETHING_WENT_WRONG);
                                        customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE,  PatchResponseCodes.NotificationCallback.OnFailure.ERR_SOMETHING_WENT_WRONG);
                                        return;
                                    }
                                    if (SocketIOManager.getIsUnAuthorized()) {
                                        //sdkReady = true;
                                        customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE,  PatchResponseCodes.NotificationCallback.OnFailure.ERR_CUID_ALREADY_CONNECTED_ELSEWHERE);
                                        return;
                                    }
                                    if (notifyOptions.has("payload") && notifyOptions.get("payload") instanceof JSONObject
                                            && notifyOptions.has("cuid") && notifyOptions.get("cuid") instanceof String) {
                                        JSONObject payload = notifyOptions.getJSONObject("payload");
                                        if (!payload.has("title") || !payload.has("body") /*|| !payload.has("picture")*/) {
                                            customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE,  ERR_INCORRECT_NOTIFICATION_PARAMS_IN_SDK_INITIALIZATION);
                                            return;
                                        }

                                        if (notifyOptions.has("buttons")) {
                                            Boolean isButtonsValid = true;
                                            if (notifyOptions.get("buttons") instanceof JSONObject) {
                                                Iterator<String> itr = notifyOptions.getJSONObject("buttons").keys();
                                                while (itr.hasNext()) {
                                                    String key = itr.next();
                                                    if (key.equals("1") || key.equals("2") || key.equals("3")) {
                                                        continue;
                                                    } else {
                                                        isButtonsValid = false;
                                                        break;
                                                    }
                                                }
                                            }

                                            if (!isButtonsValid) {
                                                customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE,  ERR_INCORRECT_NOTIFICATION_PARAMS_IN_SDK_INITIALIZATION);
                                                return;
                                            }
                                        }
                                    } else {
                                        customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE,  ERR_INCORRECT_NOTIFICATION_PARAMS_IN_SDK_INITIALIZATION);
                                        return;
                                    }
                                    String senderCuid = sharedPref.getString("patch_cuid", null);
                                    if (senderCuid == null || senderCuid.isEmpty()) {
                                        customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE, ERR_INCORRECT_NOTIFICATION_PARAMS_IN_SDK_INITIALIZATION);
                                        return;
                                    }

                                    notifyOptions.put("senderCuid", senderCuid);
                                    socketInit.setOutgoingNotificationResponse(outgoingNotificationResponse);
                                    jobSchedulerSocketService = JobSchedulerSocketService.getInstance(context);
                                    if (SocketIOManager.isSocketConnected()) {
                                        jobSchedulerSocketService.sendNotification(notifyOptions, outgoingNotificationResponse);
                                    } else {
                                        //outgoingNotificationResponse.onFailure(ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                        customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE, ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                    }
                                } catch (Exception e) {
                                    if (activityContext != null)
                                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), activityContext);
                                }

                            } else {
                                customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE, ERR_NOTIFICATION_INITIALIZATION_REQUIRED);
                                return;
                            }
                        } else {
                            //sdkReady = true;
                            customHandler.sendNotiAnnotations(outgoingNotificationResponse, CustomHandler.Noti.ON_FAILURE,  ERR_NETWORK_NOT_AVAILABLE);
                        }
                    } catch (Exception e) {
                        if (activityContext != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), activityContext);
                    }
                }
            }).start();
        } catch (Exception e) {
            if (activityContext != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), activityContext);
        }
    }

    /**
     * called when the user is trying to make a call using phone and cc.
     *
     * @param activityContext:-      context of the activity from which this method is called.
     * @param callee:-               jsonObject containing phone and cc.
     * @param context:-              context of the call.
     * @param callOptions:-          jsonObject containig call options passed while making a call.
     * @param outgoingCallResponse:- statuses of the call to be returned while making a call i.e. success or failure.
     * @throws JSONException
     */
    public void call(final Context activityContext, final JSONObject callee, final String context, final JSONObject callOptions, final OutgoingCallResponse outgoingCallResponse) throws JSONException {
        try {
            sdkReady = false;
            final String[] PERMISSIONS = {
                    android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    android.Manifest.permission.RECORD_AUDIO
            };
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (activityContext == null) {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_INVALID_ACTIVITY_CONTEXT);
                            return;
                        }
                        if (callOptions == null) {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CALL_OPTIONS_REQUIRED);
                            return;
                        }
                        if (PatchCommonUtil.getInstance().hasInternetAccess(activityContext)) {
                            if (PatchCommonUtil.getInstance().hasPermissions(activityContext, PERMISSIONS)) {
                                if (!PatchCommonUtil.getInstance().isNetworkBandwidthGood(activityContext)) {
                                    sdkReady = true;
                                    //outgoingCallResponse.onFailure(ERR_BAD_NETWORK);
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_BAD_NETWORK);
                                } else {
                                    voipOrPstnCall(activityContext, callee, context, callOptions, outgoingCallResponse);
                                }
                            } else {
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_MICROPHONE_PERMISSION_NOT_GRANTED);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MICROPHONE_PERMISSION_NOT_GRANTED);
                            }
                        } else {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_NETWORK_NOT_AVAILABLE);
                        }
                    } catch (Exception e) {
                        sdkReady = true;
                        if (activityContext != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), activityContext);
                    }
                }
            }).start();
        } catch (Exception e) {
            sdkReady = true;
            if (activityContext != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), activityContext);
        }
    }

    private void voipOrPstnCall(Context activityContext, JSONObject callee, final String context, JSONObject callOptions, OutgoingCallResponse outgoingCallResponse) {
        try {
            if (SocketIOManager.getIsUnAuthorized()) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CUID_ALREADY_CONNECTED_ELSEWHERE);
                return;
            }
            SocketInit socketInit = SocketInit.getInstance();
            JSONArray tags = null;
            if (callOptions.has("tags") && callOptions.get("tags") instanceof JSONArray) {
                tags = callOptions.getJSONArray("tags");
            }
            if (!callOptions.has("pstn")) {
                callOptions.put("pstn", false);
            }
            if (!callOptions.has("recording")) {
                callOptions.put("recording", false);
            }
            if (!callOptions.has("webhook")) {
                callOptions.put("webhook", "");
            }
            if (!callOptions.has("var1")) {
                callOptions.put("var1", "");
            }
            if (!callOptions.has("var2")) {
                callOptions.put("var2", "");
            }
            if (!callOptions.has("var3")) {
                callOptions.put("var3", "");
            }
            if (!callOptions.has("var4")) {
                callOptions.put("var4", "");
            }
            if (!callOptions.has("var5")) {
                callOptions.put("var5", "");
            }
            String webhook = callOptions.getString("webhook");
            if (socketInit.getSocket() == null) {
                //outgoingCallResponse.onFailure(ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                sdkReady = true;
                return;
            }
            if (context == null || context.equals("")) {
                sdkReady = true;
                //outgoingCallResponse.onFailure(PatchResponseCodes.OutgoingCallResponse.OnFailure.ERR_INVALID_CUID);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CALL_CONTEXT_REQUIRED);
                return;
            }
            if (context.length() > 64) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CALL_CONTEXT_LENGTH_EXCEEDED_BY_64);
                return;
            }
            String numberRegex = "\\d+";
            if (!callee.has("cc") || !callee.has("phone")) {
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_BOTH_CC_AND_PHONE_REQUIRED);
                return;
            }
            if (callee.has("cc") &&
                    (callee.getString("cc").length() < 1 || callee.getString("cc").length() > 4 || !(callee.getString("cc").matches(numberRegex)))) {
                //Log.d("PATCH", "Please pass a valid country code.");
                //showtoast(activityContext,"Please pass a valid country code.");
                sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_INVALID_CC_LENGTH);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_CC_LENGTH);
                return;
            }
            if (callee.has("phone") &&
                    ((callee.getString("phone").length() < 6) || callee.getString("phone").length() > 20 || !(callee.getString("phone").matches(numberRegex)))) {
                //Log.d("PATCH", "Please pass a valid phone number");
                //showtoast(activityContext,"Please pass a valid phone number");
                sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_INVALID_PHONE_NUMBER_LENGTH);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_PHONE_NUMBER_LENGTH);
                return;
            }
            if (tags != null) {
                if (tags.length() > 0) {
                    if (tags.length() > 10) {
                        sdkReady = true;
                        //outgoingCallResponse.onFailure(ERR_TAGS_EXCEEDED_BY_10);
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_TAGS_COUNT_EXCEEDED_BY_10);
                        return;
                    }
                    for (int i = 0; i < tags.length(); i++) {
                        if (tags.getString(i).length() > 32) {
                            //Log.d("PATCH", "length of tag " + tags.getString(i) + " at position " + (i + 1) + " is more than 32");
                            sdkReady = true;
                            //outgoingCallResponse.onFailure(ERR_TAG_LENGTH_EXCEEDED_BY_32);
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_TAG_LENGTH_EXCEEDED_BY_32);
                            return;
                        }
                    }
                } else {
                    //Log.d("PATCH", "no tags are specified");
                }
            }
            if (webhook.length() > 0) {
                try {
                    new URL(webhook).toURI();
                } catch (MalformedURLException e) {
                    //e.printStackTrace();
                    //Log.d("PATCH", "Please enter the correct webhook");
                    sdkReady = true;
                    //outgoingCallResponse.onFailure(ERR_INVALID_FORMAT_OF_WEBHOOK);
                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_FORMAT_OF_WEBHOOK);
                    return;
                } catch (URISyntaxException e) {
                    //e.printStackTrace();
                    //Log.d("PATCH", "Please enter the correct webhook");
                    sdkReady = true;
                    //outgoingCallResponse.onFailure(ERR_INVALID_FORMAT_OF_WEBHOOK);
                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_FORMAT_OF_WEBHOOK);
                    return;
                } catch (Exception e) {

                }
            }
            if (callOptions.getString("var1").length() > 128) {
                sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                return;
            }
            if (callOptions.getString("var2").length() > 128) {
                sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                return;
            }
            if (callOptions.getString("var3").length() > 128) {
                sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                return;
            }
            if (callOptions.getString("var4").length() > 128) {
                sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                return;
            }
            if (callOptions.getString("var5").length() > 128) {
                sdkReady = true;
                //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                return;
            }
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activityContext);
            if (sharedPref.getString("patch_ecta", null) == "true") {
                if (callOptions.has("callToken") && callOptions.getString("callToken") != null && callOptions.getString("callToken") != "") {
                } else {
                    //outgoingCallResponse.onFailure(ERR_EXPECT_CALL_TOKEN_TO_MAKE_CALL);
                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_EXPECT_CALL_TOKEN_TO_MAKE_CALL);
                    sdkReady = true;
                    //Log.d("Patch", "CallToken required to place call");
                    return;
                }
            }
            String cc = sharedPref.getString("patch_userCC", "");
            String phone = sharedPref.getString("patch_userPhone", "");
            String userCid = sharedPref.getString("patch_cuid", "");
            if ((callOptions.getBoolean("pstn") ||
                    (callOptions.has("autoFallback") && callOptions.getBoolean("autoFallback")))
                    && userCid.length() > 0) {
                if (cc.length() == 0 && phone.length() == 0) {
                    if (!(getVerifiedNumbers(activityContext).size() > 0)) {
                        //Log.d("Patch", "no verified clis");
                        sdkReady = true;
                        //outgoingCallResponse.onFailure(ERR_EMPTY_VERIFIED_CLI_LIST);
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_EMPTY_VERIFIED_CLI_LIST);
                        return;
                    } else if (callOptions.has("cli")) {
                        JSONObject cliJson = callOptions.getJSONObject("cli");
                        if (!cliJson.has("cc") || !cliJson.has("phone")) {
                            sdkReady = true;
                            //outgoingCallResponse.onFailure(ERR_MISSING_CC_OR_PHONE_IN_CLI);
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MISSING_CC_OR_PHONE_IN_CLI);
                            return;
                        }
                        String cliCC = cliJson.getString("cc");
                        String cliPhone = cliJson.getString("phone");
                        if (cliCC.length() > 0 && cliPhone.length() > 0) {
                            if (isCliPassedAuthorized(activityContext, cliJson)) {
                                //Log.d("Patch", "cli passed is authorized");
                                SocketInit.getInstance().setCli(cliJson);

                            } else {
                                //Log.d("Patch", "cli passed is unauthorized");
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_UNAUTHORIZED_CLI);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_UNAUTHORIZED_CLI);
                                return;
                            }
                        } else {
                            sdkReady = true;
                            //outgoingCallResponse.onFailure(ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                            return;
                        }
                    } else {
                        SocketInit.getInstance().setCli(getCliFromList(activityContext, 0));
                    }
                } else {
                    if (!(getVerifiedNumbers(activityContext).size() > 0)) {
                        sdkReady = true;
                        //outgoingCallResponse.onFailure(ERR_EMPTY_VERIFIED_CLI_LIST);
                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_EMPTY_VERIFIED_CLI_LIST);
                        return;
                    } else if (callOptions.has("cli")) {
                        JSONObject cliJson = callOptions.getJSONObject("cli");
                        if (!cliJson.has("cc") || !cliJson.has("phone")) {
                            sdkReady = true;
                            //outgoingCallResponse.onFailure(ERR_MISSING_CC_OR_PHONE_IN_CLI);
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MISSING_CC_OR_PHONE_IN_CLI);
                            return;
                        }
                        String cliCC = cliJson.getString("cc");
                        String cliPhone = cliJson.getString("phone");
                        if (cliCC.length() > 0 && cliPhone.length() > 0) {
                            if (isCliPassedAuthorized(activityContext, cliJson)) {
                                //Log.d("Patch", "cli passed is authorized");
                                SocketInit.getInstance().setCli(cliJson);

                            } else {
                                //Log.d("Patch", "cli passed is unauthorized");
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_UNAUTHORIZED_CLI);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_UNAUTHORIZED_CLI);
                                return;
                            }
                        } else {
                            sdkReady = true;
                            //outgoingCallResponse.onFailure(ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                            return;
                        }
                    } else {
                        SocketInit.getInstance().setCli(getCliFromList(activityContext, 0));
                    }
                }
            }

            socketInit.setOutgoingCallResponse(outgoingCallResponse);
            jobSchedulerSocketService = JobSchedulerSocketService.getInstance(activityContext);
            if (SocketIOManager.isSocketConnected()) {
                jobSchedulerSocketService.makeCall(callee.getString("cc"), callee.getString("phone"), "", context, callOptions, outgoingCallResponse, null);
            } else {
                //outgoingCallResponse.onFailure(ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                sdkReady = true;
                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
            }
        } catch (Exception e) {
            sdkReady = true;
            if (activityContext != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), activityContext);
        }
    }

    /**
     * called when the user is trying to make a call using cuid.
     *
     * @param activityContext:-      context of the activity from which user is trying to make a call.
     * @param calleeCuid:-           cuid of the user.
     * @param context:-              context of the call.
     * @param callOptions:-          jsonObject containing call options passed while making a call.
     * @param outgoingCallResponse:- statuses of the call to be returned while making a call i.e. success or failure.
     * @throws JSONException
     */

    public void call(final Context activityContext, final String calleeCuid, final String context, final JSONObject callOptions, final OutgoingCallResponse outgoingCallResponse) {
        try {
            sdkReady = false;
            final String[] PERMISSIONS = {
                    android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                    android.Manifest.permission.RECORD_AUDIO
            };

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (activityContext == null) {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_INVALID_ACTIVITY_CONTEXT);
                            return;
                        }
                        if (callOptions == null) {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CALL_OPTIONS_REQUIRED);
                            return;
                        }
                        if (PatchCommonUtil.getInstance().hasInternetAccess(activityContext)) {
                            if (PatchCommonUtil.getInstance().hasPermissions(activityContext, PERMISSIONS)) {
                                if (!PatchCommonUtil.getInstance().isNetworkBandwidthGood(activityContext)) {
                                    sdkReady = true;
                                    //outgoingCallResponse.onFailure(ERR_BAD_NETWORK);
                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_BAD_NETWORK);
                                } else {
                                    if (SocketIOManager.getIsUnAuthorized()) {
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CUID_ALREADY_CONNECTED_ELSEWHERE);
                                        sdkReady = true;
                                        return;
                                    }
                                    SocketInit socketInit = SocketInit.getInstance();
                                    JSONArray tags = null;
                                    if (callOptions.has("tags") && callOptions.get("tags") instanceof JSONArray) {
                                        tags = callOptions.getJSONArray("tags");
                                    }
                                    if (!callOptions.has("pstn")) {
                                        callOptions.put("pstn", false);
                                    }
                                    if (!callOptions.has("recording")) {
                                        callOptions.put("recording", false);
                                    }
                                    if (!callOptions.has("webhook")) {
                                        callOptions.put("webhook", "");
                                    }
                                    if (!callOptions.has("var1")) {
                                        callOptions.put("var1", "");
                                    }
                                    if (!callOptions.has("var2")) {
                                        callOptions.put("var2", "");
                                    }
                                    if (!callOptions.has("var3")) {
                                        callOptions.put("var3", "");
                                    }
                                    if (!callOptions.has("var4")) {
                                        callOptions.put("var4", "");
                                    }
                                    if (!callOptions.has("var5")) {
                                        callOptions.put("var5", "");
                                    }
                                    String webhook = callOptions.getString("webhook");
                                    if (socketInit.getSocket() == null) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                        return;
                                    }
                                    if (context == null || context.equals("")) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(PatchResponseCodes.OutgoingCallResponse.OnFailure.ERR_INVALID_CUID);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CALL_CONTEXT_REQUIRED);
                                        return;
                                    }
                                    if (context.length() > 64) {
                                        sdkReady = true;
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_CALL_CONTEXT_LENGTH_EXCEEDED_BY_64);
                                        return;
                                    }
                                    if (calleeCuid == null || calleeCuid.length() == 0 || calleeCuid.trim().length() == 0) {
                                        //Log.d("Patch", "Please pass a valid cuid");
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(PatchResponseCodes.OutgoingCallResponse.OnFailure.ERR_INVALID_CUID);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, PatchResponseCodes.OutgoingCallCallback.OnFailure.ERR_INVALID_CALLEE_CUID);
                                        return;
                                    }
                                    if (tags != null) {
                                        if (tags.length() > 0) {
                                            if (tags.length() > 10) {
                                                //Log.d("PATCH", "length of tags is more than 10");
                                                sdkReady = true;
                                                //outgoingCallResponse.onFailure(ERR_TAGS_EXCEEDED_BY_10);
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_TAGS_COUNT_EXCEEDED_BY_10);
                                                return;
                                            }
                                            for (int i = 0; i < tags.length(); i++) {
                                                if (tags.getString(i).length() > 32) {
                                                    //Log.d("PATCH", "length of tag " + tags.getString(i) + " at position " + (i + 1) + " is more than 32");
                                                    sdkReady = true;
                                                    //outgoingCallResponse.onFailure(ERR_TAG_LENGTH_EXCEEDED_BY_32);
                                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_TAG_LENGTH_EXCEEDED_BY_32);
                                                    return;
                                                }
                                            }
                                        } else {
                                            //Log.d("PATCH", "no tags are specified");
                                        }
                                    }
                                    if (webhook.length() > 0) {
                                        try {
                                            new URL(webhook).toURI();
                                        } catch (MalformedURLException e) {
                                            //e.printStackTrace();
                                            //Log.d("PATCH", "Please enter the correct webhook");
                                            sdkReady = true;
                                            //outgoingCallResponse.onFailure(ERR_INVALID_FORMAT_OF_WEBHOOK);
                                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_FORMAT_OF_WEBHOOK);
                                            return;
                                        } catch (URISyntaxException e) {
                                            //e.printStackTrace();
                                            //Log.d("PATCH", "Please enter the correct webhook");
                                            sdkReady = true;
                                            //outgoingCallResponse.onFailure(ERR_INVALID_FORMAT_OF_WEBHOOK);
                                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_FORMAT_OF_WEBHOOK);
                                            return;
                                        } catch (Exception e) {

                                        }
                                    }
                                    if (callOptions.getString("var1").length() > 128) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        return;
                                    }
                                    if (callOptions.getString("var2").length() > 128) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        return;
                                    }
                                    if (callOptions.getString("var3").length() > 128) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        return;
                                    }
                                    if (callOptions.getString("var4").length() > 128) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        return;
                                    }
                                    if (callOptions.getString("var5").length() > 128) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_VAR_LENGTH_EXCEEDED_BY_128);
                                        return;
                                    }
                                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activityContext);
                                    if (sharedPref.getString("patch_ecta", null) != null &&
                                            sharedPref.getString("patch_ecta", null).equals("true")) {
                                        if (callOptions.has("callToken") && callOptions.getString("callToken") != null && callOptions.getString("callToken") != "") {
                                        } else {
                                            sdkReady = true;
                                            //outgoingCallResponse.onFailure(ERR_EXPECT_CALL_TOKEN_TO_MAKE_CALL);
                                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_EXPECT_CALL_TOKEN_TO_MAKE_CALL);
                                            Log.d("Patch", "CallToken required to place call");
                                            return;
                                        }
                                    }
                                    String cc = sharedPref.getString("patch_userCC", "");
                                    String phone = sharedPref.getString("patch_userPhone", "");
                                    String userCid = sharedPref.getString("patch_cuid", "");
                                    if (userCid.equals(calleeCuid)) {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(PatchResponseCodes.OutgoingCallResponse.OnFailure.ERR_INVALID_CUID);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, PatchResponseCodes.OutgoingCallCallback.OnFailure.FAILURE_CAN_NOT_CALL_SELF);
                                        return;
                                    }
                                    if ((callOptions.getBoolean("pstn") || (callOptions.has("autoFallback") && callOptions.getBoolean("autoFallback")) && userCid.length() > 0)) {
                                        if (cc.length() == 0 && phone.length() == 0) {

                                            if (!(getVerifiedNumbers(activityContext).size() > 0)) {
                                                //Log.d("Patch", "no verified clis");
                                                sdkReady = true;
                                                //outgoingCallResponse.onFailure(ERR_EMPTY_VERIFIED_CLI_LIST);
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_EMPTY_VERIFIED_CLI_LIST);
                                                return;
                                            } else if (callOptions.has("cli")) {
                                                JSONObject cliJson = callOptions.getJSONObject("cli");
                                                if (!cliJson.has("cc") || !cliJson.has("phone")) {
                                                    sdkReady = true;
                                                    //outgoingCallResponse.onFailure(ERR_MISSING_CC_OR_PHONE_IN_CLI);
                                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MISSING_CC_OR_PHONE_IN_CLI);
                                                    return;
                                                }
                                                String cliCC = cliJson.getString("cc");
                                                String cliPhone = cliJson.getString("phone");
                                                if (cliCC.length() > 0 && cliPhone.length() > 0) {
                                                    if (isCliPassedAuthorized(activityContext, cliJson)) {
                                                        //Log.d("Patch", "cli passed is authorized");
                                                        SocketInit.getInstance().setCli(cliJson);

                                                    } else {
                                                        //Log.d("Patch", "cli passed is unauthorized");
                                                        sdkReady = true;
                                                        //outgoingCallResponse.onFailure(ERR_UNAUTHORIZED_CLI);
                                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_UNAUTHORIZED_CLI);
                                                        return;
                                                    }
                                                } else {
                                                    sdkReady = true;
                                                    //outgoingCallResponse.onFailure(ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                                                    return;
                                                }
                                            } else {
                                                SocketInit.getInstance().setCli(getCliFromList(activityContext, 0));
                                            }
                                        } else {
                                            if (!(getVerifiedNumbers(activityContext).size() > 0)) {
                                                //Log.d("Patch", "no verified clis");
                                                sdkReady = true;
                                                //outgoingCallResponse.onFailure(ERR_EMPTY_VERIFIED_CLI_LIST);
                                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_EMPTY_VERIFIED_CLI_LIST);
                                                return;
                                            } else if (callOptions.has("cli")) {
                                                JSONObject cliJson = callOptions.getJSONObject("cli");
                                                if (!cliJson.has("cc") || !cliJson.has("phone")) {
                                                    sdkReady = true;
                                                    //outgoingCallResponse.onFailure(ERR_MISSING_CC_OR_PHONE_IN_CLI);
                                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MISSING_CC_OR_PHONE_IN_CLI);
                                                    return;
                                                }
                                                String cliCC = cliJson.getString("cc");
                                                String cliPhone = cliJson.getString("phone");
                                                if (cliCC.length() > 0 && cliPhone.length() > 0) {
                                                    if (isCliPassedAuthorized(activityContext, cliJson)) {
                                                        //Log.d("Patch", "cli passed is authorized");
                                                        SocketInit.getInstance().setCli(cliJson);

                                                    } else {
                                                        sdkReady = true;
                                                        // outgoingCallResponse.onFailure(ERR_UNAUTHORIZED_CLI);
                                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_UNAUTHORIZED_CLI);
                                                        return;
                                                    }
                                                } else {
                                                    sdkReady = true;
                                                    //outgoingCallResponse.onFailure(ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                                                    customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_INVALID_LENGTH_OF_CC_OR_PHONE_IN_CLI);
                                                    return;
                                                }
                                            } else {
                                                SocketInit.getInstance().setCli(getCliFromList(activityContext, 0));
                                            }
                                        }
                                    }

                                    socketInit.setOutgoingCallResponse(outgoingCallResponse);
                                    jobSchedulerSocketService = JobSchedulerSocketService.getInstance(activityContext);
                                    if (SocketIOManager.isSocketConnected()) {
                                        jobSchedulerSocketService.makeCall("", "", calleeCuid, context, callOptions, outgoingCallResponse, null);
                                    } else {
                                        sdkReady = true;
                                        //outgoingCallResponse.onFailure(ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                        customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_CLIENT_DISCONNECTED_DUE_TO_NETWORK_PROBLEM);
                                    }
                                }
                            } else {
                                sdkReady = true;
                                //outgoingCallResponse.onFailure(ERR_MICROPHONE_PERMISSION_NOT_GRANTED);
                                customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_MICROPHONE_PERMISSION_NOT_GRANTED);
                            }
                        } else {
                            sdkReady = true;
                            customHandler.sendCallAnnotation(outgoingCallResponse, CustomHandler.OutCall.ON_FAILURE, ERR_NETWORK_NOT_AVAILABLE);
                        }
                    } catch (Exception e) {
                        sdkReady = true;
                        if (activityContext != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), activityContext);
                    }
                }
            }).start();

        } catch (Exception e) {
            sdkReady = true;
            if (activityContext != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), activityContext);
        }
    }

    /**
     * called when user is trying to send a message.
     *
     * @param messageOptions:-          cc of the user.
     *                                  phone of the user.
     *                                  message to be send.
     * @param outgoingMessageResponse:- statuses of the message to be returned while sending a message i.e. success or failure.
     */
    public void sendMessage(Context context, JSONObject messageOptions, OutgoingMessageResponse outgoingMessageResponse) throws Exception {
        try {
            if (SocketIOManager.getIsUnAuthorized()) {
                sdkReady = true;
                customHandler.sendOutMessAnnotations(outgoingMessageResponse, CustomHandler.OutMess.ON_FAILURE, PatchResponseCodes.OutgoingMessageCallback.OnFailure.ERR_CUID_ALREADY_CONNECTED_ELSEWHERE);
                return;
            }
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            //String messaging = sharedPref.getString("messaging", null);
            //if (messaging != null) {
                if (messageOptions != null) {
                    if ((messageOptions.has("cc") && messageOptions.has("phone")) || messageOptions.has("cuid")) {
                        if (messageOptions.has("message") && messageOptions.getString("message") != null &&
                                !messageOptions.getString("message").trim().isEmpty()) {
                            jobSchedulerSocketService = JobSchedulerSocketService.getInstance(context);
                            jobSchedulerSocketService.sendMessage(messageOptions, outgoingMessageResponse);
                        } else {
                            //outgoingMessageResponse.onFailure(ERR_MISSING_MESSAGE);
                            customHandler.sendOutMessAnnotations(outgoingMessageResponse, CustomHandler.OutMess.ON_FAILURE, ERR_MISSING_MESSAGE);
                        }
                    }
                } else {
                    //outgoingMessageResponse.onFailure(ERR_MISSING_CUID);
                    customHandler.sendOutMessAnnotations(outgoingMessageResponse, CustomHandler.OutMess.ON_FAILURE, ERR_MISSING_CUID);
                }
            /*} else {
                outgoingMessageResponse.onFailure(ERR_MESSAGING_MUST_BE_INITIALIZED);
            }*/
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public List<Cli> getVerifiedNumbers(Context activityContext) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activityContext);
            String json = sharedPref.getString("patch_cliList", "");
            Type type = new TypeToken<List<Cli>>() {
            }.getType();
            List<Cli> verifiedNumbers = gson.fromJson(json, type);
            return verifiedNumbers;
        } catch (Exception e) {

        }
        return null;
    }

    private JSONObject getCliFromList(Context context, int index) {
        try {
            return new JSONObject(new Gson().toJson(getVerifiedNumbers(context).get(index)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Boolean isCliPassedAuthorized(Context context, JSONObject jsonObject) {
        try {
            for (int index = 0; index < getVerifiedNumbers(context).size(); index++) {
                JSONObject jsonObject1 = getCliFromList(context, index);
                if (jsonObject.has("cc") && jsonObject1.has("cc")) {
                    if (jsonObject.getString("cc").equals(jsonObject1.getString("cc"))) {
                        if (jsonObject.has("phone") && jsonObject1.has("phone")) {
                            if (jsonObject.getString("phone").equals(jsonObject1.getString("phone"))) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void handleFcmMessage(@NonNull final Context context,
                                 @NonNull final Map<String, String> remoteData/*,
                                 @Nullable PatchFcmResponse patchFcmResponse*/) {
        try {
            //resetting fcm cache
            FcmCacheManger.getInstance().resetFcmCache();

            final Map<String, String> incomingCallData = remoteData;
            final Boolean isValid = incomingCallData.containsKey("source") && incomingCallData.get("source").equals("patch");
            if (isValid) {
                if (incomingCallData.containsKey("action")) {
                    String action = incomingCallData.get("action");
                    FcmUtil.getInstance(context).resetExpiredCallId(null);

                    if (incomingCallData.containsKey("payload")) {
                        JSONObject incomingdata = new JSONObject(incomingCallData.get("payload"));
                        switch (action) {
                            case "incoming-call":
                                startFcmService(context, incomingdata.toString(), ACTION_INCOMING_CALL);
                                break;
                            case "cancel":
                                if (incomingdata.has("callId") && incomingdata.getString("callId") != null) {
                                    startFcmService(context, incomingdata.toString(), ACTION_CANCEL_CALL);
                                }
                                break;
                            case "notification":
                                startFcmService(context, incomingdata.toString(), ACTION_FCM_NOTIFICATION_RECEIVED);

                                /*if(incomingdata.has("callId") && incomingdata.getString("callId") != null){
                                    startFcmService(context, incomingdata.toString(), ACTION_CANCEL_CALL);
                                }*/
                                //startFcmService(context, incomingdata.toString(), ACTION_CANCEL_CALL);

                                break;
                        }
                    }
                }

            }
        } catch (Exception e) {
        }
    }

    public void startFcmService(final Context context, final String data, final String actionType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE); //<-----  this is mandatory statement
                    if (jobScheduler != null) {
                        jobScheduler.cancelAll();
                    }
                } catch (Exception e) {

                }
                try {
                    Intent intent = new Intent(context, FcmSigsockService.class);
                    intent.setAction(actionType);
                    intent.putExtra(Constants.FCM_NOTIFICATION_PAYLOAD, data);
                    context.startService(intent);
                } catch (Exception e) {
                }
            }
        }).start();
    }


    public void registerNewFcmToken(final Context context, final String fcmToken, final PatchFcmResponse patchFcmResponse) {
        try {
            if (patchFcmResponse == null) {
                return;
            }
            if (fcmToken == null || fcmToken == "") {
                customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_FAILURE, PatchResponseCodes.PatchFcmResponse.OnFailure.ERR_FCM_TOKEN_REQUIRED);
                return;
            } else {
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                String cuid = sharedPref.getString("patch_cuid", "");
                String session = sharedPref.getString("patch_session", null);
                final String contactId = sharedPref.getString("patch_contactId", null);
                if (cuid != null && session != null && contactId != null) {
                    try {
                        ContactDeviceId contactDeviceId = new ContactDeviceId();
                        contactDeviceId.setDeviceId(fcmToken);
                        String authToken = PatchCommonUtil.getInstance().getAccessToken(context);
                        if (authToken == null) {
                            //this null check prevent client app to hit invalid request, /verify will take care of null authtoken by logging out the sdk
                            return;
                        }
                        routeApiService = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                                create(ApiInterface.class);
                        Call<Object> call = routeApiService.updateDeviceId(contactId, contactDeviceId, authToken);
                        call.enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                try {
                                    if (response.isSuccessful()) {
                                        //token is updated for the contact so updating the same in local sharedpref
                                        PatchCommonUtil.getInstance().setFcmToken(context, fcmToken);
                                        customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_SUCCESS, SUCCESS_FCM_TOKEN_UPDATED);
                                    } else {
                                        customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_FAILURE, ERR_INTERNAL_SERVER_ERROR);
                                    }
                                } catch (Exception e) {
                                    customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_FAILURE, ERR_INTERNAL_SERVER_ERROR);
                                }
                            }

                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_FAILURE, ERR_FAILED_TO_UPDATE_FCM_TOKEN);
                            }
                        });
                    } catch (Exception e) {
                        customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_FAILURE, ERR_FAILED_TO_UPDATE_FCM_TOKEN);
                    }
//                    JwtUtil.getInstance().getAdminToken(context, new TokenResponse() {
//                        @Override
//                        public void onSuccess(String response) {
//
//                        }
//
//                        @Override
//                        public void onFailure(String failure) {
//                            customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_FAILURE, ERR_FAILED_TO_UPDATE_FCM_TOKEN);
//                        }
//                    });
                } else {
                    // customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_FAILURE, ERR_NO_ACTIVE_PATCH_SESSION);
                }
            }
        } catch (Exception e) {
            customHandler.sendFcmAnnotations(patchFcmResponse, CustomHandler.Fcm.ON_FAILURE, ERR_FAILED_TO_UPDATE_FCM_TOKEN);
        }
    }


    public void init(Context activityContext, PatchInitOptions initOptions, PatchInitResponse patchInitResponse) {
        init(activityContext, initOptions.getInitJson(), patchInitResponse, initOptions.isReadPhoneStateEnabled(),
                initOptions.isNotificationEnabled(), !initOptions.isNotificationUIEnabled(),
                initOptions.getNotificationListenerHost(),
                initOptions.getMissedCallReceiverActions(), initOptions.getMissedCallReceiverHost(),
                initOptions.getMissedCallInitiatorActions(), initOptions.getMissedCallInitiatorHost(), initOptions);
    }

    public interface MissedCallNotificationOpenedHandler {
        /**
         * Fires when a user taps on a notification.
         *
         * @param result a {@link MissedCallNotificationOpenResult} with the user's response and properties of this notification
         */
        void onnMissedCallNotificationOpened(Context context, MissedCallNotificationOpenResult result);
    }

    public interface PinviewTextObserver {
        void onPinTextCompleted(String pin, PinTextVerificationHandler pinTextVerificationHandler);
    }

    public interface PinTextVerificationHandler {
        void onSuccess();

        void onFailure();
    }

    public static void addPinviewTextObserver(PinviewTextObserver patchPinviewTextObserver) {
        PatchObservable.getInstance().setPatchPinviewTextObserver(patchPinviewTextObserver);
    }

    public void addTag(final Context context, final String tagId, final TagListener tagListener){
        Preconditions.checkNotNull(tagId, "tagId must not be null");
        Preconditions.checkNotNull(tagListener, "tagListener must not be null");
        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String session = sharedPref.getString("patch_session", null);
            final String contactId = sharedPref.getString("patch_contactId", null);
            if (session != null && contactId != null) {
                try {
                    String authToken = PatchCommonUtil.getInstance().getAccessToken(context);
                    if (authToken != null) {
                        ApiInterface apiInterface = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                                create(ApiInterface.class);
                        List<String> contactIdList =  new ArrayList<>();
                        contactIdList.add(contactId);
                        Call<Object> call = apiInterface.addTag(tagId, contactIdList, authToken);
                        call.enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                try {
                                    if (response.isSuccessful()) {
                                        tagListener.onTagAdded();
                                    } else {
                                        if (response.code() == 401) {
                                            PatchCommonUtil.getInstance().removeDevToken(context);
                                            tagListener.onFailure(FAILURE_SESSION_EXPIRED_RESTART_THE_APP);
                                        } else {
                                            tagListener.onFailure(FAILURE_WHIILE_ADDING_TAG);
                                        }
                                    }
                                } catch (Exception e) {
                                    tagListener.onFailure(FAILURE_WHIILE_ADDING_TAG);
                                }
                            }

                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                tagListener.onFailure(FAILURE_WHIILE_ADDING_TAG);
                            }
                        });
                    }
                }catch (Exception e){

                }
            }else {
                tagListener.onFailure(FAILURE_NO_ACTIVE_PATCH_SESSION);
            }
        } catch (Exception e) {
        }
    }

    public void removeTag(final Context context, final String tagId, final TagListener tagListener){
        Preconditions.checkNotNull(tagId, "tagId must not be null");
        Preconditions.checkNotNull(tagListener, "tagListener must not be null");
        try {
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String session = sharedPref.getString("patch_session", null);
            final String contactId = sharedPref.getString("patch_contactId", null);
            if (session != null && contactId != null) {
                try {
                    String authToken = PatchCommonUtil.getInstance().getAccessToken(context);
                    if (authToken != null) {
                        ApiInterface apiInterface = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                                create(ApiInterface.class);
                        Call<Object> call = apiInterface.removeTag(tagId, contactId, authToken);
                        call.enqueue(new Callback<Object>() {
                            @Override
                            public void onResponse(Call<Object> call, Response<Object> response) {
                                try {
                                    if (response.isSuccessful()) {
                                        tagListener.onTagRemoved();
                                    } else {
                                        if (response.code() == 401) {
                                            PatchCommonUtil.getInstance().removeDevToken(context);
                                            tagListener.onFailure(FAILURE_SESSION_EXPIRED_RESTART_THE_APP);
                                        } else {
                                            tagListener.onFailure(FAILURE_WHIILE_REMOVING_TAG);
                                        }
                                    }
                                } catch (Exception e) {
                                    tagListener.onFailure(FAILURE_WHIILE_REMOVING_TAG);
                                }
                            }

                            @Override
                            public void onFailure(Call<Object> call, Throwable t) {
                                tagListener.onFailure(FAILURE_WHIILE_REMOVING_TAG);
                            }
                        });
                    }
                }catch (Exception e){

                }
            }else {
                tagListener.onFailure(FAILURE_NO_ACTIVE_PATCH_SESSION);
            }
        } catch (Exception e) {
        }
    }

    /*public void fetchAvailableCampaignTags(final Context context, final TagListener tagListener){
        Preconditions.checkNotNull(tagListener, "tagListener must not be null");
    }*/

}
