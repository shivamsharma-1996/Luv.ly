package com.patch.patchcalling.retrofitresponse.createcontact;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by sanyamjain on 15/11/18.
 */

public class Branding {
    @SerializedName("color")
    @Expose
    private String color;
    @SerializedName("bgColor")
    @Expose
    private String bgColor;
    @SerializedName("logo")
    @Expose
    private String logo;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getBgColor() {
        return bgColor;
    }

    public void setBgColor(String bgColor) {
        this.bgColor = bgColor;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}
