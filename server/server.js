/*
  Pssst! Einfach. Sicher.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see http://www.gnu.org/licenses/.

  Christian Uhsat <christian@uhsat.de>
*/

var express = require('express');

// Load config
var config = require('./config/config.json');
var cradle = require('./config/cradle.js');
var routes = require('./config/routes.js');

// Load app
app = express();
app.use(express.bodyParser());

// Load pssst
var pssst = require('./app/pssst.js');

// Debug handling
app.use(function debug(req, res, next) {

  // Level 1
  if (config.debug > 0) {
    console.log('Pssst!', req.method, req.url);
  }

  // Level 2
  if (config.debug > 1) {
    console.log('Pssst!', req.body);
  }

  next();
});

// Error handling
app.use(function error(err, req, res, next) {
  console.error(err.stack);
  res.send(500, 'Error');
});

// Setting up routes
routes(app, pssst(config, cradle()));

// Up and running...
app.listen(config.port, function log() {
  console.log('Pssst! Server (localhost:' + config.port + ')');
});
