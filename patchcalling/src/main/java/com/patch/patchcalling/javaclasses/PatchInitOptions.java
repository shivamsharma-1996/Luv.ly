package com.patch.patchcalling.javaclasses;

import com.patch.patchcalling.models.MissedCallActions;

import org.json.JSONObject;

import java.util.List;

public class PatchInitOptions {
    //required
    private JSONObject initJson;

    //optional
    private Boolean enableReadPhoneState = false;
    private Boolean enablePatchNotification = false;
    private Boolean disablePatchNotificationUI = false;
    private List<MissedCallActions> missedCallReceiverActions = null, missedCallInitiatorActions = null;
    private String missedCallReceiverHost = null, missedCallInitiatorHost = null;
    private String notificationListenerHost = null /* ,notificationOpenedHost = null*/;
    private PatchTemplates.Pinview pinview;
    private PatchTemplates.ScratchCard scratchCard;

    /**
     * InitOptions can only be constructed via {@link PatchInitOptions.Builder}
     */
    private PatchInitOptions() {

    }

    private PatchInitOptions(Builder builder) {
        this.initJson = builder.initJson;
        this.enableReadPhoneState = builder.enableReadPhoneState;
        this.enablePatchNotification = builder.enablePatchNotification;
        this.disablePatchNotificationUI = builder.disablePatchNotificationUI;
        this.missedCallReceiverActions = builder.missedCallReceiverActions;
        this.missedCallReceiverHost = builder.missedCallReceiverHost;

        this.missedCallInitiatorActions = builder.missedCallInitiatorActions;
        this.missedCallInitiatorHost = builder.missedCallInitiatorHost;
        this.notificationListenerHost = builder.notificationListenerHost;
        //this.notificationOpenedHost = builder.notificationOpenedHost;
        this.pinview = builder.pinview;
        this.scratchCard = builder.scratchCard;
    }

    public JSONObject getInitJson() {
        return this.initJson;
    }

    Boolean isReadPhoneStateEnabled() {
        return this.enableReadPhoneState;
    }

    Boolean isNotificationEnabled() {
        return this.enablePatchNotification;
    }

    Boolean isNotificationUIEnabled() {
        return this.disablePatchNotificationUI;
    }

    List<MissedCallActions> getMissedCallReceiverActions() {
        return this.missedCallReceiverActions;
    }

    String getMissedCallReceiverHost() {
        return this.missedCallReceiverHost;
    }

    List<MissedCallActions> getMissedCallInitiatorActions() {
        return this.missedCallInitiatorActions;
    }

    String getMissedCallInitiatorHost() {
        return this.missedCallInitiatorHost;
    }

    public String getNotificationListenerHost() {
        return notificationListenerHost;
    }

   /* public String getNotificationOpenedHost() {
        return notificationOpenedHost;
    }*/

    public PatchTemplates.Pinview getPinviewConfig() {
        return pinview;
    }

    public PatchTemplates.ScratchCard getScratchCardConfig() {
        return scratchCard;
    }


    public static class Builder {
        //required
        private JSONObject initJson;

        //optional
        private Boolean enableReadPhoneState = false;
        private Boolean enablePatchNotification = false;
        private Boolean disablePatchNotificationUI = false;
        private List<MissedCallActions> missedCallReceiverActions = null, missedCallInitiatorActions = null;
        private String missedCallReceiverHost = null, missedCallInitiatorHost = null;
        private String notificationListenerHost = null/*, notificationOpenedHost = null*/;
        private PatchTemplates.Pinview pinview;
        private PatchTemplates.ScratchCard scratchCard;

        public Builder(JSONObject initJson) {
            this.initJson = initJson;
        }

        public Builder enableReadPhoneState(Boolean enable) {
            this.enableReadPhoneState = enable;
            return this;
        }

        public Builder enablePatchNotification(Boolean enable, String notificationListenerHost) {
            this.enablePatchNotification = enable;
            this.notificationListenerHost = notificationListenerHost;
            return this;
        }

        public Builder disablePatchNotificationUI(Boolean enable) {
            this.disablePatchNotificationUI = enable;
            return this;
        }

        public Builder setMissedCallInitiatorActions(List<MissedCallActions> missedCallInitiatorActions, String missedCallInitiatorHost) {
            this.missedCallInitiatorActions = missedCallInitiatorActions;
            this.missedCallInitiatorHost = missedCallInitiatorHost;
            return this;
        }

        public Builder setMissedCallReceiverActions(List<MissedCallActions> missedCallReceiverActions, String missedCallReceiverHost) {
            this.missedCallReceiverActions = missedCallReceiverActions;
            this.missedCallReceiverHost = missedCallReceiverHost;
            return this;
        }

        /*public Builder setNotificationListenerHost(String notificationReceiverHost) {
            this.notificationListenerHost = notificationReceiverHost;
            return this;

        }*/

//        public Builder setNotificationOpenedHost(String notificationOpenedHost) {
//            this.notificationOpenedHost = notificationOpenedHost;
//            return this;
//        }

        public PatchInitOptions build() {
            return new PatchInitOptions(this);
        }

        public Builder enablePinviewTemplate(PatchTemplates.Pinview pinview) {
            this.pinview = pinview;
            return this;
        }

        public Builder enableScratchCardTemplate(PatchTemplates.ScratchCard scratchCard) {
            this.scratchCard = scratchCard;
            return this;
        }
    }
}