package com.patch.patchcalling.retrofitresponse.messaging.conversations;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class FetchConversationResponse {

    @SerializedName("convoId")
    @Expose
    private String convoId;
    @SerializedName("customerCuid")
    @Expose
    private String customerCuid;
    @SerializedName("messages")
    @Expose
    private List<Message> messages = null;

    public FetchConversationResponse() {
    }

    public String getConvoId() {
        return convoId;
    }

    public void setConvoId(String convoId) {
        this.convoId = convoId;
    }

    public String getCustomerCuid() {
        return customerCuid;
    }

    public void setCustomerCuid(String customerCuid) {
        this.customerCuid = customerCuid;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "FetchConversationResponse{" +
                "convoId='" + convoId + '\'' +
                ", customerCuid='" + customerCuid + '\'' +
                ", messages=" + messages +
                '}';
    }
}

