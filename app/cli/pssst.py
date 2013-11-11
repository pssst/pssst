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
import json
import os
import re
import sys
import time
import urllib

from base64 import b64encode, b64decode
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


class Name:
    """
    Pssst! class for canonical name parsing.

    """
    def __init__(self, user, box=None, password=None):
        """
        Inits the instance with the parsed name.

        Parameters
        ----------
        param user : string
            User name (full or partly).
        param box : string, optional (default is None)
            Box name.
        param password : string, optional (default is None)
            User private key password.

        """
        user = user.strip()

        if not re.match("^(pssst\.)?\w{2,63}(\.\w{2,63})?(:\S*)?$", user):
            raise Exception("User name invalid")

        if user.startswith("pssst."):
            user = user[6:]

        if ":" in user and not password:
            user, password = user.rsplit(":", 1)

        if "." in user and not box:
            user, box = user.rsplit(".", 1)

        self.user = user.lower()
        self.box = box.lower() if box else box
        self.password = password

        self.all = (self.user, self.box)

        if self.box:
            self.path = "%s/%s/" % self.all
        else:
            self.path = "%s/" % self.user

    def __str__(self):
        """
        Returns the full name in canonical notation.

        """
        if self.box:
            return "pssst.%s.%s" % self.all
        else:
            return "pssst.%s" % self.user


