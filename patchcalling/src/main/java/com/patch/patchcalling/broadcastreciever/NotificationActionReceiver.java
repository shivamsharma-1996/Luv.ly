package com.patch.patchcalling.broadcastreciever;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.patch.patchcalling.Constants;
import com.patch.patchcalling.R;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.PatchLogger;
import com.patch.patchcalling.utils.PendingSentimentHandler;
import com.patch.patchcalling.utils.SocketIOManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.socket.client.Ack;

public class NotificationActionReceiver extends BroadcastReceiver {
    NotificationManager notificationManager;
    private SocketInit socketInit;
    private int sentiment, isNeutral;
    private String notificationId, senderCuid, notificationScheduledTime, callId = null;
    private int localNotificationId;

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent notificationTrayCloseIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(notificationTrayCloseIntent); //it closes notification tray

            sentiment = intent.getIntExtra("sentiment", 0);
            Bundle bundle = intent.getExtras().getBundle("bundle");
            if (bundle.getString("callId") != null) {
                callId = bundle.getString("callId");
            }
            notificationId = bundle.getString("notificationId");
            senderCuid = bundle.getString("senderCuid");
            notificationScheduledTime = bundle.getString("sentAt");
            localNotificationId = bundle.getInt("localNotificationId", 0);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!SocketIOManager.isSocketConnected() || !PatchCommonUtil.getInstance().hasInternetAccess(context)) {
                            //emit sentiment response to sigsock
                            saveSentimentResponseToLocal(context, sentiment, notificationId, notificationScheduledTime, callId);
                        } else {
                            performAction(context, sentiment, notificationId, senderCuid, callId);
                        }
                    } catch (Exception e) {
                        if (context != null)
                            PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                    }
                }
            });
            thread.start();

            notificationManager.cancel(localNotificationId);   //removing notification from notification tray
        } catch (Exception e) {

            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }


    private void saveSentimentResponseToLocal(final Context context, int sentiment, String notificationId, String notificationScheduledTime, String callId) {
        try {
            JSONObject jsonObjectToWrite = new JSONObject();
            jsonObjectToWrite.put("btnSentiment", sentiment);
            jsonObjectToWrite.put("notificationId", notificationId);
            jsonObjectToWrite.put("callId", callId);
            jsonObjectToWrite.put("notificationScheduledTime", notificationScheduledTime);
            PendingSentimentHandler.getInstance(context).writeToFile(jsonObjectToWrite);
        } catch (JSONException e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public void performAction(final Context context, final int sentimentValue, String notificationId, String senderCuid, String callId) {
        socketInit = SocketInit.getInstance();
        //without notificationId its meaningless to emit for notification-response event
        if (socketInit != null && notificationId != null) {
            try {
                JSONObject notificationActionObject = new JSONObject();
                JSONArray btnSentiment = new JSONArray();
                btnSentiment.put(sentimentValue);
                notificationActionObject.put("btnSentiment", btnSentiment);
                notificationActionObject.put("notificationId", notificationId);
                if (callId != null) {
                    notificationActionObject.put("callId", callId); //put here callID
                }
                if (senderCuid != null) {
                    notificationActionObject.put("senderCuid", senderCuid);
                }

                socketInit.getSocket().emit(context.getString(R.string.snotificationRespsonse), notificationActionObject, new Ack() {
                    @Override
                    public void call(Object... args) {
                        try {
                            JSONObject data = (JSONObject) args[0];

                            if (data.getBoolean("status")) {
                            } else {
                            }
                        } catch (JSONException e) {
                            if (context != null)
                                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                        }
                    }
                });
            } catch (Exception e) {
                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            }
        }

    }
}