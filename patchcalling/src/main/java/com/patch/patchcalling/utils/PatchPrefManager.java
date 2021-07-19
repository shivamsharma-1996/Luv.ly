package com.patch.patchcalling.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PatchPrefManager {

    private static final String PATCH_PREFERENCE = "PATCH_ANDROID_SDK";

    private static final String SOME_STRING_VALUE = "SOME_STRING_VALUE";

    private static final String DEFAULT_VALUE_STRING = null;
    private static final int DEFAULT_VALUE_INT = 0;
    private static final Long DEFAULT_VALUE_LONG = 0L;
            
    private PatchPrefManager() {
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(PATCH_PREFERENCE, Context.MODE_PRIVATE);
    }

    public static String getStringPref(Context context, String key) {
        return getSharedPreferences(context).getString(key, DEFAULT_VALUE_STRING);
    }

    public static void setStringPref(Context context, String key, String value) {
        getSharedPreferences(context).edit()
                .putString(key, value).
                apply();
    }
}