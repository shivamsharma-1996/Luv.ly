package com.patch.patchcalling.javaclasses;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.patch.patchcalling.R;

public class PatchTemplates {

   public static class Pinview {
        //private int mViewType;
        private int mPinItemCount;
        /*private int mPinInputType;
        private int mPinItemWidth;
        private int mPinItemHeight;
        private float mPinTextSize;
        private int mPinItemRadius;
        private int mPinItemSpacing;
        private ColorStateList mLineColor;
        private int mLineWidth;
        private boolean isCursorVisible;
        private int mCursorColor;
        private int mCursorWidth;
        private Drawable mItemBackground;
        private boolean mHideLineWhenFilled;*/

        public Pinview() {

        }

        public void setItemCount(int count) {
            mPinItemCount = count;
        }

//        public void setItemWidth(@Px int itemWidth) {
//            mPinItemWidth = itemWidth;
//        }
//
//        public void setItemHeight(@Px int itemHeight) {
//            mPinItemHeight = itemHeight;
//        }
//
//        public void setTextSize(float size) {
//            mPinTextSize = size;
//        }
//
//        public void setItemRadius(@Px int itemRadius) {
//            mPinItemRadius = itemRadius;
//        }
//
//        public void setLineColor(@ColorInt int color) {
//            mLineColor = ColorStateList.valueOf(color);
//        }
//
//        /**
//         * Sets the line color.
//         *
//         * @attr ref R.styleable#PinView_lineColor
//         * @see #setLineColor(int)
//         */
//        public void setLineColor(ColorStateList colors) {
//            mLineColor = colors;
//        }
//
//        public void setCursorColor(@ColorInt int color) {
//            mCursorColor = color;
//        }
//
//        /**
//         * Sets the width (in pixels) of cursor.
//         *
//         * @attr ref R.styleable#PinView_cursorWidth
//         */
//        public void setCursorWidth(@Px int width) {
//            mCursorWidth = width;
//        }
//
//        /**
//         * Sets the line width.
//         *
//         * @attr ref R.styleable#PinView_lineWidth
//         */
//        public void setLineWidth(@Px int borderWidth) {
//            mLineWidth = borderWidth;
//        }
//
//        public void setCursorVisible(boolean visible) {
//            isCursorVisible = visible;
//        }
//
//        /**
//         * Specifies whether the line (border) should be hidden or visible when text entered.
//         * By the default, this flag is false and the line is always drawn.
//         *
//         * @param hideLineWhenFilled true to hide line on a position where text entered,
//         *                           false to always show line
//         * @attr ref R.styleable#PinView_hideLineWhenFilled
//         */
//        public void setHideLineWhenFilled(boolean hideLineWhenFilled) {
//            this.mHideLineWhenFilled = hideLineWhenFilled;
//        }
//
//        public void setItemBackgroundColor(@ColorInt int color) {
//            setItemBackground(new ColorDrawable(color));
//        }
//
//        public void setItemBackground(Drawable background) {
//            mItemBackground = background;
//        }
//
//        public void setInputType(int mPinInputType) {
//            this.mPinInputType = mPinInputType;
//        }
//
        public int getItemCount() {
            return mPinItemCount;
        }
//
//        public int getInputType() {
//            return mPinInputType;
//        }
//
//        public int getItemWidth() {
//            return mPinItemWidth;
//        }
//
//        public int getItemHeight() {
//            return mPinItemHeight;
//        }
//
//        public float getTextSize() {
//            return mPinTextSize;
//        }
//
//        public int getItemRadius() {
//            return mPinItemRadius;
//        }
//
//        public int getItemSpacing() {
//            return mPinItemSpacing;
//        }
//
//        public ColorStateList getLineColor() {
//            return mLineColor;
//        }
//
//        public int getLineWidth() {
//            return mLineWidth;
//        }
//
//        public boolean isCursorVisible() {
//            return isCursorVisible;
//        }
//
//        public int getCursorColor() {
//            return mCursorColor;
//        }
//
//        public int getCursorWidth() {
//            return mCursorWidth;
//        }
//
//        public Drawable getItemBackground() {
//            return mItemBackground;
//        }
//
//        public boolean ismHideLineWhenFilled() {
//            return mHideLineWhenFilled;
//        }
    }

    public static class ScratchCard {
        //Outer Layer
        private String mOuterTextHeader = "";
        private String mOuterTextFooter = "";
        private int mOuterDrawableRes;
        //private String mOuterImageUrl;

        //Inner Layer
        private int mInnerBackgroundColorRes = R.color.darkGray;
        private int mInnerDrawableRes;
        //private String mInnerImageUrl;
        private String mInnerText = "";


        public String getOuterTextHeader() {
            return mOuterTextHeader;
        }

        public void setOuterTextHeader(@NonNull String mOuterTextHeader) {
            this.mOuterTextHeader = mOuterTextHeader;
        }

        public String getOuterTextFooter() {
            return mOuterTextFooter;
        }

        public void setOuterTextFooter(@NonNull String mOuterTextFooter) {
            this.mOuterTextFooter = mOuterTextFooter;
        }

        public int getOuterDrawableRes() {
            return mOuterDrawableRes;
        }

        public void setOuterDrawableRes(@DrawableRes int mOuterDrawableRes) {
            this.mOuterDrawableRes = mOuterDrawableRes;
        }

        /*public String getOuterImageUrl() {
            return mOuterImageUrl;
        }

        public void setOuterImageUrl(String mOuterImageUrl) {
            this.mOuterImageUrl = mOuterImageUrl;
        }*/

        public int getInnerBackgroundColorRes() {
            return mInnerBackgroundColorRes;
        }

        public void setInnerBackgroundColorRes(@ColorRes int mInnerBackgroundColorRes) {
            this.mInnerBackgroundColorRes = mInnerBackgroundColorRes;
        }

        public int getInnerDrawableRes() {
            return mInnerDrawableRes;
        }

        public void setInnerDrawableRes(int mInnerDrawableRes) {
            this.mInnerDrawableRes = mInnerDrawableRes;
        }

       /* public String getInnerImageUrl() {
            return mInnerImageUrl;
        }

        public void setInnerImageUrl(String mInnerImageUrl) {
            this.mInnerImageUrl = mInnerImageUrl;
        }*/

        public String getInnerText() {
            return mInnerText;
        }

        public void setInnerText(@NonNull String mInnerText) {
            this.mInnerText = mInnerText;
        }
    }
}
