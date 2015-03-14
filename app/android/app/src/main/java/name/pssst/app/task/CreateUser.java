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
import name.pssst.api.entity.Name;
import name.pssst.app.R;

/**
 * Create user task.
 */
public class CreateUser extends AsyncTask<Void, Void, Pssst> {
    public static enum Mode {
        CREATE, LOGIN
    }

    private final Activity mActivity;
    private final Callback mCallback;
    private final String mUsername;
    private final String mPassword;

    private ProgressDialog mProgress;
    private String mResult = null;
    private Mode mMode;

    /**
     * Initializes the task.
     * @param activity Activity
     * @param callback Callback
     * @param mode Mode
     */
    public CreateUser(Activity activity, Callback callback, Mode mode) {
        mMode = mode;
        mActivity = activity;
        mCallback = callback;
        mUsername = ((EditText) activity.findViewById(R.id.username)).getText().toString();
        mPassword = ((EditText) activity.findViewById(R.id.password)).getText().toString();
    }

    /**
     * Shows a mode specific progress dialog.
     */
    @Override
    protected void onPreExecute() {
        String title;
        String user;

        try {
            user = new Name(mUsername).toString();
        } catch (PssstException e) {
            user = e.getMessage();
        }

        switch (mMode) {
            case CREATE:
                title = String.format("Creating %s", user);
                break;

            case LOGIN:
                title = String.format("Logging in %s", user);
                break;

            default:
                title = "Mode unknown";
                break;
        }

        mProgress = ProgressDialog.show(mActivity, null, title, true);
    }

    /**
     * Creates a new Pssst instance.
     * @param unused Unused parameter
     * @return Pssst instance
     */
    @Override
    protected Pssst doInBackground(Void... unused) {
        try {
            // Check user exists
            if (mMode == Mode.LOGIN) {
                if (!Pssst.exists(mUsername)) {
                    throw new PssstException("User not found");
                }
            }

            final Pssst pssst = new Pssst(mUsername, mPassword);

            // Create new user
            if (mMode == Mode.CREATE) {
                pssst.create();
            }

            return pssst;
        } catch (PssstException e) {
            mResult = e.getMessage();
            return null;
        }
    }

    /**
     * Starts the pull activity.
     * @param pssst Pssst instance
     */
    @Override
    protected void onPostExecute(Pssst pssst) {
        mProgress.cancel();

        if (mResult != null) {
            Toast.makeText(mActivity.getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
        }

        if (pssst != null) {
            mCallback.execute(pssst);
        }
    }
}
