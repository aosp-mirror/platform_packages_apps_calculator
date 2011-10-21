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

import com.android.calculator2.CalculatorDisplay.Scroll;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.widget.EditText;
import android.content.Context;

import java.util.Locale;

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

    public static final String MARKER_EVALUATE_ON_RESUME = "?";

    // the two strings below are the result of Double.toString() for Infinity & NaN
    // they are not output to the user and don't require internationalization
    private static final String INFINITY = "Infinity";
    private static final String NAN      = "NaN";

    static final char MINUS = '\u2212';

    private final String mErrorString;

    public final static int DELETE_MODE_BACKSPACE = 0;
    public final static int DELETE_MODE_CLEAR = 1;

    private int mDeleteMode = DELETE_MODE_BACKSPACE;

    public interface Listener {
        void onDeleteModeChange();
    }

    private Listener mListener;

    Logic(Context context, History history, CalculatorDisplay display) {
        mErrorString = context.getResources().getString(R.string.error);
        mHistory = history;
        mDisplay = display;
        mDisplay.setLogic(this);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void setDeleteMode(int mode) {
        if (mDeleteMode != mode) {
            mDeleteMode = mode;
            mListener.onDeleteModeChange();
        }
    }

    public int getDeleteMode() {
        return mDeleteMode;
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
        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    public void onTextChanged() {
        setDeleteMode(DELETE_MODE_BACKSPACE);
    }

    public void resumeWithHistory() {
        clearWithHistory(false);
    }

    private void clearWithHistory(boolean scroll) {
        String text = mHistory.getText();
        if (MARKER_EVALUATE_ON_RESUME.equals(text)) {
            if (!mHistory.moveToPrevious()) {
                text = "";
            }
            text = mHistory.getText();
            evaluateAndShowResult(text, CalculatorDisplay.Scroll.NONE);
        } else {
            mResult = "";
            mDisplay.setText(
                    text, scroll ? CalculatorDisplay.Scroll.UP : CalculatorDisplay.Scroll.NONE);
            mIsError = false;
        }
    }

    private void clear(boolean scroll) {
        mHistory.enter("");
        mDisplay.setText("", scroll ? CalculatorDisplay.Scroll.UP : CalculatorDisplay.Scroll.NONE);
        cleared();
    }

    void cleared() {
        mResult = "";
        mIsError = false;
        updateHistory();

        setDeleteMode(DELETE_MODE_BACKSPACE);
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
        clear(mDeleteMode == DELETE_MODE_CLEAR);
    }

    void onEnter() {
        if (mDeleteMode == DELETE_MODE_CLEAR) {
            clearWithHistory(false); // clear after an Enter on result
        } else {
            evaluateAndShowResult(getText(), CalculatorDisplay.Scroll.UP);
        }
    }

    public void evaluateAndShowResult(String text, Scroll scroll) {
        try {
            String result = evaluate(text);
            if (!text.equals(result)) {
                mHistory.enter(text);
                mResult = result;
                mDisplay.setText(mResult, scroll);
                setDeleteMode(DELETE_MODE_CLEAR);
            }
        } catch (SyntaxException e) {
            mIsError = true;
            mResult = mErrorString;
            mDisplay.setText(mResult, scroll);
            setDeleteMode(DELETE_MODE_CLEAR);
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
        String text = getText();
        // Don't set the ? marker for empty text or the error string.
        // There is no need to evaluate those later.
        if (!TextUtils.isEmpty(text) && !TextUtils.equals(text, mErrorString)
                && text.equals(mResult)) {
            mHistory.update(MARKER_EVALUATE_ON_RESUME);
        } else {
            mHistory.update(getText());
        }
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

        double value = mSymbols.eval(input);

        String result = "";
        for (int precision = mLineLength; precision > 6; precision--) {
            result = tryFormattingWithPrecision(value, precision);
            if (result.length() <= mLineLength) {
                break;
            }
        }
        return result.replace('-', MINUS).replace(INFINITY, INFINITY_UNICODE);
    }

    private String tryFormattingWithPrecision(double value, int precision) {
        // The standard scientific formatter is basically what we need. We will
        // start with what it produces and then massage it a bit.
        String result = String.format(Locale.US, "%" + mLineLength + "." + precision + "g", value);
        if (result.equals(NAN)) { // treat NaN as Error
            mIsError = true;
            return mErrorString;
        }
        String mantissa = result;
        String exponent = null;
        int e = result.indexOf('e');
        if (e != -1) {
            mantissa = result.substring(0, e);

            // Strip "+" and unnecessary 0's from the exponent
            exponent = result.substring(e + 1);
            if (exponent.startsWith("+")) {
                exponent = exponent.substring(1);
            }
            exponent = String.valueOf(Integer.parseInt(exponent));
        } else {
            mantissa = result;
        }

        int period = mantissa.indexOf('.');
        if (period == -1) {
            period = mantissa.indexOf(',');
        }
        if (period != -1) {
            // Strip trailing 0's
            while (mantissa.length() > 0 && mantissa.endsWith("0")) {
                mantissa = mantissa.substring(0, mantissa.length() - 1);
            }
            if (mantissa.length() == period + 1) {
                mantissa = mantissa.substring(0, mantissa.length() - 1);
            }
        }

        if (exponent != null) {
            result = mantissa + 'e' + exponent;
        } else {
            result = mantissa;
        }
        return result;
    }

    static boolean isOperator(String text) {
        return text.length() == 1 && isOperator(text.charAt(0));
    }

    static boolean isOperator(char c) {
        //plus minus times div
        return "+\u2212\u00d7\u00f7/*".indexOf(c) != -1;
    }
}
