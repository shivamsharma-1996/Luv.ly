package com.patch.patchcalling.interfaces;

/**
 * Created by sanyamjain on 26/12/18.
 */

public interface OutgoingMessageResponse extends BaseInterface{
    void onSuccess(int success);
    void onFailure(int failure);
}
