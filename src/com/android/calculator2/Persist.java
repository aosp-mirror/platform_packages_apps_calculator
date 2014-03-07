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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class Persist {
    private static final int LAST_VERSION = 2;
    private static final String FILE_NAME = "calculator.data";

    private Context mContext;
    private History mHistory;
    private int mDeleteMode;

    public Persist(Context context) {
        mContext = context;
    }

    public History getHistory() {
        return mHistory;
    }

    public void setDeleteMode(int mode) {
        mDeleteMode = mode;
    }

    public int getDeleteMode() {
        return mDeleteMode;
    }

    public void load() {
        try {
            final DataInputStream in = new DataInputStream(
                    new BufferedInputStream(mContext.openFileInput(FILE_NAME), 8192));
            final int version = in.readInt();
            if (version > 1) {
                mDeleteMode = in.readInt();
            } else if (version > LAST_VERSION) {
                throw new IOException("data version " + version + "; expected " + LAST_VERSION);
            }

            mHistory = new History(version, in);

            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mHistory == null) {
            mHistory = new History();
        }
    }

    public void save() {
        if (mHistory == null) {
            return;
        }

        try {
            final DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(mContext.openFileOutput(FILE_NAME, 0), 8192));
            out.writeInt(LAST_VERSION);
            out.writeInt(mDeleteMode);

            mHistory.write(out);

            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
