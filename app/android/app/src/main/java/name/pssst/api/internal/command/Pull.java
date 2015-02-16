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

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import name.pssst.api.PssstException;
import name.pssst.api.entity.Message;
import name.pssst.api.internal.Request;
import name.pssst.api.internal.RequestProvider;
import name.pssst.api.internal.Response;
import name.pssst.api.internal.entity.AesData;

/**
 * Internal API pull command.
 */
public final class Pull {
    private final String mUser;
    private final String mBox;

    /**
     * Constructs the command.
     * @param user User name
     * @param box Box name
     */
    public Pull(String user, String box) {
        mUser = user;
        mBox = box;
    }

    public Pull(String user) {
        this(user, null);
    }

    /**
     * Returns a pulled message from the box.
     * @param requestProvider Request provider
     * @return Message
     * @throws PssstException
     */
    public final Message execute(RequestProvider requestProvider) throws PssstException {
        final Response response = requestProvider.requestApi(Request.Method.GET, mUser, mBox);

        if (response.isEmpty()) {
            return null; // No new message found
        }

        final JSONObject json;
        final JSONObject head;

        try {
            json = response.getJson();
            head = json.getJSONObject("head");

            final String user = head.getString("user");
            final long time = head.getLong("time");

            final byte[] nonce = Base64.decode(head.getString("nonce"), Base64.NO_WRAP);
            final byte[] body = Base64.decode(json.getString("body"), Base64.NO_WRAP);

            final AesData aesData = new AesData(body, nonce);

            return new Message(requestProvider.getKeyStorage().getUserKey().decrypt(aesData), user, time);
        } catch (JSONException e) {
            throw new PssstException("JSON invalid", e);
        }
    }
}
