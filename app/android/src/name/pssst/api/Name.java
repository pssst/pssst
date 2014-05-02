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
package name.pssst.api;

import name.pssst.api.exception.ApiException;

/**
 * Canonical name parser.
 * @author Christian & Christian
 */
public final class Name {
    protected String user;
    protected String box;
    protected String password;
    protected String official;
    protected String path;

    /**
     * Initializes the instance with the parsed name.
     * @param user user name
     * @param box box name
     * @param password password
     * @throws ApiException
     */
    public Name(String user, String box, String password) throws ApiException {
        user = user.trim();

        if (!user.matches("^(pssst\\.)?\\w{2,63}(\\.\\w{2,63})?(:\\S*)?$")) {
            throw new ApiException("User name invalid");
        }

        // Remove service identifier
        if (user.startsWith("pssst.")) {
            user = user.substring(6);
        }

        // Parse inline password
        if (user.contains(":") && (password == null || password.isEmpty())) {
            String[] t = user.split(":", 2);
            user = t[0];
            password = t[1];
        }

        // Parse inline box
        if (user.contains(".") && (box == null || box.isEmpty())) {
            String[] t = user.split("\\.", 2);
            user = t[0];
            box = t[1];
        }

        this.user = user.toLowerCase();
        this.box = box;
        this.password = password;

        if (box != null && !box.isEmpty()) {
            this.box = box.toLowerCase();
            this.path = String.format("%s/%s/", this.user, this.box);
            this.official = String.format("pssst.%s.%s", this.user, this.box);
        } else {
            this.path = String.format("%s/", this.user);
            this.official = String.format("pssst.%s", this.user);
        }
    }

    /**
     * Initializes the instance with the parsed name.
     * @param user user name
     * @param box box name
     * @throws ApiException
     */
    public Name(String user, String box) throws ApiException {
        this(user, box, "");
    }

    /**
     * Initializes the instance with the parsed name.
     * @param user user name
     * @throws ApiException
     */
    public Name(String user) throws ApiException {
        this(user, "");
    }

    /**
     * Returns the user name.
     * @return user name
     */
    public final String getUser() {
        return user;
    }

    /**
     * Returns the box name.
     * @return box name
     */
    public final String getBox() {
        return box;
    }

    /**
     * Returns the password.
     * @return password
     */
    public final String getPassword() {
        return password;
    }

    /**
     * Returns the full name in canonical notation.
     * @return full name
     */
    public final String getOfficial() {
        return official;
    }

    /**
     * Returns the users server path.
     * @return path
     */
    public final String getPath() {
        return path;
    }
}
