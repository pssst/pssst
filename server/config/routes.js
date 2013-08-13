/*
  Pssst!

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see http://www.gnu.org/licenses/.

  Christian & Christian <pssst@pssst.name>
*/

module.exports = function(app, pssst) {

  var module = require('./crypto.js');
  var crypto = module();

  var module = require('./file.js');
  var static = module('static');

  var config = app.get('config');

  var db = app.get('db');

  // Extend express objects
  function ext(fn) {

    // Process 'content-hash' HTTP header
    return function(req, res) {

      // Sign response
      res.sign = function(status, body) {
        body = body || '';

        var sig = crypto.sign(body);

        res.setHeader('content-hash', sig.sec + '; ' + sig.sig);
        res.send(status, body);
      }

      // Verify request
      req.verify = function(pem) {
        if (pem.indexOf('BEGIN PUBLIC KEY') < 0) {
          var doc = db.load(pem);

          if (doc) {
            pem = doc.key;
          } else {
            pem = null;
          }
        }

        if (!pem) {
          throw {status: 404, message: 'Verification failed'};
        }

        var sig = req.headers['content-hash'];

        if (!new RegExp('^[0-9]+; ?[A-Za-z0-9\+/]+=*$').test(sig)) {
          throw {status: 400, message: 'Verification failed'};
        }

        var sig = sig.split(';', 2);

        if (!crypto.verify(req.body, sig[0], sig[1], pem)) {
          throw {status: 401, message: 'Verification failed'};
        }
      }

      try {
        fn(req, res);
      } catch(e) {
        if (config.debug > 0) {
          console.error(e);
        }

        if (e.status && e.message) {
          res.sign(e.status, e.message);
        } else {
          res.sign(500);
        }
      }
    }
  }


  // Create user
  app.post('/user/:user', ext(pssst.user.create));

  // Delete user
  app.delete('/user/:user', ext(pssst.user.delete));

  // Find user
  app.get('/user/:user/key', ext(pssst.user.find));


  // Create box
  app.post('/user/:user/:box?*', ext(pssst.box.create));

  // Delete box
  app.delete('/user/:user/:box?*', ext(pssst.box.delete));

  // Push message
  app.put('/user/:user/:box?*', ext(pssst.box.push));

  // Pull message
  app.get('/user/:user/:box?*', ext(pssst.box.pull));


  // Static files
  app.get('/:file', ext(static));


  // All other
  app.get('*', function route(req, res) {
    res.send(404);
  });
}
