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
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.api.entity.Message;

import static name.pssst.app.R.layout.activity_list;
import static name.pssst.app.R.layout.fragment_message;

/**
 * List messages activity.
 */
public class ListActivity extends Activity {
    private static final int PULL_DELAY_ACTIVE = 3000; // Milliseconds
    private static final int PULL_DELAY_PAUSED = 3000; // Milliseconds

    private final ArrayList<Message> mMessages = new ArrayList<>();

    private Pssst mPssst;
    private Handler mHandler;
    private MessageAdapter mAdapter;
    private int delay = PULL_DELAY_ACTIVE;

    private NotificationManager mNotificationManager;
    private ConnectivityManager mConnectivityManager;

    /**
     * Initializes the activity.
     * @param savedInstanceState Saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_list);

        mPssst = ((App) getApplication()).getPssstInstance();
        mAdapter = new MessageAdapter(this, mMessages);

        //noinspection ConstantConditions
        getActionBar().setTitle(mPssst.getUsername());

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        final ListView content = (ListView) findViewById(R.id.content);
        content.setAdapter(mAdapter);
        content.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                final Message message = (Message) content.getItemAtPosition(position);
                final Intent intent = new Intent(ListActivity.this, SendActivity.class);

                try {
                    intent.putExtra("receiver", message.getUsername());
                } catch (PssstException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }

                startActivity(intent);
            }
        });

        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                pullMessage();
                mHandler.postDelayed(this, delay);
            }
        }, delay);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onPause() {
        App.setIsVisible(false);
        delay = PULL_DELAY_PAUSED;
        super.onPause();
    }

    @Override
    public void onResume() {
        App.setIsVisible(true);
        delay = PULL_DELAY_ACTIVE;
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_write:
                startActivity(new Intent(this, SendActivity.class));
                return true;

            case R.id.action_delete_user:
                confirmDeleteUser();
                return true;

            case R.id.action_logout:
                logout();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Adds a new message to the list and notifies the system.
     * @param message Message
     */
    private void addMessage(Message message) {
        mAdapter.addMessage(message);

        if (!App.getIsVisible()) {
            try {
                final Intent intent = new Intent(this, StartActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                final Notification newMessage = new Notification.Builder(this)
                        .setContentIntent(PendingIntent.getActivity(this, 0, intent, 0))
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(String.format("New message from %s", message.getUsername()))
                        .setSmallIcon(R.drawable.ic_stat_app)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build();

                mNotificationManager.notify(0, newMessage);
            } catch (PssstException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Pulls a new message if a network is connected.
     */
    private void pullMessage() {
        NetworkInfo network = mConnectivityManager.getActiveNetworkInfo();

        if (network != null && network.isConnectedOrConnecting()) {
            new PullTask().execute(mPssst);
        }
    }

    /**
     * Confirm the deletion of the logged in user.
     */
    private void confirmDeleteUser() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setMessage(getResources().getString(R.string.alert_delete_user))
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteUserTask().execute(mPssst);
                    }
                })
                .show();
    }

    /**
     * Logs out the user and closes the application.
     */
    private void logout() {
        ((App) getApplication()).setPssstInstance(null);
        finish();
    }

    /**
     * Message adapter.
     */
    private class MessageAdapter extends BaseAdapter {
        private final Context mContext;
        private final ArrayList<Message> mMessages;

        public MessageAdapter(Context context, ArrayList<Message> messages) {
            mContext = context;
            mMessages = messages;
        }

        @Override
        public int getCount() {
            return mMessages.size();
        }

        @Override
        public Object getItem(int position) {
            return mMessages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Message message = mMessages.get(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(fragment_message, null);
            }

            final TextView head = (TextView) convertView.findViewById(R.id.head);
            final TextView body = (TextView) convertView.findViewById(R.id.body);

            final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            format.setTimeZone(Calendar.getInstance().getTimeZone());

            try {
                head.setText(String.format("%s, %s", message.getUsername(), format.format(message.getTimestamp())));
                body.setText(message.getText());
            } catch (PssstException | UnsupportedEncodingException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }

            return convertView;
        }

        public void addMessage(Message message) {
            mMessages.add(message);
            notifyDataSetChanged();
        }
    }

    /**
     * Delete user task.
     */
    private class DeleteUserTask extends AsyncTask<Pssst, Void, Boolean> {
        private ProgressDialog mProgress;
        private String mResult = null;

        @Override
        protected void onPreExecute() {
            mProgress = ProgressDialog.show(ListActivity.this, null, "Deleting...", true);
        }

        @Override
        protected Boolean doInBackground(Pssst... pssst) {
            try {
                pssst[0].delete();
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
                logout();
            }
        }
    }

    /**
     * Pull task.
     */
    private class PullTask extends AsyncTask<Pssst, Void, Message> {
        private String mResult = null;

        @Override
        protected Message doInBackground(Pssst... pssst) {
            try {
                return pssst[0].pull();
            } catch (PssstException e) {
                mResult = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Message message) {
            if (mResult != null) {
                Toast.makeText(getApplicationContext(), mResult, Toast.LENGTH_LONG).show();
            }

            if (message != null) {
                addMessage(message);
            }
        }
    }
}
