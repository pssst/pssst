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

import name.pssst.api.Name;
import name.pssst.api.internal.CommandBase;
import name.pssst.api.internal.KeyStore;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;

/**
 * Create command for users and boxes.
 * @author Christian & Christian
 */
public final class Create extends CommandBase {
    /**
     * Initializes command.
     * @param store key store
     */
    public Create(KeyStore store) {
        super(store);
    }

    /**
     * Executes command and returns result.
     * @param box box
     * @return true
     * @throws Exception
     */
    public Object execute(Object box) throws Exception {
        JSONObject json = null;

        // Send key for new user
        if (box == null) {
            json = new JSONObject();
            json.put("key", store.getKey().exportPublicKey());
        }

        requestApi(new HttpPost(), new Name(store.getUser(), (String) box).getPath(), json);

        return true;
    }
}
