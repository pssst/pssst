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
 * Pssst rounting and handling.
 *
 * @param {Object} express app
 * @param {Object} database wrapper
 * @param {String} denied names regex
 */
module.exports = function Pssst(app, db, deny) {

  // Required static classes
  var User = require('./pssst.user.js');
  var Box  = require('./pssst.box.js');

  // Pssst API version 1
  var api = {
    /**
     * Handles and verifies a request.
     *
     * @param {Object} request
     * @param {Object} response
     * @param {Function} callback
     * @param {Mixed} verify sender
     */
    request: function request(req, res, callback, auth) {
      req.params.box = req.params.box || 'box'; // Default

      // Assert valid user name
      if (!new RegExp('^[a-z0-9]{2,63}$').test(req.params.user)) {
        return res.sign(400, 'User name invalid');
      }

      // Assert valid box name
      if (!new RegExp('^[a-z0-9]{2,63}$').test(req.params.box)) {
        return res.sign(400, 'Box name invalid');
      }

      // Pass verification
      if (auth === false) {
        req.verify = function pass(unused, callback) {
          callback();
        };
      }

      req.verify(auth || req.params.user, function verify() {
        db.get(req.params.user, function get(err, user) {
          if (err) {
            return res.error(err);
          }

          // Assert user is not deleted
          if (user !== null && User.isDeleted(user)) {
            return res.sign(410, 'User was deleted');
          }

          // Assert user exists
          if (user === null && req.method !== 'POST') {
            return res.sign(404, 'User not found');
          }

          if (user && req.params.box) {
            var box = Box.find(user, req.params.box);

            // Assert box exists
            if (box === null && req.method !== 'POST') {
              return res.sign(404, 'Box not found');
            }
          }

          var body = callback(user, box || null);

          // Send response
          if (typeof body === 'string') {
            api.respond(req, res, user, body);
          }
        });
      });
    },

    /**
     * Stores changes and sends a response.
     *
     * @param {Object} request
     * @param {Object} response
     * @param {Object} the user
     * @param {String} response body
     */
    respond: function respond(req, res, user, body) {
      db.set(req.params.user, user, function set(err) {
        if (err) {
          res.error(err);
        } else if (body) {
          res.sign(200, body);
        } else {
          res.sign(204);
        }
      })
    }
  };

  /**
   * Creates an user.
   */
  app.post('/1/:user', function create(req, res) {
    api.request(req, res, function request(user, box) {

      // Assert user name is allowed
      if (User.isDenied(req.params.user, deny)) {
        return res.sign(403, 'User name restricted');
      }

      // Assert user does not exist
      if (user !== null) {
        return res.sign(409, 'User already exists');
      }

      // Assert key is a public key
      if (req.body.key.indexOf('PUBLIC KEY') < 0) {
        return res.sign(400, 'Public key invalid');
      }

      user = User.create(req.body.key);

      api.respond(req, res, user, 'User created');
    }, req.body.key);
  });

  /**
   * Deletes an user.
   */
  app.delete('/1/:user', function erase(req, res) {
    api.request(req, res, function request(user, box) {
      User.erase(user);

      return 'User deleted';
    });
  });

  /**
   * Gets the public key of an user.
   */
  app.get('/1/:user/key', function key(req, res) {
    api.request(req, res, function request(user, box) {
      res.sign(200, user.key);
    }, false);
  });

  /**
   * Lists all box names.
   */
  app.get('/1/:user/list', function list(req, res) {
    api.request(req, res, function request(user, box) {
      res.sign(200, Box.list(user));
    });
  });

  /**
   * Creates a new box.
   */
  app.post('/1/:user/:box?', function create(req, res) {
    api.request(req, res, function request(user, box) {

      // Assert box name is allowed
      if (Box.isDenied(req.params.box)) {
        return res.sign(403, 'Box name restricted');
      }

      // Assert box does not exist
      if (box !== null) {
        return res.sign(409, 'Box already exists');
      }

      Box.create(user, req.params.box);

      return 'Box created';
    });
  });

  /**
   * Deletes a box.
   */
  app.delete('/1/:user/:box?', function erase(req, res) {
    api.request(req, res, function request(user, box) {

      // Assert box name is allowed
      if (Box.isDenied(req.params.box)) {
        return res.sign(403, 'Box name restricted');
      }

      Box.erase(user, req.params.box);

      return 'Box deleted';
    });
  });

  /**
   * Pushes a message into a box.
   */
  app.put('/1/:user/:box?', function push(req, res) {
    api.request(req, res, function request(user, box) {

      // Add request timestamp to message
      req.body.head.time = req.timestamp;

      box.push(req.body);

      return 'Message sent';
    }, req.body.head.user);
  });

  /**
   * Pulls a message from a box.
   */
  app.get('/1/:user/:box?', function pull(req, res) {
    api.request(req, res, function request(user, box) {
      api.respond(req, res, user, box.pull());
    });
  });

  return this;
}
