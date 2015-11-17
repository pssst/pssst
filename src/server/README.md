Server
======
Pssst server.

Usage
-----
```
$ node start [option]
```

Start
-----
Just execute this command to start the server using the default Redis port:

```
$ npm install && node start
```

> The default server port is `62421` and can be changed in the automatically 
> created `config.json` file. A commented sample configuration can be found 
> under `config.json.sample`.

Install
-------
At least [required](packages.json):

* Node.js 0.10
* A Redis database instance

> We also have built-in support for Heroku and all Redis database add-ons.

----
Please use the `--help` option to show further help.
