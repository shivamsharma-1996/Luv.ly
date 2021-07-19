package com.patch.patchcalling.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

import com.patch.patchcalling.R;

/**
 * Created by sanyamjain on 17/08/18.
 */

public class CustomTextView extends AppCompatTextView {
    public static final String ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android";


    public CustomTextView(Context context) {
        super(context);
        applyCustomFont(context, null);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        applyCustomFont(context, attrs);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        applyCustomFont(context, attrs);
    }


    private void applyCustomFont(Context context, AttributeSet attrs) {

        TypedArray attributeArray = context.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
        String fontName = attributeArray.getString(R.styleable.CustomTextView_customFont);


        Typeface customFont = selectTypeface(context, fontName);
        setTypeface(customFont);

        attributeArray.recycle();
    }


    private Typeface selectTypeface(Context context, String fontName) {

        if (fontName.contentEquals(context.getString(R.string.OpenSans_Light))) {
            return FontCache.getTypeface("OpenSans-Light.ttf", context);

        } else if (fontName.contentEquals(context.getString(R.string.OpenSans_Reguler))) {
            return FontCache.getTypeface("OpenSans-Regular.ttf", context);
        } else if (fontName.contentEquals(context.getString(R.string.OpenSans_Semibold))) {
            return FontCache.getTypeface("OpenSans-Semibold.ttf", context);
        } else {
            // no matching font found
            return FontCache.getTypeface("OpenSans-Regular.ttf", context);
        }
    }
}
