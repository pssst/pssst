![Pssst](http://www.gravatar.org/avatar/2aae9030772d5b59240388522f91468f?s=96)

Pssst [![](https://travis-ci.org/pssst/pssst.svg)](https://travis-ci.org/pssst/pssst)
=====
Pssst is a simple and secure way to exchange information. We do not provide
services by our self, but we provide you with the tools to start your own
service. These tools build upon open-source software and use strong end-to-end
encryption.

**How to get:**
```
$ curl -s https://get.pssst.name | bash
```

**How to use:**
```
$ pssst me you "Hello"
```

Commands
--------
`pssst create`

Creates an user or a box.

`pssst delete`

Deletes an user or a box.

`pssst list`

Lists all boxes.

`pssst push`

Pushes a messages into a box.

`pssst pull`

Pulls a message from a box.

Install
-------
### CLI

Needed for the CLI (command line interface):

* Python 2.7
* Requests 2.0
* PyCrypto 2.6

Just run the `src/cli/pssst.py` script to start the CLI:

`$ pssst.py`

### GUI

Needed for the GUI (HTML interface) also:

* CherryPy 3.2
* Bower 1.3

Just run the `src/gui/start.sh` script to start the GUI:

`$ start.sh`

### Server

Needed if you want to run your own server:

* Node.js 0.10
* An Redis database instance

Just execute this command in your `src/server` directory to start the server:

`$ npm install && npm start`

API
---
Our full [API](/docs/api/api.md) documentation can be found under `docs/api/`.

CVE
---
No security advisories are known as of today.

FAQ
---
**How can I use my own server?**

If you want to use your own server and not our test API, simply create a file
named `.pssst` in your home directory with the desired server address:

`$ echo http://localhost:62421 > ~/.pssst`

**What is the default server port?**

The default server port is `62421`.

**What services do you support?**

We have built-in support for Heroku and all its Redis database add-ons.

**Where is my user specific data stored?**

All user specific data is stored in zip files named `.pssst.<user>` in the
users home directory.

**How can I make secure HTTPS connections with Python 2?**

If you use Python 2.7 than pyASN1, pyOpenSSL and ndg-httpsclient are also
required to make verified HTTPS connections.

Contact
-------
* If you want to be informed about new releases, general news
  and information about Pssst, please visit our website under:
  https://pssst.name

* If you want to be informed about code updates, bug fixes and
  security fixes of Pssst, please visit our project on GitHub:
  https://github.com/pssst/pssst/

* If you need additional Pssst configurations or utility scripts,
  please visit our other repositories on GitHub:
  https://github.com/pssst/

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
