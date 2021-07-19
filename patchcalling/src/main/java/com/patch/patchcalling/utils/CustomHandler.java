package com.patch.patchcalling.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.IntDef;
import androidx.annotation.StringDef;

import com.patch.patchcalling.fcm.PatchFcmResponse;
import com.patch.patchcalling.interfaces.MessageInitResponse;
import com.patch.patchcalling.interfaces.NotificationResponse;
import com.patch.patchcalling.interfaces.OutgoingCallResponse;
import com.patch.patchcalling.interfaces.OutgoingMessageResponse;
import com.patch.patchcalling.interfaces.PatchInitResponse;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Shivam Sharma on 26-09-2019.
 */
public class CustomHandler {

    private static CustomHandler instance = null;
    private static final String TAG = "CustomHandler";

    public static CustomHandler getInstance() {
        if (instance == null) {
            instance = new CustomHandler();
        }
        return instance;
    }

    private CustomHandler() {
    }

    @IntDef({OutCall.CALL_STATUS, OutCall.ON_SUCCESS, OutCall.ON_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OutCall {
        int CALL_STATUS = 1;
        int ON_SUCCESS = 2;
        int ON_FAILURE = 3;
    }

    @IntDef({Init.ON_SUCCESS, Init.ON_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Init {
        int ON_SUCCESS = 1;
        int ON_FAILURE = 2;
    }

    @IntDef({Fcm.ON_SUCCESS, Fcm.ON_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Fcm {
        int ON_SUCCESS = 1;
        int ON_FAILURE = 2;
    }

    @StringDef({Noti.ON_SUCCESS, Noti.ON_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Noti{
        String ON_SUCCESS = "success";
        String ON_FAILURE = "failure";
    }

    @IntDef({Mess.ON_SUCCESS, Mess.ON_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mess{
        int ON_SUCCESS = 1;
        int ON_FAILURE = 2;
    }

    @IntDef({OutMess.ON_SUCCESS, OutMess.ON_FAILURE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OutMess{
        int ON_SUCCESS = 1;
        int ON_FAILURE = 2;
    }

    public void sendCallAnnotation(final OutgoingCallResponse outgoingCallResponse, @OutCall final int callbackType, final int annotation) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (callbackType) {
                            case OutCall.CALL_STATUS:
                                outgoingCallResponse.callStatus(annotation);
                                break;
                            case OutCall.ON_SUCCESS:
                                outgoingCallResponse.onSuccess(annotation);
                                break;
                            case OutCall.ON_FAILURE:
                                outgoingCallResponse.onFailure(annotation);
                                break;
                        }
                    }catch (Exception e){

                    }

                }
            });
        }catch (Exception e){

        }
    }

    public void sendInitAnnotations(final PatchInitResponse patchInitResponse, @Init final int callbackType, final int annotation) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (callbackType) {
                            case 1:
                                patchInitResponse.onSuccess(annotation);
                                break;
                            case 2:
                                patchInitResponse.onFailure(annotation);
                                break;
                        }
                    }catch (Exception e){

                    }

                }
            });
        }catch (Exception e){

        }

    }

    public void sendFcmAnnotations(final PatchFcmResponse patchFcmResponse, @Fcm final int callbackType, final int annotation) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (callbackType) {
                            case 1:
                                patchFcmResponse.onSuccess(annotation);
                                break;
                            case 2:
                                patchFcmResponse.onFailure(annotation);
                                break;
                        }
                    }catch (Exception e){

                    }

                }
            });
        }catch (Exception e){

        }

    }

    public void sendNotiAnnotations(final NotificationResponse notificationResponse, @Noti final String  callbackType, final int failure) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (callbackType) {
                            case Noti.ON_SUCCESS:
                                notificationResponse.onSuccess();
                                break;
                            case Noti.ON_FAILURE:
                                notificationResponse.onFailure(failure);
                                break;
                        }
                    }catch (Exception e){

                    }

                }
            });
        }catch (Exception e){

        }

    }

    public void sendMessAnnotations(final MessageInitResponse messageInitResponse, @Mess final int callbackType, final int annotation) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (callbackType) {
                            case 1:
                                messageInitResponse.onSuccess(annotation);
                                break;
                            case 2:
                                messageInitResponse.onFailure(annotation);
                                break;
                        }
                    }catch (Exception e){

                    }

                }
            });
        }catch (Exception e){

        }

    }

    public void sendOutMessAnnotations(final OutgoingMessageResponse outgoingMessageResponse,@OutMess final int callbackType, final int annotation) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (callbackType) {
                            case 1:
                                outgoingMessageResponse.onSuccess(annotation);
                                break;
                            case 2:
                                outgoingMessageResponse.onFailure(annotation);
                                break;
                        }
                    }catch (Exception e){

                    }

                }
            });
        }catch (Exception e){

        }

    }
}
