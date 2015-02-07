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
import android.content.Intent;
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
import static name.pssst.app.R.layout.activity_start;

/**
 * Start activity.
 */
public class StartActivity extends Activity {
    private App mApp;

    /**
     * Initializes the activity and fast forward if user is already logged in.
     * @param savedInstanceState Saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_start);

        final String name = getResources().getString(R.string.app_name);

        //noinspection ConstantConditions
        getActionBar().setTitle(String.format("%s %s", name, Pssst.getVersion()));

        mApp = (App) getApplication();

        if (mApp.getPssstInstance() != null) {
            final Intent intent = new Intent(this, ListActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }

        try {
            final AutoCompleteTextView username = (AutoCompleteTextView) findViewById(R.id.username);
            username.setAdapter(new ArrayAdapter<>(this, simple_list_item_1, Pssst.getUsernames()));
        } catch (PssstException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        final Button create = (Button) findViewById(R.id.create);
        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new LoginTask().execute(Boolean.TRUE);
            }
        });

        final Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                new LoginTask().execute(Boolean.FALSE);
            }
        });
    }

    /**
     * Login task.
     */
    private class LoginTask extends AsyncTask<Boolean, Void, Pssst> {
        private ProgressDialog mProgress;
        private String mResult = null;

        @Override
        protected void onPreExecute() {
            mProgress = ProgressDialog.show(StartActivity.this, null, "Login...", true);
        }

        @Override
        protected Pssst doInBackground(Boolean... create) {
            final String username = ((EditText) findViewById(R.id.username)).getText().toString();
            if (username == null || username.isEmpty()) {
                return null;
            }

            final String password = ((EditText) findViewById(R.id.password)).getText().toString();
            if (password == null || password.isEmpty()) {
                return null;
            }

            try {
                final Pssst pssst = new Pssst(username, password);

                if (create[0]) {
                    pssst.create();
                }

                return pssst;
            } catch (PssstException e) {
                mResult = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Pssst pssst) {
            mProgress.cancel();

            if (mResult != null) {
                Toast.makeText(getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
            }

            if (pssst != null) {
                mApp.setPssstInstance(pssst);
                startActivity(new Intent(StartActivity.this, ListActivity.class));
            }
        }
    }
}
