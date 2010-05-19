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

import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.content.Context;

import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;
import org.javia.arity.Util;

class Logic {
    private CalculatorDisplay mDisplay;
    private Symbols mSymbols = new Symbols();
    private History mHistory;
    private String  mResult = "";
    private boolean mIsError = false;
    private int mLineLength = 0;

    private static final String INFINITY_UNICODE = "\u221e";

    // the two strings below are the result of Double.toString() for Infinity & NaN
    // they are not output to the user and don't require internationalization
    private static final String INFINITY = "Infinity"; 
    private static final String NAN      = "NaN";

    static final char MINUS = '\u2212';

    private final String mErrorString;

    Logic(Context context, History history, CalculatorDisplay display, Button equalButton) {
        mErrorString = context.getResources().getString(R.string.error);
        mHistory = history;
        mDisplay = display;
        mDisplay.setLogic(this);

        clearWithHistory(false);
    }

    void setLineLength(int nDigits) {
        mLineLength = nDigits;
    }

    boolean eatHorizontalMove(boolean toLeft) {
        EditText editText = mDisplay.getEditText();
        int cursorPos = editText.getSelectionStart();
        return toLeft ? cursorPos == 0 : cursorPos >= editText.length(); 
    }

    private String getText() {
        return mDisplay.getText().toString();
    }

    void insert(String delta) {
        mDisplay.insert(delta);
    }

    private void setText(CharSequence text) {
        mDisplay.setText(text, CalculatorDisplay.Scroll.UP);
    }

    private void clearWithHistory(boolean scroll) {
        mDisplay.setText(mHistory.getText(), 
                         scroll ? CalculatorDisplay.Scroll.UP : CalculatorDisplay.Scroll.NONE);
        mResult = "";
        mIsError = false;
    }

    private void clear(boolean scroll) {
        mDisplay.setText("", scroll ? CalculatorDisplay.Scroll.UP : CalculatorDisplay.Scroll.NONE);
        cleared();
    }

    void cleared() {
        mResult = "";
        mIsError = false;
        updateHistory();
    }

    boolean acceptInsert(String delta) {
        String text = getText();
        return !mIsError &&
            (!mResult.equals(text) || 
             isOperator(delta) ||
             mDisplay.getSelectionStart() != text.length());
    }

    void onDelete() {
        if (getText().equals(mResult) || mIsError) {
            clear(false);
        } else {
            mDisplay.dispatchKeyEvent(new KeyEvent(0, KeyEvent.KEYCODE_DEL));
            mResult = "";
        }
    }

    void onClear() {
        clear(false);
    }

    void onEnter() {
        String text = getText();
        if (text.equals(mResult)) {
            clearWithHistory(false); //clear after an Enter on result
        } else {
            mHistory.enter(text);
            try {
                mResult = evaluate(text);
            } catch (SyntaxException e) {
                mIsError = true;
                mResult = mErrorString;
            }
            if (text.equals(mResult)) {
                //no need to show result, it is exactly what the user entered
                clearWithHistory(true);
            } else {
                setText(mResult);
                //mEqualButton.setText(mEnterString);
            }
        }
    }

    void onUp() {
        String text = getText();
        if (!text.equals(mResult)) {
            mHistory.update(text);
        }
        if (mHistory.moveToPrevious()) {
            mDisplay.setText(mHistory.getText(), CalculatorDisplay.Scroll.DOWN);
        }
    }

    void onDown() {
        String text = getText();
        if (!text.equals(mResult)) {
            mHistory.update(text);
        }
        if (mHistory.moveToNext()) {
            mDisplay.setText(mHistory.getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    void updateHistory() {
        mHistory.update(getText());
    }

    private static final int ROUND_DIGITS = 1;
    String evaluate(String input) throws SyntaxException {
        if (input.trim().equals("")) {
            return "";
        }

        // drop final infix operators (they can only result in error)
        int size = input.length();
        while (size > 0 && isOperator(input.charAt(size - 1))) {
            input = input.substring(0, size - 1);
            --size;
        }

        String result = Util.doubleToString(mSymbols.eval(input), mLineLength, ROUND_DIGITS);
        if (result.equals(NAN)) { // treat NaN as Error
            mIsError = true;
            return mErrorString;
        }
        return result.replace('-', MINUS).replace(INFINITY, INFINITY_UNICODE);
    }

    static boolean isOperator(String text) {
        return text.length() == 1 && isOperator(text.charAt(0));
    }

    static boolean isOperator(char c) {
        //plus minus times div
        return "+\u2212\u00d7\u00f7/*".indexOf(c) != -1;
    }
}
