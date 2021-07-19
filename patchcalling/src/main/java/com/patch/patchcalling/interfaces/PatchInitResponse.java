package com.patch.patchcalling.interfaces;

/**
 * Created by sanyamjain on 21/07/18.
 */

public interface PatchInitResponse extends BaseInterface{
    void onSuccess(int response);
    void onFailure(int failure);
}
