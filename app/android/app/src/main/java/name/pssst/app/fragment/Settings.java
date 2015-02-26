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

package name.pssst.app.fragment;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import name.pssst.app.R;

/**
 * Settings fragment.
 */
public class Settings extends PreferenceFragment {
    private OnSharedPreferenceChangeListener mListener;

    /**
     * Initializes the fragment.
     * @param savedInstanceState Saved instance state
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        mListener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateDisplay(key);
            }
        };

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(mListener);

        updateDisplay("APP_DEFAULT_BOX");
        updateDisplay("APP_PULL_INTERVAL_ACTIVE");
        updateDisplay("APP_PULL_INTERVAL_PAUSED");
        updateDisplay("API_SERVER_ADDRESS");
    }

    /**
     * Update the display value.
     * @param key Preference key
     */
    private void updateDisplay(String key) {
        final Preference preference = findPreference(key);
        final String summary = ((EditTextPreference) preference).getText();

        // Special treatment to make preference summary more clear
        if (key.equals("APP_DEFAULT_BOX")) {
            preference.setSummary(String.format("Load %s on start up", summary));
        } else if (key.startsWith("APP_PULL_INTERVAL_")) {
            preference.setSummary(String.format("Check every %s seconds", summary));
        } else {
            preference.setSummary(summary);
        }
    }
}
