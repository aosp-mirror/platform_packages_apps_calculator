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

import android.content.Context;

import org.javia.arity.Symbols;
import org.javia.arity.SyntaxException;
import org.javia.arity.Util;

public class CalculatorExpressionEvaluator {
    private static final int MAX_DIGITS = 14;

    private final Symbols mSymbols;
    private final CalculatorExpressionTokenizer mTokenizer;

    private final String mErrorNaN;
    private final String mErrorSyntax;

    public CalculatorExpressionEvaluator(Context context) {
        mSymbols = new Symbols();
        mTokenizer = new CalculatorExpressionTokenizer(context);

        mErrorNaN = context.getString(R.string.error_nan);
        mErrorSyntax = context.getString(R.string.error_syntax);
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
            if (expr == null || expr.length() == 0 || Double.valueOf(expr) != null) {
                callback.onEvaluate(expr, null, null);
                return;
            }
        } catch (NumberFormatException e) {
            // expr is not a simple number
        }

        try {
            double result = mSymbols.eval(expr);
            if (Double.isNaN(result)) {
                callback.onEvaluate(expr, null, mErrorNaN);
            } else {
                callback.onEvaluate(expr, mTokenizer.getLocalizedExpression(
                        Util.doubleToString(result, MAX_DIGITS, 0)), null);
            }
        } catch (SyntaxException e) {
            callback.onEvaluate(expr, null, mErrorSyntax);
        }
    }

    public interface EvaluateCallback {
        public void onEvaluate(String expr, String result, String error);
    }
}
