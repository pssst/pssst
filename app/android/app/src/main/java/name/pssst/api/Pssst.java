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

package name.pssst.api;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.security.Security;

import name.pssst.api.entity.Message;
import name.pssst.api.entity.Name;
import name.pssst.api.internal.Key;
import name.pssst.api.internal.KeyStorage;
import name.pssst.api.internal.RequestProvider;
import name.pssst.api.internal.command.Create;
import name.pssst.api.internal.command.Delete;
import name.pssst.api.internal.command.Find;
import name.pssst.api.internal.command.List;
import name.pssst.api.internal.command.Pull;
import name.pssst.api.internal.command.Push;

/**
 * Pssst API low level communication class.
 */
public final class Pssst {
    private static final String FINGERPRINT = "5A749F99DBC2A03B0CDE327BAFCF9BD7DC616830";
    private static final String DEFAULT_API = "https://api.pssst.name";
    private static final String DEFAULT_BOX = "box";
    private static final String VERSION = "0.2.38";

    private static String sServer = DEFAULT_API;
    private static String sDirectory = ".";

    private final Name mUser;
    private final KeyStorage mKeyStorage;
    private final RequestProvider mRequestProvider;

    /**
     * Constructs a new Pssst instance from username and password.
     * @param username User name
     * @param password User private key password
     * @throws PssstException
     */
    public Pssst(String username, String password) throws PssstException {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        if (username == null || username.isEmpty()) {
            throw new PssstException("Username is required");
        }

        if (password == null || password.isEmpty()) {
            throw new PssstException("Password is required");
        }

        final String apiKey = RequestProvider.requestUrl("key").getText();

        if (sServer.equals(DEFAULT_API)) {
            assertServerFingerprint(apiKey);
            assertServerGraceTime();
        }

        mUser = new Name(username);

        mKeyStorage = new KeyStorage(sDirectory, sServer, username, password);
        mKeyStorage.saveApiKey(apiKey);

        mRequestProvider = new RequestProvider(mKeyStorage);
    }

    /**
     * Returns client identifier.
     * @return Canonical name
     */
    @Override
    public final String toString() {
        return String.format("Pssst %s", VERSION);
    }

    /**
     * Returns if the user file exists.
     * @param username User name
     * @return Existence
     * @throws PssstException
     */
    public static boolean exists(String username) throws PssstException {
        return KeyStorage.exists(sDirectory, username);
    }

    /**
     * Returns the Pssst server.
     * @return Pssst server
     */
    public static String getServer() {
        return sServer;
    }

    /**
     * Sets the Pssst Server.
     * @param server Pssst server
     */
    public static void setServer(String server) {
        sServer = server;
    }

    /**
     * Returns the Pssst directory.
     * @return Pssst directory
     */
    public static String getDirectory() {
        return new File(sDirectory).getPath();
    }

    /**
     * Sets the Pssst directory.
     * @param directory Pssst directory
     */
    public static void setDirectory(String directory) {
        sDirectory = directory;
    }

    /**
     * Returns the default API URL.
     * @return Default API URL
     */
    public static String getDefaultApi() {
        return DEFAULT_API;
    }

    /**
     * Returns the default box name.
     * @return Default box name
     */
    public static String getDefaultBox() {
        return DEFAULT_BOX;
    }

    /**
     * Returns the API version.
     * @return API version
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Returns the user name.
     * @return User name
     */
    public final String getUsername() {
        return mUser.toString();
    }

    /**
     * Creates a new user.
     * @throws PssstException
     */
    public final void create() throws PssstException {
        new Create(mUser.getUser(), mKeyStorage.getUserKey()).execute(mRequestProvider);
    }

    /**
     * Creates a new box.
     * @param box Box name
     * @throws PssstException
     */
    public final void create(String box) throws PssstException {
        new Create(mUser.getUser(), box).execute(mRequestProvider);
    }

    /**
     * Deletes the user.
     * @throws PssstException
     */
    public final void delete() throws PssstException {
        new Delete(mUser.getUser()).execute(mRequestProvider);

        mKeyStorage.delete();
    }

    /**
     * Deletes the box.
     * @param box Box name
     * @throws PssstException
     */
    public final void delete(String box) throws PssstException {
        new Delete(mUser.getUser(), box).execute(mRequestProvider);
    }

    /**
     * Returns the users public key.
     * @param user User name
     * @return Public key
     * @throws PssstException
     */
    public final String find(String user) throws PssstException {
        return new Find(new Name(user).getUser()).execute(mRequestProvider);
    }

    /**
     * Returns an alphabetical list of all user box names.
     * @return Box names
     * @throws PssstException
     */
    public final String[] list() throws PssstException {
        return new List(mUser.getUser()).execute(mRequestProvider);
    }

    /**
     * Returns a pulled message from the box.
     * @return Message
     * @throws PssstException
     */
    public final Message pull(String box) throws PssstException {
        return new Pull(mUser.getUser(), box).execute(mRequestProvider);
    }

    /**
     * Returns a pulled message from the default box.
     * @return Message
     * @throws PssstException
     */
    public final Message pull() throws PssstException {
        return pull(null);
    }

    /**
     * Push the message text into the receivers box.
     * @param receiver User name
     * @param data Message data
     * @throws PssstException
     */
    public final void push(String receiver, byte[] data) throws PssstException {
        final Name name = new Name(receiver);

        // Cache the receivers public key
        if (!mRequestProvider.getKeyStorage().listKeys().contains(name.getUser())) {
            mRequestProvider.getKeyStorage().saveKey(name.getUser(), find(name.getUser()));
        }

        new Push(mUser.getUser(), name.getUser(), name.getBox(), data).execute(mRequestProvider);
    }

    /**
     * Push the message text into the receivers box.
     * @param receiver User name
     * @param message Message text
     * @throws PssstException
     */
    public final void push(String receiver, String message) throws PssstException {
        push(receiver, message.getBytes());
    }

    /**
     * Pushes the message data into the receivers box.
     * @param receivers User names
     * @param data Message data
     * @throws PssstException
     */
    public final void push(String[] receivers, byte[] data) throws PssstException {
        for (String receiver: receivers) {
            push(receiver, data);
        }
    }

    /**
     * Pushes the message text into the receivers box.
     * @param receivers User names
     * @param message Message text
     * @throws PssstException
     */
    public final void push(String[] receivers, String message) throws PssstException {
        push(receivers, message.getBytes());
    }

    /**
     * Asserts the servers public key is correct.
     * @param key API key
     * @throws PssstException
     */
    private void assertServerFingerprint(String key) throws PssstException {
        if (!FINGERPRINT.equalsIgnoreCase(Key.fingerprint(key))) {
            throw new PssstException("Server could not be authenticated");
        }
    }

    /**
     * Asserts the servers timestamp is correct.
     * @throws PssstException
     */
    private void assertServerGraceTime() throws PssstException {
        if (!RequestProvider.checkServerTime()) {
            throw new PssstException("Server could not be authenticated");
        }
    }
}
