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

package name.pssst.test;

import android.annotation.SuppressLint;
import android.test.AndroidTestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.api.entity.Name;

/**
 * API unit tests
 */
@SuppressLint("SdCardPath")
public final class ApiTest extends AndroidTestCase {
    private final static String USER_DIRECTORY = "/data/data/name.pssst.app/files"; // TODO: Create own test directory
    private final static String SERVER_ADDRESS = "https://dev.pssst.name";

    private final List<String> usernames = new ArrayList<>();

    /**
     * Generic test for minimum name notation.
     * @throws PssstException
     */
    public void test_api_name_maximum() throws PssstException {
        final Name name = new Name(" pssst.USERNAME.Box ");

        assertEquals(name.toString(), "pssst.username.box");
        assertEquals(name.getUser(), "username");
        assertEquals(name.getBox(), "box");
    }

    /**
     * Generic test for minimum name notation.
     * @throws PssstException
     */
    public void test_api_name_minimum() throws PssstException {
        final Name name = new Name("xy");

        assertEquals(name.toString(), "pssst.xy");
        assertEquals(name.getUser(), "xy");
        assertTrue(name.getBox().isEmpty());
    }

    /**
     * Generic test for the API create command.
     * @throws PssstException
     */
    public void test_api_command_create() throws PssstException {
        final Pssst pssst = createInstance();

        pssst.create();
        pssst.create("test");
    }

    /**
     * Generic test for the API delete command.
     * @throws PssstException
     */
    public void test_api_command_delete() throws PssstException {
        final Pssst pssst = createInstance();

        pssst.create();
        pssst.create("test");
        pssst.delete("test");
        pssst.delete();
    }

    /**
     * Generic test for the API find command.
     * @throws PssstException
     */
    public void test_api_command_find() throws PssstException {
        final Pssst pssst = createInstance();

        pssst.create();
        pssst.find(pssst.getUsername());
    }

    /**
     * Generic test for the API list command.
     * @throws PssstException
     */
    public void test_api_command_list() throws PssstException {
        final Pssst pssst = createInstance();

        pssst.create();
        pssst.list();
    }

    /**
     * Generic test for the API push command.
     * @throws PssstException
     */
    public void test_api_command_push() throws PssstException {
        final Pssst pssst = createInstance();

        pssst.create();
        pssst.push(pssst.getUsername(), "test");
    }

    /**
     * Generic test for the API pull command.
     * @throws PssstException
     */
    public void test_api_command_pull() throws PssstException {
        final Pssst pssst = createInstance();

        pssst.create();
        pssst.push(pssst.getUsername(), "test");
        pssst.pull();
        pssst.pull();
    }

    /**
     * Generic test with fuzzy binary data.
     * @throws PssstException
     */
    public void test_api_fuzzy() throws PssstException {
        final Pssst pssst = createInstance();
        final Random random = new Random();

        pssst.create();

        for (double exp = 0; exp <= 13; exp++) {
            final byte[] blob = new byte[(int) Math.pow(2.0, exp)];

            random.nextBytes(blob);
            pssst.push(pssst.getUsername(), blob);
            assertTrue(Arrays.equals(blob, pssst.pull().getRawData()));
        }
    }

    /**
     * Set up unit tests.
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        Pssst.setServer(SERVER_ADDRESS);
        Pssst.setDirectory(USER_DIRECTORY);
    }

    /**
     * Clean up after unit tests.
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        for (String username: usernames) {
            final File file = new File(String.format("%s/.pssst.%s", USER_DIRECTORY, username));

            if (file.exists() && !file.delete()) {
                throw new Exception("File could not be deleted");
            }
        }
    }

    /**
     * Returns a new Pssst instance.
     * @return Pssst instance
     * @throws PssstException
     */
    private Pssst createInstance() throws PssstException {
        final String username = getRandomString(16);
        final String password = getRandomString(16);

        usernames.add(username);

        return new Pssst(username, password);
    }

    /**
     * Returns a new random string.
     * @param length String length
     * @return String
     */
    private String getRandomString(int length) {
        final String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        final StringBuilder builder = new StringBuilder();
        final Random random = new Random();

        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }

        return builder.toString();
    }
}
