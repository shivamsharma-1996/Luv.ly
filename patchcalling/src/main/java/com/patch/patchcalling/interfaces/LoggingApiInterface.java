package com.patch.patchcalling.interfaces;

import com.patch.patchcalling.models.logs.ErrorLog;
import com.patch.patchcalling.models.logs.JwtAuth;
import com.patch.patchcalling.retrofitresponse.logs.ErrorlogResponse;
import com.patch.patchcalling.retrofitresponse.logs.IpResponse;
import com.patch.patchcalling.retrofitresponse.logs.JwtTokenResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Created by sanyamjain on 21/07/18.
 */

public interface LoggingApiInterface {

    @POST("_open/auth")
    Call<JwtTokenResponse> getJwtLogToken(@Body JwtAuth jwtAuth);

    @POST("_db/android-logs/_api/document/Errors")
    Call<ErrorlogResponse> createLog(@Header("Authorization") String jwtToken , @Body ErrorLog errorLog);

    @GET
    Call<IpResponse> getPublicIP(@Url String url);
}
