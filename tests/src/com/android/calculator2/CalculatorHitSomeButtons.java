/**
 * Copyright (c) 2008, Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.android.calculator2;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.ActivityInstrumentationTestCase;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Rect;
import android.test.TouchUtils;

import com.android.calculator2.Calculator;
import com.android.calculator2.R;
import com.android.calculator2.CalculatorDisplay;

/**
 * Instrumentation tests for poking some buttons
 *
 */

public class CalculatorHitSomeButtons extends ActivityInstrumentationTestCase <Calculator>{
    public boolean setup = false;
    private static final String TAG = "CalculatorTests";
    Calculator mActivity = null;
    Instrumentation mInst = null;
    
    public CalculatorHitSomeButtons() {
        super("com.android.calculator2", Calculator.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        mActivity = getActivity();
        mInst = getInstrumentation();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    @LargeTest
    public void testPressSomeKeys() {
        Log.v(TAG, "Pressing some keys!");
        
        // Make sure that we clear the output
        press(KeyEvent.KEYCODE_ENTER);
        press(KeyEvent.KEYCODE_CLEAR);
        
        // 3 + 4 * 5 => 23
        press(KeyEvent.KEYCODE_3);
        press(KeyEvent.KEYCODE_PLUS);
        press(KeyEvent.KEYCODE_4);
        press(KeyEvent.KEYCODE_9 | KeyEvent.META_SHIFT_ON);
        press(KeyEvent.KEYCODE_5);
        press(KeyEvent.KEYCODE_ENTER);
        
        assertEquals(displayVal(), "23");
    }
    
    @LargeTest
    public void testTapSomeButtons() {
        Log.v(TAG, "Tapping some buttons!");
        
        // Make sure that we clear the output
        tap(R.id.equal);
        tap(R.id.del);
        
        // 567 / 3 => 189
        tap(R.id.digit5);
        tap(R.id.digit6);
        tap(R.id.digit7);
        tap(R.id.div);
        tap(R.id.digit3);
        tap(R.id.equal);
        
        assertEquals(displayVal(), "189");
        
        // make sure we can continue calculations also
        // 189 - 789 => -600
        tap(R.id.minus);
        tap(R.id.digit7);
        tap(R.id.digit8);
        tap(R.id.digit9);
        tap(R.id.equal);
        
        // Careful: the first digit in the expected value is \u2212, not "-" (a hyphen)
        assertEquals(displayVal(), mActivity.getString(R.string.minus) + "600");
    }
  
    // helper functions
    private void press(int keycode) {
        mInst.sendKeyDownUpSync(keycode);
    }
    
    private boolean tap(int id) {
        View view = mActivity.findViewById(id);
        if(view != null) {
            TouchUtils.clickView(this, view);
            return true;
        }
        return false;
    }
  
    private String displayVal() {
        CalculatorDisplay display = (CalculatorDisplay) mActivity.findViewById(R.id.display);
        assertNotNull(display);
        
        EditText box = (EditText) display.getCurrentView();
        assertNotNull(box);
        
        return box.getText().toString();
    }
}

