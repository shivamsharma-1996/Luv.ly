package com.patch.patchcalling.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.patch.patchcalling.BuildConfig;
import com.patch.patchcalling.Constants;
import com.patch.patchcalling.interfaces.LoggingApiInterface;
import com.patch.patchcalling.models.logs.Device;
import com.patch.patchcalling.models.logs.ErrorLog;
import com.patch.patchcalling.models.logs.JwtAuth;
import com.patch.patchcalling.retrofitresponse.logs.ErrorlogResponse;
import com.patch.patchcalling.retrofitresponse.logs.IpResponse;
import com.patch.patchcalling.retrofitresponse.logs.JwtTokenResponse;
import com.patch.patchcalling.services.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Shivam Sharma on 18-06-2019.
 */
public class PatchLogger {

    private static PatchLogger instance = null;
    private static final String TAG = "AppUtil";
    private static Context context;
    public static LoggingApiInterface loggingApiInterface = RetrofitClient.getLoggingClient().create(LoggingApiInterface.class);

    public static PatchLogger getInstance(Context applicationContext) {
        if (instance == null) {
            instance = new PatchLogger();
            //context = applicationContext;
        }
        return instance;
    }

    private PatchLogger() {
    }


    public static void createLog(final String errorMessage, final String stackTrace, final Context context) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                getLogToken(errorMessage, stackTrace, context);
            }
        }).start();
    }

    private static void getLogToken(final String errorMessage, final String stackTrace, final Context context) {
        try {
            JwtAuth jwtAuth = new JwtAuth("android-logger", "patch@123");
            Call<JwtTokenResponse> getJwtLogTokenCall = loggingApiInterface.getJwtLogToken(jwtAuth);
            getJwtLogTokenCall.enqueue(new Callback<JwtTokenResponse>() {
                @Override
                public void onResponse(Call<JwtTokenResponse> call, Response<JwtTokenResponse> response) {
                    if (response.isSuccessful()) {
                        try {
                            JwtTokenResponse result = response.body();
                            if (result != null && result.getJwt()!= null) {
                                getPublicIp(result.getJwt(), errorMessage, stackTrace, context);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<JwtTokenResponse> call, Throwable t) {

                }
            });
        } catch (Exception e) {

        }
    }

    private static void getPublicIp(final String jwtToken, final String errorMessage, final String stackTrace, final Context context) {
        try {
            Call<IpResponse> getPublicIpCall = loggingApiInterface.getPublicIP("https://api.ipify.org/?format=json");
            getPublicIpCall.enqueue(new Callback<IpResponse>() {
                @Override
                public void onResponse(Call<IpResponse> call, Response<IpResponse> response) {
                    if (response.isSuccessful()) {
                        try {
                            IpResponse result = response.body();
                            if (result != null && result.getIp()!= null) {
                                postLogToServer(result.getIp(), jwtToken, errorMessage, stackTrace, context);
                            }
                        } catch (Exception e) {
                            //e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<IpResponse> call, Throwable t) {

                }
            });
        } catch (Exception e) {

        }
    }

    private static void postLogToServer(String ip, String jwtToken, String errorMessage, String stackTrace, Context context) {
        try {
            Call<ErrorlogResponse> createLogCall = loggingApiInterface.createLog("bearer " +jwtToken, getErrorLog(context,ip, errorMessage, stackTrace));

            createLogCall.enqueue(new Callback<ErrorlogResponse>() {
                @Override
                public void onResponse(Call<ErrorlogResponse> call, Response<ErrorlogResponse> response) {
                    if (response.isSuccessful()) {
                        ErrorlogResponse result = response.body();
                        if(result!=null){
                           // Log.d("Patch", "logged details");
                        }
                    }
                }

                @Override
                public void onFailure(Call<ErrorlogResponse> call, Throwable t) {

                }
            });
        } catch (Exception e) {

        }
    }

    private static ErrorLog getErrorLog(Context context, String ip, String errorMessage, String stackTrace) {
        //Log.d("Patch","button press on device name = "+android.os.Build.MODEL +" brand = "+android.os.Build.BRAND +" OS version = "+android.os.Build.VERSION.RELEASE + " SDK version = " +android.os.Build.VERSION.SDK_INT);
        ErrorLog errorLog = new ErrorLog();
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String accountId = sharedPref.getString("patch_accountId", null);
            String cuid = sharedPref.getString("patch_cuid", "");

            Device deviceDetails = new Device();
            deviceDetails.setManufacturer(Build.BRAND);
            deviceDetails.setModel(Build.MODEL);
            deviceDetails.setOsVersion(Build.VERSION.RELEASE);
            deviceDetails.setApiLevel(String.valueOf(Build.VERSION.SDK_INT));
            deviceDetails.setIp(ip);

            errorLog.setMessage(errorMessage);
            errorLog.setStackTrace(stackTrace);
            errorLog.setAccountId(accountId);
            errorLog.setCuid(cuid);
            errorLog.setTimeStamp(PatchCommonUtil.getInstance().getCurrentIsoDateTime());
            errorLog.setSdkVersion(Constants.VERSION_NAME);
            errorLog.setDevice(deviceDetails);
        } catch (Exception e) {

        }
        return errorLog;
    }
}
