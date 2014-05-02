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

import name.pssst.api.command.Create;
import name.pssst.api.command.Delete;
import name.pssst.api.command.File;
import name.pssst.api.command.Find;
import name.pssst.api.command.List;
import name.pssst.api.command.Pull;
import name.pssst.api.command.Push;
import name.pssst.api.exception.ApiException;
import name.pssst.api.internal.Command;
import name.pssst.api.internal.KeyStore;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.encoders.Hex;

import java.security.*;
import java.util.ArrayList;

/**
 * Class for API communication and abstraction.
 * @author Christian & Christian
 */
public final class Pssst {
    public static final String API = "https://api.pssst.name";
    public static final String VERSION = "0.2.21";

    // User Settings
    public static java.io.File directory = null;
    public static String host = null;

    private static final String FINGERPRINT = "563cb9031992f503a21f3fa7be160567f1380467";
    private KeyStore store;

    /**
     * Initializes the instance.
     * @param username username
     * @param password password
     * @throws Exception
     */
    private Pssst(String username, String password) throws Exception {
        String key = file("key");

        boolean verify = host.equalsIgnoreCase(API);

        long serverTime = Long.parseLong(file("time"));
        long systemTime = System.currentTimeMillis() / 1000;

        MessageDigest sha1 = MessageDigest.getInstance("SHA1");

        if (verify && !FINGERPRINT.equals(Hex.toHexString(sha1.digest(key.getBytes("US-ASCII"))))) {
            throw new Exception("Server could not be authenticated");
        }

        if (verify && Math.abs(serverTime - systemTime) > 30) {
            throw new Exception("Client time is not synchronized");
        }

        if (verify && password.isEmpty()) {
            throw new Exception("Password is required");
        }

        store = new KeyStore(username, password);

        if (!store.listKeys().contains(host)) {
            store.saveKey(host, key);
        }
    }

    /**
     * Returns an new instance.
     * @param username username
     * @param password password
     * @return instance
     * @throws ApiException
     */
    public static Pssst newInstance(String username, String password) throws ApiException {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        try {
            return new Pssst(username, password);
        } catch (Exception e) {
            throw new ApiException(e.getMessage(), e);
        }
    }

    /**
     * Sets the general API address.
     * @param host api address
     */
    public static void setApiAddress(String host) {
        Pssst.host = host;
    }

    /**
     * Gets the general API address.
     * @return api address
     */
    public static String getApiAddress() {
        if (host != null && !host.isEmpty()) {
            return host;
        }

        return API; // Default
    }

    /**
     * Sets the general user directory.
     * @param directory user directory
     */
    public static void setUserDirectory(java.io.File directory) {
        Pssst.directory = directory;
    }

    /**
     * Gets the general user directory.
     * @return user directory
     */
    public static java.io.File getUserDirectory() {
        if (directory != null && directory.isDirectory()) {
            return directory;
        }

        return new java.io.File("."); // Default
    }

    /**
     * Gets the given user name.
     * @return user name
     */
    public String getUser() {
        return store.getUser();
    }

    /**
     * Gets all stored user names.
     * @return user names
     */
    public ArrayList<String> getUsers() {
        return store.listUsers();
    }

    /**
     * Creates an user or a box.
     * @param box box name
     * @throws ApiException
     */
    public final void create(String box) throws ApiException {
        execute(new Create(store), box);
    }

    /**
     * Creates an user.
     * @throws ApiException
     */
    public final void create() throws ApiException {
        create(null);
    }

    /**
     * Deletes an user or a box.
     * @param box box name
     * @throws ApiException
     */
    public final void delete(String box) throws ApiException {
        execute(new Delete(store), box);
    }

    /**
     * Deletes an user.
     * @throws ApiException
     */
    public final void delete() throws ApiException {
        delete(null);
    }

    /**
     * Returns an alphabetical list of all boxes of an user.
     * @return box names
     * @throws ApiException
     */
    @SuppressWarnings({"unchecked"})
    public final ArrayList<String> list() throws ApiException {
        return (ArrayList<String>) execute(new List(store), null);
    }

    /**
     * Returns the content of a file.
     * @param path file path
     * @return file content
     * @throws ApiException
     */
    public final String file(String path) throws ApiException {
        return (String) execute(new File(), path);
    }

    /**
     * Returns the public key of an user.
     * @param user user name
     * @return PEM formatted public key
     * @throws ApiException
     */
    public final String find(String user) throws ApiException {
        return (String) execute(new Find(store), user);
    }

    /**
     * Pulls a message from a box.
     * @param box box name
     * @return message
     * @throws ApiException
     */
    public final Message pull(String box) throws ApiException {
        return (Message) execute(new Pull(store), box);
    }

    /**
     * Pulls a message from the default box.
     * @return message
     * @throws ApiException
     */
    public final Message pull() throws ApiException {
        return pull(null);
    }

    /**
     * Pushes a message into a box.
     * @param message message
     * @throws ApiException
     */
    public final void push(Message message) throws ApiException {
        String user = message.getName().getUser();

        // Add new user to key store
        try {
            if (!store.listKeys().contains(user)) {
                store.saveKey(user, find(user));
            }
        } catch (Exception e) {
            throw new ApiException(e.getMessage(), e);
        }

        execute(new Push(store), message);
    }

    /**
     * Executes a command and returns its result.
     * @param command command
     * @param param parameter
     * @return result
     * @throws ApiException
     */
    private Object execute(Command command, Object param) throws ApiException {
        try {
            return command.execute(param);
        } catch (Exception e) {
            throw new ApiException(e.getMessage(), e);
        }
    }
}
