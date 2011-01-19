/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Displays text using the special AndroidClock font.
 */
public class AndroidClockTextView extends View {

    private static final String SYSTEM = "/system/fonts/";
    private static final String SYSTEM_FONT_TIME_BACKGROUND = SYSTEM + "AndroidClock.ttf";
    private static final String SYSTEM_FONT_TIME_FOREGROUND = SYSTEM + "AndroidClock_Highlight.ttf";

    private static final String ATTR_FONT_HIGHLIGHTS_ENABLED = "fontHighlightsEnabled";
    private static final String ATTR_SHORT_FORM = "shortForm";

    private static Typeface sTypeface;
    private static Typeface sHighlightTypeface;

    // An invisible text view that is used for parsing properties
    private TextView mProperties;
    private int mColor;
    private Paint mTextPaint;
    private Paint mHighlightPaint;
    private String mText = "";

    private int mTextAlpha = 154;
    private int mHighlightAlpha = 230;

    private float mTextSize;
    private Rect mTextBounds = new Rect();
    private Rect mTempRect = new Rect();
    private int mX = -1;
    private int mY = -1;
    private float mFontDescent;
    private boolean mHighlightsEnabled;
    private boolean mShortForm;

    public AndroidClockTextView(Context context) {
        super(context);
    }

    public AndroidClockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mProperties = new TextView(context, attrs);

        mHighlightsEnabled =
            attrs.getAttributeBooleanValue(null, ATTR_FONT_HIGHLIGHTS_ENABLED, true);
        mShortForm =attrs.getAttributeBooleanValue(null, ATTR_SHORT_FORM, false);

        mTextSize = mProperties.getTextSize();
        mColor = mProperties.getTextColors().getDefaultColor();

        if (sTypeface == null) {
            sTypeface = Typeface.createFromFile(SYSTEM_FONT_TIME_BACKGROUND);
            sHighlightTypeface = Typeface.createFromFile(SYSTEM_FONT_TIME_FOREGROUND);
        }

        mTextPaint = new Paint();
        mTextPaint.setTypeface(sTypeface);
        mTextPaint.setColor(mColor);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setAlpha(mTextAlpha);

        mHighlightPaint = new Paint();
        mHighlightPaint.setTypeface(sHighlightTypeface);
        mHighlightPaint.setColor(mColor);
        mHighlightPaint.setTextAlign(Paint.Align.LEFT);
        mHighlightPaint.setAntiAlias(true);
        mHighlightPaint.setTextSize(mTextSize);
        mHighlightPaint.setAlpha(mHighlightsEnabled ? mHighlightAlpha : mTextAlpha);

        mFontDescent = mTextPaint.getFontMetrics().descent;
    }

    public void setTextColor(int color) {
        mColor = color;
        mTextPaint.setColor(color);
        mHighlightPaint.setColor(color);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth, measuredHeight;
        mTextPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
        mHighlightPaint.getTextBounds(mText, 0, mText.length(), mTempRect);
        mTextBounds.union(mTempRect);

        int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        } else {
            measuredWidth = mTextBounds.right + getPaddingLeft() + getPaddingRight();
        }
        mode = MeasureSpec.getMode(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            // Text bounds are measured from the bottom-left corner, so mTextBounds.top
            // is a negative number
            measuredHeight = -mTextBounds.top + getPaddingTop() + getPaddingBottom();
        }
        setMeasuredDimension(measuredWidth, measuredHeight);

        mX = getPaddingLeft();
        mY = (int) (measuredHeight - getPaddingBottom() - mFontDescent);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        mX = getPaddingLeft();
        mY = (int) (getHeight() - getPaddingBottom() - mFontDescent);
    }

    public void setText(CharSequence time) {
        if (mShortForm) {
            if (time.length() > 0) {
                mText = String.valueOf(time.charAt(0));
            } else {
                mText = null;
            }
        } else {
            mText = String.valueOf(time);
        }
        requestLayout();
        invalidate();
    }

    @Override
    public int getBaseline() {
        return mY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mText != null) {
            canvas.drawText(mText, mX, mY, mTextPaint);
            canvas.drawText(mText, mX, mY, mHighlightPaint);
        }
    }
}
