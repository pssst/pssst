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

package name.pssst.app;

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

import static android.R.layout.simple_list_item_1;
import static name.pssst.app.R.layout.activity_send;

/**
 * Send message activity.
 */
public class SendActivity extends Activity {

    /**
     * Initializes the activity.
     * @param savedInstanceState Saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_send);

        final Pssst pssst = ((App) getApplication()).getPssstInstance();
        final Bundle extras = getIntent().getExtras();

        //noinspection ConstantConditions
        getActionBar().setTitle(pssst.getUsername());

        try {
            final AutoCompleteTextView receiver = (AutoCompleteTextView) findViewById(R.id.receiver);
            receiver.setAdapter(new ArrayAdapter<>(this, simple_list_item_1, pssst.getReceivers()));

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
                new PushTask().execute(pssst);
            }
        });
    }

    /**
     * Push task.
     */
    private class PushTask extends AsyncTask<Pssst, Void, Boolean> {
        private ProgressDialog mProgress;
        private String mResult = null;

        @Override
        protected void onPreExecute() {
            mProgress = ProgressDialog.show(SendActivity.this, null, "Sending...", true);
        }

        @Override
        protected Boolean doInBackground(Pssst... pssst) {
            final String receiver = ((EditText) findViewById(R.id.receiver)).getText().toString();
            if (receiver == null || receiver.isEmpty()) {
                return false;
            }

            final String message = ((EditText) findViewById(R.id.message)).getText().toString();
            if (message == null || message.isEmpty()) {
                return false;
            }

            try {
                pssst[0].push(new String[] { receiver }, message);
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
