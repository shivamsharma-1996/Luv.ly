package com.patch.patchcalling.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.patch.patchcalling.R;

import java.io.IOException;
import java.util.Map;

public class OutgoingUtil {
    private static MediaPlayer mediaPlayer;
    private static OutgoingUtil instance = null;
    private static final String TAG = "OutgoingUtil";

    public static OutgoingUtil getInstance() {
        if (instance == null) {
            instance = new OutgoingUtil();
        }
        return instance;
    }

    private OutgoingUtil() {
    }

    public void setOutgoingRingtone(Context context) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String ringtone = sharedPref.getString("patch_ringtone", null);
            if (ringtone == null) {
                try {
                    mediaPlayer = MediaPlayer.create(context, R.raw.outgoing_tone);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                } catch (Exception e) {
                    if (context != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                }
            } else {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(ringtone);
                    mediaPlayer.prepare();
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                } catch (IOException e) {
                    if (context != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                } catch (Exception e) {
                    if (context != null)
                        PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
                }
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }

    }

    public void stopMediaPlayer(Context context) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                //mediaPlayer = null;
            } catch (Exception e) {
                if (context != null)
                    PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
            }
        }
    }

    public void releaseMediaPlayer(Context context) {

        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }
    }

    public void setBranding(final Context context, final Map<String, View> brandingViewParams, final String tag) {
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            String color = sharedPref.getString("patch_fontColor", null);
            String backgrounColor = sharedPref.getString("patch_bgColor", null);
            String logoUrl = sharedPref.getString("patch_logo", null);
            int bgColor = Color.parseColor(backgrounColor);
            if (logoUrl != null && logoUrl.length() != 0 && validateBrandingParam(brandingViewParams, "ivLogo")) {
                RequestOptions requestOptions = RequestOptions
                        .diskCacheStrategyOf(DiskCacheStrategy.ALL);

                Glide.with(context)
                        .load(logoUrl)
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                //((ImageView) brandingViewParams.get("ivLogo")).setImageResource(R.drawable.patch);
                                //iv_logo.setImageResource(R.drawable.patch);
                                //Log.d("PATCH", "Problem fetching brand logo");
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                //Log.d("PATCH", "brand logo is fetched");
                                return false;
                            }
                        }).apply(requestOptions)
//                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .into(((ImageView) brandingViewParams.get("ivLogo")));
            } else {
                // if (validateBrandingParam(brandingViewParams, "ivLogo"))
                //((ImageView) brandingViewParams.get("ivLogo")).setImageResource(R.drawable.patch);
            }
            if (backgrounColor.length() != 0 && backgrounColor.length() > 3 && validateBrandingParam(brandingViewParams, "llBackground")) {
                (brandingViewParams.get("llBackground")).setBackgroundColor(bgColor);
            } else {
                if (validateBrandingParam(brandingViewParams, "llBackground"))
                    (brandingViewParams.get("llBackground")).setBackgroundColor(context.getResources().getColor(R.color.bg_incomingcolor));
            }

            if (color != null  && color.length() > 3) {
                int fontColor = Color.parseColor(color);
                ((TextView) brandingViewParams.get("tvContext")).setTextColor(fontColor);
                ((TextView) brandingViewParams.get("tvPoweredBy")).setTextColor(fontColor);
                ((TextView) brandingViewParams.get("tvCallScreenLabel")).setTextColor(fontColor);
                switch (tag) {
                    case "PatchCallscreenFragment":
                        ((TextView) brandingViewParams.get("tvMute")).setTextColor(fontColor);
                        ((TextView) brandingViewParams.get("tvDisconnect")).setTextColor(fontColor);
                        ((TextView) brandingViewParams.get("tvHold")).setTextColor(fontColor);
                        ((TextView) brandingViewParams.get("tvSpeaker")).setTextColor(fontColor);
                        ((TextView) brandingViewParams.get("tvHoldState")).setTextColor(fontColor);
                        ((TextView) brandingViewParams.get("tvNetworkLatency")).setTextColor(fontColor);
                        ((Chronometer) brandingViewParams.get("chTimer")).setTextColor(fontColor);
                        break;
                    case "PatchOutgoingFragment":
                        ((TextView) brandingViewParams.get("tvCallStatus")).setTextColor(fontColor);
                        //((TextView) brandingViewParams.get("tvOutgoingCall")).setTextColor(fontColor);
                        break;
                    case "PatchIncomingFragment":
                        ((TextView) brandingViewParams.get("tvAccept")).setTextColor(fontColor);
                        ((TextView) brandingViewParams.get("tvDecline")).setTextColor(fontColor);
                        break;
                }
            } else {
                ((TextView) brandingViewParams.get("tvContext")).setTextColor(getColorResource(context, R.color.white));
                ((TextView) brandingViewParams.get("tvPoweredBy")).setTextColor(getColorResource(context, R.color.white));
                switch (tag) {
                    case "PatchCallscreenFragment":
                        ((TextView) brandingViewParams.get("tvMute")).setTextColor(getColorResource(context, R.color.white));
                        ((TextView) brandingViewParams.get("tvDisconnect")).setTextColor(getColorResource(context, R.color.white));
                        ((TextView) brandingViewParams.get("tvSpeaker")).setTextColor(getColorResource(context, R.color.white));
                        ((Chronometer) brandingViewParams.get("chTimer")).setTextColor(getColorResource(context, R.color.white));
                        break;
                    case "PatchOutgoingFragment":
                        ((TextView) brandingViewParams.get("tvCallStatus")).setTextColor(getColorResource(context, R.color.white));
                        ((TextView) brandingViewParams.get("tvOutgoingCall")).setTextColor(getColorResource(context, R.color.white));
                        break;
                    case "PatchIncomingFragment":
                        ((TextView) brandingViewParams.get("tvAccept")).setTextColor(getColorResource(context, R.color.white));
                        ((TextView) brandingViewParams.get("tvDecline")).setTextColor(getColorResource(context, R.color.white));
                        break;
                }
            }
        } catch (Exception e) {
            if (context != null)
                PatchLogger.createLog(e.getMessage(), Log.getStackTraceString(e), context);
        }

    }

    public boolean validateBrandingParam(Map<String, View> brandingViewParams, String id) {
        return brandingViewParams.containsKey(id) && brandingViewParams.get(id) != null;
    }

    public int getColorResource(Context context, int color) {
        return context.getResources().getColor(color);
    }

}
