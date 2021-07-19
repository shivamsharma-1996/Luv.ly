package com.patch.patchcalling.interfaces;

//for outgoing notificaations
public interface NotificationResponse extends BaseInterface{
    void onSuccess();
    void onFailure(int failure);
}