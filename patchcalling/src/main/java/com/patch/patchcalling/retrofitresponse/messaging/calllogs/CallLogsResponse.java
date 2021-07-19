package com.patch.patchcalling.retrofitresponse.messaging.calllogs;

/**
 * Created by Shivam Sharma on 10-08-2019.
 */
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CallLogsResponse {

    @SerializedName("createdAt")
    @Expose
    private String createdAt;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("initiatorId")
    @Expose
    private String initiatorId;
    @SerializedName("initiator")
    @Expose
    private Initiator initiator;

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public Initiator getInitiator() {
        return initiator;
    }

    public void setInitiator(Initiator initiator) {
        this.initiator = initiator;
    }

    @Override
    public String toString() {
        return "CallLogsResponse{" +
                "createdAt='" + createdAt + '\'' +
                ", status='" + status + '\'' +
                ", initiatorId='" + initiatorId + '\'' +
                ", initiator=" + initiator +
                '}';
    }
}


 class Initiator {

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("id")
    @Expose
    private String id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

     @Override
     public String toString() {
         return "Initiator{" +
                 "name='" + name + '\'' +
                 ", id='" + id + '\'' +
                 '}';
     }
 }
