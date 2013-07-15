#!/usr/bin/env python
"""
Pssst! Einfach. Sicher.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

You should have received a copy of the GNU General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

Christian Uhsat <christian@uhsat.de>
"""
import json
import os
import re
import sys

from httplib import HTTPConnection
from zipfile import ZipFile


# The Python Cryptography Toolkit

try:
    from Crypto import Random
    from Crypto.Cipher import AES
    from Crypto.PublicKey import RSA

except ImportError:
    sys.exit("Requires PyCrypto (https://github.com/dlitz/pycrypto)")


__all__ = ["Pssst"]


class PKCS5:
    @staticmethod
    def pad(block):
        return block+(16-len(block)%16)*chr(16-len(block)%16)

    @staticmethod
    def cut(block):
        return block[0:-ord(block[-1])]


class Key:
    def __init__(self, data=None, size=2048):
        if data:
            self.key = RSA.importKey(data)
        else:
            self.key = RSA.generate(size)

    def export(self, private=False):
        if not private:
            return self.key.publickey().exportKey()
        else:
            return self.key.exportKey()

    def cipher(self, key, iv="0123456789ABCDEF"):
        return AES.new(key, AES.MODE_CBC, iv)

    def encrypt(self, data, key):
        code = Random.get_random_bytes(32)

        data = PKCS5.pad(data)
        data = self.cipher(code).encrypt(data)
        code = RSA.importKey(key).encrypt(code, 32)[0]

        return (data, code)

    def decrypt(self, data, code):
        code = self.key.decrypt(code)
        data = self.cipher(code).decrypt(data)
        data = PKCS5.cut(data)

        return data


class User:
    def __init__(self, name):
        create = not User.exists(name)

        self.file = ZipFile(".pssst.%s" % name, "a")
        self.name = name

        if not create:
            self.key = Key(self.load(name + ".private"))
        else:
            self.key = Key()

            self.save(name + ".private", self.key.export(True))
            self.save(name, self.key.export())

    @staticmethod
    def exists(name):
        return os.path.exists(".pssst.%s" % name)

    @staticmethod
    def simple(name):
        if re.match("^pssst\.[a-z0-9]{2,63}$", name):
            return name[6:].lower()

    def list(self):
        return self.file.namelist()

    def load(self, name):
        return self.file.read(name)

    def save(self, name, value):
        self.file.writestr(name, value)


class Pssst:
    def __init__(self, name):
        if os.path.exists(".pssst"):
            self.host = open(".pssst", "r").read().strip()
        else:
            self.host = "api.pssst.name"

        name = User.simple(name)

        if not name:
            raise Exception("User name invalid.")

        found, data = self.find(name)

        if User.exists(name) != found:
            raise Exception("User doesn't exist")

        self.user = User(name)

        if not found:
            created, data = self.create(self.user)

            if not created:
                raise Exception(data)

    def request(self, host, method, url, body={}):
        server = HTTPConnection(host)
        server.request(method, "/user/" + url, json.dumps(body), {
            "Content-Type": "application/json" if body else "text/plain"
        })

        response = server.getresponse()

        mime = response.getheader("content-type", "text/plain")
        body = response.read()

        if body and mime.startswith("application/json"):
            body = json.loads(body)

        return (response.status in [200, 201], body)

    def POST(self, path, data=None):
        return self.request(self.host, "POST", path, data)

    def PUT(self, path, data=None):
        return self.request(self.host, "PUT", path, data)

    def GET(self, path, data=None):
        return self.request(self.host, "GET", path, data)

    def create(self, user):
        return self.POST(user.name, {"key": user.key.export()})

    def find(self, name):
        return self.GET("%s/key" % name)

    def push(self, names, message):
        for name in [User.simple(name) for name in names]:

            if not name:
                raise Exception("User name invalid")

            if not name in self.user.list():
                found, data = self.find(name)

                if found:
                    self.user.save(name, data)
                else:
                    raise Exception(data)

            data, code = self.user.key.encrypt(message, self.user.load(name))

            self.PUT(name, {
                "from": self.user.name,
                "data": data.encode("base64"),
                "code": code.encode("base64")
            })

    def pull(self):
        found, data = self.GET(self.user.name)

        if found and data:
            return (data["from"],
                self.user.key.decrypt(
                    data["data"].decode("base64"),
                    data["code"].decode("base64")
            ))
        else:
            return (None, None)


def main(script, command="--help", user=None, receiver=None, *message):
    """
    Usage: %s COMMAND [USER] [RECEIVER] [MESSAGE]

    -c --create      Creates the user (local and remote).
    -d --decrypt     Decrypts a message by the user.
    -e --encrypt     Encrypts a message by the user.
    -h --help        Shows this text.
    -l --license     Shows license.
    -v --version     Shows version.

    Name format: pssst.[a-z0-9]
    """
    try:
        if command in ("-v", "--version"):
            return "Pssst! cli 0.1.0"

        if command in ("-l", "--license"):
            return __doc__.strip()

        if command in ("-h", "--help"):
            return main.__doc__.lstrip() % os.path.basename(script)

        if not user:
            return "Please specify the user."

        if command in ("-c", "--create"):
            Pssst(user)
            return "User created: %s" % user

        if command in ("-d", "--decrypt"):
            name, message = Pssst(user).pull()
            return "Message pulled: %s - pssst.%s" % (message, name)

        if not receiver:
            return "Please specify the receiver."

        if command in ("-e", "--encrypt"):
            Pssst(user).push([receiver], " ".join(message))
            return "Message pushed"

        print "Unknown command:", command
        print "Please use -h for help on commands."

    except KeyboardInterrupt:
        print "Exit"

    except Exception, cause:
        return "Error: " + str(cause)


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
