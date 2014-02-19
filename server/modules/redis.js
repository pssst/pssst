// Copyright (C) 2013-2014  Christian & Christian  <pssst@pssst.name>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.


// A simple Redis wrapper. The Database number is based on the Git branch:
//
//   0 = develop
//   1 = release
//   2 = master
//
// @param {Object} the config
// @param {Function} callback
//
module.exports = function Redis(config, callback) {
  var self = this;

  // Required imports
  var url   = require('url');
  var redis = require('redis');

  var client;

  // Heroku support
  if (process.env.REDISCLOUD_URL) {
    var url = url.parse(process.env.REDISCLOUD_URL);

    client = redis.createClient(url.port, url.hostname);
    client.auth(url.auth.split(':')[1]);
  } else {
    client = redis.createClient(config.source);
  }

  // Error event handler
  client.on('error', function error(err) {
    console.error(err.stack ? err.stack : err);
  });

  // Ready event handler
  client.on('ready', function ready(err) {
    if (err) {
      return callback(err);
    }

    client.select(config.number, function select(err) {
      return callback(err, self);
    });
  });

  // Gets the value to the key.
  //
  // @param {String} the key
  // @param {Function} callback
  //
  this.get = function get(key, callback) {
    client.get(key, function get(err, val) {
      try {
        callback(err, JSON.parse(val));
      } catch (err) {
        callback(err);
      }
    });
  };

  // Sets the value to the key.
  //
  // @param {String} the key
  // @param {Object} the value
  // @param {Function} callback
  //
  this.set = function set(key, val, callback) {
    client.set(key, JSON.stringify(val), function set(err) {
      try {
        callback(err);
      } catch (err) {
        callback(err);
      }
    });
  };
}
