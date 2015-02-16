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

import java.util.ArrayList;

import name.pssst.api.Pssst;
import name.pssst.api.entity.Message;

/**
 * Global application state.
 */
public class App extends android.app.Application {
    private ArrayList<Message> mPssstMessages = new ArrayList<>();
    private Pssst mPssstInstance = null;
    private boolean mIsVisible = true;

    /**
     * Clears all Pssst specific data.
     */
    public void clearPssstData() {
        mPssstInstance = null;
        mPssstMessages = new ArrayList<>();
    }

    /**
     * Sets the current Pssst instance.
     * @param pssst Pssst instance
     */
    public void setPssstInstance(Pssst pssst) {
        mPssstInstance = pssst;
    }

    /**
     * Returns the current Pssst instance.
     * @return Pssst instance
     */
    public Pssst getPssstInstance() {
        return mPssstInstance;
    }

    /**
     * Returns the received Pssst messages.
     * @return Pssst messages
     */
    public ArrayList<Message> getPssstMessages() {
        return mPssstMessages;
    }

    /**
     * Sets the App visibility.
     * @param isVisible App is visible
     */
    public void setIsVisible(boolean isVisible) {
        mIsVisible = isVisible;
    }

    /**
     * Returns the App visibility.
     * @return App is visible
     */
    public boolean getIsVisible() {
        return mIsVisible;
    }
}
