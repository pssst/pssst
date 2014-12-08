Pssst ![Build](https://travis-ci.org/pssst/pssst.png?branch=master)
=====
Pssst is a simple and secure way to exchange information. We do not provide
services by our self, but we provide you with the tools to start your own
service. These tools build upon open-source software and use strong end-to-end
encryption.

As this project is under continuous development, we advise you to not rely on
our test server and run your own. We may change things, we may break things.

Created by Christian & Christian just for the joy of it.

Install
-------
### CLI

Required for the command line interface (CLI):

* At least Python 2.7.3
* At least Requests 2.0.1
* At least PyCrypto 2.6.1

There is no need to install anything, just run the `app/cli/pssst.py` script:

`$ pssst.py [option|command]`

If you wish to install the CLI on a POSIX system, just execute:

`$ curl -s https://get.pssst.name | bash`

If you want to use any other than our test server, simply create a file
named `.pssst` in your home directory with the desired server address:

`$ echo http://localhost:62421 > ~/.pssst`

Please use the `--help` option to show further help on the CLI. All user
specific data will be stored as zip files named `.pssst.<username>` in
the users home directory.

> If you use Python 2.7 the pyASN1, pyOpenSSL and ndg-httpsclient
> packages are also required for verified HTTPS connections.

### GUI

Required for the HTML 5 interface (GUI):

* At least CherryPy 3.2.2
* At least Bower 1.3.12

Please start the GUI with the `app/gui/start` script. This will
download and install all necessary prerequisite.

### Server

Required if you want to run your own server:

* At least Node.js 0.10
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

### Names

All Pssst user names are specified by this format:

`pssst.<username>.<boxname>`

All user and box names must be between 2 and 63 characters long and must only
contain of the lowercase letters a-z and numbers. The service prefix `pssst.`
can be omitted, but should be specified for clarity reasons. If no box name
is given, the users default box `box` is used.

### Example

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

Server
------
To setup your own server, just execute the following command inside your
`server` directory using the default Redis configuration:

`$ npm install && node start`

The server will now start and create a default configuration file. A commented
sample configuration can be found under `server/config.json.sample`.

> We also have built-in support for Heroku and all Redis database add-ons.

### Limits

Every user has a fix limit of `512 MB` overall buffered data. This includes
all user specific data, such as the public key, boxes and messages. Neither
the number of the users boxes, nor the size of a message are limited
separately. This is limit is hard coded by the Redis database and may change
in future releases. Only messages not yet pulled by the user will count to
this limit.

API
===
Our REST API is based on HTTP and designed to be lean and mean. All messages
will be stored protocol agnostic. You can also add more fields to the HTTP
body. The only field required by the server is `head.user`. All clients are
requested to send an unique `user-agent` header per implementation.

URL
---
The official address of our (test) API is:

`https://api.pssst.name`

If you want to test your code, please use the addresses below accordingly.
But we advise you to please setup your own local server and database (which
is _very easy_), and test your apps and/or bug fixes there.

* `https://dev.pssst.name` for the latest `develop` commit
* `https://api.pssst.name` for the latest `master` commit

Each address uses its own Redis server. The database of the `dev` address
will be reset each day at midnight and is not persisted. Please be warned:

> We do not backup our Redis databases. A message can only be pulled once.

Additional informations about the Server can be requested under the following
addresses:

* `https://<server>/`     respond the servers protocol version.
* `https://<server>/key`  respond the servers public key in PEM format.
* `https://<server>/time` respond the servers actual time in EPOCH format.

Cryptography
------------
### Encoding

All data is encoded in ASCII and exchanged in either JSON or plain text format
with HTTPS requests / responses. Except served static files, which are encoded
in UTF-8. Please refer to the mime type in the HTTP `content-type` header to
decide which format and encoding is returned. Server errors will always be
returned in plain text. Line endings must only consists of a `Line Feed`
character.

### Encryption

Encryption of the message `nonce` and `body` is done in the following steps:

1. Generate cyptographically secure 48 random bytes as message code.
2. Encrypt the data with AES-256 (CFB8, no padding) using the first 32
   bytes from the message code as key and the last 16 bytes as IV.
3. Encrypt the message code with PKCS#1 OAEP and the receivers public key.

Please be aware:

> The message code is called _nonce_ for a reason. Never use it twice.

RSA keys are requiered to be 4096 bit strong and encoded in PEM / PKCS#8.

### Decryption

Decryption of the received `nonce` and `body` is done in the following steps:

1. Decrypt the nonce with PKCS#1 OAEP and the receivers private key.
2. Decrypt the data with AES-256 (CFB8, no padding) using the first 32
   bytes from the decrypted message code as key and the last 16 bytes as IV.

All encrypted data is exchanged as JSON object in the request / response body
within `head` and `body` fields. The `head.nonce` and `body` fields are both
encoded in standard Base64 with padding and omitted line breaks.

### Authentication

Authentication for client and server is done via the HTTP `content-hash`
header. This header must be set for all client API request, except `find`.
The format of this header is specified as:

`content-hash: <timestamp>; <signature>`

Where `timestamp` is the EPOCH without decimals of the request / response
and `signature` the calculated and signed hash of the HTTP body encoded in
standard Base64 with padding. Calculation of the hash is done in the following
steps:

1. Create a SHA-512 HMAC of the HTTP body with the timestamp string as key.
2. Create a SHA-512 hash of the resulting HMAC one more time.
3. Sign the resulting hash with the senders private key using PKCS#1 v1.5.

To verify a request / response, calculate its hash as described above in the
steps 1 and 2. And verify it with the senders public key using PKCS#1 v1.5.

The grace period for requests / responses to be verified is 30 seconds. Which
derives to -30 or +30 seconds from the actual EPOCH at the time of
processing.

### Fingerprint

The public key of our official API has the following SHA-1 fingerprint:

`5a:74:9f:99:db:c2:a0:3b:0c:de:32:7b:af:cf:9b:d7:dc:61:68:30`

If a client connects to the official APIs `master` Branch, it is required to
match the APIs delivered public key in PEM format against this fingerprint
using the SHA-1 hash. If they do not match, the client must terminate
immediately.

User Actions
------------
All user actions, except `find`, must be signed with the senders private key.
Only required HTTP headers are listed.

### Create

Creates a new user with the given public key. Every user is created with the
default box `box`. The given key must be in PEM format.

#### Request

```
POST /1/<username> HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-type: application/json
content-hash: <timestamp>; <signature>

{"key":"<key>"}
```

#### Response

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

#### Request

```
DELETE /1/<username> HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

#### Response

```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

User disabled
```

### Find

Returns the users public key in PEM format.

#### Request

```
GET /1/<username>/key HTTP/1.1
host: api.pssst.name
user-agent: <app>
```

#### Response

```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

<key>
```

### List

Returns a list of all user box names. This list is not accessible for other
users.

#### Request

```
GET /1/<username>/list HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

#### Response

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

#### Request

```
POST /1/<username>/<boxname> HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

#### Response

```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

Box created
```

### Delete

Deletes a box of the user. All messages in this box will be lost. The default
box `box` can not be deleted.

#### Request

```
DELETE /1/<username>/<boxname> HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

#### Response

```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

Box deleted
```

### Pull

Returns the next message from the users box. Messages will be pulled in order
from first to last. If no box is specified, the default box `box` is used.
The `head.time` field of the message will be filled in by the server with
the servers current EPOCH timestamp while processing the pushed message.

#### Request

```
GET /1/<username>/<boxname>/ HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-hash: <timestamp>; <signature>
```

#### Response

```
HTTP/1.1 200 OK
content-type: application/json
content-hash: <timestamp>; <signature>

{"head":{"user":"<sender>","nonce":"<nonce>","time":"<time>"},"body":"<data>"}
```

### Push

Pushes a message into an users box. If no box is specified, the default box
`box` is used. The sender will be authenticated with the `head.user` field.

#### Request

```
PUT /1/<username>/<boxname>/ HTTP/1.1
host: api.pssst.name
user-agent: <app>
content-type: application/json
content-hash: <timestamp>; <signature>

{"head":{"user":"<sender>","nonce":"<nonce>"},"body":"<data>"}
```

#### Response

```
HTTP/1.1 200 OK
content-type: text/plain
content-hash: <timestamp>; <signature>

Message pushed
```

Folders
-------
### Config

This folder contains our Redis configurations.

#### Amazon

* `t2.micro.conf` - Redis config for an AWS T2 Micro instance

#### Uberspace

* `live.conf` - Redis database config used by `api.pssst.name`
* `test.conf` - Redis database config used by `dev.pssst.name`

### Script

This folder contains our maintenance and helper scripts.

#### Amazon

* `Dockerfile` - Docker container for Amazon Web Services

#### Debian

* `install.sh` - Installs the CLI (w/o root permission)
* `makedeb.sh` - Creates a Debian package of the CLI
* `makeiso.sh` - Creates a Debian minimal ISO image with user keys and CLI

#### Docker

* `Dockerfile` - Docker container based on Debian

#### Example

* `pssst-pull.py` - Example script derived from the CLI for file pulling
* `pssst-push.py` - Example script derived from the CLI for file pushing
* `pssst-box.sh`  - Displays and persists the latest messages from a box

#### Heroku

* `update-pssst.sh` - Starts a server instance on https://heroku.com

#### Uberspace

* `update-pssst.sh` - Starts a server instance on https://uberspace.de

#### Windows

* `makebin.cmd` - Creates a Windows PE file of the CLI

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
Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
