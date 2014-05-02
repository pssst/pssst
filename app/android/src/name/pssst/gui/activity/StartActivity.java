// Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.
package name.pssst.gui.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import name.pssst.api.Name;
import name.pssst.api.Pssst;
import name.pssst.app.R;
import name.pssst.gui.AppTask;
import name.pssst.gui.App;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Start activity.
 * @author Christian & Christian
 */
public final class StartActivity extends Activity implements View.OnClickListener {
    private static final int SETTINGS = 1;
    private ArrayList<String> users = new ArrayList<String>();

    /**
     * Sets up activity and default values.
     * @param savedInstanceState state
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getResources().getString(R.string.app_name));
        setContentView(R.layout.start);

        loadPreferences();

        // User and file names use the same format
        Collections.addAll(users, Pssst.getUserDirectory().list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().startsWith("pssst");
            }
        }));

        AutoCompleteTextView usernameView = (AutoCompleteTextView) findViewById(R.id.username);
        usernameView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, users));
        usernameView.setThreshold(1);

        findViewById(R.id.start).setOnClickListener(this);
    }

    /**
     * Sets up action bar menu.
     * @param menu menu
     * @return true
     */
    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.start, menu);

        return true;
    }

    /**
     * Executes menu actions.
     * @param item item
     * @return result
     */
    @Override
    public final boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.info:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle(getResources().getString(R.string.app_name));
                alert.setMessage("Version " + Pssst.VERSION);
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.cancel();
                    }
                });
                alert.show();
                return true;

            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS);

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Executes start.
     * @param v view
     */
    public final void onClick(View v) {
        TextView usernameView = (TextView) findViewById(R.id.username);
        TextView passwordView = (TextView) findViewById(R.id.password);

        String username = usernameView.getText().toString();
        String password = passwordView.getText().toString();

        if (!username.isEmpty() && !password.isEmpty()) {
            new CreateTask(this).execute(username, password);
        }
    }

    /**
     * Saves shared preferences.
     * @param requestCode request code
     * @param resultCode result code
     * @param data data
     */
    @Override
    protected final void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SETTINGS:
                SharedPreferences config = PreferenceManager.getDefaultSharedPreferences(this);

                Pssst.setApiAddress(config.getString("API_ADDRESS", Pssst.API));
                Pssst.setUserDirectory(new File(config.getString("USER_DIRECTORY", ".")));
                break;
        }
    }

    /**
     * Background create task.
     * @author Christian & Christian
     */
    private final class CreateTask extends AppTask<String, Pssst> {
        /**
         * Saves activity context.
         * @param context context
         */
        public CreateTask(Context context) {
            super(context);
        }

        /**
         * Disables user input.
         */
        @Override
        public final void onExecute() {
            findViewById(R.id.username).setEnabled(false);
            findViewById(R.id.password).setEnabled(false);
            findViewById(R.id.start).setEnabled(false);
        }

        /**
         * Sets the overall Pssst instance.
         * @param param parameter
         */
        public final Pssst doExecute(String... param) throws Exception {
            String username = param[0];
            String password = param[1];

            Pssst pssst = Pssst.newInstance(username, password);

            // Create new user
            if (!users.contains(new Name(username).getOfficial())) {
                pssst.create();
            }

            return pssst;
        }

        /**
         * Switches to read activity.
         * @param error error error
         * @param pssst pssst instance
         */
        @Override
        public final void onFinished(boolean error, Pssst pssst) {
            if (error) {
                findViewById(R.id.username).setEnabled(true);
                findViewById(R.id.password).setEnabled(true);
                findViewById(R.id.start).setEnabled(true);
            } else {
                ((App) getApplication()).setPssst(pssst);
                startActivity(new Intent(context, ReadActivity.class));
                finish();
            }
        }
    }

    /**
     * Loads the overall Pssst settings.
     */
    private void loadPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Pssst.setApiAddress(preferences.getString("API_ADDRESS", ""));
        Pssst.setUserDirectory(new File(preferences.getString("USER_DIRECTORY", "")));
    }
}
