package com.patch.patchcalling.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Shivam Sharma on 14-04-2020.
 */
public class VoiceCallStatus {
    @SerializedName("status")
    @Expose
    private String callStatus;

    public VoiceCallStatus() {
    }

    public String getCallStatus() {
        return callStatus;
    }

    public void setCallStatus(String callStatus) {
        this.callStatus = callStatus;
    }
}



