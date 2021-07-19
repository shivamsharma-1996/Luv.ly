package com.patch.patchcalling.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.patch.patchcalling.utils.PatchLogger;

/**
 * This class keeps a check of network connectivity of the user. whther the user has active internet connection or not.
 * Created by sanyamjain on 18/12/18.
 */

public class ConnectivityReceiver extends BroadcastReceiver {

    private ConnectivityReceiverListener mConnectivityReceiverListener;

    public ConnectivityReceiver(ConnectivityReceiverListener listener) {
        mConnectivityReceiverListener = listener;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            mConnectivityReceiverListener.onNetworkConnectionChanged(isConnected(context));
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage() , Log.getStackTraceString(e), context);
        }
    }

    public static boolean isConnected(Context context) throws Exception {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public interface ConnectivityReceiverListener {
        void onNetworkConnectionChanged(boolean isConnected);
    }
}