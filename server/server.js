#!/usr/bin/env node
/**
 * Pssst!
 * Copyright (C) 2013  Christian & Christian  <pssst@pssst.name>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Pssst server. Usage: node server.js [option]
 *
 *   -l --license   print license
 *   -v --version   print version
 *
 * Available debug levels:
 *
 *   0 = off
 *   1 = print request method and url
 *   2 = print request headers also
 *   3 = print request body also
 *
 * @param {Function} callback
 */
function start(ready) {

  // Required imports
  var express = require('express');
  var config  = require('./config/config.json');
  var Router  = require('./config/router.js');

  // Required modules
  var Redis = require('./modules/redis.js');
  var App   = require('./app/app.js');

  app = express();
  app.set('json spaces', 0);
  app.set('debug', config.debug);

  app.use(express.bodyParser());

  // Error hook
  app.use(function error(err, req, res, next) {
    res.sendError(err);
  });

  // Debug hook
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

  new Redis(config.db, function (err, db) {
    router = new Router(app, db);
    router.map(new App(db));

    app.listen(config.port, ready);
  });
}

/**
 * Ready callback.
 */
function ready() {
  console.log('Pssst! is listening...');
}

/**
 * Error callback.
 */
function error(err) {
  console.error(err.stack ? err.stack : err);
}

if (process.argv.length > 2) {
  var option = process.argv[2];
  var config = require('./package.json');

  // License option
  if (option === '-l' || option === '--license') {
    return console.log('License %s', config['license']);
  }

  // Version option
  if (option === '-v' || option === '--version') {
    return console.log('Version %s', config['version']);
  }

  // Any other option
  return console.log('Usage: node server.js [option] \n'
                   + '                               \n'
                   + '  -l, --license   Shows license\n'
                   + '  -v, --version   Shows version\n');
}

try {
  start(ready);
} catch (err) {
  error(err);
}
