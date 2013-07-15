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
import os
import sys
import time

from pssst import Pssst

os.chdir(os.path.split(os.path.abspath(sys.argv[0]))[0])

if os.path.exists(".pssst"):
    host = open(".pssst", "r").read().strip()
else:
    host = ""

local = host.startswith("localhost")

if local and not os.name in ["posix"]:
    sys.exit("System not supported")

if local: os.system("node ../../server/server.js & sleep 1")

try:
    name = "pssst.%s" % int(round(time.time()))
    pssst = Pssst(name)

    print "Pssst! Client (%s)" % pssst.host

    for size in [2 ** exp for exp in range(8, 14)]:

        print "Pssst! Test...",

        message = os.urandom(size)
        pssst.push([name], message)

        if pssst.pull()[1] == message:
            print size
        else:
            print "failed"

finally:
    if local: os.system("killall node")
