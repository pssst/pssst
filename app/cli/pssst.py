#!/usr/bin/env python
"""
Pssst!

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
import time

from httplib import HTTPConnection
from zipfile import ZipFile


# The Python Cryptography Toolkit

try:
    from Crypto import Random
    from Crypto.Cipher import AES
    from Crypto.Hash import HMAC, SHA256
    from Crypto.Protocol.KDF import PBKDF2
    from Crypto.PublicKey import RSA
    from Crypto.Signature import PKCS1_v1_5

except ImportError:
    sys.exit("Requires PyCrypto (https://github.com/dlitz/pycrypto)")


__all__ = ["Pssst"]


class Base64:

    @staticmethod
    def encode(data):
        if data:
            data = data.encode("base64")
            data = data.replace("\n", "")

        return data

    @staticmethod
    def decode(data):
        if data:
            data = data.replace("\n", "")
            data = data.decode("base64")

        return data


class PKCS5:

    @staticmethod
    def pad(data, size=AES.block_size):
        return data + (size - len(data) % size) * chr(size - len(data) % size)

    @staticmethod
    def cut(data, size=AES.block_size):
        return data[0:-ord(data[-1])]


class Key:

    def __init__(self, data=None, size=2048):
        try:
            if data:
                self.key = RSA.importKey(*data)
            else:
                self.key = RSA.generate(size)

        except (ValueError, IndexError, TypeError) as ex:
            raise Exception("Password wrong")

    def export(self, private=False, password=None):
        if not private:
            return self.key.publickey().exportKey("PEM")
        else:
            return self.key.exportKey("PEM", password)

    def cipher(self, seed):
        sha = lambda p, s: HMAC.new(p, s, SHA256).digest()
        key = PBKDF2(seed[:32], seed[32:], 32 + 16, 1000, sha)

        return AES.new(key[:32], AES.MODE_CBC, key[32:])

    def encrypt(self, data):
        seed = Random.get_random_bytes(64)
        data = PKCS5.pad(data)
        data = self.cipher(seed).encrypt(data)
        code = self.key.encrypt(seed, 32)[0]

        return (data, code)

    def decrypt(self, data, code):
        seed = self.key.decrypt(code)
        data = self.cipher(seed).decrypt(data)
        data = PKCS5.cut(data)

        return data

    def verify(self, data, sec, sig):
        now = int(round(time.time()))
        mac = HMAC.new(str(sec), data, SHA256).digest()
        mac = SHA256.new(mac)
        pkcs = PKCS1_v1_5.new(self.key)

        if (now + 3) > sec > (now - 3):
            return pkcs.verify(mac, sig)
        else:
            return False

    def sign(self, data):
        now = int(round(time.time()))
        mac = HMAC.new(str(now), data, SHA256).digest()
        mac = SHA256.new(mac)
        pkcs = PKCS1_v1_5.new(self.key)

        return (now, pkcs.sign(mac))


class User:

    def __init__(self, name, password):
        create = not User.exists(name)

        self.file = ZipFile(".pssst.%s" % name, "a")
        self.name = name

        if not create:
            self.key = Key((self.load(".private"), password))
        else:
            self.key = Key()

            self.save(".private", self.key.export(True, password))
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

    def __init__(self, name, password=None):
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

        self.user = User(name, password)
        self.user.save("pssst", self.static("key"))

        if not found:
            created, data = self.create(self.user)

            if not created:
                raise Exception(data)

    def request(self, method, url, body={}):
        body = str(json.dumps(body, separators=(",", ":")))

        if hasattr(self, "user"):
            sec, sig = self.user.key.sign(body)
        else:
            sec, sig = 0, ""

        server = HTTPConnection(self.host)
        server.request(method.upper(), "/user/%s" % url, body, {
            "content-type": "application/json" if body else "text/plain",
            "content-hash": "%s; %s" % (sec, Base64.encode(sig))
        })

        response = server.getresponse()

        mime = response.getheader("content-type", "text/plain")
        sign = response.getheader("content-hash")
        body = response.read()

        if hasattr(self, "user"):
            if not re.match("^[0-9]+; ?[A-Za-z0-9\+/]+=*$", sign):
                raise Exception("Verification failed")

            pssst = Key((self.user.load("pssst"),))

            sec, sig = sign.split(";", 1)
            sec, sig = int(sec), Base64.decode(sig)

            if not pssst.verify(body, sec, sig):
                raise Exception("Verification failed")

        if body and mime.startswith("application/json"):
            body = json.loads(body)

        return (response.status in [200, 201, 204], body)

    def static(self, file):
        server = HTTPConnection(self.host)
        server.request("GET", "/%s" % file)

        return server.getresponse().read()

    def create(self, user):
        return self.request("post", user.name, {"key": user.key.export()})

    def find(self, name):
        return self.request("get", "%s/key" % name)

    def push(self, names, message):
        for name in [User.simple(name) for name in names]:

            if not name:
                raise Exception("User name invalid")

            if name not in self.user.list():
                found, data = self.find(name)

                if found:
                    self.user.save(name, data)
                else:
                    raise Exception(data)

            data, code = Key((self.user.load(name),)).encrypt(message)

            self.request("put", name, {
                "code": Base64.encode(code),
                "data": Base64.encode(data),
                "meta": {
                    "from": self.user.name,
                    "time": int(round(time.time()))
                }
            })

    def pull(self):
        found, data = self.request("get", self.user.name)

        if found:
            if data:
                return (
                    self.user.key.decrypt(
                        Base64.decode(data["data"]),
                        Base64.decode(data["code"])
                    ),
                    data["meta"]
                )
            else:
                return (None, None)
        else:
            raise Exception(data)


def main(script, command="--help", user=None, receiver=None, *message):
    """
    Usage: %s COMMAND USER[:PASSWORD] RECEIVER MESSAGE

    -c --create      Creates the user (local and remote)
    -d --decrypt     Decrypts a message from the user
    -e --encrypt     Encrypts a message from the user
    -h --help        Shows this text
    -l --license     Shows license
    -v --version     Shows version

    Name format: pssst.[a-z0-9]
    """
    try:
        if user and ":" in user:
            name, password = user.split(":", 1)
        else:
            name, password = user, None

        if command in ("-v", "--version"):
            return "Pssst! CLI 0.1.0"

        if command in ("-l", "--license"):
            return __doc__.strip()

        if command in ("-h", "--help"):
            return main.__doc__.lstrip() % os.path.basename(script)

        if not user:
            return "Please specify the user."

        if command in ("-c", "--create"):
            Pssst(name, password)
            return "User created: %s" % user

        if command in ("-d", "--decrypt"):
            message, meta = Pssst(name, password).pull()

            if message and meta:
                return "pssst.%s: %s" % (meta["from"], message)
            else:
                return "No messages"

        if not receiver:
            return "Please specify the receiver."

        if command in ("-e", "--encrypt"):
            Pssst(name, password).push([receiver], " ".join(message))
            return "Message sent"

        print "Unknown command:", command
        print "Please use -h for help on commands."

    except KeyboardInterrupt:
        print "Exit"

    except Exception, cause:
        return "Error: " + str(cause)


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
