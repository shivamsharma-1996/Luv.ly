package com.patch.patchcalling.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CampaignTagsResponse {

    @SerializedName("label")
    @Expose
    private String label;
    @SerializedName("id")
    @Expose
    private String id;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}