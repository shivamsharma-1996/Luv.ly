package com.patch.patchcalling.utils;

import com.patch.patchcalling.javaclasses.PatchSDK;

public class PatchObservable {

    PatchSDK.PinviewTextObserver pinviewTextObserver;


    private static PatchObservable instance = null;
    public static PatchObservable getInstance() {
        if (instance == null) {
            instance = new PatchObservable();
        }
        return instance;
    }

    private PatchObservable() {
    }

    public PatchSDK.PinviewTextObserver getPatchPinviewTextObserver() {
        return pinviewTextObserver;
    }

    public void setPatchPinviewTextObserver(PatchSDK.PinviewTextObserver pinviewTextObserver) {
        this.pinviewTextObserver = pinviewTextObserver;
    }
}
