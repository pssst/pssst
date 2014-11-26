#!/usr/bin/env python
"""
PLEASE BE AWARE: THIS CODE IS ONLY AN EXAMPLE ON HOW TO DERIVE FROM THE CLI
"""
import base64
import io
import json
import os
import sys

from datetime import datetime
from getpass import getpass


try:
    from pssst import Pssst
except ImportError:
    sys.exit("Please install the Pssst CLI (https://pssst.name)")


def main(script, username=None, path="."):
    """
    Usage: %s USERNAME [PATH]
    """
    if not username:
        return main.__doc__.strip() % os.path.basename(script)

    try:
        name = Pssst.Name(username)
        data = Pssst(name.user, name.password or getpass()).pull()

        if data:
            user, time, message = data
            data = json.loads(message)

            if not os.path.exists(path):
                os.mkdir(path)

            filename = os.path.join(path, os.path.basename(data["file"]))
            filedata = base64.b64decode(data["data"].encode("ascii"))

            io.open(filename, "wb").write(filedata)

            print("Pulled %s" % filename)
            print("%s, %s" % (Pssst.Name(user), datetime.fromtimestamp(time)))

    except Exception as ex:
        return "Error: %s" % ex


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