class Pssst:
    """
    Pssst! class for API communication.

    Methods
    -------
    create(box=None)
        Creates an user or a box.
    delete(box=None)
        Deletes an user or a box.
    find(user)
        Returns the public key of an user.
    list(user)
        Returns all boxes of an user.
    pull(box=None)
        Pulls a message from a box.
    push(names, message)
        Pushes a message onto a box.

    """
    class User:
        """
        Class for storing the user private and public keys.

        Methods
        -------
        delete()
            Deletes the user file.
        list()
            Returns a list of all known user names.
        load(user)
            Returns a user key.
        save(user, key)
            Saves a user key.

        Notes
        -----
        This class is not meant to be called externally.

        """
        def __init__(self, user, password):
            path = ".pssst." + user

            exists = os.path.exists(path)

            self.file = ZipFile(path, "a")
            self.path = path
            self.name = user

            if exists:
                self.key = Pssst.Key(self.load(user + ".private"), password)
            else:
                self.key = Pssst.Key()

                self.save(user + ".private", self.key.private(password))
                self.save(user, self.key.public())

        def delete(self):
            if os.path.exists(self.path):
                os.remove(self.path)

        def list(self):
            return self.file.namelist()

        def load(self, user):
            return self.file.read(user)

        def save(self, user, key):
            self.file.writestr(user, key)


    class Key:
        """
        Class for providing cryptographical methods.

        Methods
        -------
        private(password=None)
            Returns the users private key (PEM format).
        public()
            Returns the users public key (PEM format).
        encrypt(data)
            Returns the data encrypted.
        decrypt(data, code)
            Returns the data decrypted.
        verify(data, timestamp, signature)
            Returns if data timestamp and signature could be verified.
        sign(data)
            Returns the data timestamp and signature.

        Notes
        -----
        This class is not meant to be called externally.

        """
        def __init__(self, key=None, password=None):
            try:
                if key:
                    self.key = RSA.importKey(key, password)
                else:
                    self.key = RSA.generate(2048)

            except (IndexError, TypeError, ValueError) as ex:
                raise Exception("Password wrong")

        @staticmethod
        def __cipher(code, size=32):
            data = PBKDF2(code[:size], code[size:], size + 16)

            key = data[:size] # 32 bytes for key
            iv  = data[size:] # 16 bytes for IV

            return AES.new(key, AES.MODE_CBC, iv)

        @staticmethod # PKCS5
        def __pad(data, size=AES.block_size):
            return data + (size-len(data) % size) * chr(size-len(data) % size)

        @staticmethod # PKCS5
        def __cut(data):
            return data[0:-ord(data[-1])]

        def private(self, password=None):
            return self.key.exportKey("PEM", password)

        def public(self):
            return self.key.publickey().exportKey("PEM")

        def encrypt(self, data):
            code = Random.get_random_bytes(64)

            data = Pssst.Key.__pad(data)
            data = Pssst.Key.__cipher(code).encrypt(data)

            code = PKCS1_OAEP.new(self.key).encrypt(code)

            return (data, code)

        def decrypt(self, data, code):
            code = PKCS1_OAEP.new(self.key).decrypt(code)

            data = Pssst.Key.__cipher(code).decrypt(data)
            data = Pssst.Key.__cut(data)

            return data

        def verify(self, data, timestamp, signature):
            actual = int(round(time.time()))

            hmac = HMAC.new(str(timestamp), data, SHA256)
            hmac = SHA256.new(hmac.digest())

            if (actual -5) < timestamp < (actual +5):
                verified = PKCS1_v1_5.new(self.key).verify(hmac, signature)
            else:
                verified = False

            return verified

        def sign(self, data):
            timestamp = int(round(time.time()))

            hmac = HMAC.new(str(timestamp), data, SHA256)
            hmac = SHA256.new(hmac.digest())

            signature = PKCS1_v1_5.new(self.key).sign(hmac)

            return (timestamp, signature)


    def __init__(self, name, password=None):
        """
        Inits the instance with an user object.

        Parameters
        ----------
        param name : string
            User name.
        param password : string, optional (default is None)
            User private key password.

        Notes
        -----
        If a file in the current directory with the name '.pssst' exists, the
        content of this file is parsed and used as the API server address and
        port.

        """
        if os.path.exists(".pssst"):
            self.api = open(".pssst", "r").read().strip()
        else:
            self.api = "api.pssst.name"

        self.user = Pssst.User(Name(name).user, password)
        self.user.save("pssst", self.__file("key"))

    def __api(self, method, url, body={}):
        """
        Sends an API request (sign and verify).

        Parameters
        ----------
        param method : string
            Request method.
        param url : string
            Request path.
        param body : JSON, optional (default is {})
            Request body.

        Returns
        -------
        string
            The response body.

        Raises
        ------
        Exception
            Because the verification is failed.

        """
        body = str(json.dumps(body, separators=(",", ":")))

        timestamp, signature = self.user.key.sign(body)

        server = HTTPConnection(self.api)
        server.request(method, "/user/" + urllib.quote(url), body, {
            "content-type": "application/json" if body else "text/plain",
            "content-hash": "%s; %s" % (timestamp, b64encode(signature))
        })

        response = server.getresponse()

        mime = response.getheader("content-type", "text/plain")
        head = response.getheader("content-hash")
        body = response.read()

        if not re.match("^[0-9]+; ?[A-Za-z0-9\+/]+=*$", head):
            raise Exception("Verification failed")

        timestamp, signature = head.split(";", 1)
        timestamp, signature = int(timestamp), b64decode(signature)

        pssst = Pssst.Key(self.user.load("pssst"))

        if not pssst.verify(body, timestamp, signature):
            raise Exception("Verification failed")

        if body and mime.startswith("application/json"):
            body = json.loads(body)

        if response.status not in [200, 204]:
            raise Exception(body)

        return body

    def __file(self, file):
        """
        Sends a file request with out any checks.

        Parameters
        ----------
        param file : string
            Requested file.

        Returns
        -------
        string
            The file content.


        Raises
        ------
        Exception
            Because the file was not found.

        """
        server = HTTPConnection(self.api)
        server.request("GET", "/" + file)

        response = server.getresponse()

        if response.status == 404:
            raise Exception("File not found")

        return response.read()

    def create(self, box=None):
        """
        Creates an user or a box.

        Parameters
        ----------
        param box : string, optional (default is None)
            Name of the users box.

        """
        if not box:
            body = {"key": self.user.key.public()}
        else:
            body = {}

        self.__api("POST", Name(self.user.name, box).path, body)

    def delete(self, box=None):
        """
        Deletes an user or a box.

        Parameters
        ----------
        param box : string, optional (default is None)
            Name of the users box.

        """
        self.__api("DELETE", Name(self.user.name, box).path)

        if not box:
            self.user.delete() # Delete user file

    def find(self, user):
        """
        Returns the public key of an user.

        Parameters
        ----------
        param user : string
            Name of the user.

        Returns
        -------
        string
            PEM formated public key.

        """
        return self.__api("GET", user + "/key")

    def list(self):
        """
        Returns all boxes of an user.

        Parameters
        ----------
        param user : string
            Name of the user.

        Returns
        -------
        list of strings
            List of user boxes.

        """
        return self.__api("GET", self.user.name + "/list")

    def pull(self, box=None):
        """
        Pulls a message from a box.

        Parameters
        ----------
        param box : string, optional (default is None)
            Name of the users box.

        Returns
        -------
        string
            The message.

        """
        body = self.__api("GET", Name(self.user.name, box).path)

        if not body:
            return None # Box is empty

        data = b64decode(body["data"])
        code = b64decode(body["code"])

        return self.user.key.decrypt(data, code)

    def push(self, names, message):
        """
        Pushes a message onto a box.

        Parameters
        ----------
        param names : list of strings
            List of user names.
        param message : string
            The message.

        """
        for user, box in [Name(name).all for name in names]:

            if user not in self.user.list():
                self.user.save(user, self.find(user)) # Add public key

            data, code = Pssst.Key(self.user.load(user)).encrypt(message)

            body = {
                "code": b64encode(code),
                "data": b64encode(data),
                "from": self.user.name
            }

            self.__api("PUT", Name(user, box).path, body)


