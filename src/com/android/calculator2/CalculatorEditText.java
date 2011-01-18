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

package com.android.calculator2;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.Toast;

public class CalculatorEditText extends EditText {

    public CalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean performLongClick() {
        Editable text = getText();
        if (TextUtils.isEmpty(text)) {
            return false;
        }

        setSelection(0, text.length());
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(null, getText()));
        Toast.makeText(getContext(), R.string.text_copied_toast, Toast.LENGTH_SHORT).show();
        return true;
    }
}
