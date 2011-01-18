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
import android.util.Config;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class Calculator extends Activity implements PanelSwitcher.Listener, Logic.Listener {
    EventListener mListener = new EventListener();
    private CalculatorDisplay mDisplay;
    private Persist mPersist;
    private History mHistory;
    private Logic mLogic;
    private PanelSwitcher mPanelSwitcher;
    private View mClearButton;
    private View mBackspaceButton;

    private static final int CMD_CLEAR_HISTORY  = 1;
    private static final int CMD_BASIC_PANEL    = 2;
    private static final int CMD_ADVANCED_PANEL = 3;

    static final int BASIC_PANEL    = 0;
    static final int ADVANCED_PANEL = 1;

    private static final String LOG_TAG = "Calculator";
    private static final boolean DEBUG  = false;
    private static final boolean LOG_ENABLED = DEBUG ? Config.LOGD : Config.LOGV;
    private static final String STATE_CURRENT_VIEW = "state-current-view";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

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

        HistoryAdapter historyAdapter = new HistoryAdapter(this, mHistory, mLogic);
        mHistory.setObserver(historyAdapter);

        mPanelSwitcher = (PanelSwitcher) findViewById(R.id.panelswitch);
        if (mPanelSwitcher != null) {
            mPanelSwitcher.setCurrentIndex(state==null ? 0 : state.getInt(STATE_CURRENT_VIEW, 0));
            mPanelSwitcher.setListener(this);
        }

        mListener.setHandler(mLogic, mPanelSwitcher);
        mDisplay.setOnKeyListener(mListener);

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
        MenuItem item;

        item = menu.add(0, CMD_CLEAR_HISTORY, 0, R.string.clear_history);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item.setIcon(R.drawable.clear_history);

        item = menu.add(0, CMD_ADVANCED_PANEL, 0, R.string.advanced);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item.setIcon(R.drawable.advanced);

        item = menu.add(0, CMD_BASIC_PANEL, 0, R.string.basic);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        item.setIcon(R.drawable.simple);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(CMD_BASIC_PANEL).setVisible(mPanelSwitcher != null &&
                          mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL);

        menu.findItem(CMD_ADVANCED_PANEL).setVisible(mPanelSwitcher != null &&
                          mPanelSwitcher.getCurrentIndex() == BASIC_PANEL);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case CMD_CLEAR_HISTORY:
            mHistory.clear();
            break;

        case CMD_BASIC_PANEL:
            if (mPanelSwitcher != null &&
                mPanelSwitcher.getCurrentIndex() == ADVANCED_PANEL) {
                mPanelSwitcher.moveRight();
            }
            break;

        case CMD_ADVANCED_PANEL:
            if (mPanelSwitcher != null &&
                mPanelSwitcher.getCurrentIndex() == BASIC_PANEL) {
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
