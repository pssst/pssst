// Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>
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

/**
 * Routes for extending and app mapping.
 *
 * @param {Object} express app
 * @param {Object} redis wrapper
 */
module.exports = function Routes(app, redis) {

  // Required imports
  var http = require('http');
  var util = require('util');

  // Required modules
  var file   = require('../modules/file.js');
  var crypto = require('../modules/crypto.js')();

  // Required constants
  var FORMAT = '^[0-9]+; ?[A-Za-z0-9\+/]+=*$';
  var HEADER = 'content-hash';

  var routes = {
    user: '/user/:user',
    key:  '/user/:user/key',
    list: '/user/:user/list',
    box:  '/user/:user/:box?*'
  };

  /**
   * Checks if data is a valid (PEM) key.
   *
   * @param {String} the public key
   * @return {Boolean} true if valid
   */
  function isValidKey(data) {
     return (data.indexOf('BEGIN PUBLIC KEY') >= 0);
  }

  /**
   * Returns the new header.
   *
   * @param {Object} signature
   * @return {String} the header
   */
  function buildHeader(signature) {
    return util.format('%s; %s', signature.timestamp, signature.signature);
  }

  /**
   * Returns the parsed signature.
   *
   * @param {String} the header
   * @return {Object} signature
   */
  function parseHeader(header) {
    var token = header.split(';', 2);

    return {
      timestamp: token[0].trim(),
      signature: token[1].trim()
    };
  }

  /**
   * Verifies a HTTP request.
   *
   * @param {Object} request
   * @param {Object} response
   * @param {String} public key
   * @param {Object} callback
   * @return {Object} the header
   */
  function verifyRequest(req, res, pub, callback) {
    var header = req.headers[HEADER];

    // Assert valid public key string
    if (!pub) {
      return res.sendSigned(404, 'Verification failed');
    }

    // Assert valid signature format
    if (!new RegExp(FORMAT).test(header)) {
      return res.sendSigned(400, 'Verification failed');
    }

    // Assert valid signature information
    if (!crypto.verify(req.body, parseHeader(header), pub)) {
      return res.sendSigned(401, 'Verification failed');
    }

    callback();
  }

  // Add crypto methods to requests/responses
  app.use(function middleware(req, res, next) {

    /**
     * Verifies a HTTP request.
     *
     * @param {String} public key or user name
     * @param {Function} callback
     */
    req.verify = function verify(user, callback) {
      if (!isValidKey(user)) {

        // Get public key from database if necessary
        redis.get(user, function get(err, val) {
          if (!err) {
            verifyRequest(req, res, val ? val.key : null, callback);
          } else {
            res.sendError(err);
          }
        });
      } else {
        verifyRequest(req, res, user, callback);
      }
    };

    /**
     * Sends a signed HTTP response.
     *
     * @param {String} response status
     * @param {String} response body
     * @return {Boolean} always true
     */
    res.sendSigned = function sendSigned(status, body) {
      body = body || '';

      res.setHeader(HEADER, buildHeader(crypto.sign(body)));
      res.send(status, body);

      return true;
    };

    /**
     * Sends a signed HTTP error response.
     *
     * @param {Object} exception
     * @return {Boolean} true if error was sent
     */
    res.sendError = function sendError(err) {
      console.error(String(err));
      console.error(err.stack);

      // Apply information hiding
      if (config.debug > 0) {
        return res.sendSigned(500, String(err));
      } else {
        return res.sendSigned(500);
      }
    }

    next();
  });

  /**
   * Map all available routes.
   *
   * @param {Object} pssst
   */
  this.map = function map(pssst) {

    // Pssst.User CRUD
    app.post(routes.user, pssst.user.create);
    app.get(routes.key, pssst.user.key);
    app.get(routes.list, pssst.user.list);
    app.delete(routes.user, pssst.user.disable);

    // Pssst.Box CRUD
    app.post(routes.box, pssst.box.create);
    app.get(routes.box, pssst.box.pull);
    app.put(routes.box, pssst.box.push);
    app.delete(routes.box, pssst.box.erase);

    // Time server
    app.get('/time', function time(req, res) {
      res.send(Number((new Date()).getTime() / 1000).toFixed(0));
    });

    // File server
    app.get('/:file', file('public').serve);

    // Server index
    app.get('/', function index(req, res) {
      res.redirect('https://pssst.name');
    });

    // Server other
    app.get('*', function other(req, res) {
      res.send(404, 'Not found');
    });
  };

  return this;
}