def color(usage):
    """
    Prints the usage colored.

    Parameters
    ----------
    param usage : string
        Plain usage to color.

    """
    for line in usage.split("\n")[1:-1]:
        line = line[4:]

        # Color description
        if re.match("^  CLI", line):
            line = line.replace("version", "version\x1B[34;1m")
            line = "\x1B[39;1m%s\x1B[0m" % line

        # Color list titles
        elif re.match("^[A-Za-z ]+:$", line):
            line = "\x1B[34m%s\x1B[0m" % line

        # Color list points
        elif re.match("^  (-.|[a-z]+)", line):
            line = line.replace("   ", "   \x1B[37;0m")
            line = "\x1B[34;1m%s\x1B[0m" % line

        print line


def main(script, command="--help", user=None, receiver=None, *message):
    """
          ________               ___  ___
         /  ___  /______________/  /_/  /
        /  /__/ / ___/ ___/ ___/  __/  /
       /  _____/__  /__  /__  /  /_/__/
      /__/    /____/____/____/\___/__/

      CLI version 0.2.0

    Usage:
      %s [option|command user:password] [receiver] [message]

    Options:
      -h --help      Shows this text
      -l --license   Shows license
      -v --version   Shows version

    Available commands:
      create   Create an user or box
      delete   Delete an user or box
      list     List all boxes
      pull     Pull a message
      push     Push a message

    Report bugs to <pssst@pssst.name>
    """
    try:
        if command in ("-h", "--help"):
            return color(main.__doc__ % os.path.basename(script))

        if command in ("-l", "--license"):
            return __doc__.strip()

        if command in ("-v", "--version"):
            return "Pssst! CLI 0.2.0"

        if not user:
            return "Please specify the user."

        name = Name(user)

        if command in ("--create", "create"):
            Pssst(name.user, name.password).create(name.box)
            return "Created: %s" % name

        if command in ("--delete", "delete"):
            Pssst(name.user, name.password).delete(name.box)
            return "Deleted: %s" % name

        if command in ("--list", "list"):
            boxes = Pssst(name.user, name.password).list()
            return ", ".join(boxes)

        if command in ("--pull", "pull"):
            message = Pssst(name.user, name.password).pull(name.box)
            return message or "No new messages"

        if not receiver:
            return "Please specify the receiver."

        if command in ("--push", "push"):
            Pssst(name.user, name.password).push([receiver],"".join(message))
            return "Message sent"

        print "Unknown command:", command
        print "Please use -h for help on commands."

    except KeyboardInterrupt:
        print "Exit"

    except Exception, ex:
        return "Error: %s" % ex


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
