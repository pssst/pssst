#!/usr/bin/env python
"""
Pssst!
Copyright (C) 2013  Christian & Christian  <pssst@pssst.name>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
"""
import os
import random
import string
import sys


from pssst import Pssst, Name


try:
    import pytest

except ImportError:
    sys.exit("Requires py.test (http://pytest.org)")


def createUserName(length=16):
    """
    Returns a random user name.

    Parameters
    ----------
    param length : int, optional (default is 16)
        Length of the name in characters.

    Returns
    -------
    string
        A random user name.

    """
    pool = string.ascii_lowercase + string.digits
    chars = [random.choice(pool) for x in range(length)]

    return "".join(chars)


class TestName:
    """
    Tests Name parsing with the test cases:

    * User name and box name
    * User name alone
    * User name is invalid

    Methods
    -------
    test_name_with_all()
        Tests if name is parsed correctly.
    test_name_without_all()
        Tests if name is parsed correctly.
    test_user_name_invalid()
        Tests if name is invalid.

    """
    def test_name_with_all(self):
        """
        Tests if name is parsed correctly.
        """
        name = Name(" pssst.User.Box:P455w0rd ")

        assert name.path == "user/box/"
        assert name.user == "user"
        assert name.box == "box"
        assert name.all == ("user", "box")
        assert name.password == "P455w0rd"
        assert str(name) == "pssst.user.box"

    def test_name_without_all(self):
        """
        Tests if name is parsed correctly.
        """
        name = Name("user")

        assert name.path == "user/"
        assert name.user == "user"
        assert name.box == None
        assert name.all == ("user", None)
        assert name.password == None
        assert str(name) == "pssst.user"

    def test_user_name_invalid(self):
        """
        Tests if name is invalid.
        """
        with pytest.raises(Exception) as ex:
            Name("Invalid user.name !")

        assert ex.value.message == "User name invalid"


class TestCrypto:
    """
    Tests crypto methods with this test cases:

    * Verification failed, user not found
    * Verification failed, signature invalid
    * Verification failed, signature wrong

    Methods
    -------
    test_request_verify_user_not_found()
        Tests if request user is not found.
    test_request_verify_signature_invalid()
        Tests if request signature is invalid.
    test_request_verify_signature_wrong()
        Tests if request signature is wrong.

    """
    def test_request_verify_user_not_found(self):
        """
        Tests if request user is not found.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.pull()

        assert ex.value.message == "Verification failed"

    def test_request_verify_signature_invalid(self):
        """
        Tests if request signature is invalid.
        """
        original = Pssst.Key.sign

        with pytest.raises(Exception) as ex:
            Pssst.Key.sign = lambda self, data: ("!", "!")

            pssst = Pssst(createUserName())
            pssst.create()
            pssst.pull()

        Pssst.Key.sign = original

        assert ex.value.message == "Verification failed"

    def test_request_verify_signature_wrong(self):
        """
        Tests if request verification signature is correct.
        """
        original = Pssst.Key.sign

        with pytest.raises(Exception) as ex:
            Pssst.Key.sign = lambda self, data: original(self, "test")

            pssst = Pssst(createUserName())
            pssst.create()
            pssst.pull()

        Pssst.Key.sign = original

        assert ex.value.message == "Verification failed"


class TestUser:
    """
    Tests user with this test cases:

    * User create
    * User create failed, name restricted
    * User create failed, already exists
    * User delete
    * User find
    * User find failed, user was deleted
    * User find failed, user not found
    * User list
    * User name invalid

    Methods
    -------
    test_create_user()
        Tests if an user can be created.
    test_create_user_name_restricted()
        Tests if an user name is restricted.
    test_create_user_already_exists()
        Tests if an user already exists.
    test_delete_user()
        Tests if an user can be deleted.
    test_find_user()
        Tests if an user public can be found.
    test_find_user_was_deleted()
        Tests if an user was deleted.
    test_find_user_not_found()
        Tests if an user is not found.
    test_list()
        Tests if an user boxes can be listed.
    test_user_name_invalid()
        Tests if an user name is invalid.

    """
    def test_create_user(self):
        """
        Tests if an user can be created.
        """
        pssst = Pssst(createUserName())
        pssst.create()

    def test_create_user_name_restricted(self):
        """
        Tests if an user name is restricted.
        """
        if os.path.exists(".pssst.name"):
            os.remove(".pssst.name")

        with pytest.raises(Exception) as ex:
            pssst = Pssst("name")
            pssst.create()

        assert ex.value.message == "User name restricted"

    def test_create_user_already_exists(self):
        """
        Tests if an user already exists.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.create()
            pssst.create()

        assert ex.value.message == "User already exists"

    def test_delete_user(self):
        """
        Tests if an user can be deleted.
        """
        pssst = Pssst(createUserName())
        pssst.create()
        pssst.delete()

    def test_find_user(self):
        """
        Tests if an user public can be found.
        """
        name = createUserName()
        pssst = Pssst(name)
        pssst.create()
        pssst.find(name)

    def test_find_user_was_deleted(self):
        """
        Tests if an user was deleted.
        """
        with pytest.raises(Exception) as ex:
            name = createUserName()
            pssst = Pssst(name)
            pssst.create()
            pssst.delete()

            pssst = Pssst(createUserName())
            pssst.find(name)

        assert ex.value.message == "User was deleted"

    def test_find_user_not_found(self):
        """
        Tests if an user is not found.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.find("usernotfound")

        assert ex.value.message == "User not found"

    def test_list(self):
        """
        Tests if an user boxes can be listed.
        """
        pssst = Pssst(createUserName())
        pssst.create()
        pssst.create("xyz")

        assert pssst.list() == ["all", "xyz"]

    def test_user_name_invalid(self):
        """
        Tests if an user name is invalid.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.find("test !")

        assert ex.value.message == "User name invalid"


