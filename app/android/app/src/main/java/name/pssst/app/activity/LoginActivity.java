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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Arrays;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.api.entity.Name;
import name.pssst.app.App;
import name.pssst.app.R;

import static android.R.layout.simple_list_item_1;
import static name.pssst.app.R.layout.activity_login;

/**
 * Start activity.
 */
public class LoginActivity extends Activity {
    private static final int SETTINGS = 1;
    private static enum Mode {
        CREATE, LOGIN
    }

    private App mApp;

    /**
     * Initializes the activity and fast forward if user is already logged in.
     * @param savedInstanceState Saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_login);

        //noinspection ConstantConditions
        getActionBar().setTitle(getResources().getString(R.string.app_name));
        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setIcon(R.mipmap.ic_launcher);

        mApp = (App) getApplication();

        if (mApp.getPssstInstance() != null) {
            final Intent intent = new Intent(this, PullActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        try {
            final AutoCompleteTextView username = (AutoCompleteTextView) findViewById(R.id.username);
            username.setAdapter(new ArrayAdapter<>(this, simple_list_item_1, Pssst.getUsernames()));
        } catch (PssstException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }

        final Button create = (Button) findViewById(R.id.create);
        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isInputValid()) {
                    new LoginTask(Mode.CREATE).execute();
                }
            }
        });

        final Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isInputValid()) {
                    new LoginTask(Mode.LOGIN).execute();
                }
            }
        });

        loadSettings();

        Pssst.setDirectory(getFilesDir().getAbsolutePath());
    }

    /**
     * Creates the options menu.
     * @param menu Menu
     * @return True
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    /**
     * Handles the selected menu option.
     * @param item Menu item
     * @return Result
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handles the activity result.
     * @param requestCode Request code
     * @param resultCode Result code
     * @param data Data
     */
    @Override
    protected final void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SETTINGS:
                loadSettings();
                break;
        }
    }

    /**
     * Loads the apps shared preferences.
     */
    private void loadSettings() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Pssst.setServer(preferences.getString("API_SERVER_ADDRESS", ""));
    }

    /**
     * Returns if the user input is valid.
     * @return Validity
     */
    private boolean isInputValid() {
        final String username = ((EditText) findViewById(R.id.username)).getText().toString();
        if (username == null || username.isEmpty()) {
            return false;
        }

        final String password = ((EditText) findViewById(R.id.password)).getText().toString();
        if (password == null || password.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Login task.
     */
    private class LoginTask extends AsyncTask<Void, Void, Pssst> {
        private final String mUsername;
        private final String mPassword;

        private ProgressDialog mProgress;
        private String mResult = null;
        private Mode mMode;

        public LoginTask(Mode mode) {
            mMode = mode;
            mUsername = ((EditText) findViewById(R.id.username)).getText().toString();
            mPassword = ((EditText) findViewById(R.id.password)).getText().toString();
        }

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

            mProgress = ProgressDialog.show(LoginActivity.this, null, title, true);
        }

        @Override
        protected Pssst doInBackground(Void... unused) {
            try {
                // Check user exists
                if (mMode == Mode.LOGIN) {
                    if (!Arrays.asList(Pssst.getUsernames()).contains(new Name(mUsername).toString())) {
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

        @Override
        protected void onPostExecute(Pssst pssst) {
            mProgress.cancel();

            if (mResult != null) {
                Toast.makeText(getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
            }

            if (pssst != null) {
                mApp.setPssstInstance(pssst);
                startActivity(new Intent(LoginActivity.this, PullActivity.class));
                finish();
            }
        }
    }
}
