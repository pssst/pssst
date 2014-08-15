#!/usr/bin/env node
/**
 * Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>
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
 */
try {

  // Prerequisites
  var fs = require('fs');
  var info = require('./package.json');
  var config = JSON.stringify({
    "debug": 0,
    "deny": null,
    "port": 80,
    "db": {
      "source": 6379,
      "number": 0
    }
  }, null, 2);

  // Check given options
  if (process.argv.length <= 2) {

    // Create the default config
    if (!fs.existsSync(__dirname + '/config.json')) {
      fs.writeFileSync(__dirname + '/config.json', config);
    }

    // Required imports
    var express = require('express');
    var parser  = require('body-parser');
    var config  = require('./config.json');

    // Required libraries
    var server = require('./lib/server.js');
    var debug  = require('./lib/debug.js');

    app = express();
    app.set('json spaces', 0);

    // Setup parser
    app.use(parser.urlencoded({extended: true}))
    app.use(parser.json())

    // Error hook
    app.use(function hook(err, req, res, next) {
      res.error(err);
    });

    // Debug hook
    app.use(function hook(req, res, next) {
      debug(config.debug, req, res, next);
    });

    server = server(app, config, function ready(err) {
      if (!err) {
        console.log('Pssst', info['version'], 'ready');
      } else {
        console.error(err);
      }
    });
  } else {
    switch (process.argv[2]) {

      // Print license
      case '-l':
      case '--license':
        console.log('Licensed under', info['license']);
        process.exit(0);

      // Print version
      case '-v':
      case '--version':
        console.log('Pssst', info['version']);
        process.exit(0);

      // Print usage
      default:
        console.log([
          'Usage: node start [OPTION]',
          '',
          '  -l --license   Shows the server license',
          '  -v --version   Shows the server version',
          '',
          'Report bugs to <hello@pssst.name>'
        ].join('\n'));
        process.exit(2);
    }
  }
} catch (err) {
  console.error(err.stack || err);
  process.exit(1);
}
