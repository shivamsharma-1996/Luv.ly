package com.patch.patchcalling.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Shivam Sharma on 26-05-2020.
 */
public class ContactDeviceId {
    @SerializedName("device_id")
    @Expose
    private String deviceId;

    public ContactDeviceId() {
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
