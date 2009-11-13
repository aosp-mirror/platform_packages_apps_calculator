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
import android.text.Editable;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.animation.TranslateAnimation;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewSwitcher;
import android.graphics.Rect;
import android.graphics.Paint;

/**
 * Provides vertical scrolling for the input/result EditText.
 */
class CalculatorDisplay extends ViewSwitcher {
    // only these chars are accepted from keyboard
    private static final char[] ACCEPTED_CHARS = 
        "0123456789.+-*/\u2212\u00d7\u00f7()!%^".toCharArray();

    private static final int ANIM_DURATION = 500;
    enum Scroll { UP, DOWN, NONE }
    
    TranslateAnimation inAnimUp;
    TranslateAnimation outAnimUp;
    TranslateAnimation inAnimDown;
    TranslateAnimation outAnimDown;

    private Logic mLogic;
    private boolean mComputedLineLength = false;
    
    public CalculatorDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Calculator calc = (Calculator) getContext();
        calc.adjustFontSize((TextView)getChildAt(0));
        calc.adjustFontSize((TextView)getChildAt(1));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!mComputedLineLength) {
            mLogic.setLineLength(getNumberFittingDigits((TextView) getCurrentView()));
            mComputedLineLength = true;
        }
    }

    // compute the maximum number of digits that fit in the
    // calculator display without scrolling.
    private int getNumberFittingDigits(TextView display) {
        int available = display.getWidth()
            - display.getTotalPaddingLeft() - display.getTotalPaddingRight();
        Paint paint = display.getPaint();
        float digitWidth = paint.measureText("2222222222") / 10f;
        return (int) (available / digitWidth);
    }

    protected void setLogic(Logic logic) {
        mLogic = logic;
        NumberKeyListener calculatorKeyListener =
            new NumberKeyListener() {
                public int getInputType() {
                    // Don't display soft keyboard.
                    return InputType.TYPE_NULL;
                }
            
                protected char[] getAcceptedChars() {
                    return ACCEPTED_CHARS;
                }

                public CharSequence filter(CharSequence source, int start, int end,
                                           Spanned dest, int dstart, int dend) {
                    /* the EditText should still accept letters (eg. 'sin')
                       coming from the on-screen touch buttons, so don't filter anything.
                    */
                    return null;
                }
            };

        Editable.Factory factory = new CalculatorEditable.Factory(logic);
        for (int i = 0; i < 2; ++i) {
            EditText text = (EditText) getChildAt(i);
            text.setBackgroundDrawable(null);
            text.setEditableFactory(factory);
            text.setKeyListener(calculatorKeyListener);
        }
    }

    @Override
    public void setOnKeyListener(OnKeyListener l) {
        getChildAt(0).setOnKeyListener(l);
        getChildAt(1).setOnKeyListener(l);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        inAnimUp = new TranslateAnimation(0, 0, h, 0);
        inAnimUp.setDuration(ANIM_DURATION);
        outAnimUp = new TranslateAnimation(0, 0, 0, -h);
        outAnimUp.setDuration(ANIM_DURATION);

        inAnimDown = new TranslateAnimation(0, 0, -h, 0);
        inAnimDown.setDuration(ANIM_DURATION);
        outAnimDown = new TranslateAnimation(0, 0, 0, h);
        outAnimDown.setDuration(ANIM_DURATION);
    }

    void insert(String delta) {
        EditText editor = (EditText) getCurrentView();
        int cursor = editor.getSelectionStart();
        editor.getText().insert(cursor, delta);
    }

    EditText getEditText() {
        return (EditText) getCurrentView();
    }
        
    Editable getText() {
        EditText text = (EditText) getCurrentView();
        return text.getText();
    }
    
    void setText(CharSequence text, Scroll dir) {
        if (getText().length() == 0) {
            dir = Scroll.NONE;
        }
        
        if (dir == Scroll.UP) {
            setInAnimation(inAnimUp);
            setOutAnimation(outAnimUp);            
        } else if (dir == Scroll.DOWN) {
            setInAnimation(inAnimDown);
            setOutAnimation(outAnimDown);            
        } else { // Scroll.NONE
            setInAnimation(null);
            setOutAnimation(null);
        }
        
        EditText editText = (EditText) getNextView();
        editText.setText(text);
        //Calculator.log("selection to " + text.length() + "; " + text);
        editText.setSelection(text.length());
        showNext();
    }
    
    void setSelection(int i) {
        EditText text = (EditText) getCurrentView();
        text.setSelection(i);
    }
    
    int getSelectionStart() {
        EditText text = (EditText) getCurrentView();
        return text.getSelectionStart();
    }
    
    @Override
    protected void onFocusChanged(boolean gain, int direction, Rect prev) {
        //Calculator.log("focus " + gain + "; " + direction + "; " + prev);
        if (!gain) {
            requestFocus();
        }
    }
}
