CLI
===
Pssst command line interface.

Usage
-----
```
$ pssst [option|command] [username:password] [receiver message...]
```

Start
-----
Just run the `pssst.py` script to start the CLI:

```
$ pssst.py
```

All user specific data is stored as zip files named `.pssst.<user>` in the
users home directory. If you want to use any other than our test API, simply
create a file named `.pssst` in your home directory with the desired address:

```
$ echo http://localhost:62421 > ~/.pssst
```

Install
-------
At least [required](pssst.pip):

* Python 2.7.3
* Requests 2.0.1
* PyCrypto 2.6.1

> If you use Python 2.7 the pyASN1, pyOpenSSL and ndg-httpsclient packages are
> also required for verified HTTPS connections.

----
Please use the `--help` option to show further help.
