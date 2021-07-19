package com.patch.patchcalling.broadcastreciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.patch.patchcalling.R;
import com.patch.patchcalling.activity.PatchCallingActivity;
import com.patch.patchcalling.fragments.PatchIncomingFragment;
import com.patch.patchcalling.interfaces.CallNotificationAction;
import com.patch.patchcalling.interfaces.OnSetMissedCallReceiver;
import com.patch.patchcalling.javaclasses.MissedCallNotificationAction;
import com.patch.patchcalling.javaclasses.MissedCallNotificationOpenResult;
import com.patch.patchcalling.javaclasses.PatchSDK;
import com.patch.patchcalling.javaclasses.SocketInit;
import com.patch.patchcalling.models.MissedCallActions;
import com.patch.patchcalling.utils.PatchCommonUtil;
import com.patch.patchcalling.utils.NotificationHandler;
import com.patch.patchcalling.utils.PatchLogger;

public class CallNotificationActionReceiver extends BroadcastReceiver {

    //private NotificationManager notificationManager;
    private static CallNotificationAction callNotificationActionListener;
    public static Boolean isAnswerClickEnabled = false;

    public static void setCallNotificationActionListener(CallNotificationAction actionListener) {
        callNotificationActionListener = actionListener;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        try {
            //notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent notificationTrayCloseIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(notificationTrayCloseIntent); //it closes notification tray

            if (intent.getStringExtra("actionType") != null) {
                String actionType = intent.getStringExtra("actionType");

                switch (actionType) {
                    case "Hangup_Outgoing":
                    case "Hangup_Ongoing":
                        try {
                            callNotificationActionListener.onActionClick(actionType);
                        }catch (Exception e){

                        }
                        break;
                    case "Decline":
                        try {
                            //It will make prevent to revise the missed call status to decline by tapping on delayed IncomingcallnotificationService
                            if(SocketInit.getInstance().isClientbusyOnVoIP()==false){
                                return;
                            }
                            if(PatchIncomingFragment.getInstance()!=null) {
                                callNotificationActionListener.onActionClick(actionType);
                            }else {
                                PatchCommonUtil.getInstance().calldeclined(context);
                            }
                        }catch (Exception e){

                        }
                        break;
                    case "Answer":
                        /*context.startActivity(new Intent(context, PatchCallingActivity.class).setFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        ));
                        callNotificationActionListener.onActionClick(actionType);*/
                        try {
                            try {
                                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                                    if(isAnswerClickEnabled == true){
                                        return;
                                    }
                                }
                            }catch (Exception e){

                            }
                            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                                isAnswerClickEnabled = true;
                            }
                            Intent patchCallingIntent = new Intent(SocketInit.getInstance().getContext()!=null ? SocketInit.getInstance().getContext() : context, PatchCallingActivity.class);
                            //patchCallingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            if(PatchIncomingFragment.getInstance()!=null){
                                patchCallingIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                                if(SocketInit.getInstance().getContext()!=null){
                                    SocketInit.getInstance().getContext().startActivity(patchCallingIntent);
                                }/*else if(context!=null){
                                    context.startActivity(patchCallingIntent);
                                }*/else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        patchCallingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    }
                                    SocketInit.getInstance().getAppContext().getApplicationContext().startActivity(patchCallingIntent);
                                }
                                callNotificationActionListener.onActionClick(actionType);
                            }else {
                                if(intent.hasExtra("callDetails") && intent.getStringExtra("callDetails")!=null){
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        patchCallingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    }
                                    patchCallingIntent.putExtra("callDetails", intent.getStringExtra("callDetails"));
                                    patchCallingIntent.putExtra("sid", intent.hasExtra("sid") ? intent.getStringArrayExtra("sid") : "");
                                    patchCallingIntent.putExtra(context.getString(R.string.screen), context.getString(R.string.incoming));
                                    patchCallingIntent.putExtra(context.getString(R.string.call_answer), context.getString(R.string.call_answer));
                                    if(SocketInit.getInstance().getContext()!=null){
                                        SocketInit.getInstance().getContext().startActivity(patchCallingIntent);
                                    }else {
                                        SocketInit.getInstance().getAppContext().startActivity(patchCallingIntent);
                                    }
                                }
                            }
                        }catch (Exception e){
                        }
                        break;
                    case "Missed":
                        try {
                            final MissedCallActions missedCallActions;
                            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                            NotificationHandler.CallNotificationHandler.getInstance(context).removeNotification(
                                    NotificationHandler.CallNotificationHandler.CallNotificationTypes.MISSED_CALL);

                            if(intent.hasExtra("missedCallActions") && intent.getStringExtra("missedCallActions")!=null){

                                missedCallActions = new Gson().fromJson(intent.getStringExtra("missedCallActions"), MissedCallActions.class);

                                String callDirection = intent.getStringExtra("callDirection");

                                MissedCallNotificationOpenResult result = new MissedCallNotificationOpenResult();
                                result.action = new MissedCallNotificationAction();
                                result.callDetails = new MissedCallNotificationOpenResult.CallDetails();
                                result.action.actionID = missedCallActions.getActionId();
                                result.action.actionLabel = missedCallActions.getActionLabel();
                                if(intent.hasExtra("callerCuid")) {
                                    result.callDetails.callerCuid = intent.getStringExtra("callerCuid");
                                }
                                if(intent.hasExtra("calleeCuid")) {
                                    result.callDetails.calleeCuid = intent.getStringExtra("calleeCuid");
                                }
                                if(intent.hasExtra("callContext")) {
                                    result.callDetails.callContext = intent.getStringExtra("callContext");
                                }
                                    if(callDirection!=null && callDirection.equals("outgoing")){
                                    final String missedCallInitiatorHost = sharedPref.getString(context.getString(R.string.patch_missedCallInitiatorHost), null);
                                    SocketInit.getInstance().setMissedCallInitiatorHost(missedCallInitiatorHost, new OnSetMissedCallReceiver() {
                                        @Override
                                        public void onSetMissedCallReceiver(PatchSDK.MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler) {
                                            try {
                                                if (missedCallNotificationOpenedHandler != null) {
                                                    if (context != null)
                                                        missedCallNotificationOpenedHandler.onnMissedCallNotificationOpened(context.getApplicationContext(), result);
                                                }
                                            } catch (Exception e) {
                                                if (context != null) {
                                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                                                }
                                            }
                                        }
                                    });
                                }else if(callDirection!=null && callDirection.equals("incoming")){
                                    final String missedCallReceiverHost = sharedPref.getString(context.getString(R.string.patch_missedCallReceiverHost), null);
                                    SocketInit.getInstance().setMissedCallReceiverHost(missedCallReceiverHost, new OnSetMissedCallReceiver() {
                                        @Override
                                        public void onSetMissedCallReceiver(PatchSDK.MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler) {
                                            try {
                                                if (missedCallNotificationOpenedHandler != null) {
                                                    if (context != null)
                                                        missedCallNotificationOpenedHandler.onnMissedCallNotificationOpened(context.getApplicationContext(), result);
                                                }
                                            } catch (Exception e) {
                                                if (context != null) {
                                                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                                                }
                                            }
                                        }
                                    });
                                }
                            }
                        }catch (Exception ignored){
                            Log.d("patchsharma", "ignored:" + ignored);
                        }
                        break;
                    default:
                }
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }
}