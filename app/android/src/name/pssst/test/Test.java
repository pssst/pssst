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
import name.pssst.api.Message;
import name.pssst.api.Name;
import name.pssst.api.Pssst;
import name.pssst.api.exception.ApiException;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.io.File;

/**
 * Unit tests for Pssst.
 * @author Christian & Christian
 */
@RunWith(RobolectricTestRunner.class)
public final class Test {
    private final static String DEVAPI = "http://dev.pssst.name";
    private final static String TMPDIR = System.getProperty("java.io.tmpdir");

    /**
     * Sets up Pssst settings and turns off fake HTTP layer.
     */
    public Test() {
        Pssst.setApiAddress(DEVAPI);
        Pssst.setUserDirectory(new File(TMPDIR));

        Robolectric.getFakeHttpLayer().interceptHttpRequests(false);
    }

    /**
     * Tests if a maximum name is parsed correctly.
     */
    @org.junit.Test
    public final void testName_maximum() {
        try {
            Name name = new Name(" pssst.User.Box:P455w0rd ");

            Assert.assertEquals(name.getUser(), "user");
            Assert.assertEquals(name.getBox(), "box");
            Assert.assertEquals(name.getPassword(), "P455w0rd");
            Assert.assertEquals(name.getOfficial(), "pssst.user.box");
            Assert.assertEquals(name.getPath(), "user/box/");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests if a minimum name is parsed correctly.
     */
    @org.junit.Test
    public final void testName_minimum() {
        try {
            Name name = new Name("user");

            Assert.assertEquals(name.getUser(), "user");
            Assert.assertTrue(name.getBox().isEmpty());
            Assert.assertTrue(name.getPassword().isEmpty());
            Assert.assertEquals(name.getOfficial(), "pssst.user");
            Assert.assertEquals(name.getPath(), "user/");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests if a name is invalid.
     */
    @org.junit.Test
    public final void testName_invalid() {
        try {
            new Name("Invalid user.name !");
        } catch (ApiException e) {
            Assert.assertEquals(e.getMessage(), "User name invalid");
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Tests if all commands are successful.
     */
    @org.junit.Test
    public final void testPssst_commands() {
        try {
            String user = Long.toHexString(Double.doubleToLongBits(Math.random()));
            String box = "test";

            Pssst pssst = Pssst.newInstance(user, "4NdR01d!");

            pssst.create();
            pssst.create(box);
            pssst.find(user);
            pssst.list();
            pssst.push(new Message(user + "." + box, "Hello World!"));
            pssst.pull();
            pssst.delete(box);
            pssst.delete();

            Assert.assertTrue(true);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Sandbox for various tests.
     */
    @org.junit.Test
    public final void testPssst_sandbox() {
        Assert.assertTrue(true);
    }
}
