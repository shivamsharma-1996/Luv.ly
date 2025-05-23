package com.patch.patchcalling.fcm;

/**
 * Created by Shivam Sharma on 08-04-2020.
 */
public class FcmCacheManger {
    private static final FcmCacheManger ourInstance = new FcmCacheManger();

    public static FcmCacheManger getInstance() {
        return ourInstance;
    }

    private FcmCacheManger() {
    }

    private Boolean isAnswered = false;
    private Boolean isRejected = false;  //normal decline from UI or due to invalid_cuid/microphone-permission/userBusy
    private String  rejectedReason = null;  //normal decline from UI or due to invalid_cuid/microphone-permission/userBusy
    private Integer rejectedReasonCode = null;
    private Boolean shouldMarkNotificationStatusDelivered = false;

    public Boolean isAnswered() {
        return isAnswered;
    }

    public void setAnswered(Boolean answered) {
        isAnswered = answered;
    }

    public Boolean isRejected() {
        return isRejected;
    }

    public void setRejected(Boolean rejected) {
        isRejected = rejected;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }

    public Integer getRejectedReasonCode() {
        return rejectedReasonCode;
    }

    public void setRejectedReasonCode(Integer rejectedReasonCode) {
        this.rejectedReasonCode = rejectedReasonCode;
    }

    public Boolean getShouldMarkNotificationStatusDelivered() {
        return shouldMarkNotificationStatusDelivered;
    }

    public void setShouldMarkNotificationStatusDelivered(Boolean shouldMarkNotificationStatusDelivered) {
        this.shouldMarkNotificationStatusDelivered = shouldMarkNotificationStatusDelivered;
    }

    //reset the cache every time only when new incoming call is received via FCM
    public void resetFcmCache(){
        this.isAnswered = false;
        this.isRejected = false;
        this.shouldMarkNotificationStatusDelivered = false;
        this.rejectedReason = null;
        this.rejectedReasonCode = null;
    }
}


