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
 * Usage: node start.js [--license|--version]
 *
 *   -l --license   print license
 *   -v --version   print version
 */
try {

  // Prerequisite
  var fs  = require('fs');
  var npm = require('./package.json');

  // Check available options
  if (process.argv.length <= 2) {

    // Check config
    if (!fs.existsSync(__dirname + '/config/config.json')) {
      throw "Have you created 'config.json' first?";
    }

    // Required imports
    var express = require('express');
    var config  = require('./config/config.json');
    var routes  = require('./config/routes.js');

    // Required modules
    var server = require('./modules/server.js');
    var debug  = require('./modules/debug.js');
    var redis  = require('./modules/redis.js');
    var pssst  = require('./app/app.js');

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

    redis(config.db, function (err, db) {
      port = Number(process.env.PORT || config.port);

      routes = routes(app, db);
      routes.map(pssst(db));

      server = server(app);
      server.listen(port, function ready() {
        console.log('Pssst', npm['version']);
        console.log('Ready');
      });
    });
  } else {
    switch (process.argv[2]) {

      // Print license
      case '-l':
      case '--license':
        console.log('Licensed under', npm['license']);
        process.exit(0);

      // Print version
      case '-v':
      case '--version':
        console.log('Pssst Server', npm['version']);
        process.exit(0);

      // Print usage
      default:
        console.log('Usage: node server.js [-l|-v]');
        process.exit(2);
    }
  }
} catch (err) {
  console.error(err.stack || err);
  process.exit(1);
}
