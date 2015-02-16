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

package name.pssst.api.internal.command;

import name.pssst.api.PssstException;
import name.pssst.api.internal.Request;
import name.pssst.api.internal.RequestProvider;

/**
 * Internal API delete command.
 */
public final class Delete {
    private final String mUser;
    private final String mBox;

    /**
     * Constructs the command.
     * @param user User name
     * @param box Box name
     */
    public Delete(String user, String box) {
        mUser = user;
        mBox = box;
    }

    /**
     * Constructs the command.
     * @param user User name
     */
    public Delete(String user) {
        this(user, null);
    }

    /**
     * Deletes the user or the box.
     * @param requestProvider Request provider
     * @throws PssstException
     */
    public final void execute(RequestProvider requestProvider) throws PssstException {
        requestProvider.requestApi(Request.Method.DELETE, mUser, mBox);
    }
}
