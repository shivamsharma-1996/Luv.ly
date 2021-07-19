package com.patch.patchcalling.interfaces.messaging;

/**
 * Created by Shivam Sharma on 10-08-2019.
 */
public interface MarkMessageSeenCallback {
    void onMarkMessagesSeenSuccess(int response);
    void onFailure(int error);

}
