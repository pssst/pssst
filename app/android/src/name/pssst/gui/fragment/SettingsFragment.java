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
package name.pssst.gui.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import name.pssst.app.R;

/**
 * Settings fragment.
 * @author Christian & Christian
 */
public final class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    /**
     * Sets up fragment.
     * @param savedInstanceState state
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        updateSummary("DEFAULT_BOX");
        updateSummary("API_ADDRESS");
        updateSummary("USER_DIRECTORY");

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Updates preference summary.
     * @param sharedPreferences shared preferences
     * @param key key
     */
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        updateSummary(key);
    }

    /**
     * Updates the preference summary.
     * @param key key
     */
    private void updateSummary(String key) {
        Preference preference = findPreference(key);
        preference.setSummary(((EditTextPreference) preference).getText());
    }
}
