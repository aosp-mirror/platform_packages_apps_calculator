/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.calculator2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.MotionEvent;
import android.content.res.Resources;

import java.util.Map;

/**
 * Button with click-animation effect.
 */
class ColorButton extends Button implements OnClickListener {
    int CLICK_FEEDBACK_COLOR;
    static final int CLICK_FEEDBACK_INTERVAL = 10;
    static final int CLICK_FEEDBACK_DURATION = 350;
    
    Drawable mButtonBackground;
    Drawable mButton;
    float mTextX;
    float mTextY;
    long mAnimStart;
    OnClickListener mListener;
    
    public ColorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        mListener = ((Calculator) context).mListener;
        setOnClickListener(this);
    }

    public void onClick(View view) {
        animateClickFeedback();
        mListener.onClick(this);
    }

    private void init() {
        setBackgroundDrawable(null);

        Resources res = getResources();

        mButtonBackground = res.getDrawable(R.drawable.button_bg);
        mButton = res.getDrawable(R.drawable.button);
        CLICK_FEEDBACK_COLOR = res.getColor(R.color.magic_flame);
        getPaint().setColor(res.getColor(R.color.button_text));
        
        mAnimStart = -1;
    }


    @Override 
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        int selfW = mButton.getIntrinsicWidth();
        int selfH = mButton.getIntrinsicHeight();
        int marginX = (w - selfW) / 2;
        int marginY = (h - selfH) / 2;
        mButtonBackground.setBounds(marginX, marginY, marginX + selfW, marginY + selfH);
        mButton.setBounds(marginX, marginY, marginX + selfW, marginY + selfH);
        measureText();
    }

    private void measureText() {
        Paint paint = getPaint();
        mTextX = (getWidth() - paint.measureText(getText().toString())) / 2;
        mTextY = (getHeight() - paint.ascent() - paint.descent()) / 2;
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        measureText();
    }

    private void drawMagicFlame(int duration, Canvas canvas) {
        int alpha = 255 - 255 * duration / CLICK_FEEDBACK_DURATION;
        int color = CLICK_FEEDBACK_COLOR | (alpha << 24);
        mButtonBackground.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;
        float angle = 250.0f * duration / CLICK_FEEDBACK_DURATION;
        canvas.rotate(angle, cx, cy);
        mButtonBackground.draw(canvas);
        canvas.rotate(-angle, cx, cy);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mAnimStart != -1) {
            int animDuration = (int) (System.currentTimeMillis() - mAnimStart);
            
            if (animDuration >= CLICK_FEEDBACK_DURATION) {
                mButtonBackground.clearColorFilter();
                mAnimStart = -1;
            } else {
                drawMagicFlame(animDuration, canvas);
                postInvalidateDelayed(CLICK_FEEDBACK_INTERVAL);
            }
        } else if (isPressed()) {
            drawMagicFlame(0, canvas);
        }
        
        mButton.draw(canvas);
        
        CharSequence text = getText();
        canvas.drawText(text, 0, text.length(), mTextX, mTextY, getPaint());
    }

    public void animateClickFeedback() {
        mAnimStart = System.currentTimeMillis();
        invalidate();        
    } 
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int a = event.getAction();
        if (a == MotionEvent.ACTION_DOWN 
                || a == MotionEvent.ACTION_CANCEL
                || a == MotionEvent.ACTION_UP) {
            invalidate();
        }
        return super.onTouchEvent(event);
    }
}
