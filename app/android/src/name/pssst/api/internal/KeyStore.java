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
package name.pssst.api.internal;

import name.pssst.api.Name;
import name.pssst.api.Pssst;
import name.pssst.api.exception.ApiException;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Class for storing public and private keys.
 * @author Christian & Christian
 */
public final class KeyStore {
    private String user;
    private File directory;
    private Key key;

    /**
     * Initializes the store and set up keys.
     * @param user user name
     * @param password store password
     * @throws ApiException
     */
    public KeyStore(String user, String password) throws Exception {
        this.directory = new File(Pssst.getUserDirectory(), "pssst." + user);
        this.user = user;

        if (!password.matches("^((?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,})$")) {
            throw new ApiException("Password weak");
        }

        if (directory.exists()) {
            key = Key.fromPrivateKey(loadKey(user + ".private"), password);
        } else {
            key = Key.generateKey();

            if (!directory.mkdirs()) {
                throw new ApiException("Could not create directory");
            }

            saveKey(user + ".private", key.exportPrivateKey(password));
            saveKey(user, key.exportPublicKey());
        }
    }

    /**
     * Returns all user names in alphabetical order.
     * @return user names
     */
    public final ArrayList<String> listUsers() {
        final ArrayList<String> list = new ArrayList<String>();

        for (String key: directory.list()) {
            if (key.matches("^\\w+$")) {
                try {
                    list.add(new Name(URLDecoder.decode(key)).getOfficial());
                } catch (ApiException e) {
                    // Should never fail
                }
            }
        }

        Collections.sort(list);

        return list;
    }

    /**
     * Returns all key names in alphabetical order.
     * @return key names
     */
    public final ArrayList<String> listKeys() {
        final ArrayList<String> list = new ArrayList<String>();

        for (String key: directory.list()) {
            list.add(URLDecoder.decode(key));
        }

        Collections.sort(list);

        return list;
    }

    /**
     * Returns a key in PEM format.
     * @param name key name
     * @return key value
     * @throws Exception
     */
    public final String loadKey(String name) throws Exception {
        name = URLEncoder.encode(name, "US-ASCII");

        InputStreamReader stream = new InputStreamReader(new FileInputStream(new File(directory, name)));
        BufferedReader reader = new BufferedReader(stream);
        StringBuilder buffer = new StringBuilder("");

        String line = reader.readLine();
        while (line != null) {
            buffer.append(line);
            buffer.append("\n");
            line = reader.readLine();
        }

        stream.close();

        return buffer.toString();
    }

    /**
     * Saves a key in PEM format.
     * @param name key name
     * @param key key value
     * @throws Exception
     */
    public final void saveKey(String name, String key) throws Exception {
        name = URLEncoder.encode(name, "US-ASCII");

        FileWriter stream = new FileWriter(new File(directory, name));
        stream.write(key);
        stream.close();
    }

    /**
     * Deletes the key store.
     */
    public final void delete() throws Exception {
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file: files) {
                if (!file.delete()) {
                    throw new Exception("Could not delete " + file.getName());
                }
            }

            if (!directory.delete()) {
                throw new Exception("Could not delete " + directory.getName());
            }
        }
    }

    /**
     * Returns the user name
     * @return user name
     */
    public final String getUser() {
        return user;
    }

    /**
     * Returns the directory.
     * @return key store directory
     */
    public final File getDirectory() {
        return directory;
    }

    /**
     * Returns the user key.
     * @return user key
     */
    public final Key getKey() {
        return key;
    }
}
