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
import json
import os
import re
import sys
import time
import urllib


from httplib import HTTPConnection
from zipfile import ZipFile


try:
    from Crypto import Random
    from Crypto.Cipher import AES, PKCS1_OAEP
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

    def _cipher(self, seed):
        key = PBKDF2(seed[:32], seed[32:], 32 + 16, 1000)
        aes = AES.new(key[:32], AES.MODE_CBC, key[32:])

        return aes

    def export(self, private=False, password=None):
        if not private:
            return self.key.publickey().exportKey("PEM")
        else:
            return self.key.exportKey("PEM", password)

    def encrypt(self, data):
        seed = Random.get_random_bytes(64)
        data = PKCS5.pad(data)
        data = self._cipher(seed).encrypt(data)
        code = PKCS1_OAEP.new(self.key).encrypt(seed)

        return (data, code)

    def decrypt(self, data, code):
        seed = PKCS1_OAEP.new(self.key).decrypt(code)
        data = self._cipher(seed).decrypt(data)
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

    def list(self):
        return self.file.namelist()

    def load(self, key):
        return self.file.read(key)

    def save(self, key, value):
        self.file.writestr(key, value)

    @staticmethod
    def exists(name):
        return os.path.exists(".pssst.%s" % name)

    @staticmethod
    def delete(name):
        if User.exists(name):
            os.remove(".pssst.%s" % name)


class Name:

    @staticmethod
    def expand(name):
        if re.match("^pssst(\.[a-z0-9]{2,63}){1,2}$", name.lower()):
            name = name.replace("pssst.", "", 1)
            return name.split(".", 1) if "." in name else (name, None)
        else:
            return (None, None)

    @staticmethod
    def collapse(user, box, extern=False):
        format = "pssst.%s/%s" if extern else "%s/%s"

        return format % (user, box) if box else user


class Pssst:

    def __init__(self, name, password=None):
        if os.path.exists(".pssst"):
            self.host = open(".pssst", "r").read().strip()
        else:
            self.host = "api.pssst.name"

        user, box = Name.expand(name)

        if not user:
            raise Exception("User name invalid")

        self.user = User(user, password)
        self.user.save("pssst", self._static("key"))

    def _request(self, method, url, body={}):
        body = str(json.dumps(body, separators=(",", ":")))

        if hasattr(self, "user"):
            sec, sig = self.user.key.sign(body)
        else:
            sec, sig = 0, ""

        server = HTTPConnection(self.host)
        server.request(method.upper(), "/user/%s" % urllib.quote(url), body, {
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

        if response.status not in [200, 201, 204]:
            raise Exception(body)

        return body

    def _static(self, file):
        server = HTTPConnection(self.host)
        server.request("GET", "/%s" % file)

        response = server.getresponse()

        if response.status == 404:
            raise Exception("File not found")

        return response.read()

    def create(self, box=None):
        body = {} if box else {"key": self.user.key.export()}

        self._request("post", Name.collapse(self.user.name, box), body)

    def delete(self, box=None):
        self._request("delete", Name.collapse(self.user.name, box))

        if not box:
            User.delete(self.user.name)

    def find(self, user):
        return self._request("get", "%s/key" % user)

    def push(self, names, message):
        for user, box in [Name.expand(name) for name in names]:

            if not user:
                raise Exception("User name invalid")

            if user not in self.user.list():
                self.user.save(user, self.find(user))

            data, code = Key((self.user.load(user),)).encrypt(message)

            self._request("put", Name.collapse(user, box), {
                "code": Base64.encode(code),
                "data": Base64.encode(data),
                "meta": {
                    "from": self.user.name,
                    "time": int(round(time.time()))
                }
            })

    def pull(self, box=None):
        data = self._request("get", Name.collapse(self.user.name, box))

        if not data:
            return (None, None)
        else:
            return (self.user.key.decrypt(
                Base64.decode(data["data"]),
                Base64.decode(data["code"])
            ), data["meta"])


def main(script, command="--help", user=None, receiver=None, *message):
    """
    Usage: %s COMMAND USER[:PASSWORD] RECEIVER MESSAGE

    -c --create      Creates an user or a box
    -x --delete      Deletes an user or a box

    -e --encrypt     Encrypts a message
    -d --decrypt     Decrypts a message

    -h --help        Shows this text
    -l --license     Shows license
    -v --version     Shows version
    """
    try:
        if command in ("-h", "--help"):
            return main.__doc__.lstrip() % os.path.basename(script)

        if command in ("-l", "--license"):
            return __doc__.strip()

        if command in ("-v", "--version"):
            return "Pssst! CLI 0.1.0"

        if not user:
            return "Please specify the user."

        if user.count(":") > 0:
            name, password = user.split(":", 1)
        else:
            name, password = user, None

        if name.count(".") > 1:
            user, box = name.rsplit(".", 1)
        else:
            user, box = name, None

        if command in ("-c", "--create"):
            Pssst(user, password).create(box)
            return "Created: %s" % name

        if command in ("-x", "--delete"):
            Pssst(user, password).delete(box)
            return "Deleted: %s" % name

        if command in ("-d", "--decrypt"):
            message, meta = Pssst(user, password).pull(box)

            if message and meta:
                return "pssst.%s: %s" % (meta["from"], message)
            else:
                return "No messages"

        if not receiver:
            return "Please specify the receiver."

        if command in ("-e", "--encrypt"):
            Pssst(user, password).push([receiver], " ".join(message))
            return "Message sent"

        print "Unknown command:", command
        print "Please use -h for help on commands."

    except KeyboardInterrupt:
        print "Exit"

    except Exception, ex:
        return "Error: %s" % ex


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
