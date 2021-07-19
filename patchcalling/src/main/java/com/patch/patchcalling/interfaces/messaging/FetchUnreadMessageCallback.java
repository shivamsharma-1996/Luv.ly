package com.patch.patchcalling.interfaces.messaging;

/**
 * Created by Shivam Sharma on 10-08-2019.
 */
public interface FetchUnreadMessageCallback {
    void onUnreadCountResponse(int unreadCount);
    void onFailure(int error);
}
