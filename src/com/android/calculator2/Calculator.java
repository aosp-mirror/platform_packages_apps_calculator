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

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android.calculator2.R;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener,
        OnMenuItemClickListener, View.OnClickListener {
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    private final EventListener mListener = new EventListener();

    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private ViewPager mPager;

    private View mClr;
    private View mDel;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.main);

        mPager = (ViewPager) findViewById(R.id.panelswitch);
        if (mPager != null) {
            final LayoutInflater inflater = LayoutInflater.from(this);
            final View simple = inflater.inflate(R.layout.simple_pad, mPager, false);
            final View advanced = inflater.inflate(R.layout.advanced_pad, mPager, false);
            mClr = simple.findViewById(R.id.clear);
            mDel = simple.findViewById(R.id.del);

            final PageAdapter adapter = new PageAdapter();
            adapter.add(simple);
            adapter.add(advanced);

            mPager.setAdapter(adapter);
            mPager.setCurrentItem(state == null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));
        }

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.getHistory();

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);
        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        final HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        mListener.setHandler(mLogic, mPager);
        mDisplay.setOnKeyListener(mListener);
        mLogic.resumeWithHistory();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        mListener.onClick(v);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);

        if (mPager != null) {
            state.putInt(STATE_CURRENT_VIEW, mPager.getCurrentItem());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mLogic.updateHistory();
        mPersist.setDeleteMode(mLogic.getDeleteMode());
        mPersist.save();
    }

    @Override
    public void onDeleteModeChange(int deleteMode) {
        if (deleteMode == Logic.DELETE_MODE_BACKSPACE) {
            mDel.setVisibility(View.VISIBLE);
            mClr.setVisibility(View.GONE);
        } else {
            mDel.setVisibility(View.GONE);
            mClr.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onChange() {
        invalidateOptionsMenu();
    }
}
