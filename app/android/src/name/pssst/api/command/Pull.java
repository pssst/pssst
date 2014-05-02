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

import name.pssst.api.Message;
import name.pssst.api.Name;
import name.pssst.api.internal.CommandBase;
import name.pssst.api.internal.KeyStore;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

/**
 * Pull command for receiving messages.
 * @author Christian & Christian
 */
public final class Pull extends CommandBase {
    /**
     * Initializes the command.
     * @param store key store
     */
    public Pull(KeyStore store) {
        super(store);
    }

    /**
     * Executes the command and returns result.
     * @param box box
     * @return result
     * @throws Exception
     */
    public Object execute(Object box) throws Exception {
        String body = requestApi(new HttpGet(), new Name(store.getUser(), (String) box).getPath(), null);

        // No new message
        if (body.isEmpty()) {
            return null;
        }

        JSONObject json = new JSONObject(body);
        JSONObject meta = json.getJSONObject("meta");

        byte[] data = Base64.decode(json.getString("data").getBytes("US-ASCII"));
        byte[] once = Base64.decode(meta.getString("once").getBytes("US-ASCII"));

        String text = new String(store.getKey().decrypt(data, once), "US-ASCII");

        return new Message(meta.getString("name"), text, meta.getLong("time"));
    }
}
