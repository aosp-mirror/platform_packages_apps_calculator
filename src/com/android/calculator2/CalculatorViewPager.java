/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.android.calculator2.R;

public class CalculatorViewPager extends ViewPager {
    private Drawable mShadowRight;
    private int mShadowWidth;

    public CalculatorViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPageTransformer(false, mTransformer);

        final Resources res = context.getResources();
        mShadowRight = res.getDrawable(R.drawable.shadow_right);
        mShadowWidth = res.getDimensionPixelSize(R.dimen.pager_shadow_width);
    }

    /**
     * ViewPager inherits ViewGroup's default behavior of delayed clicks on its
     * children, but in order to make the calc buttons more responsive we
     * disable that here.
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        final boolean result = super.drawChild(canvas, child, drawingTime);
        mShadowRight.setBounds(child.getLeft() - mShadowWidth, child.getTop(), child.getLeft(),
                child.getBottom());
        mShadowRight.draw(canvas);
        return result;
    }

    private final PageTransformer mTransformer = new PageTransformer() {
        @Override
        public void transformPage(View v, float position) {
            final int pageWidth = v.getWidth();

            if (position < -1) {
                v.setAlpha(0);
            } else if (position <= 0) {
                // Pin the left page to the left side.
                v.setTranslationX(pageWidth * -position);
            } else if (position <= 1) {
                // Use the default slide transition when moving to the right
                // page
                v.setAlpha(1);
                v.setTranslationX(0);
            } else {
                v.setAlpha(0);
            }
        }
    };
}
