package com.patch.patchcalling.interfaces;

/**
 * Created by sanyamjain on 17/10/18.
 */

public interface CallStatus {
    void onAnswer();
    void onDecline();
    void onMiss();
    void onIosApf(String data);

     interface incomingCallStatus {
        void onCancel();
    }
}
