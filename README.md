![Pssst](http://www.gravatar.org/avatar/2aae9030772d5b59240388522f91468f?s=96)

Pssst [![](https://travis-ci.org/pssst/pssst.svg)](https://travis-ci.org/pssst/pssst)
=====
Pssst is a simple and secure way to exchange information. We do not provide
services by our self, but we provide you with the tools to start your own
service. These tools build upon open-source software and use strong end-to-end
encryption.

**How to get**
```
$ curl -s https://get.pssst.name | bash
```

**How to use**
```
$ pssst me you "Hello"
```

Usage
-----
**Create the users `me` and `you`**
```
$ pssst create me
$ pssst create you
```

**Create the box `you.spam`**
```
$ pssst create you.spam
```

**Push a message from `me` to `you.spam`**
```
$ pssst push me you.spam "Hello"
```

**Pull new messages from `you.spam`**
```
$ pssst pull you.spam
```

**Delete the box `you.spam`** (_because nobody likes spam_)
```
$ pssst delete you.spam
```

Install
-------
### CLI

Required (at least) for the CLI (command line interface):

* Python 2.7
* Requests 2.0
* PyCrypto 2.6

There is no need to install anything, just run the script `src/cli/pssst.py`:

`$ pssst.py`

If you wish to install the CLI on a POSIX compatible system just execute:

`$ curl -s https://get.pssst.name | bash`

### GUI

Required (at least) for the GUI (HTML interface):

* CherryPy 3.2
* Bower 1.3

Please start the GUI per `src/gui/start` script, this will download and
install all necessary Bower packages. The GUI uses the CLI as a local
proxy, so those requirements apply here also.

### Server

Required (at least) if you want to run your own server:

* Node.js 0.10
* A Redis database instance

To start your own server, just execute the following command inside the
`server` directory using a default Redis Database configuration:

`$ npm start`

The server will now start and create a default configuration file. A commented
sample configuration file can be found under `src/server/config.json.sample`.

API
---
Our full [API](/docs/api/api.md) documentation can be found under `docs/api/`.

### Commands

These commands are currently supported by the API:

* `create` an user or a box.
* `delete` an user or a box.
* `find` a users public key.
* `list` all boxes of an user.
* `pull` a message from a box.
* `push` a message onto a box.

### Names

All user/box names are specified by this format:

`pssst.<user>.<box>`

All user and box names must be between 2 and 63 characters long and must only
contain of the lowercase letters a-z and numbers. The service prefix `pssst.`
can be omitted, but should be specified for clarity reasons. If no box name
is given, the users default box (named `box`) is used.

### Limits

Every user has a fix limit of `512 MB` overall buffered data. This includes
all user specific data, such as the public key, boxes and messages. Neither
the number of the users boxes, nor the size of a message are limited
separately. This is limit is hard coded by the Redis database and may change
in future releases. Only messages not yet pulled by the user will count to
this limit.

> If you want to lower the limit per user, please set the `quota` config
> setting in your `src/server/config.json` file.

FAQ
---
**How can I use my own server?**

If you want to use any other server besides our test API, simply create a file
named `.pssst` in your home directory with the desired server address:

`$ echo http://localhost:62421 > ~/.pssst`

**What is the default server port?**

The default server port is `62421`.

**What services do you support?**

We have built-in support for Heroku and all its Redis database add-ons.

**Where are my user settings stored?**

All user specific data is stored as zip files named `.pssst.<user>` in the
users home directory.

**How can I use verified HTTPS connections with Python 2.x?**

If you use Python 2.7 than pyASN1, pyOpenSSL and ndg-httpsclient are also
required to make verified HTTPS connections.

**Where can I find additional resources?**

Please take a look at our [documentation](/docs/) which can be under `docs/`.

We also have additional repositories:
* [Pssst Configs](https://github.com/pssst/pssst-config/)
* [Pssst Scripts](https://github.com/pssst/pssst-script/)

Warnings
--------
No security advisories are known as of today.

Contact
-------
* If you want to be informed about new releases, general news
  and information about Pssst, please visit our website under:
  https://pssst.name

* If you want to be informed about code updates, bug fixes and
  security fixes of Pssst, please visit our project on GitHub:
  https://github.com/pssst/pssst/

* If you have a concrete bug report for Pssst please go to the
  Pssst issue tracker on GitHub and submit your report:
  https://github.com/pssst/pssst/issues

Authors
-------
Please see the files `AUTHORS` and `THANKS` for further information.

License
-------
Copyright (C) 2013-2015  Christian & Christian  <hello@pssst.name>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

----------
Created by
[Christian](https://github.com/7-bit) & [Christian](https://github.com/cuhsat)
just for the joy of it.