class TestBox:
    """
    Tests box with this test cases:

    * Box create
    * Box create failed, name restricted
    * Box create failed, already exists
    * Box delete
    * Box delete failed, name restricted
    * Box push
    * Box pull
    * Box was deleted
    * Box not found
    * Box name invalid

    Methods
    -------
    test_create_box()
        Tests if a box can be created.
    test_create_box_name_restricted()
        Tests if a box name is restricted.
    test_create_box_already_exists()
        Tests if a box already exists.
    test_delete_box()
        Tests if a box can be deleted.
    test_delete_box_name_restricted()
        Tests if a box name is restricted.
    test_push_box()
        Tests if message could be pushed.
    test_pull_box()
        Tests if message could be pulled.
    test_box_was_deleted()
        Tests if a box was deleted.
    test_box_not_found()
        Tests if a box is not found.
    test_box_name_invalid()
        Tests if a box name is invalid.

    """
    def test_create_box(self):
        """
        Tests if a box can be created.
        """
        pssst = Pssst(createUserName())
        pssst.create()
        pssst.create("test")

    def test_create_box_name_restricted(self):
        """
        Tests if a box name is restricted.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.create()
            pssst.create("all")

        assert ex.value.message == "Box name restricted"

    def test_create_box_already_exists(self):
        """
        Tests if a box already exists.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.create()
            pssst.create("test")
            pssst.create("test")

        assert ex.value.message == "Box already exists"

    def test_delete_box(self):
        """
        Tests if a box can be deleted.
        """
        pssst = Pssst(createUserName())
        pssst.create()
        pssst.create("test")
        pssst.delete("test")

    def test_delete_box_name_restricted(self):
        """
        Tests if a box name is restricted.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.create()
            pssst.delete("all")

        assert ex.value.message == "Box name restricted"

    def test_push_box(self):
        """
        Tests if message could be pushed.
        """
        name1 = createUserName()
        name2 = createUserName()

        pssst1 = Pssst(name1)
        pssst1.create()

        pssst2 = Pssst(name2)
        pssst2.create()
        pssst2.push([name1], "test")

    def test_pull_box(self):
        """
        Tests if message could be pulled.
        """
        pssst = Pssst(createUserName())
        pssst.create()
        pssst.pull()

    def test_box_was_deleted(self):
        """
        Tests if a box was deleted.
        """
        with pytest.raises(Exception) as ex:
            name1 = createUserName()
            name2 = createUserName()

            pssst1 = Pssst(name1)
            pssst1.create()
            pssst1.delete()

            pssst2 = Pssst(name2)
            pssst2.create()
            pssst2.push([name1], "test")

        assert ex.value.message == "User was deleted"

    def test_box_not_found(self):
        """
        Tests if a box is not found.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.create()
            pssst.pull("test")

        assert ex.value.message == "Box not found"

    def test_box_name_invalid(self):
        """
        Tests if a box name is invalid.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.create()
            pssst.pull("test !")

        assert ex.value.message == "Box name invalid"


class TestPssst:
    """
    Tests pssst with this test cases:

    * Push self
    * Push single
    * Push multi
    * Push failed, user name invalid
    * Pull empty
    * Password wrong

    Methods
    -------
    test_push_self()
        Tests if a message could be pushed to sender.
    test_push_single()
        Tests if a message could be pushed to receiver.
    test_push_multi()
        Tests if a message could be pushed to many receivers.
    test_push_user_name_invalid()
        Tests if user name is invalid.
    test_pull_empty()
        Tests if box is empty.
    test_password_wrong()
        Tests if password is wrong.

    """
    def test_push_self(self):
        """
        Tests if a message could be pushed to sender.
        """
        name = createUserName()
        text = "Hello World !"

        pssst = Pssst(name)
        pssst.create()
        pssst.push([name], text)

        assert text == pssst.pull()

    def test_push_single(self):
        """
        Tests if a message could be pushed to receiver.
        """
        name1 = createUserName()
        name2 = createUserName()
        text = "Hello World !"

        pssst1 = Pssst(name1)
        pssst1.create()

        pssst2 = Pssst(name2)
        pssst2.create()
        pssst2.push([name1], text)

        assert text == pssst1.pull()

    def test_push_multi(self):
        """
        Tests if a message could be pushed to many receivers.
        """
        send = createUserName()
        text = "Hello World !"

        names = [createUserName() for i in range(5)]

        for name in names:
            pssst = Pssst(name)
            pssst.create()

        pssst = Pssst(send)
        pssst.create()
        pssst.push(names, text)

        for name in names:
            pssst = Pssst(name)

            assert text == pssst.pull()

    def test_push_user_name_invalid(self):
        """
        Tests if user name is invalid.
        """
        with pytest.raises(Exception) as ex:
            pssst = Pssst(createUserName())
            pssst.push(["test !"], "test")

        assert ex.value.message == "User name invalid"

    def test_pull_empty(self):
        """
        Tests if box is empty.
        """
        pssst = Pssst(createUserName())
        pssst.create()

        assert None == pssst.pull()

    def test_password_wrong(self):
        """
        Tests if password is wrong.
        """
        with pytest.raises(Exception) as ex:
            name = createUserName()
            Pssst(name, "right")
            Pssst(name, "wrong")

        assert ex.value.message == "Password wrong"


class TestFuzzy:
    """
    Tests with fuzzy data.

    Methods
    -------
    test_fuzzy()
        Tests if fuzzy data is returned correctly.

    """
    def test_fuzzy(self):
        """
        Tests if fuzzy data is returned correctly.

        * Test 1K random data
        * Test 2K random data
        * Test 4K random data
        * Test 8K random data
        """
        for size in [2 ** n for n in range(10, 13)]:
            blob = os.urandom(size)
            name = createUserName()
            pssst = Pssst(name)
            pssst.create()
            pssst.push([name], blob)

            assert blob == pssst.pull()


def main(script, *args):
    """
    Starts unit testing.

    Parameters
    ----------
    param script : string
        Full script path.
    param args : tuple of strings, optional
        All remaining arguments.

    """
    pytest.main(["-x", script])


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
