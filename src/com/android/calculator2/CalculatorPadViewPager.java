/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.calculator2;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class CalculatorPadViewPager extends ViewPager {

    private final PagerAdapter mStaticPagerAdapter = new PagerAdapter() {
        @Override
        public int getCount() {
            return getChildCount();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            return getChildAt(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            removeViewAt(position);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public float getPageWidth(int position) {
            return position == 1 ? 7.0f / 9.0f : 1.0f;
        }
    };

    private final OnPageChangeListener mOnPageChangeListener = new SimpleOnPageChangeListener() {
        private void recursivelySetEnabled(View view, boolean enabled) {
            if (view instanceof ViewGroup) {
                final ViewGroup viewGroup = (ViewGroup) view;
                for (int childIndex = 0; childIndex < viewGroup.getChildCount(); ++childIndex) {
                    recursivelySetEnabled(viewGroup.getChildAt(childIndex), enabled);
                }
            } else {
                view.setEnabled(enabled);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (getAdapter() == mStaticPagerAdapter) {
                for (int childIndex = 0; childIndex < getChildCount(); ++childIndex) {
                    // Only enable subviews of the current page.
                    recursivelySetEnabled(getChildAt(childIndex), childIndex == position);
                }
            }
        }
    };

    private final PageTransformer mPageTransformer = new PageTransformer() {
        @Override
        public void transformPage(View view, float position) {
            if (position < 0.0f) {
                // Pin the left page to the left side.
                view.setTranslationX(getWidth() * -position);
                view.setAlpha(Math.max(1.0f + position, 0.0f));
            } else {
                // Use the default slide transition when moving to the next page.
                view.setTranslationX(0.0f);
                view.setAlpha(1.0f);
            }
        }
    };

    public CalculatorPadViewPager(Context context) {
        this(context, null);
    }

    public CalculatorPadViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);

        setAdapter(mStaticPagerAdapter);
        setBackgroundColor(getResources().getColor(android.R.color.black));
        setOnPageChangeListener(mOnPageChangeListener);
        setPageMargin(getResources().getDimensionPixelSize(R.dimen.pad_page_margin));
        setPageTransformer(false, mPageTransformer);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Invalidate the adapter's data set since children may have been added during inflation.
        if (getAdapter() == mStaticPagerAdapter) {
            mStaticPagerAdapter.notifyDataSetChanged();
        }
    }
}
