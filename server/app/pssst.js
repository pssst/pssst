/*
  Pssst!

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see http://www.gnu.org/licenses/.

  Christian Uhsat <christian@uhsat.de>
*/

module.exports = function(config) {

  var fs = require('fs');
  var mime = require('mime');

  // DB module
  var mod = require('../config/db.js');
  var db = mod(config);

  // Crypto module
  var mod = require('../config/crypto.js')
  var crypto = mod(config);

  var enc = 'utf8';
  var dir = __dirname + '/../static/';

  // Setup mimetype
  mime.default_type = 'text/plain';

  // Pssst object
  return {

    // Static files
    static: function(req, res) {
      extend(req, res);

      fs.readFile(dir + req.params.file, enc, function (err, data) {
        if (!err) {
          res.setHeader('content-type', mime.lookup(req.params.file));
          res.sign(200, data);
        } else {
          res.sign(404, 'File not found');
        }
      });
    },

    // Create user
    create: function(req, res) {
      extend(req, res);

      // Test user name
      if (new RegExp(config.name).test(req.params.user)) {
        return res.sign(400, 'User name restricted');
      }

      // Find user
      findUser(req, res, db, function(user) {
        res.sign(409, 'User already exists');
      }, function(name) {
        req.verify(req.body.key, function() {

          // Create user object
          db.save(name, {
            'key': req.body.key,
            'boxes': {
              'all': []
            }
          }, function(err, doc) {
            if (!err) {
              res.sign(201, 'User created');
            } else {
              res.sign(500, err);
            }
          });

        });
      });
    },

    // Find user key
    find: function(req, res) {
      extend(req, res);

      findUser(req, res, db, function(user) {
        res.sign(200, user.key);
      });
    },

    // Push message into box
    push: function(req, res) {
      extend(req, res);

      findUser(req, res, db, function(user) {
        findBox(req, res, user, function(box) {
          req.verify(req.body.meta.from, function() {
            box.push(req.body);
            res.sign(201, 'Message created');
          });
        });
      });
    },

    // Pull message from box
    pull: function(req, res) {
      extend(req, res);

      findUser(req, res, db, function(user) {
        findBox(req, res, user, function(box) {
          req.verify(req.params.user, function() {
            var body = box.pull();

            if (body != undefined) {
              res.sign(200, body);
            } else {
              res.sign(204);
            }
          });
        });
      });
    }
  };

  // Extend express objects
  function extend(req, res) {

    // Sign response
    res.sign = function(status, body) {
      if (body == undefined) {
        body = '';
      }

      var sig = crypto.sign(body);

      res.setHeader('content-hash', sig.sec + '; ' + sig.sig);
      res.send(status, body);
    };

    // Verify request
    req.verify = function(pem, fn) {
      if (pem[0] != '-') {
        db.get(pem, function(err, user) {
          pem = err ? null : user.key;
        });
      }

      var sig = req.headers['content-hash'];

      if (new RegExp('^[0-9]+; ?[A-Za-z0-9\+/]+=*$').test(sig)) {
        var sig = sig.split(';', 2);

        if (pem && crypto.verify(req.body, sig[0], sig[1], pem)) {
          fn();
        } else {
          res.sign(403, 'Forbidden');
        }
      } else {
        res.sign(400, 'Bad Request');
      }
    };
  }

  // Find user
  function findUser(req, res, db, success, failure) {
    if (!new RegExp('^[a-z0-9]{2,63}$').test(req.params.user)) {
      res.sign(400, 'User name invalid');
    } else {
      db.get(req.params.user, function(err, user) {
        if (err) {
          if (failure != undefined) {
            failure(req.params.user);
          } else {
            res.sign(404, 'User not found');
          }
        } else {
          success(user);
        }
      });
    }
  }

  // Find box
  function findBox(req, res, user, success) {
    if (req.params.box != null) {
      var name = req.params.box;
    } else {
      var name = "all";
    }

    if (name in user.boxes) {
      success({

        // Save message
        push: function(data) {
          user.boxes[name].push(data);
        },

        // Load message
        pull: function() {
          return user.boxes[name].shift();
        }

      });
    } else {
      res.sign(404, 'Box not found');
    }
  }
}
