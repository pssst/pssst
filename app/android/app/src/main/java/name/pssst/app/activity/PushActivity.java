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

package name.pssst.app.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.app.App;
import name.pssst.app.R;

import static android.R.layout.simple_list_item_1;
import static name.pssst.app.R.layout.activity_push;

/**
 * Send message activity.
 */
public class PushActivity extends Activity {

    /**
     * Initializes the activity.
     * @param savedInstanceState Saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_push);

        final Pssst pssst = ((App) getApplication()).getPssstInstance();
        final Bundle extras = getIntent().getExtras();

        //noinspection ConstantConditions
        getActionBar().setTitle(pssst.getUsername());
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setIcon(R.mipmap.ic_launcher);

        try {
            final AutoCompleteTextView receiver = (AutoCompleteTextView) findViewById(R.id.receiver);
            receiver.setAdapter(new ArrayAdapter<>(this, simple_list_item_1, pssst.getCachedReceivers()));

            // Preselect receiver
            if (extras != null) {
                receiver.setText(extras.getString("receiver"));
                findViewById(R.id.message).requestFocus();
            }
        } catch (PssstException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        final Button send = (Button) findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isInputValid()) {
                    new PushTask().execute(pssst);
                }
            }
        });
    }

    /**
     * Returns if the user input is valid.
     * @return Validity
     */
    private boolean isInputValid() {
        final String receiver = ((EditText) findViewById(R.id.receiver)).getText().toString();
        if (receiver == null || receiver.isEmpty()) {
            return false;
        }

        final String message = ((EditText) findViewById(R.id.message)).getText().toString();
        if (message == null || message.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Push task.
     */
    private class PushTask extends AsyncTask<Pssst, Void, Boolean> {
        private ProgressDialog mProgress;
        private String mResult = null;

        @Override
        protected void onPreExecute() {
            mProgress = ProgressDialog.show(PushActivity.this, null, "Sending message", true);
        }

        @Override
        protected Boolean doInBackground(Pssst... pssst) {
            final String receiver = ((EditText) findViewById(R.id.receiver)).getText().toString();
            final String message = ((EditText) findViewById(R.id.message)).getText().toString();

            try {
                pssst[0].push(receiver, message);
                return true;
            } catch (PssstException e) {
                mResult = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mProgress.cancel();

            if (mResult != null) {
                Toast.makeText(getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
            }

            if (success) {
                finish();
            }
        }
    }
}
