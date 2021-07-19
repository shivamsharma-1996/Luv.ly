package com.patch.patchcalling.fcm;

/**
 * Created by Shivam Sharma on 20-04-2020.
 */
public interface PatchFcmResponse {
    void onSuccess(int response);
    void onFailure(int failure);
}
