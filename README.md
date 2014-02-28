Pssst [![Build](https://travis-ci.org/pssst/pssst.png?branch=master)](https://travis-ci.org/pssst/pssst)
=====
Pssst is a simple and secure way to communicate. We are not a service
provider, but we provide you with the tools to start your own service.
These tools are build upon open source software and strong end-to-end
encryption.

As this project is under continuous development, we advise you to not rely
on our server and run your own. We may change things, we may break things.

Created by Christian & Christian just for the joy of it.

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
$ pssst create sender
```
```
$ pssst create receiver
```
```
$ pssst create receiver.spam
```
```
$ pssst push sender receiver.spam "Hello World!"
```
```
$ pssst pull receiver.spam
```
```
$ pssst delete receiver.spam
```

Names
-----
All Pssst user names are specified by this format:

`pssst.<username>.<boxname>`

All user and box names must be between `2` and `63` characters long and must
only contain of the lowercase letters `a-z` and numbers. The service prefix
`pssst` can be omited, but should be specified for clarity reasons. If the
box name is omited, the users default box `box` is used.

Server
------
If you want to use any other than the official server, simply create a file
named `.pssst` in the directory of the app with the desired server address:

`$ echo https://localhost:443 > app/cli/.pssst`

To setup your own server, please create a valid configuration file first. A
sample configuration can be found with `server/config/config.json.sample`.
When done, execute the following command inside your `server` directory:

`$ npm install && npm start`

The server will now start and print `Ready`.

> We also have built-in support for Heroku with the Redis Cloud add-on.

API
===
The official address of the Pssst REST API is:

`https://api.pssst.name` (alias for `2.pssst.name`)

If you want to test your code, please use the addresses below accordingly.
But we advise you to please setup a local server and database (which
is _very easy_), and test your apps and/or bug fixes there:

* `https://0.pssst.name` reserved for `develop` branch (and other branches)
* `https://1.pssst.name` reserved for `release` branch
* `https://2.pssst.name` reserved for `master` branch

Every branch uses its own redis database. The databases for the `develop` and
`release` branch will be reset each day at midnight. Please be warned:

> **WE DO NOT BACKUP OUR REDIS DATABASES**

Additional informations about the official Pssst server under can be requested
under the following addresses below:

* `https://api.pssst.name/key` returns the servers public key in `PEM` format.
* `https://api.pssst.name/branch` returns the used Git branch.
* `https://api.pssst.name/version` returns the servers version.

Basics
------
All data is encoded in `ASCII` and exchanged in either `JSON` or `plain text`
format with HTTPS requests/responses. Except served static files, which are
encoded in `UTF-8`. Please refer to the mime type in the HTTP `content-type`
header to decide which format and encoding is returned. Server errors will
always be returned in plain text. Please be aware:

> All messages will be stored protocol agnostic.

All clients are requested to send an unique `user-agent` header.

### Keys

All RSA keys have a key size of 4096 bits.

### Encryption

Encryption of the message data is done as follows:

1. Generate cyptographically secure `48` random bytes as message code.
2. Encrypt the data with `AES 256` (`CFB`) using the first `32` bytes from
   the message code as key and the last `16` bytes as IV.
3. Encrypt the message code with `PKCS#1 OAEP` and the receivers public key.

### Decryption

Decryption of the received `data` and `once` is done as follows:

1. Decrypt the received once with `PKCS#1 OAEP` and the receivers private key.
2. Decrypt the data with `AES 256` (`CFB`) using the first `32` bytes from
   the decrypted message code as key and the last `16` bytes as IV.

All encrypted data is exchanged as `JSON` object in the request/response body
with `meta` and `data` fields. The `data` and `once` fields are both encoded 
in `Base64`. Please be aware:

> The message code is called _once_ for a reason. Never use this code twice.

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

### Fingerprint

The public key of the official API has the following `SHA512` fingerprint:

```
47:4c:fa:ac:9f:9d:6d:02:ba:1f:c1:85:cf:41:b4:90
7c:18:74:a5:95:53:fd:47:fc:36:42:73:c5:a5:e6:0f
33:d3:c1:fe:38:3c:03:03:c5:ae:0d:0c:b3:20:64:a0
d6:83:29:dc:cb:80:38:8b:56:97:8e:44:00:0a:32:84
```

If a client connects to the official APIs `master` Branch, it is required to
match the APIs delivered public key against this fingerprint using `SHA512`.
If they do not match, the client must terminate immediately.

User Actions
------------
All user actions, except `find`, must be signed with the senders private key.

### Create

Creates a new user with the given public key. Every user is created with one
default box named `box`.

**Request**

* Action: `POST` `https://api.pssst.name/user/<username>`
* Params: The `<username>` in the address. An JSON object with a `key` field
          in the body, which holds the users public key in `PEM` format.

**Response**

* Result: `200` `User created`
* Format: `text/plain`

### Delete

Deletes the user. All boxes of the user will also be deleted and all message
in there will be lost. The name of this user will be locked and can not be
used afterwards for a new user.

**Request**

* Action: `DELETE` `https://api.pssst.name/user/<username>`
* Params: The `<username>` in the address.

**Response**

* Result: `200` `User disabled`
* Format: `text/plain`

### Find

Returns the users public key.

**Request**

* Action: `GET` `https://api.pssst.name/user/<username>/key`
* Params: The `<username>` in the address.

**Response**

* Result: `200` and the users public key in `PEM` format.
* Format: `text/plain`

### List

Returns a list of the users box names. This list is not accessible
for other users.

**Request**

* Action: `GET` `https://api.pssst.name/user/<username>/list`
* Params: The `<username>` in the address.

**Response**

* Result: `200` and a list of the users box names as strings.
* Format: `application/json`

Box Actions
-----------
All box actions must be signed with the senders private key.

### Create

Creates a new empty box for the user.

**Request**

* Action: `POST` `https://api.pssst.name/user/<username>/<boxname>`
* Params: The `<username>` and `<boxname>` in the address.

**Response**

* Result: `200` `Box created`
* Format: `text/plain`

### Delete

Deletes a box of the user. All messages in this box will be lost.

**Request**

* Action: `DELETE` `https://api.pssst.name/user/<username>/<boxname>`
* Params: The `<username>` and `<boxname>` in the address.

**Response**

* Result: `200` `Box deleted`
* Format: `text/plain`

### Pull

Returns the next message from the users box. Messages will be pulled in order
from first to last. If no box is specified, the default box `box` is used.

**Request**

* Action: `GET` `https://api.pssst.name/user/<username>/<boxname>/`
* Params: The `<username>` and `<boxname>` in the address.

**Response**

* Result: `200` and an JSON object with `once`, `data` and `name` fields.
* Format: `application/json`

### Push

Pushes a message into an users box. If no box is specified, the default box
`box` is used. The sender will be verified with the `name` field in
the body.

**Request**

* Action: `PUT` `https://api.pssst.name/user/<username>/<boxname>/`
* Params: The `<username>` and `<boxname>` in the address. An JSON object with
          `once`, `data` and `name` fields in the body. The `name` field must
          contain the senders name.

**Response**

* Result: `200` `Message pushed`
* Format: `text/plain`

Authors
-------
Please see the files called `AUTHORS` and `THANKS` for more details.

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
