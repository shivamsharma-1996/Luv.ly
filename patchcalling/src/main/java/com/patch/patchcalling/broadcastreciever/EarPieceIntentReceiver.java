package com.patch.patchcalling.broadcastreciever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;

import com.patch.patchcalling.fragments.PatchCallscreenFragment;
import com.patch.patchcalling.javaclasses.SocketInit;

public class EarPieceIntentReceiver extends BroadcastReceiver {
    private static final String TAG = "EarPieceIntentReceiver";
    AudioManager audioManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        try {
                            if(!PatchCallscreenFragment.isSpeakerOn){
                                audioManager = SocketInit.getInstance().getAudioManager();
                                if(audioManager.isSpeakerphoneOn()){
                                    audioManager.setSpeakerphoneOn(false);
                                }
                            }
                        }catch (Exception e){

                        }
                        break;
                    case 1:
                        try {
                            PatchCallscreenFragment.setSpeakerOff();
                        }catch (Exception e){

                        }
                        break;
                    default:
                        break;
                }
            }

        }catch (Exception e){

        }
    }
}