#!/usr/bin/env python
"""
Pssst Server Scaling Test
"""
import os
import random
import string
import sys
import time

from threading import Thread


try:
    from pssst import Pssst
except ImportError:
    sys.exit("Please install the Pssst CLI (https://pssst.name)")


class Client:
    """
    Simple test client that performs pulls
    """
    def __init__(self, username, password="test"):
        self.thread = Thread(target=self.run, args=[])
        self.pssst = Pssst(username, password)
        self.pssst.create()

    def start(self):
        self.thread.start()

    def stop(self):
        self.exit = True

    def run(self):
        self.exit = False
        self.time = []

        while not self.exit:
            run = time.time()
            self.pssst.pull()
            self.time.append(time.time() - run)
        else:
            self.pssst.delete() # Clean up


def spawn():
    """
    Returns a new random client
    """
    pool = string.ascii_lowercase + string.digits
    name = "".join([random.choice(pool) for x in range(16)])

    return Client(name)


def average(client):
    """
    Returns the clients average pull time
    """
    return reduce(lambda x, y: x + y, client.time) / len(client.time)


def main(script, count=None, duration=60):
    """
    Usage: %s CLIENTS [DURATION]
    """
    script = os.path.basename(script)

    if not count:
        return main.__doc__.strip() % os.path.basename(script)

    try:
        clients = []

        for n in range(int(count)):
            clients.append(spawn())

        for client in clients:
            client.start()

        time.sleep(int(duration))

        for client in clients:
            client.stop()

        time.sleep(10)

        for client in clients:
            print("Average time %0.3fs" % (round(average(client), 3)))

    except Exception as ex:
        return "%s error: %s" % (script, ex)


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
