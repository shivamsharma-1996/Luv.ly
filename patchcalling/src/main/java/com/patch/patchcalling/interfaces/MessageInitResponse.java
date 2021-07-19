package com.patch.patchcalling.interfaces;

/**
 * Created by Shivam Sharma on 07-08-2019.
 */
public interface MessageInitResponse extends BaseInterface{
    void onSuccess(int respose);
    void onFailure(int error);
}
