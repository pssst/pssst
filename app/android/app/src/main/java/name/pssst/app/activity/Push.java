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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import name.pssst.api.Pssst;
import name.pssst.app.App;
import name.pssst.app.R;
import name.pssst.app.task.Callback;

import static name.pssst.app.R.layout.activity_push;

/**
 * Send message activity.
 */
public class Push extends Activity {

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
        final ActionBar actionbar = getActionBar();
        //noinspection ConstantConditions
        actionbar.setTitle(pssst.getUsername());
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setIcon(R.mipmap.ic_actionbar);

        // Preselect receiver
        if (extras != null) {
            final EditText receiver = (EditText) findViewById(R.id.receiver);
            receiver.setText(extras.getString("receiver"));
            findViewById(R.id.message).requestFocus();
        }

        final Button send = (Button) findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (isInputValid()) {
                    new name.pssst.app.task.Push(Push.this, new Callback() {
                        @Override
                        public void execute(Object param) {
                            finish();
                        }
                    }).execute(pssst);
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
}
