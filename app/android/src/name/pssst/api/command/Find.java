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

/**
 * Find command for public keys.
 * @author Christian & Christian
 */
public final class Find extends CommandBase {
    /**
     * Initializes the command.
     * @param store key store
     */
    public Find(KeyStore store) {
        super(store);
    }

    /**
     * Executes the command and returns result.
     * @param user user
     * @return result
     * @throws Exception
     */
    public Object execute(Object user) throws Exception {
        return requestApi(new HttpGet(), user + "/key", null);
    }
}
