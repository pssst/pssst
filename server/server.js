#!/usr/bin/env node
/*
  Pssst!

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see http://www.gnu.org/licenses/.

  Christian & Christian <pssst@pssst.name>
*/

var express = require('express');
var config  = require('./config/config.json');
var routes  = require('./config/routes.js');
var db      = require('./config/db.js');
var pssst   = require('./app/pssst.js');

function main(argv, fn) {
  if (argv.h || argv.help) {
      console.log('Usage: node server.js');
      return;
  }

  try {

    // Load app
    app = express();
    app.set('json spaces', 0);
    app.set('config', config);
    app.set('db', db(config));
    app.use(express.bodyParser());

    // Debug handling
    app.use(function debug(req, res, next) {

      // Level 1
      if (config.debug > 0) {
        console.log('Pssst!', req.method, req.url);
      }

      // Level 2
      if (config.debug > 1) {
        console.log('Pssst!', req.headers);
      }

      // Level 3
      if (config.debug > 2) {
        console.log('Pssst!', req.body);
      }

      next();
    });

    // Error handling
    app.use(function error(err, req, res, next) {
      console.error(err.stack);
      res.send(500, 'Error');
    });

    // Set routes and app
    routes(app, pssst(app));

    // Up and running...
    app.listen(config.port, fn(null));

  } catch(e) {
    fn(e);
  }
}

main(process.argv, function(err) {
  if (!err) {
    console.log('Pssst! Server (localhost:' + config.port + ')');
  } else {
    console.error(err.stack == undefined ? err : err.stack);
    process.exit(1);
  }
});
