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
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.api.entity.Message;
import name.pssst.api.entity.Name;
import name.pssst.app.App;
import name.pssst.app.R;
import name.pssst.app.task.Callback;
import name.pssst.app.task.DeleteUser;

import static name.pssst.app.R.layout.activity_pull;
import static name.pssst.app.R.layout.fragment_message;

/**
 * List messages activity.
 */
public class Pull extends Activity {
    private String mBox;

    private App mApp;
    private Pssst mPssst;
    private Handler mHandler;
    private MessageAdapter mAdapter;

    private int intervalActive;
    private int intervalPaused;

    private NotificationManager mNotificationManager;
    private ConnectivityManager mConnectivityManager;

    /**
     * Initializes the activity.
     * @param savedInstanceState Saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(activity_pull);

        mApp = (App) getApplication();
        mBox = mApp.getPssstBox();
        mPssst = mApp.getPssstInstance();
        mHandler = new Handler();
        mAdapter = new MessageAdapter(this, mApp.getPssstMessages(mBox));
        mAdapter.registerDataSetObserver(new MessageObserver());

        final ActionBar actionbar = getActionBar();
        //noinspection ConstantConditions
        actionbar.setTitle(mPssst.getUsername());
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setIcon(R.mipmap.ic_actionbar);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        final ListView messages = (ListView) findViewById(R.id.messages);
        messages.setAdapter(mAdapter);
        messages.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                final Message message = (Message) messages.getItemAtPosition(position);
                final Intent intent = new Intent(Pull.this, Push.class);

                try {
                    intent.putExtra("receiver", message.getUsername());
                } catch (PssstException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }

                startActivity(intent);
            }
        });

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        intervalActive = Integer.parseInt(preferences.getString("APP_PULL_INTERVAL_ACTIVE", "2"));
        intervalPaused = Integer.parseInt(preferences.getString("APP_PULL_INTERVAL_PAUSED", "30"));
        mBox = preferences.getString("APP_DEFAULT_BOX", Pssst.getDefaultBox());

        if (!mBox.equals(Pssst.getDefaultBox())) {
            try {
                getActionBar().setTitle(new Name(mPssst.getUsername(), mBox).toString());
            } catch (PssstException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        setPullHandler(intervalActive);
    }

    /**
     * Creates the options menu.
     * @param menu Menu
     * @return True
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pull, menu);
        return true;
    }

    /**
     * Stops the message pull handler.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(null);
    }

    /**
     * Sets the paused pull interval.
     */
    @Override
    public void onPause() {
        super.onPause();
        mApp.setIsVisible(false);
        setPullHandler(intervalPaused);
    }

    /**
     * Sets the active pull interval.
     */
    @Override
    public void onResume() {
        super.onResume();
        mApp.setIsVisible(true);
        setPullHandler(intervalActive);
    }

    /**
     * Logs out the current user.
     */
    @Override
    public void onBackPressed() {
        confirmLogout();
    }

    /**
     * Handles the selected menu option.
     * @param item Menu item
     * @return Result
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_write:
                startActivity(new Intent(this, Push.class));
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
     * Sets the pull handler.
     * @param interval Interval
     */
    private void setPullHandler(long interval) {
        final long delay = interval * 1000; // Milliseconds

        mHandler.removeCallbacks(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mPssst != null) {
                    pullMessages();
                    mHandler.postDelayed(this, delay);
                }
            }
        }, delay);
    }

    /**
     * Pulls new messages if a network is connected.
     */
    private void pullMessages() {
        NetworkInfo network = mConnectivityManager.getActiveNetworkInfo();

        if (network != null && network.isConnectedOrConnecting()) {
            new name.pssst.app.task.Pull(Pull.this, new Callback() {
                @Override
                public void execute(Object param) {
                    for (Message message: ((ArrayList<Message>) param)) {
                        mAdapter.add(message);
                    }
                }
            }, mBox).execute(mPssst);
        }
    }

    /**
     * Confirm the deletion of the current user.
     */
    private void confirmDeleteUser() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setMessage(getResources().getString(R.string.alert_delete_user))
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteUser(Pull.this, new Callback() {
                            @Override
                            public void execute(Object param) {
                                logout();
                            }
                        }, mPssst.getUsername()).execute(mPssst);
                    }
                })
                .show();
    }

    /**
     * Confirm the logout of the current user.
     */
    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.app_name))
                .setMessage(getResources().getString(R.string.alert_logout))
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        logout();
                    }
                })
                .show();
    }

    /**
     * Logs out the user and closes the application.
     */
    private void logout() {
        mNotificationManager.cancelAll();
        mApp.clearPssstData();
        mPssst = null;

        finishAffinity();
    }

    /**
     * Message adapter.
     */
    private class MessageAdapter extends BaseAdapter {
        private final Context mContext;
        private final ArrayList<Message> mMessages;

        /**
         * Initializes the adapter.
         * @param context Context
         * @param messages Messages
         */
        public MessageAdapter(Context context, ArrayList<Message> messages) {
            mContext = context;
            mMessages = messages;
        }

        /**
         * Returns the message count.
         * @return Count
         */
        @Override
        public int getCount() {
            return mMessages.size();
        }

        /**
         * Returns the message.
         * @param position Position
         * @return Message
         */
        @Override
        public Object getItem(int position) {
            return mMessages.get(position);
        }

        /**
         * Returns the message id.
         * @param position Position
         * @return Id
         */
        @Override
        public long getItemId(int position) {
            return position;
        }

        /**
         * Returns the message view.
         * @param position Position
         * @param convertView View
         * @param parent Parent
         * @return View
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Message message = mMessages.get(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(fragment_message, null);
            }

            final TextView data = (TextView) convertView.findViewById(R.id.data);
            final TextView user = (TextView) convertView.findViewById(R.id.user);
            final TextView time = (TextView) convertView.findViewById(R.id.time);

            final SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.yy HH:mm");
            format.setTimeZone(Calendar.getInstance().getTimeZone());

            try {
                data.setText(message.getMessage());
                user.setText(message.getUsername());
                time.setText(format.format(message.getTimestamp()));
            } catch (PssstException e) {
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }

            return convertView;
        }

        /**
         * Adds a new message.
         * @param message Message
         */
        public void add(Message message) {
            mMessages.add(message);
            notifyDataSetChanged();
        }

        /**
         * Returns the last message or null.
         * @return Message
         */
        public Message getLast() {
            if (!mMessages.isEmpty()) {
                return mMessages.get(mMessages.size() - 1);
            } else {
                return null;
            }
        }
    }

    /**
     * Message observer.
     */
    private class MessageObserver extends DataSetObserver {

        /**
         * Sends a notification if activity is not visible.
         */
        @Override
        public void onChanged() {
            final Message message = mAdapter.getLast();

            if (!mApp.getIsVisible()) {
                try {
                    final Intent intent = new Intent(Pull.this, Login.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                    final Notification newMessage = new Notification.Builder(Pull.this)
                            .setContentIntent(PendingIntent.getActivity(Pull.this, 0, intent, 0))
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
         * Clears all notifications.
         */
        @Override
        public void onInvalidated() {
            mNotificationManager.cancelAll();
        }
    }
}
