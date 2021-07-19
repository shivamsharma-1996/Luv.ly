package com.patch.patchcalling.models.logs;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by sanyamjain on 27/08/18.
 */

public class ErrorLog {

    @SerializedName("message")
    @Expose
    private String message;
    @SerializedName("stack")
    @Expose
    private String stackTrace;
    @SerializedName("accountId")
    @Expose
    private String accountId;
    @SerializedName("cuid")
    @Expose
    private String cuid;
    @SerializedName("ts")
    @Expose
    private String timeStamp;
    @SerializedName("sdkVersion")
    @Expose
    private String sdkVersion;

    @SerializedName("device")
    @Expose
    private Device device;

    public ErrorLog() {
    }

    public ErrorLog(String message, String stackTrace, String accountId, String cuid, String timeStamp, String sdkVersion, Device device) {
        this.message = message;
        this.stackTrace = stackTrace;
        this.accountId = accountId;
        this.cuid = cuid;
        this.timeStamp = timeStamp;
        this.sdkVersion = sdkVersion;
        this.device = device;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getCuid() {
        return cuid;
    }

    public void setCuid(String cuid) {
        this.cuid = cuid;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }
}
