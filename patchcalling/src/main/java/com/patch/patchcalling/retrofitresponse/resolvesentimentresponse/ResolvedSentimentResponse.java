package com.patch.patchcalling.retrofitresponse.resolvesentimentresponse;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Shivam Sharma on 08-06-2019.
 */
public class ResolvedSentimentResponse {
    @SerializedName("id")
    @Expose
    private String notificationId;
    @SerializedName("sentiment")
    @Expose
    private Boolean sentiment;


    public ResolvedSentimentResponse(String notificationId, Boolean sentiment) {
        this.notificationId = notificationId;
        this.sentiment = sentiment;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public Boolean getSentiment() {
        return sentiment;
    }

    public void setSentiment(Boolean sentiment) {
        this.sentiment = sentiment;
    }
}

