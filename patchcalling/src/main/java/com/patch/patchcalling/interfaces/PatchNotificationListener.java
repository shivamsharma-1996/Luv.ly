package com.patch.patchcalling.interfaces;

import android.content.Context;

import org.json.JSONObject;

//for incoming notifications
public interface PatchNotificationListener {
   // void onNotificationReceived(String title, String body, String picture, String notificationID);
    void onNotificationReceived(Context context, JSONObject data);

    //for notification-response to a action
    void onSentimentReceived(Context context, int sentiment, String notificationID);

}