#!/usr/bin/env python
"""
Test Vectors
"""
import base64
import sys
import textwrap


try:
    from Crypto.Cipher import AES, PKCS1_OAEP
    from Crypto.Hash import HMAC, SHA512
    from Crypto.PublicKey import RSA
    from Crypto.Signature import PKCS1_v1_5
except ImportError:
    sys.exit("Requires PyCrypto (https://github.com/dlitz/pycrypto)")


# Raw test data
ONCE, TIME, DATA, KEY = b"x"*48, 123456789, "Hello World!", """\
-----BEGIN RSA PRIVATE KEY-----
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
-----END RSA PRIVATE KEY-----"""


def test_data(data=DATA):
    """
    Raw Data
    """
    return data.encode("ascii")


def test_time(time=TIME):
    """
    Raw Time
    """
    return str(int(round(time))).encode("ascii")


def test_once(once=ONCE):
    """
    Raw Once
    """
    return once


def test_hmac(time=TIME, data=DATA):
    """
    HMAC (SHA 512 Bit)
    """
    time = test_time(time)
    mac = HMAC.new(time, data, SHA512)
    mac = SHA512.new(mac.digest())
    return base64.b64encode(mac.digest()), len(mac.digest())


def test_aes_encrypt(data=DATA, once=ONCE):
    """
    AES Encrypt (256 Bit, CFB8 Mode, No Padding)
    """
    data = AES.new(once[:32], AES.MODE_CFB, once[32:]).encrypt(data)
    return base64.b64encode(data), len(data)


def test_aes_decrypt(data=DATA, once=ONCE):
    """
    AES Decrypt (256 Bit, CFB8 Mode, No Padding)
    """
    data = base64.b64decode(test_aes_encrypt(data, once)[0])
    return AES.new(once[:32], AES.MODE_CFB, once[32:]).decrypt(data)


def test_rsa_private(key=KEY):
    """
    RSA Private Key (1024 Bit, PEM format)
    """
    key = RSA.importKey(key)
    key = key.exportKey("PEM", None)
    return key, len(key)


def test_rsa_public(key=KEY):
    """
    RSA Public Key (PEM format)
    """
    key = RSA.importKey(key)
    key = key.publickey().exportKey("PEM")
    return key, len(key)


def test_rsa_encrypt(key=KEY, once=ONCE):
    """
    RSA Encrypt (PKCS #1, OAEP)
    """
    key = RSA.importKey(key)
    once = PKCS1_OAEP.new(key).encrypt(once)
    return base64.b64encode(once), len(once)


def test_rsa_decrypt(key=KEY, once=ONCE):
    """
    RSA Decrypt (PKCS #1, OAEP)
    """
    once = base64.b64decode(test_rsa_encrypt(key, once)[0])
    key = RSA.importKey(key)
    return PKCS1_OAEP.new(key).decrypt(once)


def test_rsa_sign(key=KEY, time=TIME, data=DATA):
    """
    RSA Sign (PKCS #1 v1.5)
    """
    time = test_time(time)
    mac = HMAC.new(time, data, SHA512)
    mac = SHA512.new(mac.digest())
    key = RSA.importKey(key)
    sig = PKCS1_v1_5.new(key).sign(mac)
    return base64.b64encode(sig), len(sig)


def test_rsa_verify(key=KEY, time=TIME, data=DATA):
    """
    RSA Verify (PKCS #1 v1.5)
    """
    time = test_time(time)
    mac = HMAC.new(time, data, SHA512)
    mac = SHA512.new(mac.digest())
    key = RSA.importKey(key)
    sig = PKCS1_v1_5.new(key).sign(mac)
    return "Yes" if PKCS1_v1_5.new(key).verify(mac, sig) else "No"


def pretty(test, *args):
    """
    Executes and prints the given test
    """
    data = test(*args)
    data, size = data if (type(data) is tuple) else (data, len(data))

    print("%s:" % test.__doc__.strip())
    for line in textwrap.wrap(data.decode("ascii"), 64):
        print(line)
    print("(%s Bytes)\n" % size)


def main(script):
    """
    Prints the test vectors
    """
    print(__doc__.lstrip())
    pretty(test_data)
    pretty(test_time)
    pretty(test_once)
    pretty(test_hmac)
    pretty(test_aes_encrypt)
    pretty(test_aes_decrypt)
    pretty(test_rsa_private)
    pretty(test_rsa_public)
    pretty(test_rsa_encrypt)
    pretty(test_rsa_decrypt)
    pretty(test_rsa_sign)
    pretty(test_rsa_verify)


if __name__ == "__main__":
    sys.exit(main(*sys.argv))
