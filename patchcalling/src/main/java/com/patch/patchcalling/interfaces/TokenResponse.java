package com.patch.patchcalling.interfaces;

/**
 * Created by Shivam Sharma on 27-07-2019.
 */
public interface TokenResponse {
    void onSuccess(String response);
    void onFailure(String  failure);
}
