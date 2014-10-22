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
 * HTTP(S) server with authentication.
 *
 * @param {Object} express app
 * @param {Object} config JSON
 * @param {Function} callback
 */
module.exports = function Server(app, config, callback) {

  // Required imports
  var fs = require('fs');
  var http  = require('http');
  var https = require('https');
  var mime  = require('mime');
  var util  = require('util');

  // Required libraries
  var pssst  = require('../app/pssst.js');
  var redis  = require('../lib/redis.js');
  var crypto = require('../lib/crypto.js');

  // Required constants
  var HEADER = 'content-hash';

  var KEY  = __dirname + '/../app/pssst.key';
  var CERT = __dirname + '/../app/pssst.cert';

  mime.default_type = 'text/plain';

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
   * Adds authentication to HTTP(S) requests/responses.
   *
   * @param {Object} database wrapper
   * @param {Object} request
   * @param {Object} response
   * @param {Object} next handler
   */
  function auth(db, req, res, next) {
    if (JSON.stringify(req.body) === '{}') {
      req.body = '';
    }

    // Add current timestamp to the request
    req.timestamp = crypto.now();

    /**
     * Verifies a HTTP(S) request.
     *
     * @param {String} user name or public key
     * @param {Function} callback
     */
    req.verify = function verify(user, callback) {
      function verify(key) {
        var header = req.headers[HEADER];

        // Assert valid public key
        if (!key) {
          return res.sign(404, 'Verification failed');
        }

        // Assert valid signature format
        if (!new RegExp('^[0-9]+; ?[A-Za-z0-9\+/]+=*$').test(header)) {
          return res.sign(400, 'Verification failed');
        }

        // Assert valid signed body
        if (!crypto.verify(req.body, parseHeader(header), key)) {
          return res.sign(401, 'Verification failed');
        }

        callback();
      }

      // Get the public key from database if necessary
      if (user.indexOf('PUBLIC KEY') < 0) {
        db.get(user, function get(err, val) {
          if (!err) {
            verify(val ? val.key : null);
          } else {
            res.error(err);
          }
        });
      } else {
        verify(user);
      }
    };

    /**
     * Sends a signed HTTP(S) response.
     *
     * @param {String} response status
     * @param {String} response body
     * @return {Boolean} always true
     */
    res.sign = function sign(status, body) {
      body = body || '';

      res.setHeader(HEADER, buildHeader(crypto.sign(body)));
      res.status(status).send(body);

      return true;
    };

    /**
     * Sends a signed HTTP(S) error.
     *
     * @param {Object} exception or error
     * @return {Boolean} always true
     */
    res.error = function error(err) {
      console.error(err.stack || err);

      // Don't leak informations
      if (config.debug > 0) {
        return res.sign(500, String(err));
      } else {
        return res.sign(500);
      }
    }

    next();
  }

  /**
   * Serves a static file.
   *
   * @param {Object} request
   * @param {Object} response
   */
  function file(req, res) {
    var file = req.params.file;
    var path = util.format('%s/../www/%s', __dirname, file);

    // Send signed file content with mime type
    fs.readFile(path, 'utf8', function(err, data) {
      if (!err) {
        res.setHeader('content-type', mime.lookup(file));
        res.sign(200, data);
      } else {
        res.sign(404, 'File not found');
      }
    });
  }

  redis(config.db, function (err, db) {
    if (!err) {
      app.use(function (req, res, next) {
        auth(db, req, res, next);
      });

      pssst(app, db, config.deny);

      // Returns current time
      app.get('/time', function time(req, res) {
        res.sign(200, req.timestamp);
      });

      // Returns a static file
      app.get('/:file', file);

      // Returns supported protocols
      app.get('/', function index(req, res) {
        res.sign(200, "Pssst");
      });

      // Returns nothing
      app.get('*', function other(req, res) {
        res.sign(404, 'Not found');
      });

      var port = Number(process.env.PORT || config.port);

      // Create HTTP(S) server
      if (!fs.existsSync(CERT)) {
        http.createServer(app).listen(port, callback);
      } else {
        https.createServer({
          key: fs.readFileSync(KEY, 'utf8'),
          cert: fs.readFileSync(CERT, 'utf8')
        }, app).listen(port, callback);
      }
    } else {
      callback(err);
    }
  });
}
