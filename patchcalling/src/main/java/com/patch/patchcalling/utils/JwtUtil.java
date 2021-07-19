package com.patch.patchcalling.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.patch.patchcalling.R;
import com.patch.patchcalling.interfaces.ApiInterface;
import com.patch.patchcalling.interfaces.PatchInitResponse;
import com.patch.patchcalling.interfaces.TokenResponse;
import com.patch.patchcalling.javaclasses.PatchSDK;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.retrofitresponse.gettoken.Token;
import com.patch.patchcalling.services.JobSchedulerSocketService;
import com.patch.patchcalling.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.patch.patchcalling.PatchResponseCodes.PatchInitCallback.OnFailure.ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP;

/**
 * Created by Shivam Sharma on 03-12-2019.
 */
public class JwtUtil {
    private static JwtUtil ourInstance =null;
    static JobInfo myJob;
    static JobScheduler jobScheduler;
    static JobSchedulerSocketService jobSchedulerSocketService;
    static ApiInterface apiService = null;

    public static JwtUtil getInstance(Context context) {
        if(ourInstance == null){
            ourInstance = new JwtUtil();
        }
        return ourInstance;
    }


    private JwtUtil() {
    }

    /**
     * fetches the jwt token before starting the sigsock.
     *
     * @param cc:-            cc of the user
     * @param phone:-         phone number of the user
     * @param accountId:-     accountId of the user
     * @param cuid:-          cuid of the user.
     * @param patchResponse:- failure and success callback.
     */
    public void getToken(final Context context, final String cc, final String phone, final String accountId, String cuid, final TokenResponse patchResponse) {
        try {
            apiService = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                    create(ApiInterface.class);
            Call<Token> call;
            String authToken = PatchCommonUtil.getInstance().getAccessToken(context);
            if (cuid != null && cuid.length() > 0) {
                call = apiService.getTokenByCuid(accountId, cuid, authToken);
            } else {
                call = apiService.getToken(phone, cc, accountId, authToken);
            }
            call.enqueue(new Callback<Token>() {
                @Override
                public void onResponse(Call<Token> call, Response<Token> response) {
                    try {
                        if (response.isSuccessful()) {
                            Token result = response.body();
                            String token = result.getToken();
                            String sna = result.getSna();
                            //SocketInit.getInstance().setSid(sna);
                            if (token == null || token.equals("")) {
                                patchResponse.onFailure("token invalid");
                            } else {
                                //jwt = token;
                                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(context.getResources().getString(R.string.sna), sna);
                                editor.putString(context.getResources().getString(R.string.patch_token), token);
                                //Log.d("sharma", "token inside getToken() is : " + token);
                                editor.putString("patch_session", "true");
                                if(SocketInit.getInstance().getMissedCallReceiverActions()!=null){
                                    Gson gson = new Gson();
                                    editor.putString(context.getString(R.string.patch_missedCallReceiverActions), gson.toJson(SocketInit.getInstance().getMissedCallReceiverActions()));
                                }
                                if(SocketInit.getInstance().getMissedCallInitiatorActions()!=null){
                                    Gson gson = new Gson();
                                    editor.putString(context.getString(R.string.patch_missedCallInitiatorActions), gson.toJson(SocketInit.getInstance().getMissedCallInitiatorActions()));
                                }
                                editor.commit();
                                editor.apply();
                                patchResponse.onSuccess("200");
                            }
                        } else {
                            if (context != null)
                                PatchLogger.createLog("JWT getToken onResponse ERROR", "" + response, context);
                            patchResponse.onFailure("failed to get the JWT token");
                        }
                    } catch (Exception e) {
                        try {
                            patchResponse.onFailure("Something went wrong. Please pass the correct parameters");
                            if (context != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                        } catch (Exception e1) {

                        }
                    }
                }

                @Override
                public void onFailure(Call<Token> call, Throwable t) {
                    try {
                        //Log.d("sharma", "getToken onFailure :  ' "  + t.toString());
                        patchResponse.onFailure(t.toString());
                        if (context != null)
                            PatchLogger.createLog("JWT getToken onFailure ERROR", "" + t.toString(), context);
                    } catch (Exception e) {

                    }
                }
            });
            /*if (SocketIOManager.getSocket() == null) {

            } else {
                patchResponse.onSuccess("Already Fetched the jwt token so only socket.connect");    //this is to prevent reregistering of socket handlers which is due to socketIOManger static object
            }*/
        } catch (Exception e) {
            try {
                patchResponse.onFailure("failed to get the JWT token");

                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            } catch (Exception e1) {

            }

        }
    }

    public void verifyTokenForSigsockService(final Context context, final PatchInitResponse patchInitResponse) {
        try {
            apiService = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                    create(ApiInterface.class);
            //Hit verifyToken endpoint(
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            final String userCuid = sharedPref.getString("patch_cuid", null);
            final String accountId = sharedPref.getString("patch_accountId", null);
            final String userCC = sharedPref.getString("patch_cc", null);
            final String userPhone = sharedPref.getString("patch_phone", null);
            String jwtTokenToVerify = sharedPref.getString(context.getResources().getString(R.string.patch_token), null);
            String authToken = PatchCommonUtil.getInstance().getAccessToken(context);
            //Log.d("sharma", "verifytoken : " + jwtTokenToVerify);
            if (apiService != null && jwtTokenToVerify != null && !jwtTokenToVerify.isEmpty()) {
                Call<Object> call = apiService.verifyToken(accountId, userCuid, jwtTokenToVerify, authToken);
                call.enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        try {
                            if (response.isSuccessful()) {
                                //Log.d("sharma", "response of onresonse " + response);

                                Object result = response.body();
                                //Log.d("sharma", "response of verifyToken is :  " + result);
                                //if true, start service then sigsock else hit /jwt
                                if (result != null && result instanceof Boolean && result.equals(true)) {
                                    //startServiceAndSigsock(context);
                                    CustomHandler.getInstance().sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, 1);

                                } else {
                                    //else means result = false , means token is expired so generate new one then store it
                                    getToken(context, userCC, userPhone, accountId, userCuid, new TokenResponse() {
                                        @Override
                                        public void onSuccess(String response) {
                                            try {
                                                if (response.equals("200")) {
                                                    //Log.d("sharma", "response of getToken inside verify :  " + response);
                                                    //200 means successfully stored the newly generated token so now starting service+sigsock
                                                    // startServiceAndSigsock(context);
                                                    CustomHandler.getInstance().sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_SUCCESS, 1);

                                                }
                                            } catch (Exception e) {
                                            }
                                        }

                                        @Override
                                        public void onFailure(String failure) {
                                            try {
                                                //return sdk not initialized please restart the app kind of annotation
                                                //stopServiceAndSigsock(context);
                                                CustomHandler.getInstance().sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, 0);

                                            } catch (Exception e) {

                                            }
                                            //Log.d("sharma", "getToken onfailure: " + failure);
                                        }
                                    });
                                }
                            } else {
                                //500 (internal server error) handling
                                //Log.d("sharma", "verifytoken onresponse else: " + response);
//                                stopServiceAndSigsock(context);
                                PatchSDK.getInstance().logout(context);
                                CustomHandler.getInstance().sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, 0);
                                //re-init the SDK here after 1.5 seconds
                            }
                        } catch (Exception e) {

                        }
                    }

                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        try {
                            //stopServiceAndSigsock(context);
                            //Log.d("sharma", "verifytoken onfailure: " + t.toString());
                            //stopServiceAndSigsock(context);
                            CustomHandler.getInstance().sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, 0);
                        } catch (Exception e) {

                        }
                    }
                });
            }
        } catch (Exception e) {

        }

    }

    public void verifyToken(final Context context, final Boolean isFcmSignalling, final PatchInitResponse patchInitResponse) {
        try {
            apiService = RetrofitClient.getApiClient(PatchCommonUtil.getInstance().getBaseUrl(context)).
                    create(ApiInterface.class);
            //Hit verifyToken endpoint
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            final String userCuid = sharedPref.getString("patch_cuid", null);
            final String accountId = sharedPref.getString("patch_accountId", null);
            final String userCC = sharedPref.getString("patch_cc", null);
            final String userPhone = sharedPref.getString("patch_phone", null);
            String jwtTokenToVerify = sharedPref.getString(context.getResources().getString(R.string.patch_token), null);

            //Log.d("sharma", "verifytoken : " + jwtTokenToVerify);
            String authToken = PatchCommonUtil.getInstance().getAccessToken(context);

            if(apiService!= null && jwtTokenToVerify!=null && !jwtTokenToVerify.isEmpty()){
                Call<Object> call = apiService.verifyToken(accountId, userCuid, jwtTokenToVerify, authToken);
                call.enqueue(new Callback<Object>() {
                    @Override
                    public void onResponse(Call<Object> call, Response<Object> response) {
                        try {
                            if(response.isSuccessful()){
                                //Log.d("sharma", "response of onresonse " + response);

                                Object result = response.body();
                                //Log.d("sharma", "response of verifyToken is :  " + result);
                                //if true, start service then sigsock else hit /jwt
                                if(result!=null && result instanceof Boolean && result.equals(true)){
                                    if(isFcmSignalling){
                                        patchInitResponse.onSuccess(121212);
                                    }else {
                                        startServiceAndSigsock(context);
                                    }
                                }else{
                                    //else means result = false , means token is expired so generate new one then store it
                                    getToken(context, userCC, userPhone, accountId, userCuid, new TokenResponse() {
                                        @Override
                                        public void onSuccess(String response) {
                                            try {
                                                if(response.equals("200")){
                                                    //Log.d("sharma", "response of getToken inside verify :  " + response);
                                                    //200 means successfully stored the newly generated token so now starting service+sigsock
                                                    if(isFcmSignalling){
                                                        patchInitResponse.onSuccess(121212);
                                                    }else {
                                                        startServiceAndSigsock(context);
                                                    }
                                                }
                                            }catch (Exception e){

                                            }
                                        }

                                        @Override
                                        public void onFailure(String failure) {
                                            try {
                                                //return sdk not initialized please restart the app kind of annotation
                                                stopServiceAndSigsock(context);
                                                CustomHandler.getInstance().sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);

                                            }catch (Exception e){

                                            }
                                            //Log.d("sharma", "getToken onfailure: " + failure);
                                        }
                                    });
                                }
                            }else{
                                //500 (internal server error) handling
                                //Log.d("sharma", "verifytoken onresponse else: " + response);
                                stopServiceAndSigsock(context);
                                PatchSDK.getInstance().logout(context);
                                CustomHandler.getInstance().sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                                //re-init the SDK here after 1.5 seconds
                            }
                        }catch (Exception e){

                        }
                    }
                    @Override
                    public void onFailure(Call<Object> call, Throwable t) {
                        try {
                            stopServiceAndSigsock(context);
                            //Log.d("sharma", "verifytoken onfailure: " + t.toString());
                            stopServiceAndSigsock(context);
                            CustomHandler.getInstance().sendInitAnnotations(patchInitResponse, CustomHandler.Init.ON_FAILURE, ERR_PATCH_SDK_NOT_INITIALIZED_RESTART_THE_APP);
                        }catch (Exception e){

                        }
                    }
                });
            }
        }catch (Exception e){

        }

    }

    public void startServiceAndSigsock(Context context) {
        try {
            if (!PatchCommonUtil.getInstance().isJobIdRunning(context, 10) || !SocketIOManager.isSocketConnected()) {
                jobSchedulerSocketService = JobSchedulerSocketService.getInstance(context);
                myJob = new JobInfo.Builder(10, new ComponentName(context, jobSchedulerSocketService.getClass()))
                        .setBackoffCriteria(4000, JobInfo.BACKOFF_POLICY_LINEAR)
                        .setPersisted(true)
                        .setMinimumLatency(1)
                        .setOverrideDeadline(1)
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .build();
                //Log.d("sharma", "startServiceAndSigsock");

                jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
                if (jobScheduler != null) {
                    jobScheduler.schedule(myJob);
                    //Log.d("sharma", "startServiceAndSigsock scheduled");
                }
            }
        } catch (Exception e) {

        }
    }

    public void stopServiceAndSigsock(Context context) {
        try {
            jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE); //<-----  this is mandatory statement
            if (jobScheduler != null) {
                jobScheduler.cancelAll();
                //Log.d("sharma", "stopedservice");
            }
            if (SocketIOManager.getSocket() != null) {
                SocketIOManager.setSocketInstanceNull();
                SocketIOManager.setIsUnAuthorized(false);
                //Log.d("sharma", "socket disconnected");
            }
        } catch (Exception e) {

        }
    }
}
