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
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.EditText;
import android.widget.Toast;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.app.R;

/**
 * Push task.
 */
public class Push extends AsyncTask<Pssst, Void, Boolean> {
    private final Activity mActivity;
    private final Callback mCallback;

    private ProgressDialog mProgress;
    private String mResult = null;

    /**
     * Initializes the task.
     * @param activity Activity
     * @param callback Callback
     */
    public Push(Activity activity, Callback callback) {
        mActivity = activity;
        mCallback = callback;
    }

    /**
     * Shows a progress dialog.
     */
    @Override
    protected void onPreExecute() {
        mProgress = ProgressDialog.show(mActivity, null, "Sending message", true);
    }

    /**
     * Pushes a message to its receivers.
     * @param pssst Pssst instance
     * @return Success
     */
    @Override
    protected Boolean doInBackground(Pssst... pssst) {
        final String receiver = ((EditText) mActivity.findViewById(R.id.receiver)).getText().toString();
        final String message = ((EditText) mActivity.findViewById(R.id.message)).getText().toString();

        try {
            pssst[0].push(receiver.split("[,|;]"), message);
            return true;
        } catch (PssstException e) {
            mResult = e.getMessage();
            return false;
        }
    }

    /**
     * Finishes the push activity.
     * @param success Success
     */
    @Override
    protected void onPostExecute(Boolean success) {
        mProgress.cancel();

        if (mResult != null) {
            Toast.makeText(mActivity.getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
        }

        if (success) {
            mCallback.execute(null);
        }
    }
}
