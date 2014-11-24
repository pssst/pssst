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
import webbrowser


try:
    from pssst import Pssst, usage, __version__
except ImportError:
    sys.exit("Please execute the start script")


try:
    import cherrypy
except ImportError:
    sys.exit("Requires CherryPy (https://www.cherrypy.org)")


try:
    from Crypto import Random
    from Crypto.Cipher import AES
except ImportError:
    sys.exit("Requires PyCrypto (https://github.com/dlitz/pycrypto)")


class CLI:
    """
    Local CLI wrapper for encrypted calls.

    Methods
    -------
    call(params)
        Calls the CLI and returns the result.
    pull(box=None)
        Pulls a message from a box.
    push(usernames, message)
        Pushes a message into a box.
    login(username, password)
        Creates the Pssst instance.
    logout()
        Clears the Pssst instance.
    version()
        Returns the CLI version.

    """
    def __init__(self, token):
        """
        Initializes the instance with the security token.

        Parameters
        ----------
        param token : Bytes
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
        Encryption is done in the following steps:

        1. Add PKCS#7 padding
        2. Decode bytes from UTF-8
        3. Encrypt with AES (256 Bit, CBC Mode, PKCS#7 Padding)
        4. Encode bytes in standard Base64

        """
        key, iv, size = self.token[:32], self.token[32:], AES.block_size

        data = text + (size - len(text) % size) * chr(size - len(text) % size)
        data = data.encode("utf-8")
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
        Decryption is done in the following steps:

        1. Decode bytes from standard Base64
        2. Decrypt with AES (256 Bit, CBC Mode, PKCS#7 Padding)
        3. Encode bytes to UTF-8
        4. Remove PKCS#7 padding

        """
        key, iv = self.token[:32], self.token[32:]

        data = base64.b64decode(data)
        data = AES.new(key, AES.MODE_CBC, iv).decrypt(data)
        data = data.decode("utf-8")
        text = data[0:-ord(data[-1])]

        return text

    def call(self, request):
        """
        Calls the CLI and returns the response.

        Parameters
        ----------
        param request : string
            Encrypted request.

        Returns
        -------
        string
            Encrypted response.

        """
        try:
            request = json.loads(self.__decrypt(request))

            method = request["method"]
            params = request["params"]

            for obj in [self, self.pssst]:
                if method in dir(obj):
                    response = json.dumps(getattr(obj, method)(*params))
                    break

        except Exception as ex:
            cherrypy.response.status = 500 # Internal Server Error
            response = str(ex)

        finally:
            return self.__encrypt(response)

    def pull(self, box=None):
        """
        Pulls a message from a box (override).

        Parameters
        ----------
        param box : string, optional (default is None)
            Name of the users box.

        Returns
        -------
        tuple or None
            The user name, time and message, None if empty.

        """
        data = self.pssst.pull(box)

        if data:
            user, time, message = data
            return (Pssst.Name(user), time, message.decode("utf-8"))

    def push(self, usernames, message):
        """
        Pushes a message into a box (override).

        Parameters
        ----------
        param usernames : list of strings
            List of user names.
        param message : byte string
            The message.

        """
        self.pssst.push(usernames, message.encode("utf-8"))

    def login(self, create, username, password):
        """
        Creates the Pssst instance.

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
            The canonical user name or None.

        """
        name = Pssst.Name(username)
        home = os.path.expanduser("~")

        # New user
        if create:
            self.pssst = Pssst(username, password)
            self.pssst.create()
            return name.user

        # Old user
        if os.path.exists(os.path.join(home, ".pssst." + name.user)):
            self.pssst = Pssst(username, password)
            return name.user

    def logout(self):
        """
        Clears the Pssst instance.

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

    call.exposed = True


def main(script, option="8211", usepipe=False):
    """
          ________               ___
         /  ___  /______________/  /_
        /  /__/ / ___/ ___/ ___/  __/
       /  _____/__  /__  /__  /  /_
      /__/    /____/____/____/\___/

      GUI version %s

    Usage:
      %s [option|port]

    Options:
      -h --help      Shows this text
      -l --license   Shows license
      -v --version   Shows version

    Report bugs to <hello@pssst.name>
    """
    try:
        if option in ("/?", "-h", "--help", "help"):
            usage(main.__doc__, __version__, os.path.basename(script))

        elif option in ("-l", "--license"):
            print(__doc__.strip())

        elif option in ("-v", "--version"):
            print("Pssst GUI " + __version__)

        elif option.isdigit():
            host, port = "127.0.0.1", int(option)

            tokenbin = Random.get_random_bytes(48)
            tokenhex = binascii.hexlify(tokenbin).decode("ascii")

            url = "http://%s:%s/pssst#%s" % (host, port, tokenhex)

            if not usepipe:
                webbrowser.open_new_tab(url)
            else:
                print(url) # URL for piping

            bower = os.path.join(os.path.dirname(__file__), "bower")
            pssst = os.path.join(os.path.dirname(__file__), "pssst")

            cherrypy.quickstart(CLI(tokenbin), "/", {
                "global": {
                    "server.socket_host": host,
                    "server.socket_port": port,
                    "log.screen": False
                },
                "/bower": {
                    "tools.staticdir.on": True,
                    "tools.staticdir.dir": os.path.abspath(bower)
                },
                "/pssst": {
                    "tools.staticdir.on": True,
                    "tools.staticdir.dir": os.path.abspath(pssst),
                    "tools.staticdir.index": "index.html"
                }
            })

        else:
            print("Unknown option: " + option)
            print("Please use --help for help on options.")
            return 2 # Incorrect usage

    except Exception as ex:
        return "Error: %s" % ex


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
