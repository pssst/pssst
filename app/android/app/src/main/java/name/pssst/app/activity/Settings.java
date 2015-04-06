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
import android.view.MenuItem;

import name.pssst.app.R;

/**
 * Settings activity.
 */
public class Settings extends Activity {

    /**
     * Initializes the activity and load settings fragment.
     * @param savedInstanceState Saved instance state
     */
    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionbar = getActionBar();
        //noinspection ConstantConditions
        actionbar.setTitle(getResources().getString(R.string.app_name));
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setIcon(R.mipmap.ic_actionbar);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new name.pssst.app.fragment.Settings()).commit();
    }

    /**
     * Finishes activity and returns result.
     * @param item Menu item
     * @return Result
     */
    @Override
    public final boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
