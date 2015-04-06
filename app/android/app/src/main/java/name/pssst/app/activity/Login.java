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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import name.pssst.api.Pssst;
import name.pssst.app.App;
import name.pssst.app.R;
import name.pssst.app.task.Callback;
import name.pssst.app.task.CreateUser;

import static name.pssst.app.R.layout.activity_login;

/**
 * Start activity.
 */
public class Login extends Activity {
    private static final int SETTINGS = 1;

    /**
     * Initializes the activity and fast forward if user is already logged in.
     * @param savedInstanceState Saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_login);

        final ActionBar actionbar = getActionBar();
        //noinspection ConstantConditions
        actionbar.setTitle(getResources().getString(R.string.app_name));
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setIcon(R.mipmap.ic_actionbar);

        final App app = (App) getApplication();

        // Forward to next activity
        if (app.getPssstInstance() != null) {
            final Intent intent = new Intent(this, Pull.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }

        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        final Callback callback = new Callback() {
            @Override
            public void execute(Object param) {
                app.setPssstInstance((Pssst) param);
                startActivity(new Intent(Login.this, Pull.class));
                finish();
            }
        };

        final Button create = (Button) findViewById(R.id.create);
        create.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isInputValid()) {
                    new CreateUser(Login.this, callback, CreateUser.Mode.CREATE).execute();
                }
            }
        });

        final Button login = (Button) findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isInputValid()) {
                    new CreateUser(Login.this, callback, CreateUser.Mode.LOGIN).execute();
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
                startActivityForResult(new Intent(this, Settings.class), SETTINGS);
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
}
