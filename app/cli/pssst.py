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


try:
	import readline
except ImportError:
	pass # Windows doesn't support this


try:
    from requests import request
    from requests.exceptions import ConnectionError, Timeout
except ImportError:
    sys.exit("Requires Requests (https://github.com/kennethreitz/requests)")


try:
    from Crypto import Random
    from Crypto.Cipher import AES, PKCS1_OAEP
    from Crypto.Hash import HMAC, SHA, SHA512
    from Crypto.PublicKey import RSA
    from Crypto.Signature import PKCS1_v1_5
except ImportError:
    sys.exit("Requires PyCrypto (https://github.com/dlitz/pycrypto)")


__all__, __version__ = ["Pssst", "Name"], "0.2.28"


def _encode64(data): # Utility shortcut
    return base64.b64encode(data).decode("ascii")


def _decode64(data): # Utility shortcut
    return base64.b64decode(data.encode("ascii"))


class Name:
    """
    Pssst canonical name parser.

    """
    def __init__(self, user, box=None, password=None):
        """
        Initializes the instance with the parsed name.

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
    push(usernames, message)
        Pushes a message into a box.

    """
    class KeyStore:
        """
        Class for storing public and private keys.

        Methods
        -------
        delete()
            Deletes the key store.
        list()
            Returns an alphabetical list of all key names.
        load(name)
            Returns a key.
        save(name, key)
            Saves a key.

        Notes
        -----
        This class is not meant to be called externally.

        """
        def __init__(self, user, password):
            self.user = user
            self.file = os.path.join(os.path.expanduser("~"), repr(self))

            if os.path.exists(self.file):
                self.key = Pssst.Key(self.load(user + ".private"), password)
            else:
                self.key = Pssst.Key()

                self.save(user + ".private", self.key.private(password))
                self.save(user, self.key.public())

        def __repr__(self):
            return ".pssst." + self.user

        def __bool__(self):
            return self.__nonzero__()

        def __nonzero__(self):
            return os.path.exists(self.file)

        def delete(self):
            os.remove(self.file)

        def list(self):
            with ZipFile(self.file, "r") as file:
                return file.namelist()

        def load(self, name):
            with ZipFile(self.file, "r") as file:
                return file.read(name)

        def save(self, name, key):
            with ZipFile(self.file, "a") as file:
                file.writestr(name, key)


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
            Returns the encrypted data and nonce.
        decrypt(data, nonce)
            Returns the decrypted data.
        verify(data, timestamp, signature)
            Returns if data could be verified with timestamp and signature.
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
            return self.key.exportKey("PEM", password, 8).decode("ascii")

        def public(self):
            return self.key.publickey().exportKey("PEM").decode("ascii")

        def encrypt(self, data):
            nonce = Random.get_random_bytes(32 + AES.block_size) # 256 bit key

            data = AES.new(nonce[:32], AES.MODE_CFB, nonce[32:]).encrypt(data)
            nonce = PKCS1_OAEP.new(self.key).encrypt(nonce)

            return (data, nonce)

        def decrypt(self, data, nonce):
            nonce = PKCS1_OAEP.new(self.key).decrypt(nonce)
            data = AES.new(nonce[:32], AES.MODE_CFB, nonce[32:]).decrypt(data)

            return data

        def verify(self, data, timestamp, signature):
            current, data = int(round(time.time())), data.encode("ascii")

            hmac = HMAC.new(str(timestamp).encode("ascii"), data, SHA512)
            hmac = SHA512.new(hmac.digest())

            if abs(timestamp - current) <= 30:
                return PKCS1_v1_5.new(self.key).verify(hmac, signature)
            else:
                return False

        def sign(self, data):
            current, data = int(round(time.time())), data.encode("ascii")

            hmac = HMAC.new(str(current).encode("ascii"), data, SHA512)
            hmac = SHA512.new(hmac.digest())

            signature = PKCS1_v1_5.new(self.key).sign(hmac)

            return (current, signature)


    def __init__(self, username, password=None):
        """
        Initializes the instance with an user object.

        Parameters
        ----------
        param username : string
            User name.
        param password : string, optional (default is None)
            User private key password.

        Raises
        ------
        Exception
            Because the server could not be authenticated.
        Exception
            Because the client time is not synchronized.
        Exception
            Because the password is required.

        Notes
        -----
        If a file in the current directory with the name '.pssst' exists, the
        content of this file is parsed and used as the API server address and
        port.

        A valid password must consist of upper and lower case letters and also
        numbers. The required minimum length of a password is 8 characters. If
        you use the offical API, a password is mandatory.

        The public key of the official API will be verified against the built-
        in fingerprint.

        """
        FINGERPRINT = "563cb9031992f503a21f3fa7be160567f1380467"

        config = os.path.join(os.path.expanduser("~"), ".pssst")

        if os.path.exists(config):
            verify, self.api = False, io.open(config).read().strip()
        else:
            verify, self.api = True, "https://api.pssst.name"

        key, sync = self.__url("key"), int(self.__url("time"))

        if verify and not FINGERPRINT == SHA.new(key).hexdigest():
            raise Exception("Server could not be authenticated")

        if verify and not abs(sync - int(round(time.time()))) <= 30:
            raise Exception("Client time is not synchronized")

        if verify and not password:
            raise Exception("Password is required")

        self.store = Pssst.KeyStore(Name(username).user, password)

        if self.api not in self.store.list():
            self.store.save(self.api, key)

    def __repr__(self):
        """
        Returns the module identifier.

        Returns
        -------
        string
            The module identifier.

        """
        return "Pssst " + __version__

    def __api(self, method, path, data=None):
        """
        Returns the result of an API request (signed and verified).

        Parameters
        ----------
        param method : string
            Request method.
        param path : string
            Request path.
        param data : JSON, optional (default is None)
            Request data.

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
        if not self.store:
            raise Exception("User was deleted")

        body = str(json.dumps(data, separators=(",", ":"))) if data else ""

        timestamp, signature = self.store.key.sign(body)

        response = request(method, "%s/1/%s" % (self.api, path), data=body,
            headers={
                "content-hash": "%s; %s" % (timestamp, _encode64(signature)),
                "content-type": "application/json" if data else "text/plain",
                "user-agent": repr(self)
            }
        )

        mime = response.headers.get("content-type", "text/plain")
        head = response.headers.get("content-hash")
        body = response.text

        if not re.match("^[0-9]+; ?[A-Za-z0-9\+/]+=*$", head):
            raise Exception("Verification failed")

        timestamp, signature = head.split(";", 1)
        timestamp, signature = int(timestamp), _decode64(signature)

        pssst = Pssst.Key(self.store.load(self.api))

        if not pssst.verify(body, timestamp, signature):
            raise Exception("Verification failed")

        if response.status_code not in [200, 204]:
            raise Exception(body)

        if mime.startswith("application/json"):
            body = response.json()

        return body

    def __url(self, path):
        """
        Returns the result of an URL request (without any checks).

        Parameters
        ----------
        param path : string
            Requested path.

        Returns
        -------
        string
            The response body.

        Raises
        ------
        ConnectionError
            Because the file was not found.

        Notes
        -----
        Please see __init__ method.

        """
        response = request("GET", "%s/%s" % (self.api, path),
            headers={
                "user-agent": repr(self)
            }
        )

        if response.status_code != 200:
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
            body = {"key": self.store.key.public()}
        else:
            body = None

        self.__api("POST", Name(self.store.user, box).path, body)

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
        any API call wil result in an error. The key store is also deleted.

        """
        self.__api("DELETE", Name(self.store.user, box).path)

        if not box:
            self.store.delete()

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
            PEM formatted public key.

        """
        return self.__api("GET", user + "/key")

    def list(self):
        """
        Returns an alphabetical list of all boxes of an user.

        Parameters
        ----------
        param user : string
            Name of the user.

        Returns
        -------
        list of strings
            List of user boxes.

        """
        return self.__api("GET", self.store.user + "/list")

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
            The user name, time and message, None if empty.

        """
        data = self.__api("GET", Name(self.store.user, box).path)

        if not data:
            return None # Box is empty

        head = data["head"]

        user = str(head["user"])
        time = int(head["time"])

        nonce = _decode64(head["nonce"])
        body  = _decode64(data["body"])

        message = self.store.key.decrypt(body, nonce)

        return (user, time, message)

    def push(self, usernames, message):
        """
        Pushes a message into a box.

        Parameters
        ----------
        param usernames : list of strings
            List of user names.
        param message : byte string
            The message.

        """
        for user, box in [Name(username).full for username in usernames]:

            if user not in self.store.list():
                self.store.save(user, self.find(user)) # Add public key

            data, nonce = Pssst.Key(self.store.load(user)).encrypt(message)

            nonce = _encode64(nonce)
            body  = _encode64(data)

            head = {
                "user": self.store.user,
                "nonce": nonce
            }

            data = {
                "head": head,
                "body": body
            }

            self.__api("PUT", Name(user, box).path, data)


