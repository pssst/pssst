#!/usr/bin/env python
"""
Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>

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
import base64
import io
import json
import os
import re
import sys
import time

from datetime import datetime
from getpass import getpass
from zipfile import ZipFile

# For compatibility with Python 3

try:
    from urllib.parse import quote
except ImportError:
    from urllib import quote


try:
    from requests import request
    from requests.exceptions import ConnectionError, Timeout
except ImportError:
    sys.exit("Requires Requests (https://github.com/kennethreitz/requests)")


try:
    from Crypto import Random
    from Crypto.Cipher import AES, PKCS1_OAEP
    from Crypto.Hash import HMAC, SHA512
    from Crypto.PublicKey import RSA
    from Crypto.Signature import PKCS1_v1_5
except ImportError:
    sys.exit("Requires PyCrypto (https://github.com/dlitz/pycrypto)")


__all__, __version__, FINGERPRINT = ["Pssst", "Name"], "0.2.19", (
    "474cfaac9f9d6d02ba1fc185cf41b4907c1874a59553fd47fc364273c5a5e60f"
    "33d3c1fe383c0303c5ae0d0cb32064a0d68329dccb80388b56978e44000a3284"
)


def _encode64(data): # Utility shortcut
    return base64.b64encode(data).decode("ascii")


def _decode64(data): # Utility shortcut
    return base64.b64decode(data.encode("ascii"))


class Name:
    """
    Pssst helper class for canonical name parsing.

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

        self.full = (self.user, self.box)

        if self.box:
            self.path = "%s/%s/" % self.full
        else:
            self.path = "%s/" % self.user

    def __repr__(self):
        """
        Returns the full name in canonical notation.

        """
        if self.box:
            return str("pssst.%s.%s" % self.full)
        else:
            return str("pssst.%s" % self.user)


