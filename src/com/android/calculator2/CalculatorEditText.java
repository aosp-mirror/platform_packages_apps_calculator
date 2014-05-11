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
import android.content.res.Resources;
import android.graphics.Paint;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.android.calculator2.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CalculatorEditText extends EditText {
    private static final String LOG_TAG = "Calculator2";
    private static final int CUT = 0;
    private static final int COPY = 1;
    private static final int PASTE = 2;

    private static Map<String, String> sReplacementTable;
    private static String[] sOperators;

    private final int mMaximumTextSize;
    private final int mMinimumTextSize;
    private final int mStepTextSize;

    private int mWidthConstraint = -1;

    private String[] mMenuItemsStrings;

    public CalculatorEditText(Context context) {
        this(context, null);
    }

    public CalculatorEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

        setCustomSelectionActionModeCallback(new NoTextSelectionMode());
        setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        setCursorVisible(false);

        final Resources res = getResources();
        mMaximumTextSize = res.getDimensionPixelSize(R.dimen.display_maximum_text_size);
        mMinimumTextSize = res.getDimensionPixelSize(R.dimen.display_minimum_text_size);
        mStepTextSize = res.getDimensionPixelSize(R.dimen.display_step_text_size);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // Hack to prevent keyboard and insertion handle from showing.
            cancelLongPress();
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performLongClick() {
        showContextMenu();

        return true;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);

        final String mathText = mathParse(getText().toString());
        // Parse the string into something more "mathematical" sounding.
        if (!TextUtils.isEmpty(mathText)) {
            event.getText().clear();
            event.getText().add(mathText);
            setContentDescription(mathText);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);

        info.setText(mathParse(getText().toString()));
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        // Do nothing.
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        mWidthConstraint =
                MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        setVariableFontSize();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        if (TextUtils.isEmpty(text)) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, mMaximumTextSize);
            return;
        }

        setVariableFontSize();
    }

    private void setVariableFontSize() {
        if (mWidthConstraint < 0) {
            // Not measured, bail early.
            return;
        }

        final Paint paint = new Paint();
        final String measureText = getText().toString();
        int lastFitTextSize = mMinimumTextSize;

        while (lastFitTextSize < mMaximumTextSize) {
            final int nextSize = lastFitTextSize + mStepTextSize;
            paint.setTextSize(nextSize);
            final float measuredTextWidth = paint.measureText(measureText);
            if (measuredTextWidth > mWidthConstraint) {
                break;
            } else {
                lastFitTextSize = nextSize;
            }
        }

        setTextSize(TypedValue.COMPLEX_UNIT_PX, lastFitTextSize);
    }

    private String mathParse(String plainText) {
        String parsedText = plainText;
        if (!TextUtils.isEmpty(parsedText)) {
            // Initialize replacement table.
            initializeReplacementTable();
            for (String operator : sOperators) {
                if (sReplacementTable.containsKey(operator)) {
                    parsedText = parsedText.replace(operator, sReplacementTable.get(operator));
                }
            }
        }

        return parsedText;
    }

    private synchronized void initializeReplacementTable() {
        if (sReplacementTable == null) {
            final Resources res = getContext().getResources();
            final String[] descs = res.getStringArray(R.array.operatorDescs);
            final String[] ops = res.getStringArray(R.array.operators);
            final HashMap<String, String> table = new HashMap<String, String>();
            final int len = ops.length;
            for (int i = 0; i < len; i++) {
                table.put(ops[i], descs[i]);
            }

            sOperators = ops;
            sReplacementTable = Collections.unmodifiableMap(table);
        }
    }

    public boolean onTextContextMenuItem(CharSequence title) {
        if (TextUtils.equals(title, mMenuItemsStrings[CUT])) {
            cutContent();
            return true;
        } else if (TextUtils.equals(title, mMenuItemsStrings[COPY])) {
            copyContent();
            return true;
        } else if (TextUtils.equals(title, mMenuItemsStrings[PASTE])) {
            pasteContent();
            return true;
        }

        return false;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu) {
        if (mMenuItemsStrings == null) {
            final Resources resources = getResources();
            mMenuItemsStrings = new String[3];
            mMenuItemsStrings[CUT] = resources.getString(android.R.string.cut);
            mMenuItemsStrings[COPY] = resources.getString(android.R.string.copy);
            mMenuItemsStrings[PASTE] = resources.getString(android.R.string.paste);
        }

        final MenuHandler handler = new MenuHandler();
        final int len = mMenuItemsStrings.length;
        for (int i = 0; i < len; i++) {
            menu.add(Menu.NONE, i, i, mMenuItemsStrings[i]).setOnMenuItemClickListener(handler);
        }

        if (getText().length() == 0) {
            menu.getItem(CUT).setVisible(false);
            menu.getItem(COPY).setVisible(false);
        }

        final ClipData primaryClip = getPrimaryClip();
        if (primaryClip == null || primaryClip.getItemCount() == 0
                || !canPaste(primaryClip.getItemAt(0).coerceToText(getContext()))) {
            menu.getItem(PASTE).setVisible(false);
        }
    }

    private void setPrimaryClip(ClipData clip) {
        final ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(clip);
    }

    private void copyContent() {
        final Editable text = getText();
        final int textLength = text.length();
        setSelection(0, textLength);
        setPrimaryClip(ClipData.newPlainText(null, text));
        setSelection(textLength);

        Toast.makeText(getContext(), R.string.text_copied_toast, Toast.LENGTH_SHORT).show();
    }

    private void cutContent() {
        final Editable text = getText();
        final int textLength = text.length();
        setSelection(0, textLength);
        setPrimaryClip(ClipData.newPlainText(null, text));
        getText().delete(0, textLength);
        setSelection(0);
    }

    private ClipData getPrimaryClip() {
        final ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(
                Context.CLIPBOARD_SERVICE);
        return clipboard.getPrimaryClip();
    }

    private void pasteContent() {
        final ClipData clip = getPrimaryClip();
        if (clip != null) {
            final int len = clip.getItemCount();
            for (int i = 0; i < len; i++) {
                final CharSequence paste = clip.getItemAt(i).coerceToText(getContext());
                if (canPaste(paste)) {
                    getText().insert(getSelectionEnd(), paste);
                }
            }
        }
    }

    private boolean canPaste(CharSequence paste) {
        try {
            Float.parseFloat(paste.toString());
            return true;
        } catch (NumberFormatException e) {
            Log.e(LOG_TAG, "Error turning string to integer. Ignoring paste.", e);
            return false;
        }
    }

    private class MenuHandler implements MenuItem.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return onTextContextMenuItem(item.getTitle());
        }
    }

    private class NoTextSelectionMode implements ActionMode.Callback {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            copyContent();
            // Prevents the selection action mode on double tap.
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }
}
