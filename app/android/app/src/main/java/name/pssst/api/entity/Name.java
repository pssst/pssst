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

package name.pssst.api.entity;

import name.pssst.api.PssstException;

/**
 * Pssst canonical name parser.
 */
public final class Name {
    private static final String PATTERN = "^(pssst\\.)?\\w{2,63}(\\.\\w{2,63})?$";
    private static final String SERVICE = "pssst";

    protected final String mUser;
    protected final String mBox;

    /**
     * Constructs a new Name from the user and box name.
     * @param user User name
     * @param box Box name
     * @throws PssstException
     */
    public Name(String user, String box) throws PssstException {
        user = user.trim();

        // Check user name
        if (!user.matches(PATTERN)) {
            throw new PssstException("User name invalid");
        }

        // Remove service identifier
        if (user.startsWith(SERVICE + ".")) {
            user = user.substring(SERVICE.length() + 1);
        }

        // Parse box name
        if (user.contains(".") && (box == null || box.isEmpty())) {
            String[] tokens = user.split("\\.", 2);
            user = tokens[0];
            box = tokens[1];
        } else if (box == null) {
            box = "";
        }

        mUser = user.toLowerCase();
        mBox = box.toLowerCase();
    }

    /**
     * Constructs a new Name from the user name.
     * @param user User name
     * @throws PssstException
     */
    public Name(String user) throws PssstException {
        this(user, null);
    }

    /**
     * Returns the full canonical name.
     * @return Canonical name
     */
    @Override
    public final String toString() {
        if (!mBox.isEmpty()) {
            return String.format(SERVICE + ".%s.%s", mUser, mBox);
        } else {
            return String.format(SERVICE + ".%s", mUser);
        }
    }

    /**
     * Returns the service name.
     * @return Service name
     */
    public static String getService() {
        return SERVICE;
    }

    /**
     * Returns the user name.
     * @return User name
     */
    public final String getUser() {
        return mUser;
    }

    /**
     * Returns the box name.
     * @return Box name
     */
    public final String getBox() {
        return mBox;
    }
}
