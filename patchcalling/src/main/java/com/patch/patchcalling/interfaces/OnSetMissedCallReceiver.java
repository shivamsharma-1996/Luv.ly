package com.patch.patchcalling.interfaces;

import com.patch.patchcalling.javaclasses.PatchSDK;

/**
 * Created by Shivam Sharma on 25-05-2019.
 */
public interface OnSetMissedCallReceiver {

    void onSetMissedCallReceiver(PatchSDK.MissedCallNotificationOpenedHandler missedCallNotificationOpenedHandler);
}
