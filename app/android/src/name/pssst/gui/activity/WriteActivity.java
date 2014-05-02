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
import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import name.pssst.api.Message;
import name.pssst.api.Pssst;
import name.pssst.app.R;
import name.pssst.gui.AppTask;
import name.pssst.gui.App;

import java.util.ArrayList;

/**
 * Write activity.
 * @author Christian & Christian
 */
public final class WriteActivity extends Activity implements View.OnClickListener {
    /**
     * Sets up activity.
     * @param savedInstanceState state
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App app = (App) getApplication();

        setTitle(getResources().getString(R.string.app_name));
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.write);

        ArrayList<String> users = app.getPssst().getUsers();

        AutoCompleteTextView nameView = (AutoCompleteTextView) findViewById(R.id.user);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, users);
        nameView.setAdapter(adapter);
        nameView.setThreshold(1);

        findViewById(R.id.send).setOnClickListener(this);
    }

    /**
     * Executes menu actions.
     * @param item item
     * @return result
     */
    @Override
    public final boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Executes send.
     * @param v view
     */
    public final void onClick(View v) {
        TextView userView = (TextView) findViewById(R.id.user);
        TextView textView = (TextView) findViewById(R.id.text);

        String user = userView.getText().toString();
        String text = textView.getText().toString();

        if (!user.isEmpty() && !text.isEmpty()) {
            new PushTask(this).execute(user, text);
        }
    }

    /**
     * Background push task.
     * @author Christian & Christian
     */
    private final class PushTask extends AppTask<String, Void> {
        /**
         * Saves activity context.
         * @param context context
         */
        public PushTask(Context context) {
            super(context);
        }

        /**
         * Disables user input.
         */
        @Override
        protected final void onExecute() {
            findViewById(R.id.user).setEnabled(false);
            findViewById(R.id.text).setEnabled(false);
            findViewById(R.id.send).setEnabled(false);
        }

        /**
         * Pushes a new message.
         * @param param parameter
         */
        protected final Void doExecute(String... param) throws Exception {
            String user = param[0];
            String text = param[1];

            Pssst pssst = ((App) getApplication()).getPssst();

            pssst.push(new Message(user, text));

            return null;
        }

        /**
         * Switches to read activity.
         * @param error error
         * @param v void
         */
        @Override
        protected final void onFinished(boolean error, Void v) {
            if (error) {
                findViewById(R.id.user).setEnabled(true);
                findViewById(R.id.text).setEnabled(true);
                findViewById(R.id.send).setEnabled(true);
            } else {
                finish();
            }
        }
    }
}
