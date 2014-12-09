/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;
import org.javia.arity.Util;

public class CalculatorExpressionEvaluator {

    /**
     * The maximum number of significant digits to display.
     */
    private static final int MAX_DIGITS = 12;

    /**
     * A {@link Double} has at least 17 significant digits, we show the first {@link #MAX_DIGITS}
     * and use the remaining digits as guard digits to hide floating point precision errors.
     */
    private static final int ROUNDING_DIGITS = Math.max(17 - MAX_DIGITS, 0);

    private final Symbols mSymbols;
    private final CalculatorExpressionTokenizer mTokenizer;

    public CalculatorExpressionEvaluator(CalculatorExpressionTokenizer tokenizer) {
        mSymbols = new Symbols();
        mTokenizer = tokenizer;
    }

    public void evaluate(CharSequence expr, EvaluateCallback callback) {
        evaluate(expr.toString(), callback);
    }

    public void evaluate(String expr, EvaluateCallback callback) {
        expr = mTokenizer.getNormalizedExpression(expr);

        // remove any trailing operators
        while (expr.length() > 0 && "+-/*".indexOf(expr.charAt(expr.length() - 1)) != -1) {
            expr = expr.substring(0, expr.length() - 1);
        }

        try {
            if (expr.length() == 0 || Double.valueOf(expr) != null) {
                callback.onEvaluate(expr, null, Calculator.INVALID_RES_ID);
                return;
            }
        } catch (NumberFormatException e) {
            // expr is not a simple number
        }

        try {
            double result = mSymbols.eval(expr);
            if (Double.isNaN(result)) {
                callback.onEvaluate(expr, null, R.string.error_nan);
            } else {
                // The arity library uses floating point arithmetic when evaluating the expression
                // leading to precision errors in the result. The method doubleToString hides these
                // errors; rounding the result by dropping N digits of precision.
                final String resultString = mTokenizer.getLocalizedExpression(
                        Util.doubleToString(result, MAX_DIGITS, ROUNDING_DIGITS));
                callback.onEvaluate(expr, resultString, Calculator.INVALID_RES_ID);
            }
        } catch (SyntaxException e) {
            callback.onEvaluate(expr, null, R.string.error_syntax);
        }
    }

    public interface EvaluateCallback {
        public void onEvaluate(String expr, String result, int errorResourceId);
    }
}
