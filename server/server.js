#!/usr/bin/env node
/**
 * Copyright (C) 2013-2014  Christian & Christian  <pssst@pssst.name>
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
 * Pssst server. Usage: node server.js [-l|-v]
 *
 *   -l --license   print license
 *   -v --version   print version
 *
 * @param {Object} list of arguments
 */
function usage(argv) {
  var license = require('./package.json')['license'];
  var version = require('./package.json')['version'];

  var config = require('./config/config.json');
  var option = argv[2];

  if (argv.length <= 2) {
    return console.log('Pssst! %s (Port %s)', version, config['port']);
  } else {

    // Print license
    if (option === '-l' || option === '--license') {
      console.log('Licensed under', license);
      process.exit();
    }

    // Print version
    if (option === '-v' || option === '--version') {
      console.log('Pssst! Server', version);
      process.exit();
    }

    console.log('Usage: node server.js [-l|-v]');
    process.exit(1);
  }
}

/**
 * Starts the server.
 *
 * @param {Function} callback
 */
function start(ready) {

  // Required imports
  var express = require('express');
  var config  = require('./config/config.json');
  var Router  = require('./config/router.js');

  // Required modules
  var debug = require('./modules/debug.js');
  var Redis = require('./modules/redis.js');
  var SSL   = require('./modules/ssl.js');
  var App   = require('./app/app.js');

  app = express();
  app.set('json spaces', 0);
  app.set('debug', config.debug);

  app.use(express.bodyParser());

  // Error hook
  app.use(function hook(err, req, res, next) {
    res.sendError(err);
  });

  // Debug hook
  app.use(function hook(req, res, next) {
    debug(config.debug, req, res, next);
  });

  new Redis(config.db, function (err, db) {
    router = new Router(app, db);
    router.map(new App(db));

    ssl = new SSL(app);
    ssl.createServer().listen(config.port, ready);
  });
}

/**
 * Ready callback.
 */
function ready() {
  console.log('Ready');
}

/**
 * Error callback.
 */
function error(err) {
  console.error(err.stack ? err.stack : err);
}

try {
  usage(process.argv);
  start(ready);
} catch (err) {
  error(err);
}
