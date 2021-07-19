package com.patch.patchcalling.retrofitresponse.logs;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class JwtTokenResponse {

    @SerializedName("jwt")
    @Expose
    private String jwt;

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }
}
