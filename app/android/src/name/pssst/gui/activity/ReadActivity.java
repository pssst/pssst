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
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import name.pssst.api.Message;
import name.pssst.api.Pssst;
import name.pssst.app.R;
import name.pssst.gui.AppTask;
import name.pssst.gui.App;

import java.util.ArrayList;

/**
 * Read activity.
 * @author Christian & Christian
 */
public final class ReadActivity extends Activity {
    private static final int DELAY_FOREGROUND = 3000;  // Milliseconds
    private static final int DELAY_BACKGROUND = 60000; // Milliseconds

//    private ArrayList<String> boxes = new ArrayList<String>();
    private String box = "box";
    private int delay = DELAY_FOREGROUND;


    private MessageAdapter adapter;

    /**
     * Sets up activity.
     * @param savedInstanceState state
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getResources().getString(R.string.app_name));
        setContentView(R.layout.read);

//        new ListTask(this).execute((Void)null);

        adapter = new MessageAdapter(new ArrayList<Message>(), this);

        ((ListView) findViewById(R.id.box)).setAdapter(adapter);

        final Handler pull = new Handler();
        pull.postDelayed(new Runnable() {
            @Override
            public void run() {
                pullMessage(box);
                pull.postDelayed(this, delay);
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
        getMenuInflater().inflate(R.menu.read, menu);

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
            case R.id.write:
                startActivity(new Intent(this, WriteActivity.class));

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

//    /**
//     * Background list task.
//     * @author Christian & Christian
//     */
//    private final class ListTask extends AppTask<Void, ArrayList<String>> {
//        /**
//         * Saves activity context.
//         * @param context context
//         */
//        public ListTask(Context context) {
//            super(context);
//        }
//
//        /**
//         * List all boxes.
//         * @param v void
//         */
//        protected final ArrayList<String> doExecute(Void... v) throws Exception {
//            Pssst pssst = ((App) getApplication()).getPssst();
//
//            return pssst.list();
//        }
//
//        /**
//         * Sets the box list.
//         * @param error error
//         * @param list all boxes
//         */
//        @Override
//        protected final void onFinished(boolean error, ArrayList<String> list) {
//            if (!error) {
//                boxes = list;
//            }
//        }
//    }
//
    /**
     * Background pull task.
     * @author Christian & Christian
     */
    private final class PullTask extends AppTask<String, ArrayList<Message>> {
        private String box;

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
            box = param[0];

            ArrayList<Message> messages = new ArrayList<Message>();

            Pssst pssst = ((App) getApplication()).getPssst();

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
                    .setContentTitle("New Message")
                    .setContentText(messages.get(0).getName().getOfficial())
                    .setSmallIcon(R.drawable.ic_stat_pssst_small)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .build();

                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(0, notification);
            }
        }
    }

    /**
     * Pulls a new message.
     * @param box box
     */
    private void pullMessage(String box) {
        new PullTask(this).execute(box);
    }
}
