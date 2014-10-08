Pssst ![Build](https://travis-ci.org/pssst/pssst.png?branch=master)
=====
Pssst is a simple and secure way to exchange information. We do not provide
services by our self, but we provide you with the tools to start your own
service. These tools build upon open-source software and use strong end-to-end
encryption.

As this project is under continuous development, we advise you to not rely
on our server and run your own. We may change things, we may break things.

Created by Christian & Christian just for the joy of it.

Install
-------
### CLI

Required for the command line interface (CLI):

* Python   `2.7.3` or newer
* Requests `2.0.1` or newer
* PyCrypto `2.6.1` or newer

> If you use Python `2.7` the pyASN1, pyOpenSSL and ndg-httpsclient
> module packages are also required for verified HTTPS connections.

There is no need to install anything, just run the `app/cli/pssst.py` script:

`$ pssst.py [option|command]`

If you wish to install the CLI on a POSIX system, just execute:

`$ curl -s https://pssst.name/install | bash`

If you want to use any other than our official server, simply create a file
named `.pssst` in the directory of the CLI with the desired server address:

`$ echo http://localhost:62421 > .pssst`

Please use the `--help` option to show further help on the CLI. All user
specific data will be stored as zip files named `.pssst.<username>` in
the users home directory.

### GUI

Required for the HTML 5 interface (GUI):

* CherryPy `3.2.2` or newer

Please start the GUI with the `app/gui/pssst-gui.sh` script. This will
download and install all necessary prerequisite.

### Server

Required if you want to run your own server:

* Node.js `0.10` or newer
* A Redis database server

Please refer to the file `server/package.json` for further details on the
required Node.js modules and their version.

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

Examples
--------
This example demonstrates you, how to create the users `foo` and `bar` as well
as bars box `spam`. Then pushing a message from `foo` to this box and pulling
it by `bar`. And finally deleting the box (_because nobody likes spam_).

```
$ pssst create foo
```
```
$ pssst create bar
```
```
$ pssst create bar.spam
```
```
$ pssst push foo bar.spam "Hello World"
```
```
$ pssst pull bar.spam
```
```
$ pssst delete bar.spam
```

Names
-----
All Pssst user names are specified by this format:

`pssst.<username>.<boxname>`

All user and box names must be between `2` and `63` characters long and must
only contain of the lowercase letters `a-z` and numbers. The service prefix
`pssst` can be omited, but should be specified for clarity reasons. If the
box name is omited, the users default box `box` is used.

Limits
------
Every user has a fix limit of `512` MB overall buffered data. This includes
all user specific data, such as the public key, boxes and messages. Neither
the number of the users boxes, nor the size of a message are limited
separately. As this is a Redis database limit, it can not be changed by
further requests.

> Only messages not yet pulled by the user will count to this limit.

Server
------
To setup your own server, just execute the following command inside your
`server` directory (the default Redis configuration will be supposed):

`$ npm install && node start`

The server will now start and create a default configuration file. A commented
sample configuration can be found under `server/config.json.sample`.

> We also have built-in support for Heroku and all Redis database add-ons.

API
===
The official address of our REST API is:

`https://api.pssst.name`

If you want to test your code, please use the addresses below accordingly.
But we advise you to please setup your own local server and database (which
is _very easy_), and test your apps and/or bug fixes there.

* `https://dev.pssst.name` for the latest `develop` commit (`develop` branch)
* `https://api.pssst.name` for the latest `stable` release (`master` branch)

Each address uses its own Redis server. The database of the `dev` address
will be reset each day at midnight and is not persisted. Please be warned:

> **We do not backup our Redis databases. A message can only be pulled once.**

Basic informations about the Pssst server can be requested under the following
addresses:

* `https://<server>/key`  is the servers public key in `PEM` format.
* `https://<server>/time` is the servers actual time in `EPOCH` format.

Additional informations about the Pssst server can be requested under the
following addresses:

* `https://<server>/branch`  is the used Git branch.
* `https://<server>/version` is the servers version.

Basics
------
All data is encoded in `ASCII` and exchanged in either `JSON` or `plain text`
format with HTTPS requests/responses. Except served static files, which are
encoded in `UTF-8`. Please refer to the mime type in the HTTP `content-type`
header to decide which format and encoding is returned. Server errors will
always be returned in plain text. Line endings must only consists of a
`Line Feed` character (ASCII code 10). Please be aware:

> All messages will be stored protocol agnostic. You can add more fields to
> the messages body. The only field required by the server is the sender.

Client implementations are requested to send an unique `user-agent` header.

### Keys

All RSA keys have a size of `4096` bits and are encoded in `PEM` / `PKCS#8`.
All AES keys have a size of `256` bits.

### Encryption

Encryption of the message data is done in the following steps:

1. Generate cyptographically secure `48` random bytes as message code.
2. Encrypt the data with `AES 256` (`CFB8`, no padding) using the first `32`
   bytes from the message code as key and the last `16` bytes as IV.
3. Encrypt the message code with `PKCS#1 OAEP` and the receivers public key.

Please be aware:

> **The message code is called _nonce_ for a reason. Never use it twice.**

### Decryption

Decryption of the received `nonce` and `data` is done in the following steps:

1. Decrypt the nonce with `PKCS#1 OAEP` and the receivers private key.
2. Decrypt the data with `AES 256` (`CFB8`, no padding) using the first `32`
   bytes from the decrypted message code as key and the last `16` bytes as IV.

All encrypted data is exchanged as `JSON` object in the request/response body
within `meta` and `data` fields. The `meta.nonce` and `data` fields are both
encoded in standard `Base64` with padding and omitted line breaks.

### Authentication

Authentication for client and server is done over the HTTP `content-hash`
header. This header must be set for all client API request, except `find`.
The format of this header is specified as:

`content-hash: <timestamp>; <signature>`

Where `timestamp` is the `EPOCH` (without decimals) of the request/response
and `signature` the calculated and signed hash of the body encoded in standard
`Base64` with padding. Calculation of the hash is done in the following steps:

1. Create a `SHA512` HMAC of the HTTP body with the timestamp string as key.
2. Create a `SHA512` hash of the resulting HMAC one more time.
3. Sign the resulting hash with the senders private key using `PKCS#1 v1.5`.

To verify a request/response, calculate its hash as described above in the
steps 1 and 2. And verify it with the senders public key using `PKCS#1 v1.5`.

The grace period for requests/responses to be verified is `30` seconds. Which
derives to `-30` or `+30` seconds from the actual `EPOCH` at the time of
processing.

### Fingerprint

The public key of our official API has the following `SHA1` fingerprint:

**`56:3c:b9:03:19:92:f5:03:a2:1f:3f:a7:be:16:05:67:f1:38:04:67`**

If a client connects to the official APIs `master` Branch, it is required to
match the APIs delivered public key against this fingerprint using `SHA1`.
If they do not match, the client must terminate immediately.

User Actions
------------
All user actions, except `find`, must be signed with the senders private key.
Only required HTTP headers are listed.

### Create

Creates a new user with the given public key. Every user is created with one
default box named `box`. The given key must be in `PEM` format.

**Request**
```
POST /1/<username> HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-type: application/json
content-hash: <timestamp>; <signature>

{"key":"<key>"}
```

**Response**
```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

User created
```

### Delete

Deletes the user. All boxes of the user will also be deleted and all message
in there will be lost. Messages pushed by the user will not be affected. The
name of this user will be locked and can not be used afterwards for by other
users.

**Request**
```
DELETE /1/<username> HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

**Response**
```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

User disabled
```

### Find

Returns the users public key in `PEM` format.

**Request**
```
GET /1/<username>/key HTTP/1.1
host: api.pssst.name
user-agent: <app>
```

**Response**
```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

<key>
```

### List

Returns a list of all user box names. This list is not accessible for other
users.

**Request**
```
GET /1/<username>/list HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

**Response**
```
HTTP/1.1 200 OK
content-type: application/json
content-hash: <timestamp>; <signature>

["box",<boxes>]
```

Box Actions
-----------
All box actions must be signed with the senders private key. Only required
HTTP headers are listed.

### Create

Creates a new empty box for the user. The box names `box`, `key` and `list`
are restricted because of protocol usage.

**Request**
```
POST /1/<username>/<boxname> HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

**Response**
```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

Box created
```

### Delete

Deletes a box of the user. All messages in this box will be lost. The default
box `box` can not be deleted.

**Request**
```
DELETE /1/<username>/<boxname> HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

**Response**
```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

Box deleted
```

### Pull

Returns the next message from the users box. Messages will be pulled in order
from first to last. If no box is specified, the default box `box` is used.
The `meta.time` field of the message will be filled in by the server with
the servers current `EPOCH` timestamp while processing the pushed message.

**Request**
```
GET /1/<username>/<boxname>/ HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

**Response**
```
HTTP/1.1 200 OK
content-type: application/json
content-hash: <timestamp>; <signature>

{"meta":{"user":"<sender>","nonce":"<nonce>","time":"<time>"},"data":"<data>"}
```

### Push

Pushes a message into an users box. If no box is specified, the default box
`box` is used. The sender will be authenticated with the `meta.user` field
in the body.

**Request**
```
PUT /1/<username>/<boxname>/ HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-type: application/json
content-hash: <timestamp>; <signature>

{"meta":{"user":"<sender>","nonce":"<nonce>"},"data":"<data>"}
```

**Response**
```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

Message pushed
```

Script
------
In the `script` folder you will find the maintenance scripts we use:

**Debian**

* `install.sh` - Installs the CLI (w/o root permission)
* `makedeb.sh` - Creates a Debian package of the CLI
* `makeiso.sh` - Creates a Debian minimal `ISO` image with user keys and CLI
* `notify.sh`  - Displays the latest message with a desktop notification

**Docker**

* `Dockerfile`  - Builds a docker container

**Heroku**

* `checkout.sh` - Builds and starts a server instance on _Heroku_

**Uberspace**

* `checkout.sh` - Builds and starts a server instance on _Uberspace_

Config
------
In the `config` folder you will our Redis server configurations:

**Uberspace**

* `live.conf` - Redis database config used by `api.pssst.name`
* `test.conf` - Redis database config used by `dev.pssst.name`

Authors
-------
Please see the files `AUTHORS` and `THANKS` for further information.

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

License
-------
Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
