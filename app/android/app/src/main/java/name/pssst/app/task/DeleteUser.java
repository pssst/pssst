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
import android.widget.Toast;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;

/**
 * Delete user task.
 */
public class DeleteUser extends AsyncTask<Pssst, Void, Boolean> {
    private final Activity mActivity;
    private final Callback mCallback;
    private final String mUsername;

    private ProgressDialog mProgress;
    private String mResult = null;

    /**
     * Initializes the task.
     * @param activity Activity
     * @param callback Callback
     * @param username Username
     */
    public DeleteUser(Activity activity, Callback callback, String username) {
        mActivity = activity;
        mCallback = callback;
        mUsername = username;
    }

    /**
     * Shows a progress dialog.
     */
    @Override
    protected void onPreExecute() {
        mProgress = ProgressDialog.show(mActivity, null, String.format("Deleting %s", mUsername), true);
    }

    /**
     * Deletes the user.
     * @param pssst Pssst instance
     * @return Success
     */
    @Override
    protected Boolean doInBackground(Pssst... pssst) {
        try {
            pssst[0].delete();
            return true;
        } catch (PssstException e) {
            mResult = e.getMessage();
            return false;
        }
    }

    /**
     * Logs out the deleted user.
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
