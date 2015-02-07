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

package name.pssst.api.internal;

import org.zeroturnaround.zip.ZipInfoCallback;
import org.zeroturnaround.zip.ZipUtil;
import org.zeroturnaround.zip.commons.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import name.pssst.api.PssstException;
import name.pssst.api.entity.Name;

/**
 * Internal storage class for public and private keys.
 */
public final class KeyStorage {
    private static final String RSA_KEY = "id_rsa";
    private static final String API_KEY = "id_api";
    private static final String ENCODING = "US-ASCII";

    private final File mStorage;
    private final Key mKey;
    private String mScheme;

    /**
     * Constructs a new key storage.
     * @param directory Pssst directory
     * @param server Pssst server
     * @param username User name
     * @param password User private key password
     * @throws PssstException
     */
    public KeyStorage(String directory, String server, String username, String password) throws PssstException {
        mStorage = new File(String.format("%s/.%s", directory, new Name(username).toString()));
        mScheme = "%s";

        if (mStorage.exists()) {
            mKey = loadKey(RSA_KEY, password);
        } else {
            mKey = Key.generate();

            saveKey(RSA_KEY, mKey.exportPrivate(password));
        }

        mScheme = server.replaceAll("^(?i)https?://(.+)", "$1/%s.pub");
    }

    /**
     * Returns a list of all key storages.
     * @param directory KeyStorage directory
     * @return Storages
     */
    public static List<String> listKeyStorages(String directory) {
        final File[] files = new File(directory).listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(String.format(".%s.", Name.getService()));
            }
        });

        final ArrayList<String> storages = new ArrayList<>();

        for (File file: files) {
            storages.add(file.getName().substring(1));
        }

        return storages;
    }

    /**
     * Assert key storage file exists.
     * @throws PssstException
     */
    public final void assertStorage() throws PssstException {
        if (!mStorage.exists()) {
            throw new PssstException("KeyStorage invalid");
        }
    }

    /**
     * Returns the users key.
     * @return Key
     */
    public final Key getUserKey() {
        return mKey;
    }

    /**
     * Returns all stored keys for this scheme.
     * @return Key names
     * @throws PssstException
     */
    public final List<String> listKeys() throws PssstException {
        assertStorage();

        final List<String> keys = new ArrayList<>();

        ZipUtil.iterate(mStorage, new ZipInfoCallback() {
            public void process(ZipEntry entry) throws IOException {
                final String name = entry.getName();

                if (name.startsWith(mScheme.split("/")[0])) {
                    keys.add(name.replaceAll("^.+/(.+)\\.pub$", "$1"));
                }
            }
        });

        return keys;
    }

    /**
     * Returns the stored key for the scheme.
     * @param name Key name
     * @param password Key Password
     * @return Key
     * @throws PssstException
     */
    public final Key loadKey(String name, String password) throws PssstException {
        assertStorage();

        name = String.format(mScheme, name);

        try {
            return Key.parse(new String(ZipUtil.unpackEntry(mStorage, name), ENCODING), password);
        } catch (UnsupportedEncodingException e) {
            throw new PssstException("Key invalid", e);
        }
    }

    /**
     * Returns the stored key for the scheme.
     * @param name Key name
     * @return Key
     * @throws PssstException
     */
    public final Key loadKey(String name) throws PssstException {
        return loadKey(name, null);
    }

    /**
     * Returns the API key.
     * @return Key
     * @throws PssstException
     */
    public final Key loadApiKey() throws PssstException {
        return loadKey(API_KEY);
    }

    /**
     * Stores the key to the scheme.
     * @param name Entry name
     * @param key Key value
     * @throws PssstException
     */
    public final void saveKey(String name, String key) throws PssstException {
        name = String.format(mScheme, name);

        if (!mStorage.exists()) {
            create(name);
        }

        try {
            if (!ZipUtil.containsEntry(mStorage, name)) {
                ZipUtil.addEntry(mStorage, name, key.getBytes(ENCODING));
            } else {
                ZipUtil.replaceEntry(mStorage, name, key.getBytes(ENCODING));
            }
        } catch (UnsupportedEncodingException e) {
            throw new PssstException("Key invalid", e);
        }
    }

    /**
     * Stores the API key.
     * @param key Key value
     * @throws PssstException
     */
    public final void saveApiKey(String key) throws PssstException {
        saveKey(API_KEY, key);
    }

    /**
     * Deletes the storage file.
     * @throws PssstException
     */
    public final void delete() throws PssstException {
        assertStorage();

        if (!mStorage.delete()) {
            throw new PssstException("Could not delete KeyStorage");
        }
    }

    /**
     * Creates the storage file (Hack).
     * @param dummy Dummy entry
     * @throws PssstException
     */
    private void create(String dummy) throws PssstException {
        FileOutputStream out = null;
        ZipOutputStream zip = null;

        try {
            out = new FileOutputStream(mStorage);
            zip = new ZipOutputStream(new BufferedOutputStream(out));
            zip.putNextEntry(new ZipEntry(dummy));
            zip.closeEntry();
        } catch (IOException e) {
            throw new PssstException("Could not create KeyStorage", e);
        } finally {
            IOUtils.closeQuietly(zip);
            IOUtils.closeQuietly(out);
        }
    }
}
