/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CalculatorExpressionTokenizer {

    private final Map<String, String> mReplacementMap;

    public CalculatorExpressionTokenizer(Context context) {
        mReplacementMap = new HashMap<>();

        mReplacementMap.put(".", context.getString(R.string.dec_point));

        mReplacementMap.put("0", context.getString(R.string.digit_0));
        mReplacementMap.put("1", context.getString(R.string.digit_1));
        mReplacementMap.put("2", context.getString(R.string.digit_2));
        mReplacementMap.put("3", context.getString(R.string.digit_3));
        mReplacementMap.put("4", context.getString(R.string.digit_4));
        mReplacementMap.put("5", context.getString(R.string.digit_5));
        mReplacementMap.put("6", context.getString(R.string.digit_6));
        mReplacementMap.put("7", context.getString(R.string.digit_7));
        mReplacementMap.put("8", context.getString(R.string.digit_8));
        mReplacementMap.put("9", context.getString(R.string.digit_9));

        mReplacementMap.put("/", context.getString(R.string.op_div));
        mReplacementMap.put("*", context.getString(R.string.op_mul));
        mReplacementMap.put("-", context.getString(R.string.op_sub));

        mReplacementMap.put("cos", context.getString(R.string.fun_cos));
        mReplacementMap.put("ln", context.getString(R.string.fun_ln));
        mReplacementMap.put("log", context.getString(R.string.fun_log));
        mReplacementMap.put("sin", context.getString(R.string.fun_sin));
        mReplacementMap.put("tan", context.getString(R.string.fun_tan));

        mReplacementMap.put("Infinity", context.getString(R.string.inf));
    }

    public String getNormalizedExpression(String expr) {
        for (Entry<String, String> replacementEntry : mReplacementMap.entrySet()) {
            expr = expr.replace(replacementEntry.getValue(), replacementEntry.getKey());
        }
        return expr;
    }

    public String getLocalizedExpression(String expr) {
        for (Entry<String, String> replacementEntry : mReplacementMap.entrySet()) {
            expr = expr.replace(replacementEntry.getKey(), replacementEntry.getValue());
        }
        return expr;
    }
}