class Pssst:
    """
    Pssst class for API communication.

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
            self.name, test = user, password or "Okay4Test"

            if not re.match("^((?=.*[A-Z])(?=.*[a-z])(?=.*\d).{8,})$", test):
                raise Exception("Password weak")

            if os.path.exists(repr(self)):
                self.key = Pssst.Key(self.load(user + ".private"), password)
            else:
                self.key = Pssst.Key()

                self.save(user + ".private", self.key.private(password))
                self.save(user, self.key.public())

        def __repr__(self):
            return ".pssst." + self.name

        def __bool__(self):
            return self.__nonzero__()

        def __nonzero__(self):
            return os.path.exists(repr(self))

        def delete(self):
            if os.path.exists(repr(self)):
                os.remove(repr(self))

        def list(self):
            with ZipFile(repr(self), "r") as file:
                return file.namelist()

        def load(self, user):
            with ZipFile(repr(self), "r") as file:
                return file.read(user)

        def save(self, user, key):
            with ZipFile(repr(self), "a") as file:
                file.writestr(user, key)


    class Key:
        """
        Class for providing cryptographic methods.

        Methods
        -------
        private(password=None)
            Returns the users private key (PEM format).
        public()
            Returns the users public key (PEM format).
        encrypt(data)
            Returns the encrypted data and once.
        decrypt(data, once)
            Returns the decrypted data.
        verify(data, timestamp, signature, grace=30)
            Returns if data timestamp and signature could be verified.
        sign(data)
            Returns the data timestamp and signature.

        Notes
        -----
        This class is not meant to be called externally.

        """
        size = 4096 # RSA key strengh

        def __init__(self, key=None, password=None):
            try:
                if key:
                    self.key = RSA.importKey(key, password)
                else:
                    self.key = RSA.generate(Pssst.Key.size)

            except (IndexError, TypeError, ValueError) as ex:
                raise Exception("Password wrong")

        def private(self, password=None):
            return self.key.exportKey("PEM", password).decode("ascii")

        def public(self):
            return self.key.publickey().exportKey("PEM").decode("ascii")

        def encrypt(self, data):
            once = Random.get_random_bytes(32 + AES.block_size) # 256 bit key

            data = AES.new(once[:32], AES.MODE_CFB, once[32:]).encrypt(data)
            once = PKCS1_OAEP.new(self.key).encrypt(once)

            return (data, once)

        def decrypt(self, data, once):
            once = PKCS1_OAEP.new(self.key).decrypt(once)
            data = AES.new(once[:32], AES.MODE_CFB, once[32:]).decrypt(data)

            return data

        def verify(self, data, timestamp, signature, grace=30):
            current, data = int(round(time.time())), data.encode("ascii")

            hmac = HMAC.new(str(timestamp).encode("ascii"), data, SHA512)
            hmac = SHA512.new(hmac.digest())

            if (current - grace) < timestamp < (current + grace):
                return PKCS1_v1_5.new(self.key).verify(hmac, signature)
            else:
                return False

        def sign(self, data):
            current, data = int(round(time.time())), data.encode("ascii")

            hmac = HMAC.new(str(current).encode("ascii"), data, SHA512)
            hmac = SHA512.new(hmac.digest())

            signature = PKCS1_v1_5.new(self.key).sign(hmac)

            return (current, signature)


    def __init__(self, name, password=None):
        """
        Inits the instance with an user object.

        Parameters
        ----------
        param name : string
            User name.
        param password : string, optional (default is None)
            User private key password.

        Raises
        ------
        Exception
            Because the server could not be authenticated.
        Exception
            Because the password is required.

        Notes
        -----
        If a file in the current directory with the name '.pssst' exists, the
        content of this file is parsed and used as the API server address and
        port. In this case, the servers SSL certificate will not be verified.

        Because Requests uses a different CA store, which does not has our CA
        stored, the server will not be verified if the official API address is
        used. This is also not necessary, because all server responses will be
        signed and verified in the HTTP 'content-hash' header with the servers
        private key.

        The public key of the official API will be verified against the built-
        in fingerprint.

        A valid password must consist of upper and lower case letters and also
        numbers. The required minimum length of a password is 8 characters. If
        you use the offical API, a password is mandatory.

        """
        if os.path.exists(".pssst"):
            verify, self.api = False, io.open(".pssst").read().strip()
        else:
            verify, self.api = True, "https://api.pssst.name"

        key, fingerprint = self.__get("key"), "".join(FINGERPRINT.split())

        if verify and not fingerprint == SHA512.new(key).hexdigest():
            raise Exception("Server could not be authenticated")

        if verify and not password:
            raise Exception("Password is required")

        self.user = Pssst.User(Name(name).user, password)

        if self.api not in self.user.list():
            self.user.save(self.api, key)

    def __repr__(self):
        """
        Returns the module identifier.

        Returns
        -------
        string
            The module identifier.

        """
        return "Pssst " + __version__

    def __api(self, method, url, body={}):
        """
        Sends an API request (signs and verifies).

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
            Because the user was deleted.
        Exception
            Because the signature is missing.
        Exception
            Because the signature is corrupt.
        Exception
            Because the verification has failed.

        Notes
        -----
        Please see __init__ method.

        """
        if not self.user:
            raise Exception("User was deleted")

        body = str(json.dumps(body, separators=(",", ":")))

        timestamp, signature = self.user.key.sign(body)

        response = request(method, "%s/user/%s" % (self.api, quote(url)),
            data=body,
            headers={
                "content-hash": "%s; %s" % (timestamp, _encode64(signature)),
                "content-type": "application/json" if body else "text/plain",
                "user-agent": repr(self)
            },
            verify=False
        )

        mime = response.headers.get("content-type", "text/plain")
        head = response.headers.get("content-hash")
        body = response.text

        if not re.match("^[0-9]+; ?[A-Za-z0-9\+/]+=*$", head):
            raise Exception("Verification failed")

        timestamp, signature = head.split(";", 1)
        timestamp, signature = int(timestamp), _decode64(signature)

        pssst = Pssst.Key(self.user.load(self.api))

        if not pssst.verify(body, timestamp, signature):
            raise Exception("Verification failed")

        if response.status_code not in [200, 204]:
            raise Exception(body)

        if mime.startswith("application/json"):
            body = response.json()

        return body

    def __get(self, url):
        """
        Sends a GET request (with out any checks).

        Parameters
        ----------
        param url : string
            Requested path.

        Returns
        -------
        string
            The file content.

        Raises
        ------
        ConnectionError
            Because the file was not found.

        Notes
        -----
        Please see __init__ method.

        """
        response = request("GET", "%s/%s" % (self.api, url),
            headers={"user-agent": repr(self)},
            verify=False
        )

        if response.status_code == 404:
            raise ConnectionError("Not Found")

        return response.text

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

        Notes
        -----
        If the user was deleted, the object can not be used any further and
        any API call wil result in an error. The user file is also deleted.

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
        tuple or None
            The name, time and message, None if empty.

        """
        body = self.__api("GET", Name(self.user.name, box).path)

        if not body:
            return None # Box is empty

        meta = body["meta"]

        name = str(meta["name"])
        time = int(meta["time"])

        data = _decode64(body["data"])
        once = _decode64(meta["once"])

        message = self.user.key.decrypt(data, once)

        return (name, time, message)

    def push(self, names, message):
        """
        Pushes a message onto a box.

        Parameters
        ----------
        param names : list of strings
            List of user names.
        param message : byte string
            The message.

        """
        for user, box in [Name(name).full for name in names]:

            if user not in self.user.list():
                self.user.save(user, self.find(user)) # Add public key

            data, once = Pssst.Key(self.user.load(user)).encrypt(message)

            once = _encode64(once)
            data = _encode64(data)

            meta = {
                "name": self.user.name,
                "once": once
            }

            body = {
                "meta": meta,
                "data": data
            }

            self.__api("PUT", Name(user, box).path, body)


