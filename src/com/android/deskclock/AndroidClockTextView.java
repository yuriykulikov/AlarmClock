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
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
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
    private static final String ATTR_USE_CLOCK_TYPEFACE = "useClockTypeface";

    private static Typeface sClockTypeface;
    private static Typeface sHighlightTypeface;
    private static Typeface sStandardTypeface;

    // An invisible text view that is used for parsing properties
    private TextView mProperties;
    private ColorStateList mColor;
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
    private boolean mHighlightsEnabled;
    private boolean mShortForm;
    private boolean mUseClockTypeface;

    public AndroidClockTextView(Context context) {
        super(context);
    }

    public AndroidClockTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mProperties = new TextView(context, attrs);

        mHighlightsEnabled =
            attrs.getAttributeBooleanValue(null, ATTR_FONT_HIGHLIGHTS_ENABLED, true);
        mShortForm =attrs.getAttributeBooleanValue(null, ATTR_SHORT_FORM, false);
        mUseClockTypeface =attrs.getAttributeBooleanValue(null, ATTR_USE_CLOCK_TYPEFACE, true);
        if (!mUseClockTypeface) {
            mHighlightsEnabled = false;
        }

        mTextSize = mProperties.getTextSize();
        mColor = mProperties.getTextColors();

        if (sClockTypeface == null) {
            sClockTypeface = Typeface.createFromFile(SYSTEM_FONT_TIME_BACKGROUND);
            sHighlightTypeface = Typeface.createFromFile(SYSTEM_FONT_TIME_FOREGROUND);
            sStandardTypeface = Typeface.DEFAULT_BOLD;
        }

        mTextPaint = new Paint();
        mTextPaint.setTypeface(mUseClockTypeface ? sClockTypeface : sStandardTypeface);
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(mTextSize);

        mHighlightPaint = new Paint();
        mHighlightPaint.setTypeface(sHighlightTypeface);
        mHighlightPaint.setTextAlign(Paint.Align.LEFT);
        mHighlightPaint.setAntiAlias(true);
        mHighlightPaint.setTextSize(mTextSize);
    }

    public void setTextColor(int color) {
        mColor = ColorStateList.valueOf(color);
        mTextPaint.setColor(color);
        mHighlightPaint.setColor(color);
        refreshDrawableState();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth, measuredHeight;
        FontMetrics fontMetrics = mTextPaint.getFontMetrics();

        mTextPaint.getTextBounds(mText, 0, mText.length(), mTextBounds);
        if (mUseClockTypeface) {
            mHighlightPaint.getTextBounds(mText, 0, mText.length(), mTempRect);
            mTextBounds.union(mTempRect);
        }

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
            // Text bounds are measured from the bottom-left corner, so fontMetrics.top
            // is a negative number
            measuredHeight = (int) (-fontMetrics.top + fontMetrics.bottom +
                    getPaddingTop() + getPaddingBottom());
        }
        setMeasuredDimension(measuredWidth, measuredHeight);

        mX = getPaddingLeft();
        mY = (int) (measuredHeight - getPaddingBottom() - fontMetrics.descent);
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
    protected void drawableStateChanged() {
        int color = mColor.getColorForState(getDrawableState(), Color.RED);
        mTextPaint.setColor(color);
        mHighlightPaint.setColor(color);
        if (mUseClockTypeface) {
            mTextPaint.setAlpha(mTextAlpha);
            mHighlightPaint.setAlpha(mHighlightsEnabled ? mHighlightAlpha : mTextAlpha);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mText != null) {
            canvas.drawText(mText, mX, mY, mTextPaint);
            if (mUseClockTypeface) {
                canvas.drawText(mText, mX, mY, mHighlightPaint);
            }
        }
    }
}
