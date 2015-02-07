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
import java.util.List;
import java.util.Random;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.api.internal.RequestProvider;

/**
 * API unit tests
 */
public class ApiTest extends AndroidTestCase {
    @SuppressLint("SdCardPath")
    private final static String DIRECTORY = "/data/data/name.pssst.app/files/";
    private final static String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final List<String> usernames = new ArrayList<>();

    /**
     * Generic API test.
     * @throws PssstException
     */
    public void test_api() throws PssstException {
        final String username = createUsername();
        final String password = createPassword();

        final Pssst pssst = new Pssst(username, password);

        // Command CREATE
        pssst.create();
        pssst.create("test");

        // Command FIND
        pssst.find(username);

        // Command LIST
        pssst.list();

        // Command PUSH
        pssst.push(username, "Test Android");

        // Command PULL
        pssst.pull();
        pssst.pull();

        // Command DELETE
        pssst.delete("test");
        pssst.delete();
    }

    /**
     * Set up unit tests.
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        Pssst.setServer("http://dev.pssst.name");

        // Warm up RequestProvider
        while (true) {
            if (!(RequestProvider.requestUrl("key").getText().isEmpty())) break;
        }
    }

    /**
     * Clean up after unit tests.
     * @throws Exception
     */
    @Override
    protected void tearDown() throws Exception {
        for (String username: usernames) {
            final File file = new File(String.format("%s.pssst.%s", DIRECTORY, username));

            if (file.exists() && !file.delete()) {
                throw new Exception("File could not deleted");
            }
        }
    }

    /**
     * Returns a new random user name.
     * @return User name
     */
    private String createUsername() {
        final String username = randomString(16);

        usernames.add(username);

        return username;
    }

    /**
     * Returns a new random password.
     * @return Password
     */
    private String createPassword() {
        return randomString(16);
    }

    /**
     * Returns a new random string.
     * @return String
     */
    private String randomString(int length) {
        final StringBuilder builder = new StringBuilder();
        final Random random = new Random();

        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }

        return builder.toString();
    }
}