def shell(intro, prompt="pssst"):
    """
    Starts an interactive shell.

    Parameters
    ----------
    param intro : string
        The shell intro.
    param prompt : string, optional (default is pssst)
        The shell prompt.

    """
    print(intro)
    print('Use "exit" to close the shell.')

    while True:
        line = raw_input("> %s " % prompt)
        args = line.split()

        if not line:
            continue

        if args[0] in ("exit", "quit"):
            break

        result = main(prompt, *args)

        if not isinstance(result or 0, int):
            print(result)


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
            if re.match("^.* version \d+\.\d+\.\d+$", line):
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


def main(script, command="--help", username=None, receiver=None, *message):
    """
          ________               ___
         /  ___  /______________/  /_
        /  /__/ / ___/ ___/ ___/  __/
       /  _____/__  /__  /__  /  /_
      /__/    /____/____/____/\___/

      CLI version %s

    Usage:
      %s [option|command] [username:password] [receiver message]

    Options:
      -s --shell     Run as interactive shell
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
        if username:
            name = Name(username)

        if username and not hasattr(Pssst, "shell"):
            Pssst.shell = Pssst(name.user, name.password or getpass())

        if command in ("/?", "-h", "--help", "help"):
            usage(main.__doc__, __version__, os.path.basename(script))

        elif command in ("-l", "--license"):
            print(__doc__.strip())

        elif command in ("-v", "--version"):
            print("Pssst CLI " + __version__)

        elif command in ("-s", "--shell") and username:
            shell("Pssst Shell " + __version__ + " for " + name.user)

        elif command in ("--create", "create") and username:
            Pssst.shell.create(name.box)
            print("Created %s" % name)

        elif command in ("--delete", "delete") and username:
            Pssst.shell.delete(name.box)
            print("Deleted %s" % name)

        elif command in ("--list", "list") and username:
            print("\n".join(Pssst.shell.list()))

        elif command in ("--pull", "pull") and username:
            data = Pssst.shell.pull(name.box)

            if data:
                user, time, message = data
                print(message.decode("utf-8"))
                print("%s, %s" % (Name(user), datetime.fromtimestamp(time)))

        elif command in ("--push", "push") and username and receiver:
            Pssst.shell.push([receiver], " ".join(message))
            print("Message pushed")

        else:
            print("Unknown command or username not given: " + command)
            print("Please use --help for help on commands.")
            return 2 # Incorrect usage

    except KeyboardInterrupt:
        print("exit")

    except ConnectionError:
        return "API connection failed"

    except Timeout:
        return "API connection timeout"

    # except Exception as ex:
    #     return "Error: %s" % ex


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
