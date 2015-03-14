/*
 * Copyright (C) 2013-2015  Christian & Christian  <hello@pssst.name>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package name.pssst.app.task;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.ArrayList;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.api.entity.Message;
import name.pssst.app.App;

/**
 * Pull task.
 */
public class Pull extends AsyncTask<Pssst, Void, ArrayList<Message>> {
    private final Activity mActivity;
    private final Callback mCallback;
    private final String mBox;

    private String mResult = null;

    /**
     * Initializes the task.
     * @param activity Activity
     * @param callback Callback
     * @param box Box
     */
    public Pull(Activity activity, Callback callback, String box) {
        mActivity = activity;
        mCallback = callback;
        mBox = box;
    }

    /**
     * Pulls all new messages from a box.
     * @param pssst Pssst instance
     * @return Messages
     */
    @Override
    protected ArrayList<Message> doInBackground(Pssst... pssst) {
        final ArrayList<Message> messages = new ArrayList<>();

        try {
            while (true) {
                final Message message = pssst[0].pull(mBox);

                if (message != null) {
                    messages.add(message);
                } else {
                    break;
                }
            }
        } catch (PssstException e) {
            mResult = e.getMessage();
        }

        return messages;
    }

    /**
     * Adds the new messages.
     * @param messages Messages
     */
    @Override
    protected void onPostExecute(ArrayList<Message> messages) {
        if (mResult != null && ((App) mActivity.getApplication()).getIsVisible()) {
            Toast.makeText(mActivity.getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
        }

        if (!messages.isEmpty()) {
            mCallback.execute(messages);
        }
    }
}