def usage(text, *args):
    """
    Prints the usage colored.

    Parameters
    ----------
    param text : string
        Usage text to print.
    param args: list of strings
        Usage parameters.

    Notes
    -----
    Color is only used on POSIX compatible systems.

    """
    for line in (text % args).split("\n")[1:-1]:
        line = line[4:]

        if os.name in ["posix"]:

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

        print(line)


def main(script, command="--help", user=None, receiver=None, *message):
    """
          ________               ___
         /  ___  /______________/  /_
        /  /__/ / ___/ ___/ ___/  __/
       /  _____/__  /__  /__  /  /_
      /__/    /____/____/____/\___/

      CLI version %s

    Usage:
      %s [option|command] [user:password] [receiver] [message|filename]

    Options:
      -h --help      Shows this text
      -l --license   Shows license
      -v --version   Shows version

    Available commands:
      create   Create an user or a box
      delete   Delete an user or a box
      list     List all boxes
      pull     Pull a message
      push     Push a message

    Report bugs to <hello@pssst.name>
    """
    try:
        if user:
            name = Name(user)

        if user and not name.password:
            name.password = getpass("Password (will be hidden): ")

        if command in ("-h", "--help"):
            usage(main.__doc__, __version__, os.path.basename(script))

        elif command in ("-l", "--license"):
            print(__doc__.strip())

        elif command in ("-v", "--version"):
            print("Pssst CLI " + __version__)

        elif command in ("--create", "create") and user:
            Pssst(name.user, name.password).create(name.box)
            print("Created %s" % name)

        elif command in ("--delete", "delete") and user:
            Pssst(name.user, name.password).delete(name.box)
            print("Deleted %s" % name)

        elif command in ("--list", "list") and user:
            print("\n".join(Pssst(name.user, name.password).list()))

        elif command in ("--pull", "pull") and user:
            data = Pssst(name.user, name.password).pull(name.box)

            if data:
                name, time, message = data
                print("%s -- %s, %s" % (
                    message.decode("utf-8"), Name(name),
                    datetime.fromtimestamp(time)
                ))

        elif command in ("--push", "push") and user and receiver:
            data = " ".join(message)

            if os.path.exists(data):
                data = io.open(data, "rb").read()

            Pssst(name.user, name.password).push([receiver], data)
            print("Message pushed")

        else:
            print("Unknown command: " + command)
            print("Please use -h for help on commands.")
            return 2 # Incorrect usage

    except KeyboardInterrupt:
        print("Exit")

    except ConnectionError:
        return "Could not connect to API"

    except Timeout:
        return "Connection timeout"

    except Exception as ex:
        return "Error: %s" % ex


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
