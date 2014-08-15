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
 *
 * A simple Redis wrapper with Heroku support.
 *
 * @param {Object} the config
 * @param {Function} callback
 */
module.exports = function Redis(config, callback) {

  // Required imports
  var url = require('url');
  var redis = require('redis');

  // Heroku Open Redis add-on
  if (process.env.OPENREDIS_URL) {
    heroku = url.parse(process.env.OPENREDIS_URL);
  }

  // Heroku Redis Cloud add-on
  if (process.env.REDISCLOUD_URL) {
    heroku = url.parse(process.env.REDISCLOUD_URL);
  }

  // Heroku Redis Green add-on
  if (process.env.REDISGREEN_URL) {
    heroku = url.parse(process.env.REDISGREEN_URL);
  }

  // Heroku Redis To Go add-on
  if (process.env.REDISTOGO_URL) {
    heroku = url.parse(process.env.REDISTOGO_URL);
  }

  var client, self = this;

  if (typeof(heroku) != "undefined") {
    client = redis.createClient(heroku.port, heroku.hostname);
    client.auth(heroku.auth.split(':')[1]);
  } else {
    client = redis.createClient(config.source);
  }

  // Error event handler
  client.on('error', function error(err) {
    console.error(err.stack || err);
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

  /**
   * Gets the value of a key (Redis GET).
   *
   * @param {String} the key
   * @param {Function} callback
   */
  this.get = function get(key, callback) {
    client.GET(key, function get(err, val) {
      callback(err, JSON.parse(val));
    });
  };

  /**
   * Sets the value of a key (Redis SET).
   *
   * @param {String} the key
   * @param {Object} the value
   * @param {Function} callback
   */
  this.set = function set(key, val, callback) {
    client.SET(key, JSON.stringify(val, null, 0), function set(err) {
      callback(err);
    });
  };
}
