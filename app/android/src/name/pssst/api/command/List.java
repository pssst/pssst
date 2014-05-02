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

import name.pssst.api.internal.CommandBase;
import name.pssst.api.internal.KeyStore;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;

/**
 * List command for user boxes.
 * @author Christian & Christian
 */
public final class List extends CommandBase {

    /**
     * Initializes the command.
     * @param store key store
     */
    public List(KeyStore store) {
        super(store);
    }

    /**
     * Executes the command and returns result.
     * @param param param
     * @return result
     * @throws Exception
     */
    public Object execute(Object param) throws Exception {
        JSONArray json = new JSONArray(requestApi(new HttpGet(), store.getUser() + "/list", null));

        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < json.length(); i++) {
            list.add(json.getString(i));
        }

        Collections.sort(list);

        return list;
    }
}
