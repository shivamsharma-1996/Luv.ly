package com.patch.patchcalling.retrofitresponse.createcontact;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by sanyamjain on 27/08/18.
 */

public class CreateContactResponse {
    @SerializedName("id")
    @Expose
    private String contactId;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("cc")
    @Expose
    private String cc;
    @SerializedName("phone")
    @Expose
    private String phone;
    @SerializedName("platform")
    @Expose
    private String platform;
    @SerializedName("jwt")
    @Expose
    private String jwt;
    @SerializedName("ecta")
    @Expose
    private String  ecta;
    @SerializedName("sna")
    @Expose
    private String sna;
    @SerializedName("clis")
    @Expose
    private List<Cli> clis;
    @SerializedName("branding")
    @Expose
    private Branding branding;
    @SerializedName("accessToken")
    @Expose
    private String  accessToken;

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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }

    public String getEcta() {
        return ecta;
    }

    public void setEcta(String ecta) {
        this.ecta = ecta;
    }

    public String getSna() {
        return sna;
    }

    public void setSna(String sna) {
        this.sna = sna;
    }

    public Branding getBranding() {
        return branding;
    }

    public void setBranding(Branding branding) {
        this.branding = branding;
    }

    public List<Cli> getCliList() {
        return clis;
    }

    public void setCliList(List<Cli> clis) {
        this.clis = clis;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }
}

