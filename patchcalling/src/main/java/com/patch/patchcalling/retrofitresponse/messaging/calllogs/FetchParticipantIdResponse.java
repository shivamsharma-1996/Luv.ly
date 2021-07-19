package com.patch.patchcalling.retrofitresponse.messaging.calllogs;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Shivam Sharma on 10-08-2019.
 */
public class FetchParticipantIdResponse {
    @SerializedName("id")
    @Expose
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
