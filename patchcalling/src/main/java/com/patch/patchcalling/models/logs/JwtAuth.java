package com.patch.patchcalling.models.logs;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by sanyamjain on 27/08/18.
 */

public class JwtAuth {

    @SerializedName("username")
    @Expose
    private String userName;
    @SerializedName("password")
    @Expose
    private String password;

    public JwtAuth() {

    }

    public JwtAuth(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
