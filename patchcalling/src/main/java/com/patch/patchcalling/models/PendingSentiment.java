package com.patch.patchcalling.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Shivam Sharma on 08-06-2019.
 */
public class PendingSentiment {
    @SerializedName("btnSentiment")
    @Expose
    private List<Integer> sentiment;
    @SerializedName("voiceCallId")
    @Expose
    private String callId;

    public PendingSentiment() {
    }

    public PendingSentiment(List<Integer> sentiment) {
        this.sentiment = sentiment;
    }

    public List<Integer> getSentiment() {
        return sentiment;
    }

    public void setSentiment(List<Integer> sentiment) {
        this.sentiment = sentiment;
    }

    public String  getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }
}

