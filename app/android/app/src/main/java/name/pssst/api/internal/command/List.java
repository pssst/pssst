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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;

import name.pssst.api.PssstException;
import name.pssst.api.internal.Request;
import name.pssst.api.internal.RequestProvider;
import name.pssst.api.internal.Response;

/**
 * Internal API list command.
 */
public final class List {
    private final String mUser;

    /**
     * Constructs the command.
     * @param user User name
     */
    public List(String user) {
        mUser = user;
    }

    /**
     * Returns an alphabetical list of all user box names.
     * @param requestProvider Request provider
     * @return Box names
     * @throws PssstException
     */
    public final String[] execute(RequestProvider requestProvider) throws PssstException {
        try {
            final Response response = requestProvider.requestApi(Request.Method.GET, mUser, "list");

            final JSONArray json = new JSONArray(response.getText());
            final ArrayList<String> boxes = new ArrayList<>();

            for (int box = 0; box < json.length(); box++) {
                boxes.add(json.getString(box));
            }

            Collections.sort(boxes);

            return boxes.toArray(new String[boxes.size()]);
        } catch (JSONException e) {
            throw new PssstException("JSON invalid", e);
        }
    }
}
