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
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import java.util.ArrayList;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener,
        OnClickListener, OnMenuItemClickListener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private PanelSwitcher mPanelSwitcher;
    private View mClearButton;
    private View mBackspaceButton;
    private View mOverflowMenuButton;

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

        mPersist = new Persist(this);
        mPersist.load();

        mHistory = mPersist.history;

        mDisplay = (CalculatorDisplay) findViewById(R.id.display);

        mClearButton = findViewById(R.id.clear);
        mClearButton.setOnClickListener(mListener);
        mClearButton.setOnLongClickListener(mListener);

        mBackspaceButton = findViewById(R.id.del);
        mBackspaceButton.setOnClickListener(mListener);
        mBackspaceButton.setOnLongClickListener(mListener);

        mLogic = new Logic(this, mHistory, mDisplay);
        mLogic.setListener(this);

        mLogic.setDeleteMode(mPersist.getDeleteMode());
        mLogic.setLineLength(mDisplay.getMaxDigits());

        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        mPanelSwitcher = (PanelSwitcher) findViewById(R.id.panelswitch);
        if (mPanelSwitcher != null) {
            mPanelSwitcher.setCurrentIndex(state==null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));
            mPanelSwitcher.setListener(this);
        }

        mListener.setHandler(mLogic, mPanelSwitcher);
        mDisplay.setOnKeyListener(mListener);

        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) {
            createFakeMenu();
        }
        setOnClickListener(R.id.digit0);
        setOnClickListener(R.id.digit1);
        setOnClickListener(R.id.digit2);
        setOnClickListener(R.id.digit3);
        setOnClickListener(R.id.digit4);
        setOnClickListener(R.id.digit5);
        setOnClickListener(R.id.digit6);
        setOnClickListener(R.id.digit7);
        setOnClickListener(R.id.digit8);
        setOnClickListener(R.id.digit9);
        setOnClickListener(R.id.dot);

        setOnClickListener(R.id.plus);
        setOnClickListener(R.id.minus);
        setOnClickListener(R.id.div);
        setOnClickListener(R.id.mul);
        setOnClickListener(R.id.leftParen);
        setOnClickListener(R.id.rightParen);
        setOnClickListener(R.id.equal);

        setOnClickListener(R.id.sin);
        setOnClickListener(R.id.ln);
        setOnClickListener(R.id.cos);
        setOnClickListener(R.id.lg);
        setOnClickListener(R.id.tan);
        setOnClickListener(R.id.e);
        setOnClickListener(R.id.pi);
        setOnClickListener(R.id.power);
        setOnClickListener(R.id.factorial);
        setOnClickListener(R.id.sqrt);

        mLogic.resumeWithHistory();
        updateDeleteMode();
    }

    private void updateDeleteMode() {
        if (mLogic.getDeleteMode() == Logic.DELETE_MODE_BACKSPACE) {
            mClearButton.setVisibility(View.GONE);
            mBackspaceButton.setVisibility(View.VISIBLE);
        } else {
            mClearButton.setVisibility(View.VISIBLE);
            mBackspaceButton.setVisibility(View.GONE);
        }
    }

    private void setOnClickListener(int id) {
        findViewById(id).setOnClickListener(mListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.basic).setVisible(getBasicVisibility());
        menu.findItem(R.id.advanced).setVisible(getAdvancedVisibility());
        return true;
    }


    private void createFakeMenu() {
        mOverflowMenuButton = findViewById(R.id.overflow_menu);
        if (mOverflowMenuButton != null) {
            mOverflowMenuButton.setVisibility(View.VISIBLE);
            mOverflowMenuButton.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.overflow_menu:
                PopupMenu menu = constructPopupMenu();
                if (menu != null) {
                    menu.show();
                }
                break;
        }
    }

    private PopupMenu constructPopupMenu() {
        final PopupMenu popupMenu = new PopupMenu(this, mOverflowMenuButton);
        final Menu menu = popupMenu.getMenu();
        popupMenu.inflate(R.menu.menu);
        popupMenu.setOnMenuItemClickListener(this);
        onPrepareOptionsMenu(menu);
        return popupMenu;
    }


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return onOptionsItemSelected(item);
    }

    private boolean getBasicVisibility() {
        return mPanelSwitcher != null && mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL;
    }

    private boolean getAdvancedVisibility() {
        return mPanelSwitcher != null && mPanelSwitcher.getCurrentIndex() == BASIC_PANEL;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_history:
                mHistory.clear();
                break;

            case R.id.basic:
                if (mPanelSwitcher != null && mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL) {
                    mPanelSwitcher.moveRight();
                }
                break;

            case R.id.advanced:
                if (mPanelSwitcher != null && mPanelSwitcher.getCurrentIndex() == BASIC_PANEL) {
                    mPanelSwitcher.moveLeft();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mPanelSwitcher != null) {
            state.putInt(STATE_CURRENT_VIEW, mPanelSwitcher.getCurrentIndex());
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
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK
            && mPanelSwitcher != null && mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL) {
            mPanelSwitcher.moveRight();
            return true;
        } else {
            return super.onKeyDown(keyCode, keyEvent);
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

    @Override
    public void onDeleteModeChange() {
        updateDeleteMode();
    }
}
