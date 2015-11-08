![Pssst](http://www.gravatar.org/avatar/2aae9030772d5b59240388522f91468f?s=96)

Pssst [![](https://travis-ci.org/pssst/pssst.svg)](https://travis-ci.org/pssst/pssst)
=====
Pssst is a simple and secure way to exchange information. We do not provide
services by our self, but we provide you with the tools to start your own
service. These tools build upon open-source software and use strong end-to-end
encryption.

As this project is under continuous development, we advise you to not rely on
our test server and run your own. We may change things, we may break things.

Install
-------
### CLI

Required for the command line interface (CLI):

* At least Python 2.7.3
* At least Requests 2.0.1
* At least PyCrypto 2.6.1

There is no need to install anything, just run the `app/cli/pssst.py` script:

`$ pssst.py [option|command]`

If you wish to install the CLI on a POSIX compatible system, just execute:

`$ curl -s https://get.pssst.name | bash`

If you want to use any other server besides our test API, simply create a file
named `.pssst` in your home directory with the desired server address:

`$ echo http://localhost:62421 > ~/.pssst`

Please use the `--help` option to show further help on the CLI. All user
specific data will be stored as zip files named `.pssst.<username>` in
the users home directory.

> If you use Python 2.7 than pyASN1, pyOpenSSL and ndg-httpsclient
> are also required to make verified HTTPS connections.

### GUI

Required for the HTML interface (GUI):

* At least CherryPy 3.2.2
* At least Bower 1.3.12

Please start the GUI with the `app/gui/start` script. This
will download and install all necessary packages.

### Server

Required if you want to run your own server:

* At least Node.js 0.10
* A Redis database server

Please refer to the file `server/package.json` for further details on the
required Node.js modules and their according versions.

> The default server port is `62421`.

Commands
--------
These commands are currently supported by the API:

* `create` an user or a box.
* `delete` an user or a box.
* `find` a users public key.
* `list` all boxes of an user.
* `pull` a message from a box.
* `push` a message onto a box.

### Names

All user names are specified by this format:

`pssst.<username>.<boxname>`

All user and box names must be between 2 and 63 characters long and must only
contain of the lowercase letters a-z and numbers. The service prefix `pssst.`
can be omitted, but should be specified for clarity reasons. If no box name
is given, the users default box named `box` is used.

Examples
--------
In this example we create the users `foo`, `bar` and the box `bar.spam`. Then
pushing a message from `foo` to this box and pulling it by `bar`. And finally
deleting the box _because nobody likes spam_.

```
$ pssst create foo
$ pssst create bar
$ pssst create bar.spam
$ pssst push foo bar.spam "Hello World"
$ pssst pull bar.spam
$ pssst delete bar.spam
```

How To Set Up Your Own Server
-----------------------------
To setup your own server, just execute the following command inside the
`server` directory using a default Redis configuration:

`$ npm install && node start`

The server will now start and create a default configuration file. A commented
sample configuration can be found under `server/config.json.sample`.

> We also have built-in support for Heroku and all its Redis database add-ons.

### Limits

Every user has a fix limit of `512 MB` overall buffered data. This includes
all user specific data, such as the public key, boxes and messages. Neither
the number of the users boxes, nor the size of a message are limited
separately. This is limit is hard coded by the Redis database and may change
in future releases. Only messages not yet pulled by the user will count to
this limit.

> If you want to lower the users limit, please set the `quota` configuration
> setting in your `server/config.json` file.

Resources
---------
Additional resouces can be found here:

### Config

Some useful configs can be found in this repository:

[pssst/pssst-config](https://github.com/pssst/pssst-config)

### Script

Some useful scripts can be found in this repository:

[pssst/pssst-script](https://github.com/pssst/pssst-script)

Security Advisories
-------------------
None known as of today.

Contact
-------
### Authors

Please see the files `AUTHORS` and `THANKS` for further information.

### Resources

* If you want to be informed about new releases, general news
  and information about Pssst, please visit our website under:
  https://pssst.name

* If you want to be informed about code updates, bug fixes and
  security fixes of Pssst, please visit our project on GitHub:
  https://github.com/pssst/pssst/

* If you have a concrete bug report for Pssst please go to the
  Pssst issue tracker on GitHub and submit your report:
  https://github.com/pssst/pssst/issues

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