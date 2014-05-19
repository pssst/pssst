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

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import name.pssst.api.Message;
import name.pssst.api.Pssst;
import name.pssst.app.R;
import name.pssst.gui.internal.TaskBase;
import name.pssst.gui.App;

import java.util.ArrayList;

/**
 * Pull activity.
 * @author Christian & Christian
 */
public final class Pull extends Activity {
    private static final int DELAY_FOREGROUND = 3000;  // Milliseconds
    private static final int DELAY_BACKGROUND = 60000; // Milliseconds

    private int delay = DELAY_FOREGROUND;
    private String box = "box";

    private MessageAdapter adapter;

    /**
     * Sets up activity.
     * @param savedInstanceState state
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getResources().getString(R.string.app_name));
        setContentView(R.layout.pull);

        adapter = new MessageAdapter(new ArrayList<Message>(), this);

        ((ListView) findViewById(R.id.box)).setAdapter(adapter);

        setDefaultBox();

        final Context context = this;
        final Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new PullTask(context).execute(box);
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    /**
     * Sets up action bar menu.
     * @param menu menu
     * @return true
     */
    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pull, menu);

        return true;
    }

    /**
     * Sets foreground delay.
     */
    @Override
    public final void onStart() {
        super.onStart();

        delay = DELAY_FOREGROUND;
    }

    /**
     * Sets background delay.
     */
    @Override
    public final void onStop() {
        super.onStop();

        delay = DELAY_BACKGROUND;
    }

    /**
     * Executes menu actions.
     * @param item item
     * @return result
     */
    @Override
    public final boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.push:
                startActivity(new Intent(this, Push.class));

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Message adapter.
     * @author Christian & Christian
     */
    private final class MessageAdapter extends BaseAdapter {
        private ArrayList<Message> messages;
        private Context context;

        /**
         * Sets up adapter.
         * @param messages messages
         * @param context context
         */
        public MessageAdapter(ArrayList<Message> messages, Context context) {
            this.messages = messages;
            this.context = context;
        }

        /**
         * Adds new messages.
         * @param messages messages
         */
        public final void addAll(ArrayList<Message> messages) {
            this.messages.addAll(messages);
            notifyDataSetChanged();
        }

        /**
         * Gets message count.
         * @return count
         */
        public final int getCount() {
            return messages.size();
        }

        /**
         * Gets the item by position.
         * @param position position
         * @return item
         */
        public final Object getItem(int position) {
            return messages.get(position);
        }

        /**
         * Gets the item id.
         * @param position position
         * @return id
         */
        public final long getItemId(int position) {
            return position;
        }

        /**
         * Gets the message view by the message position.
         * @param position position
         * @param convertView view
         * @param parent parent
         * @return view
         */
        public final View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null)
            {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.message, null);
            }

            TextView textView = (TextView) v.findViewById(R.id.text);
            TextView nameView = (TextView) v.findViewById(R.id.name);
            TextView timeView = (TextView) v.findViewById(R.id.time);

            Message message = messages.get(position);

            String date = DateFormat.getDateFormat(context).format(message.getTime());
            String time = DateFormat.getTimeFormat(context).format(message.getTime());

            textView.setText(message.getText());
            nameView.setText(message.getName().getOfficial());
            timeView.setText(date + " " + time);

            return v;
        }
    }

    /**
     * Background pull task.
     * @author Christian & Christian
     */
    private final class PullTask extends TaskBase<String, ArrayList<Message>> {
        /**
         * Saves activity context.
         * @param context context
         */
        public PullTask(Context context) {
            super(context);
        }

        /**
         * Pulls new messages.
         * @param param parameter
         */
        protected final ArrayList<Message> doExecute(String... param) throws Exception {
            String box = param[0];

            Pssst pssst = ((App) getApplication()).getPssst();

            ArrayList<Message> messages = new ArrayList<Message>();
            Message message;
            do {
                message = pssst.pull(box);

                if (message != null) {
                    messages.add(message);
                }
            } while (message != null);

            return messages;
        }

        /**
         * Sets the new message.
         * @param error error
         * @param messages messages
         */
        @Override
        protected final void onFinished(boolean error, ArrayList<Message> messages) {
            if (!error && !messages.isEmpty()) {
                adapter.addAll(messages);

                Notification notification = new Notification.Builder(context)
                    .setContentTitle(getResources().getString(R.string.notice_message_new))
                    .setContentText(messages.get(0).getName().getOfficial())
                    .setSmallIcon(R.drawable.ic_stat_notify_msg)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .build();

                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(0, notification);
            }
        }
    }

    /**
     * Sets the default box from settings.
     */
    private void setDefaultBox() {
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        box = preferences.getString("DEFAULT_BOX", "");
    }
}
