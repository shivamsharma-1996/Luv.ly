package com.patch.patchcalling.retrofitresponse.createcontact;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Cli {

    @SerializedName("cc")
    @Expose
    private String cc;
    @SerializedName("phone")
    @Expose
    private String phone;

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
}
