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


from pssst import Base64, PKCS5, Key, User, Pssst


try:
    import pytest

except ImportError:
    sys.exit("Requires py.test (http://pytest.org)")


def generateName(size=32):
    pool = string.ascii_lowercase + string.digits
    name = "".join(random.choice(pool) for x in range(size))

    return "pssst.%s" % name


class TestBase64:

    def test_base64_encode(self):
        assert Base64.encode("Hello World!") == "SGVsbG8gV29ybGQh"

    def test_base64_decode(self):
        assert Base64.decode("SGVsbG8gV29ybGQh") == "Hello World!"


class TestPKCS5:

    def test_pkcs5_pad(self):
        assert PKCS5.pad("Hello World!") == "Hello World!\x04\x04\x04\x04"

    def test_pkcs5_cut(self):
        assert PKCS5.cut("Hello World!\x04\x04\x04\x04") == "Hello World!"


class TestCrypto:

    def test_verify_user_not_found(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.pull()

        assert ex.value.message == "Verification failed"

    def test_verify_hash_invalid(self):
        original = Key.sign

        with pytest.raises(Exception) as ex:
            Key.sign = lambda self, data: ("!", "!")

            user = Pssst(generateName())
            user.create()
            user.pull()

        Key.sign = original

        assert ex.value.message == "Verification failed"

    def test_verify_failed(self):
        original = Key.sign

        with pytest.raises(Exception) as ex:
            Key.sign = lambda self, data: original(self, "test")

            user = Pssst(generateName())
            user.create()
            user.pull()

        Key.sign = original

        assert ex.value.message == "Verification failed"


class TestFile:

    def test_file_not_found(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user._static("null")

        assert ex.value.message == "File not found"

    def test_file(self):
        user = Pssst(generateName())
        file = user._static("key")

        assert "BEGIN PUBLIC KEY" in file


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
        if os.path.exists(".pssst.name"):
            os.remove(".pssst.name")

        with pytest.raises(Exception) as ex:
            user = Pssst("pssst.name")
            user.create()

        assert ex.value.message == "User name restricted"

    def test_create_user_already_exists(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.create()
            user.create()

        assert ex.value.message == "User already exists"

    def test_create(self):
        user = Pssst(generateName())
        user.create()

    def test_delete(self):
        user = Pssst(generateName())
        user.create()
        user.delete()

    def test_find_user_was_deleted(self):
        with pytest.raises(Exception) as ex:
            name = generateName()
            user = Pssst(name)
            user.create()
            user.delete()

            name = name.replace("pssst.", "", 1)
            user = Pssst(generateName())
            user.find(name)

        assert ex.value.message == "User was deleted"

    def test_find(self):
        name = generateName()
        user = Pssst(name)
        user.create()
        user.find(name.replace("pssst.", "", 1))


class TestBox:

    def test_box_name_invalid(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.create()
            user.pull("test !")

        assert ex.value.message == "Box name invalid"

    def test_user_was_deleted(self):
        with pytest.raises(Exception) as ex:
            send = generateName()
            recv = generateName()

            user = Pssst(recv)
            user.create()
            user.delete()

            user = Pssst(send)
            user.create()
            user.push([recv], "test")

        assert ex.value.message == "User was deleted"

    def test_box_not_found(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.create()
            user.pull("test")

        assert ex.value.message == "Box not found"

    def test_create_box_name_restricted(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.create()
            user.create("all")

        assert ex.value.message == "Box name restricted"

    def test_create_box_already_exists(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.create()
            user.create("test")
            user.create("test")

        assert ex.value.message == "Box already exists"

    def test_create(self):
        user = Pssst(generateName())
        user.create()
        user.create("test")

    def test_delete_box_name_restricted(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.create()
            user.delete("all")

        assert ex.value.message == "Box name restricted"

    def test_delete(self):
        user = Pssst(generateName())
        user.create()
        user.create("test")
        user.delete("test")

    def test_push(self):
        send = generateName()
        recv = generateName()

        Pssst(recv).create()

        user = Pssst(send)
        user.create()
        user.push([recv], "test")

    def test_pull(self):
        user = Pssst(generateName())
        user.create()
        user.pull()


class TestPssst:

    def test_pssst_user_name_invalid(self):
        with pytest.raises(Exception) as ex:
            Pssst("pssst.test !")

        assert ex.value.message == "User name invalid"

    def test_pssst_password_wrong(self):
        with pytest.raises(Exception) as ex:
            name = generateName()
            Pssst(name, "right")
            Pssst(name, "wrong")

        assert ex.value.message == "Password wrong"

    def test_pssst_static_file_not_found(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user._static("null")

        assert ex.value.message == "File not found"

    def test_pssst_push_user_name_invalid(self):
        with pytest.raises(Exception) as ex:
            user = Pssst(generateName())
            user.push(["pssst.test !"], "test")

        assert ex.value.message == "User name invalid"

    def test_pssst_empty(self):
        user = Pssst(generateName())
        user.create()

        data, meta = user.pull()

        assert data == None
        assert meta == None

    def test_pssst_self(self):
        name = generateName()
        text = "Hello World !"

        user = Pssst(name)
        user.create()
        user.push([name], text)

        data, meta = user.pull()

        assert "pssst.%s" % meta["from"] == name
        assert data == text

    def test_pssst_single(self):
        send = generateName()
        recv = generateName()
        text = "Hello World !"

        user = Pssst(recv)
        user.create()

        user = Pssst(send)
        user.create()
        user.push([recv], text)

        data, meta = Pssst(recv).pull()

        assert "pssst.%s" % meta["from"] == send
        assert data == text

    def test_pssst_multi(self):
        send = generateName()
        text = "Hello World !"

        recvs = [generateName() for i in range(5)]

        for recv in recvs:
            user = Pssst(recv)
            user.create()

        user = Pssst(send)
        user.create()
        user.push(recvs, text)

        for recv in recvs:
            data, meta = Pssst(recv).pull()

            assert "pssst.%s" % meta["from"] == send
            assert data == text


class TestFuzzy:

    def fuzzy(self, data):
        name = generateName()

        user = Pssst(name)
        user.create()
        user.push([name], data)

        return user.pull()[0]

    def test_fuzzy(self):
        for size in [2 ** n for n in range(10, 14)]:
            data = os.urandom(size)
            assert self.fuzzy(data) == data


if __name__ == "__main__":
    sys.exit(pytest.main(["-x"]))
