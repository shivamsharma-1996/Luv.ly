package com.patch.patchcalling.javaclasses;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.patch.patchcalling.interfaces.CallType;
import com.patch.patchcalling.utils.CustomHandler;

import static com.patch.patchcalling.PatchResponseCodes.OutgoingCallCallback.CallStatus.CALL_OVER;

/**
 * Created by sanyamjain on 18/08/18.
 */

public class myJsInterface implements AudioManager.OnAudioFocusChangeListener {
    private Context context;
    private AudioManager audioManager;
    private AudioFocusRequest mAFRequest;
    AudioManager.OnAudioFocusChangeListener afChangeListener;
    CustomHandler customHandler = CustomHandler.getInstance();

    public myJsInterface(Context con, AudioManager audioManager) {
        this.context = con;
        this.audioManager = audioManager;
    }


    @JavascriptInterface
    public String getFromAndroid() {
        return "This is from android.";
    }

    /**
     * Show Toast Message
     *
     * @param toast
     */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show Dialog
     *
     * @param dialogMsg
     */
    @JavascriptInterface
    public void showDialog(String dialogMsg) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();

        // Setting Dialog Title
        alertDialog.setTitle("JS triggered Dialog");

        // Setting Dialog Message
        alertDialog.setMessage(dialogMsg);

        // Setting alert dialog icon
        //alertDialog.setIcon((status) ? R.drawable.success : R.drawable.fail);

        // Setting OK Button
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(context, "Dialog dismissed!", Toast.LENGTH_SHORT).show();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    @JavascriptInterface
    public String getCalldetail() {
        String data = "";
        try {
            SocketInit socketInit = SocketInit.getInstance();
            data = socketInit.getCallData();
        } catch (Exception e) {

        }
        return data;
    }


    @JavascriptInterface
    public void callVoip() {
        try {
            CallType callType = SocketInit.getInstance().getCallType();
            callType.callVoip();
        } catch (Exception e) {
        }
    }

    /**
     * called when the call is ended. This function is called from the JS.
     */
    @JavascriptInterface
    public void endCall() throws Exception {
        try {
            audioManager.setSpeakerphoneOn(false);
            releaseAudio();
            if (SocketInit.getInstance().getOutgoingCallResponse() != null) {
                PatchSDK.sdkReady = true;
                //SocketInit.getInstance().getOutgoingCallResponse().callStatus(CALL_OVER);
                customHandler.sendCallAnnotation(SocketInit.getInstance().getOutgoingCallResponse(), CustomHandler.OutCall.CALL_STATUS, CALL_OVER);
            }
//            try {
//                AppUtil.getInstance().sendBroadcast(PatchCallingActivity.getInstance().getApplicationContext(), Constants.ACTION_CALL_OVER);
//            }catch (Exception e){
//            }
            SocketInit.getInstance().setClientbusyOnVoIP(false);
            ((Activity) context).finishAndRemoveTask();
        } catch (Exception e) {

        }

    }

    /**
     * called when the audio is to be started. This function is called from the JS.
     */
    @JavascriptInterface
    public void startAudio() throws Exception {
        try {
            audioManager.setSpeakerphoneOn(false);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                this.mAFRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAcceptsDelayedFocusGain(false)
                        .setOnAudioFocusChangeListener(this)
                        .setWillPauseWhenDucked(true)
                        .build();
                int res = this.audioManager.requestAudioFocus(mAFRequest);
                final Object mFocusLock = new Object();
                synchronized (mFocusLock) {
                    if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
//                    Log.d("Patch", "Failed to get audio focus");
                    } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        audioManager.setSpeakerphoneOn(false);
//                    Log.d("Patch", "Gained Audio Focus");
                    } else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                        audioManager.setSpeakerphoneOn(false);
//                    Log.d("Patch", "Delay in getting audio focus");
                    }
                }
            } else {
                int result = audioManager.requestAudioFocus(afChangeListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                    audioManager.setSpeakerphoneOn(false);
                } else {
//                Log.d("Patch", "Failed to get audio focus");
                }
            }
        } catch (Exception e) {

        }
    }

    @JavascriptInterface
    public void speaker() {
        try {
            if (audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(false);
            } else {
                audioManager.setSpeakerphoneOn(true);
            }
        } catch (Exception e) {

        }
    }

    @JavascriptInterface
    private int checkSignalStrength() throws Exception {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            int linkSpeed = wifiManager.getConnectionInfo().getRssi();
            Log.d("Patch", "internet speed on wifi is: " + linkSpeed);
            return linkSpeed;
        } else {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            @SuppressLint("MissingPermission") CellInfoLte cellinfogsm = (CellInfoLte) telephonyManager.getAllCellInfo().get(0);
            CellSignalStrengthLte cellSignalStrengthLte = cellinfogsm.getCellSignalStrength();
            //Log.d("Patch", "signal strength of mobile data is: " + cellSignalStrengthLte.getDbm());
            return cellSignalStrengthLte.getDbm();
        }
    }

    /**
     * called when the audio is released by the webview. This is called from the JS.
     */

    @JavascriptInterface
    private void releaseAudio() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(this.mAFRequest);
                this.audioManager.setMode(AudioManager.MODE_NORMAL);
            } else {
                audioManager.abandonAudioFocus(afChangeListener);
                this.audioManager.setMode(AudioManager.MODE_NORMAL);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            //PatchLogger.createLog(e.getMessage() , Log.getStackTraceString(e));
        }
    }

    @JavascriptInterface
    public void moveToNextScreen() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        // Setting Dialog Title
        alertDialog.setTitle("Alert");
        // Setting Dialog Message
        alertDialog.setMessage("Are you sure you want to leave to next screen?");
        // Setting Positive "Yes" Button
        alertDialog.setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //Move to Next screen

                    }
                });
        // Setting Negative "NO" Button
        alertDialog.setNegativeButton("NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Cancel Dialog
                        dialog.cancel();
                    }
                });
        // Showing Alert Message
        alertDialog.show();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
//            Log.d("PATCH", "")

        }
    }
//    @JavascriptInterface
//    public void isOnHold(JSONObject jsonObject){
//        Log.d("PatchHold", "jsonObject" + jsonObject);
//    }
}
