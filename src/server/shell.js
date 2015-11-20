#!/usr/bin/env node
/**
 * Copyright (C) 2013-2015  Christian & Christian  <hello@pssst.name>
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

  // Required constants
  var CONFIG = __dirname + '/config.json';
  var PROMPT = 'pssst> ';

  /**
   * Returns a shell object.
   *
   * @param {Function} callback
   */
  function shell(callback) {
    callback = callback || function noop() {};

    // Assert the config exists
    if (!fs.existsSync(CONFIG)) {
      throw new Error('Config not found');
    }

    // Required imports
    var config = require('./config.json');
    var redis = require('./lib/redis.js');

    /**
     * Gets and sets an database object.
     *
     * @param {String} key
     * @param {Function} action
     */
    function loopback(key, action) {
      redis(config.db, function redis(err, db) {
        if (err) {
          return console.error(err);
        }

        // Get database object
        db.get(key, function get(err, value) {
          if (err) {
            return console.error(err);
          }

          var result = action(value);

          // Set database object
          if (result !== null) {
            db.set(key, result, function set(err) {
              if (err) {
                return console.error(err);
              } else {
                callback();
              }
            });
          } else {
            callback();
          }
        });
      });
    }

    // Returns the shell instance
    return {
      execute: function execute(line) {
        var argv = line.split(/\s+/);

        switch (argv[0].toLowerCase()) {

          // Edit an object
          case 'edit':
            var key = argv[2];
            var val = argv[3];

            loopback(argv[1], function edit(object) {
              object[key] = eval(val);
              return object;
            });
            break;

          // View an object
          case 'view':
            loopback(argv[1], function view(object) {
              console.log(object);
              return object;
            });
            break;

          // Print error
          default:
            console.error('Command not found');
            callback();
            break;
        }
      }
    };
  }

  // Check given options
  if (process.argv.length <= 2) {
    console.log('Pssst Shell', info['version']);
    console.log('Use \'help\' for a list of commands.');

    // Using build-in readline
    var readline = require('readline');
    var readline = readline.createInterface(
      process.stdin,
      process.stdout
    );

    /**
     * Sets the default prompt.
     */
    function prompt() {
      readline.setPrompt(PROMPT, PROMPT.length);
      readline.prompt();
    }

    var shell = shell(prompt);

    // Handle shell closing
    readline.on('close', function () {
      process.exit(0);
    });

    // Handle shell command
    readline.on('line', function (line) {
      var argv = line.split(/\s+/);

      switch (argv[0].toLowerCase()) {

        // Exit shell
        case 'exit':
          readline.close();
          process.stdin.destroy();
          break;

        // Print help
        case 'help':
          console.log([
            'Redis commands:',
            '  edit <object> <key> <value>',
            '  view <object>',
            'Shell commands:',
            '  exit',
            '  help',
          ].join('\n'));
          prompt();
          break;

        // Empty line
        case '':
          prompt();
          break;

        // Execute line
        default:
          shell.execute(line);
          break;
      }
    });

    // Ready
    prompt();
  } else {
    switch (process.argv[2].toLowerCase()) {

      // Execute command
      case '-e':
      case '--exec':
        shell(process.exit).execute(process.argv.slice(3).join(' '));
        break;

      // Print license
      case '-l':
      case '--license':
        console.log('Licensed under', info['license']);
        process.exit(0);

      // Print version
      case '-v':
      case '--version':
        console.log('Pssst Shell', info['version']);
        process.exit(0);

      // Print usage
      default:
        console.log([
          'Usage: node shell [option] [command]',
          '',
          '  -e --exec      Executes a command',
          '  -l --license   Shows license',
          '  -v --version   Shows version',
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
