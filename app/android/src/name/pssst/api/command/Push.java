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
package name.pssst.api.command;

import android.util.Pair;

import name.pssst.api.Message;
import name.pssst.api.internal.CommandBase;
import name.pssst.api.internal.Key;
import name.pssst.api.internal.KeyStore;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

/**
 * Push command for sending messages.
 * @author Christian & Christian
 */
public final class Push extends CommandBase {
    /**
     * Initializes the command.
     * @param store key store
     */
    public Push(KeyStore store) {
        super(store);
    }

    /**
     * Executes the command and returns result.
     * @param message message
     * @return result
     * @throws Exception
     */
    public Object execute(Object message) throws Exception {
        String text = ((Message) message).getText();
        String user = ((Message) message).getName().getUser();
        String path = ((Message) message).getName().getPath();

        Pair<byte[], byte[]> result = Key.fromPublicKey(store.loadKey(user)).encrypt(text.getBytes("US-ASCII"));

        JSONObject json = new JSONObject();
        JSONObject meta = new JSONObject();

        meta.put("name", store.getUser());
        meta.put("once", new String(Base64.encode(result.second), "US-ASCII"));

        json.put("meta", meta);
        json.put("data", new String(Base64.encode(result.first), "US-ASCII"));

        return requestApi(new HttpPut(), path, json);
    }
}
