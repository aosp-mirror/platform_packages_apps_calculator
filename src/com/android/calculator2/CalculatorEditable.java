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

import android.text.SpannableStringBuilder;
import android.text.Editable;

class CalculatorEditable extends SpannableStringBuilder {
    private static final char[] ORIGINALS    = {'-',      '*',      '/'};
    private static final char[] REPLACEMENTS = {'\u2212', '\u00d7', '\u00f7'};
    private boolean isInsideReplace = false;
    private Logic mLogic;

    private CalculatorEditable(CharSequence source, Logic logic) {
        super(source);
        mLogic = logic;
    }

    @Override
    public SpannableStringBuilder 
    replace(int start, int end, CharSequence tb, int tbstart, int tbend) {
        if (isInsideReplace) {
            return super.replace(start, end, tb, tbstart, tbend);
        } else {        
            isInsideReplace = true;
            try {
                String delta = tb.subSequence(tbstart, tbend).toString();            
                return internalReplace(start, end, delta);
            } finally {
                isInsideReplace = false;
            }
        }
    }
    
    private SpannableStringBuilder internalReplace(int start, int end, String delta) {
        if (!mLogic.acceptInsert(delta)) {            
            mLogic.cleared();
            start = 0;
            end = length();
        }

        for (int i = ORIGINALS.length - 1; i >= 0; --i) {
            delta = delta.replace(ORIGINALS[i], REPLACEMENTS[i]);
        }

        int length = delta.length();
        if (length == 1) {
            char text = delta.charAt(0);

            //don't allow two dots in the same number
            if (text == '.') {
                int p = start - 1;
                while (p >= 0 && Character.isDigit(charAt(p))) {
                    --p;
                }
                if (p >= 0 && charAt(p) == '.') {
                    return super.replace(start, end, "");
                }
            }

            char prevChar = start > 0 ? charAt(start-1) : '\0';

            //don't allow 2 successive minuses
            if (text == Logic.MINUS && prevChar == Logic.MINUS) {
                return super.replace(start, end, "");
            }

            //don't allow multiple successive operators
            if (Logic.isOperator(text)) {
                while (Logic.isOperator(prevChar) && 
                       (text != Logic.MINUS || prevChar == '+')) {
                    --start;
                    prevChar = start > 0 ? charAt(start-1) : '\0';
                }
            }

            //don't allow leading operator + * /
            if (start == 0 && Logic.isOperator(text) && text != Logic.MINUS) {
                return super.replace(start, end, "");
            }
        } 
        return super.replace(start, end, delta);
    }

    public static class Factory extends Editable.Factory {
        private Logic mLogic;

        public Factory(Logic logic) {
            mLogic = logic;
        }

        public Editable newEditable(CharSequence source) {
            return new CalculatorEditable(source, mLogic);
        }
    }
}
