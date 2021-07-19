package com.patch.patchcalling.retrofitresponse.messaging.conversations;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("body")
    @Expose
    private String body;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("convoId")
    @Expose
    private String convoId;
    @SerializedName("senderCuid")
    @Expose
    private String senderCuid;

    public Message() {
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConvoId() {
        return convoId;
    }

    public void setConvoId(String convoId) {
        this.convoId = convoId;
    }

    public String getSenderCuid() {
        return senderCuid;
    }

    public void setSenderCuid(String senderCuid) {
        this.senderCuid = senderCuid;
    }

}