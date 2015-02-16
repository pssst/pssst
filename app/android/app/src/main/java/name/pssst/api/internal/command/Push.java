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
import name.pssst.api.internal.Request;
import name.pssst.api.internal.RequestProvider;
import name.pssst.api.internal.entity.AesData;

/**
 * Internal API push command.
 */
public final class Push {
    private final String mUser;
    private final String mReceiver;
    private final String mBox;
    private final byte[] mData;

    /**
     * Constructs the command.
     * @param user User name
     * @param receiver Receiver name
     * @param box Receiver box name
     * @param data Message data
     */
    public Push(String user, String receiver, String box, byte[] data) {
        mUser = user;
        mReceiver = receiver;
        mBox = box;
        mData = data;
    }

    /**
     * Returns the users public key.
     * @param requestProvider Request provider
     * @throws PssstException
     */
    public final void execute(RequestProvider requestProvider) throws PssstException {
        final AesData aesData = requestProvider.getKeyStorage().loadKey(mReceiver).encrypt(mData);

        final JSONObject json;
        final JSONObject head;

        try {
            head = new JSONObject();
            head.put("user", mUser);
            head.put("nonce", Base64.encodeToString(aesData.getNonce(), Base64.NO_WRAP));

            json = new JSONObject();
            json.put("head", head);
            json.put("body", Base64.encodeToString(aesData.getData(), Base64.NO_WRAP));
        } catch (JSONException e) {
            throw new PssstException("JSON invalid", e);
        }

        requestProvider.requestApi(Request.Method.PUT, mReceiver, mBox, json);
    }
}
