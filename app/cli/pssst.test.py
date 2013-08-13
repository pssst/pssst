#!/usr/bin/env python
"""
Pssst!

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

Christian & Christian <pssst@pssst.name>
"""
import os
import random
import string
import sys


from pssst import Pssst, User


try:
    import pytest

except ImportError:
    sys.exit("Requires py.test (http://pytest.org)")


def generateName(size=32):
    pool = string.ascii_lowercase + string.digits
    return "pssst." + "".join(random.choice(pool) for c in range(size))


class TestCrypto:

    def test_verify_user_not_found(self):
        assert True # TODO

    def test_verify_hash_invalid(self):
        assert True # TODO

    def test_verify_failed(self):
        assert True # TODO


class TestFile:

    def test_file_not_found(self):
        assert True # TODO

    def test_file(self):
        assert True # TODO


class TestUser:

    def test_user_name_invalid(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.find("pssst.test !")

        assert ex.value.message == "User name invalid"

    def test_user_not_found(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.find("null")

        assert ex.value.message == "User not found"

    def test_create_user_name_restricted(self):
        assert True # TODO

    def test_create_user_already_exists(self):
        assert True # TODO

    def test_create(self):
        assert True # TODO

    def test_delete_user_name_restricted(self):
        assert True # TODO

    def test_delete(self):
        assert True # TODO

    def test_find(self):
        user = Pssst(generateName())
        user.create()

        key = user.user.key.export()

        assert user.find(user.user.name) == key


class TestBox:

    def test_box_name_invalid(self):
        assert True # TODO

    def test_user_was_deleted(self):
        assert True # TODO

    def test_box_not_found(self):
        assert True # TODO

    def test_create_box_name_restricted(self):
        assert True # TODO

    def test_create_box_already_exists(self):
        assert True # TODO

    def test_create(self):
        assert True # TODO

    def test_delete_box_name_restricted(self):
        assert True # TODO

    def test_delete(self):
        assert True # TODO

    def test_push(self):
        assert True # TODO

    def test_pull(self):
        assert True # TODO


class TestPssst:
    pass


class TestFuzzy:

    def fuzzy(self, data):
        name = generateName()

        user = Pssst(name)
        user.create()
        user.push([name], data)

        return user.pull()[0]

    def test_fuzzy_1kb(self):
        data = os.urandom(1024)
        assert self.fuzzy(data) == data

    def test_fuzzy_2kb(self):
        data = os.urandom(2048)
        assert self.fuzzy(data) == data

    def test_fuzzy_4kb(self):
        data = os.urandom(4096)
        assert self.fuzzy(data) == data

    def test_fuzzy_8kb(self):
        data = os.urandom(8192)
        assert self.fuzzy(data) == data


def main(script, mode="all"):
    """
    Usage: %s [fast|all]
    """
    print main.__doc__.lstrip() % os.path.basename(script)

    try:
        pass # TODO

    except KeyboardInterrupt:
        print "Exit"


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
