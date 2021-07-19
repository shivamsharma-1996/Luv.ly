package com.patch.patchcalling.retrofitresponse.logs;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class IpResponse {

    @SerializedName("ip")
    @Expose
    private String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
