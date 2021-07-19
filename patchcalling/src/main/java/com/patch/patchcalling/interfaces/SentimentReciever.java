package com.patch.patchcalling.interfaces;

import android.content.Context;

@Deprecated
//for notification-response to a action
interface SentimentReciever {
    void onSentimentRecieved(Context context,Boolean sentiment, String notificationID);
}