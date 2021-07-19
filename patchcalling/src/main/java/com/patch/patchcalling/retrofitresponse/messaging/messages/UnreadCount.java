package com.patch.patchcalling.retrofitresponse.messaging.messages;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UnreadCount {

    @SerializedName("count")
    @Expose
    private Integer count;

    public UnreadCount() {
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "UnreadCount{" +
                "count=" + count +
                '}';
    }
}