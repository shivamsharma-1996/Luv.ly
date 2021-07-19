package com.patch.patchcalling.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by sanyamjain on 27/08/18.
 */

public class CreateContact {

    @SerializedName("appId")
    @Expose
    private String appId;

    @SerializedName("name")
    @Expose
    private String name;
//    @SerializedName("picture")
//    @Expose
//    private String picture;
    @SerializedName("cc")
    @Expose
    private String cc;
    @SerializedName("phone")
    @Expose
    private String phone;
    @SerializedName("platform")
    @Expose
    private String platform;
    @SerializedName("apikey")
    @Expose
    private String apikey;
    @SerializedName("accountId")
    @Expose
    private String accountId;
    @SerializedName("cuid")
    @Expose
    private String cuid;
    @SerializedName("device_id")
    @Expose
    private String deviceId;
    @SerializedName("sdkVersion")
    @Expose
    private String sdkVersion;

    public CreateContact(String name, String cc, String phone, String platform,String accountId, String apikey, String cuid) {
        this.name = name;
        this.cc = cc;
        this.phone = phone;
        this.platform = platform;
        //this.picture = picture;
        this.accountId = accountId;
        this.apikey = apikey;
        this.cuid = cuid;
    }

    public CreateContact() {

    }


    /*public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }*/

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    /*@Override
    public String toString() {
        return name +  "," + cc + "," + phone + "," + platform + "," + apikey + "," + accountId + "," + cuid;
    }*/
}
