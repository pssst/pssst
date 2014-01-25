Pssst! ![Build Status](https://travis-ci.org/pssst/pssst.png)
======
[Pssst!](https://www.pssst.name) is a simple and secure way to communicate.

Created and maintained by Christian and Christian only for the joy of it
(and hopefully for the joy of others too). Its design is based upon open
standards and the current state of cryptography.

As this project is under continuous development, we advise you to please use
this code and run your own server. We may change things, we may break things.

Install
-------
Required for the command line interface (CLI):

* Python   `2.7.3` or newer
* Requests `2.0.1` or newer
* PyCrypto `2.6.1` or newer

Required if you want to run your own server:

* Node.js `0.10.12` or newer
* A Redis database

Please refer to the file `server/package.json` for further details on the
required NPM modules and their version.

There is no need to install anything, just run the `app/cli/pssst.py` script:

`$ pssst.py [option|command]`

Please use the `--help` option to show further help on the CLI. All user
specific data will be stored as zip files named `.pssst.<username>` in
the current directory.

Commands
--------
Currently these commands are supported by the API:

* `create` an user or a box.
* `delete` an user or a box.
* `find` a users public key.
* `list` all boxes of an user.
* `pull` a message from a box.
* `push` a message onto a box.

Examples
--------
This full-blown real life example will demonstrate you, how to create the
user `sender` and `receiver` as well as the receivers box `spam`. Then
pushing a message from the `sender` to this box and pulling it by the
`receiver`. And finally deleting the box (_because nobody likes spam_).

```
$ pssst.py create sender
```
```
$ pssst.py create receiver
```
```
$ pssst.py create receiver.spam
```
```
$ pssst.py push sender receiver.spam "Hello World!"
```
```
$ pssst.py pull receiver.spam
```
```
$ pssst.py delete receiver.spam
```

Server
------
If you want to use any other than the official server, simply create a file
named `.pssst` in the directory of the app with the desired server address:

`$ echo https://localhost:443 > app/cli/.pssst`

To setup your own server, please create a valid configuration file first. A
sample configuration can be found with `server/config/config.json.sample`.
When done, execute the following command inside your `server` directory:

`$ npm install && node server.js`

The server will now start and print `Ready`.

API
===
The official address of the Pssst API is:

`https://api.pssst.name` (alias for `2.pssst.name`)

If you want to test your code, please use the addresses below accordingly.
But we advise you to please setup a local server and database (which
is _very easy_), and test your apps and/or bug fixes there:

* `https://0.pssst.name` reserved for `develop` branch (and other branches)
* `https://1.pssst.name` reserved for `release` branch
* `https://2.pssst.name` reserved for `master` branch

Every branch uses its own redis database. The databases for the `develop` and
`release` branch will be reset each day at midnight. Please be warned:

> **WE DO NOT BACKUP OUR REDIS DATABASES!**

Additional informations about the official Pssst server under can be requested
under the following addresses below:

* `https://api.pssst.name/key` returns the servers public key in `PEM` format.
* `https://api.pssst.name/branch` returns the used Git branch.
* `https://api.pssst.name/version` returns the servers version.

Basics
------
All data is exchanged in either `JSON` or `plain text` format with HTTPS
requests/responses. Please refer to the mime type in the HTTP `content-type`
header to decide which format is returned. All data is encoded in `UTF-8`.
Server errors will always be returned in plain text. Please be aware:

> All messages will be stored protocol agnostic without any metadata.

All clients are requested to send an unique `user-agent` header.

### Encryption

Encryption of the message data is done as follows:

1. Generate cyptographically secure `48` random bytes as the code.
2. Encrypt the data with `AES 256` (`CFB`) using the first `32` bytes from
   the code as key and the last `16` bytes as IV.
3. Encrypt the code with `PKCS#1 OAEP` and the receivers public key.

### Decryption

Decryption of the received `data` and `code` is done as follows:

1. Decrypt the received code with `PKCS#1 OAEP` and the receivers private key.
2. Decrypt the data with `AES 256` (`CFB`) using the first `32` bytes from
   the derived code as key and the last `16` bytes as IV.

All encrypted data is exchanged as `JSON` object in the request/response body
with `code` and `data` fields, both be encoded in `Base64`.

### Verification

Verification on server and client side is done over the HTTP `content-hash`
header. This header must be set for all client API request, except `find`.
The format of this header is specified as:

`content-hash: <timestamp>; <signature>`

Where `timestamp` is the `EPOCH` (without decimals) of the request/response
and `signature` the calculated and signed hash of the body encoded in
`Base64`. Calculation of the hash is done as follows:

1. Create a `SHA512` HMAC of the HTTP body with the timestamp string as key.
2. Create a `SHA512` hash of the resulting HMAC one more time.
3. Sign the resulting hash with the senders private key using `PKCS#1 v1.5`.

To verify a request/response, calculate its hash as described above in the
steps 1 and 2. And verify it with the senders public key using `PKCS#1 v1.5`.

The default time frame for requests/responses to be verified is `10` seconds.
Which derives to `-5` and `+5` seconds from the actual `EPOCH` at the time of
processing.

User Actions
------------
All user actions, except `find`, must be signed with the senders private key.

### Create

Creates a new user with the given public key. Every user is created with one
default box named `box`.

**Request**

* Action: `POST` `https://api.pssst.name/user/<user>`
* Params: The `<user>` name in the address. An JSON object with a `key` field
          in the body, which holds the users public key in `PEM` format.

**Response**

* Result: `200` `User created`
* Format: `text/plain`

### Delete

Deletes the user. All boxes of the user will also be deleted and all message
in there will be lost. The name of this user will be locked and can not be
used afterwards for a new user.

**Request**

* Action: `DELETE` `https://api.pssst.name/user/<user>`
* Params: The `<user>` name in the address.

**Response**

* Result: `200` `User disabled`
* Format: `text/plain`

### Find

Returns the users public key.

**Request**

* Action: `GET` `https://api.pssst.name/user/<user>/key`
* Params: The `<user>` name in the address.

**Response**

* Result: `200` and the users public key in `PEM` format.
* Format: `text/plain`

### List

Returns a list of the users box names. This list is not accessible
for other users.

**Request**

* Action: `GET` `https://api.pssst.name/user/<user>/list`
* Params: The `<user>` name in the address.

**Response**

* Result: `200` and a list of the users box names as strings.
* Format: `application/json`

Box Actions
-----------
All box actions must be signed with the senders private key.

### Create

Creates a new empty box for the user.

**Request**

* Action: `POST` `https://api.pssst.name/user/<user>/<box>`
* Params: The `<user>` and `<box>` names in the address.

**Response**

* Result: `200` `Box created`
* Format: `text/plain`

### Delete

Deletes a box of the user. All messages in this box will be lost.

**Request**

* Action: `DELETE` `https://api.pssst.name/user/<user>/<box>`
* Params: The `<user>` and `<box>` names in the address.

**Response**

* Result: `200` `Box deleted`
* Format: `text/plain`

### Pull

Returns the next message from the users box. Messages will be pulled in order
from first to last. If no box is specified, the default box `box` is used.

**Request**

* Action: `GET` `https://api.pssst.name/user/<user>/<box>/`
* Params: The `<user>` and `<box>` names in the address.

**Response**

* Result: `200` and an JSON object with `code` and `data` fields.
* Format: `application/json`

### Push

Pushes a message into an users box. If no box is specified, the default
box `box` is used. The `from` field will be deleted from the message,
after the sender was validated on the server.

**Request**

* Action: `PUT` `https://api.pssst.name/user/<user>/<box>/`
* Params: The `<user>` and `<box>` names in the address. An JSON object
          with `from`, `code` and `data` fields in the body.

**Response**

* Result: `200` `Message pushed`
* Format: `text/plain`

Authors
-------
Please see the file called `AUTHORS` for more details.

Contact
-------
* If you want to be informed about new releases, general news
  and information about Pssst, please visit our website under:
  https://www.pssst.name

* If you want to be informed about code updates, bug fixes and
  security fixes of Pssst, please visit our project on GitHub:
  https://github.com/pssst/pssst/

* If you have a concrete bug report for Pssst please go to the
  Pssst issue tracker on GitHub and submit your report:
  https://github.com/pssst/pssst/issues

License
-------
Copyright (C) 2013-2014  Christian & Christian  <pssst@pssst.name>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Fingerprint
-----------
Master API `5a:74:9f:99:db:c2:a0:3b:0c:de:32:7b:af:cf:9b:d7:dc:61:68:30`
