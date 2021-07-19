package com.patch.patchcalling.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
 public class MissedCallActions {
    @SerializedName("actionId")
    @Expose
    private String actionId;
    @SerializedName("actionLabel")
    @Expose
    private String actionLabel;

     public MissedCallActions(String actionId, String actionLabel) {
         this.actionId = actionId;
         this.actionLabel = actionLabel;
     }


     public String getActionId() {
         return actionId;
     }

     public void setActionId(String actionId) {
         this.actionId = this.actionId;
     }

     public String getActionLabel() {
         return actionLabel;
     }

     public void setActionLabel(String actionLabel) {
         this.actionLabel = actionLabel;
     }

     @Override
     public String toString() {
         return "MissedCallActions{" +
                 "actionId='" + actionId + '\'' +
                 ", actionLabel='" + actionLabel + '\'' +
                 '}';
     }
 }
