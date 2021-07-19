package com.patch.patchcalling.broadcastreciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.patch.patchcalling.fragments.PatchCallscreenFragment;
import com.patch.patchcalling.javaclasses.SocketInit;

/**
 * Created by Shivam Sharma on 20-09-2019.
 */
public class PhoneStateReceiver extends BroadcastReceiver {

    private static Boolean isAnswered = false;
    public void onReceive(final Context context, Intent intent) {
        try {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if(state.equals(TelephonyManager.EXTRA_STATE_RINGING)){
            }
            if ((state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK))){
                if(!isAnswered){
                    SocketInit.getInstance().setClientbusyOnPstn(true);
                    isAnswered = true;
                    if(PatchCallscreenFragment.isFragmentVisible){
                        PatchCallscreenFragment.switchHoldState();
                    }
                }
            }
            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)){
                if(isAnswered){
                    isAnswered = false;
                    SocketInit.getInstance().setClientbusyOnPstn(false);
                    if(PatchCallscreenFragment.isFragmentVisible){
                        PatchCallscreenFragment.switchHoldState();
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}