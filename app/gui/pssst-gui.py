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
import binascii
import json
import os
import sys

from pssst import __version__, Name, Pssst


try:
    import cherrypy
except ImportError:
    sys.exit("Requires CherryPy (https://www.cherrypy.org)")


try:
    from Crypto import Random
    from Crypto.Cipher import AES
except ImportError:
    sys.exit("Requires PyCrypto (https://github.com/dlitz/pycrypto)")


class Server:
    """
    Local server class for CLI wrapping.

    Methods
    -------
    call(params)
        Calls the CLI and returns the result.
    login(username, password)
        Creates a Pssst instance.
    logout()
        Clears a Pssst instance.
    version()
        Returns the CLI version.

    """
    def __init__(self, token):
        """
        Initializes the instance with the security token.

        Parameters
        ----------
        param token : string
            Security token.

        """
        self.token = token
        self.pssst = None

    def __encrypt(self, text):
        """
        Returns the encrypted data.

        Parameters
        ----------
        param text : string
            Decrypted text.

        Returns
        -------
        Base64
            Encrypted data.

        Notes
        -----
        Encryption will be done with the following steps:

        1. Decode from UTF-8
        3. Add PKCS#7 padding
        2. Encrypt with AES (256 Bit, CBC Mode, PKCS#7 Padding)
        4. Encode in standard Base64

        """
        key, iv, size = self.token[:32], self.token[32:], AES.block_size

        data = text.decode("utf-8")
        data = data + (size - len(data) % size) * chr(size - len(data) % size)
        data = AES.new(key, AES.MODE_CBC, iv).encrypt(data)
        data = base64.b64encode(data)

        return data

    def __decrypt(self, data):
        """
        Returns the decrypted text.

        Parameters
        ----------
        param data : Base64
            Encrypted data.

        Returns
        -------
        string
            Decrypted text.

        Notes
        -----
        Decryption will be done with the following steps:

        1. Decode from standard Base64
        2. Decrypt with AES (256 Bit, CBC Mode, PKCS#7 Padding)
        3. Remove PKCS#7 padding
        4. Encode in UTF-8

        """
        key, iv = self.token[:32], self.token[32:]

        data = base64.b64decode(data)
        data = AES.new(key, AES.MODE_CBC, iv).decrypt(data)
        data = data[0:-ord(data[-1])]
        text = data.encode("utf-8")

        return text

    @cherrypy.expose
    def call(self, params):
        """
        Calls the CLI and returns the result.

        Parameters
        ----------
        param params : string
            Encrypted call parameters.

        Returns
        -------
        string
            Encrypted call result or error.

        """
        try:
            params = json.loads(self.__decrypt(params))

            name = params["method"]
            args = params["args"]

            for obj in [self, self.pssst]:
                if name in dir(obj):
                    result = getattr(obj, name)(*args)
                    result = json.dumps(result)
                    break

        except Exception as ex:
            cherrypy.response.status = 500
            result = str(ex)

        finally:
            return self.__encrypt(result)

    def login(self, create, username, password):
        """
        Creates a Pssst instance.

        Parameters
        ----------
        param create : boolean
            Create user.
        param username : string
            User name.
        param password : string
            User private key password.

        Returns
        -------
        string
            The users canonical name or None.

        """
        name = Name(username)
        home = os.path.expanduser("~")

        if create:
            self.pssst = Pssst(username, password)
            self.pssst.create()
            return name.user

        if os.path.exists(os.path.join(home, ".pssst." + name.user)):
            self.pssst = Pssst(username, password)
            return name.user

    def logout(self):
        """
        Clears a Pssst instance.

        """
        self.pssst = None

    def version(self):
        """
        Returns the CLI version.

        Returns
        -------
        string
            The CLI version.

        """
        return __version__


def main(script, arg="8211"):
    """
    Usage: %s [PORT]
    """
    try:
        if arg in ("/?", "-h", "--help"):
            print(main.__doc__.strip() % os.path.basename(script))

        else:
            host, port, key = "0.0.0.0", int(arg), Random.get_random_bytes(48)

            print("http://%s:%s/#%s" % (host, port, binascii.hexlify(key)))

            cherrypy.quickstart(Server(key), "/", {
                "global": {
                    "log.screen": False,
                    "server.socket_host": host,
                    "server.socket_port": port
                },
                "/": {
                    "tools.staticdir.on": True,
                    "tools.staticdir.dir": os.path.abspath("./data"),
                    "tools.staticdir.index": "index.html"
                }
            })

    except KeyboardInterrupt:
        print("exit")

    except Exception as ex:
        return "Error: %s" % ex


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
