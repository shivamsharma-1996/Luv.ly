package com.patch.patchcalling.interfaces;

/**
 * Created by sanyamjain on 21/12/18.
 */

public interface OutgoingCallResponse extends BaseInterface {
    void callStatus(int reason);
    void onSuccess(int response);
    void onFailure(int error);
}
