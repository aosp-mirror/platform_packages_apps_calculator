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
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupMenu.OnMenuItemClickListener;

import com.android.calculator2.R;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener,
        OnMenuItemClickListener, View.OnClickListener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private ViewPager mPager;

    static final int BASIC_PANEL    = 0;
    static final int ADVANCED_PANEL = 1;

    private static final String LOG_TAG = "Calculator";
    private static final boolean DEBUG  = false;
    private static final boolean LOG_ENABLED = false;
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Disable IME for this application
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        setContentView(R.layout.main);
        mPager = (ViewPager) findViewById(R.id.panelswitch);
        if (mPager != null) {
            mPager.setAdapter(new PageAdapter(mPager));
        }

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        if (mPager != null) {
            mPager.setCurrentItem(state == null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));
        }

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
        final View clear = findViewById(R.id.clear);
        final View delete = findViewById(R.id.del);

        if (deleteMode == Logic.DELETE_MODE_BACKSPACE) {
            delete.setVisibility(View.VISIBLE);
            clear.setVisibility(View.GONE);
        } else {
            delete.setVisibility(View.GONE);
            clear.setVisibility(View.VISIBLE);
        }
    }

    static void log(String message) {
        if (LOG_ENABLED) {
            Log.v(LOG_TAG, message);
        }
    }

    @Override
    public void onChange() {
        invalidateOptionsMenu();
    }

    static class PageAdapter extends PagerAdapter {
        private View mSimplePage;
        private View mAdvancedPage;

        public PageAdapter(ViewPager parent) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            mSimplePage = inflater.inflate(R.layout.simple_pad, parent, false);
            mAdvancedPage = inflater.inflate(R.layout.advanced_pad, parent, false);
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public float getPageWidth(int position) {
            return 0.90f;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final View page = position == 0 ? mSimplePage : mAdvancedPage;
            container.addView(page);
            return page;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
        }
    }
}
