#!/usr/bin/env python
"""
This code is an example that should not be used in a productive environment
"""
import base64
import io
import json
import os
import sys

from getpass import getpass


try:
    from pssst import Pssst
except ImportError:
    sys.exit("Please install the Pssst CLI (https://pssst.name)")


def main(script, username=None, receiver=None, filename=None):
    """
    Usage: %s USERNAME RECEIVER FILENAME
    """
    if not username:
        return main.__doc__.strip() % os.path.basename(script)

    try:
        name = Pssst.Name(username)
        data = io.open(filename, "rb").read()

        pssst = Pssst(name.user, name.password or getpass())
        pssst.push([receiver], json.dumps({
            "file": os.path.basename(filename),
            "data": base64.b64encode(data).decode("ascii")
        }))

        print("Pushed %s" % filename)

    except Exception as ex:
        return "Error: %s" % ex


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
