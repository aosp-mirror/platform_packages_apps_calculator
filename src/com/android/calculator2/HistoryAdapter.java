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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.calculator2.R;

import org.javia.arity.SyntaxException;

import java.util.Vector;

class HistoryAdapter extends BaseAdapter {
    private final Vector<HistoryEntry> mEntries;
    private final LayoutInflater mInflater;
    private final Logic mEval;

    public HistoryAdapter(Context context, History history, Logic evaluator) {
        mEntries = history.getEntries();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEval = evaluator;
    }

    @Override
    public int getCount() {
        return mEntries.size() - 1;
    }

    @Override
    public Object getItem(int position) {
        return mEntries.elementAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.history_item, parent, false);
        } else {
            view = convertView;
        }

        final TextView expr = (TextView) view.findViewById(R.id.historyExpr);
        final TextView result = (TextView) view.findViewById(R.id.historyResult);

        final HistoryEntry entry = mEntries.elementAt(position);
        final String base = entry.getBase();
        expr.setText(entry.getBase());

        try {
            final String res = mEval.evaluate(base);
            result.setText("= " + res);
        } catch (SyntaxException e) {
            result.setText("");
        }

        return view;
    }
}
