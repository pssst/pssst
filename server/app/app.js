/**
 * Pssst!
 * Copyright (C) 2013  Christian & Christian  <pssst@pssst.name>
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
 * Facade class for easy mapping.
 *
 * @param {Object} redis wrapper
 */
module.exports = function App(redis) {

  // Required classes
  var Pssst = require('./pssst.js');
  var User  = require('./pssst.user.js');
  var Box   = require('./pssst.box.js');

  var pssst = new Pssst(redis);

  return {

    user: {

      /**
       * Creates a new user.
       *
       * @param {Object} request
       * @param {Object} response
       */
      create: function create(req, res) {
        pssst.handle(req, res, function handle(user, box) {

          // Assert user name is allowed
          if (User.isBlocked(req.params.user)) {
            return res.sendSigned(403, 'User name restricted');
          }

          // Assert user does not exist
          if (user !== null) {
            return res.sendSigned(409, 'User already exists');
          }

          pssst.respond(req, res, User.create(req.body.key), 'User created');
        }, {
          verify: req.body.key
        });
      },

      /**
       * Deletes (disables) an user.
       *
       * @param {Object} request
       * @param {Object} response
       */
      disable: function disable(req, res) {
        pssst.handle(req, res, function handle(user, box) {
          User.disable(user);
        }, {
          status: 'User disabled'
        });
      },

      /**
       * Lists all box names.
       *
       * @param {Object} request
       * @param {Object} response
       */
      list: function list(req, res) {
        pssst.handle(req, res, function handle(user, box) {
          res.sendSigned(200, Box.list(user));
        });
      },

      /**
       * Gets the public key of the user.
       *
       * @param {Object} request
       * @param {Object} response
       */
      key: function key(req, res) {
        pssst.handle(req, res, function handle(user, box) {
          res.sendSigned(200, user.key);
        }, {
          verify: false
        });
      }
    },

    box: {

      /**
       * Creates a new box.
       *
       * @param {Object} request
       * @param {Object} response
       */
      create: function create(req, res) {
        pssst.handle(req, res, function handle(user, box) {

          // Assert box name is allowed
          if (Box.isBlocked(req.params.box)) {
            return res.sendSigned(403, 'Box name restricted');
          }

          // Assert user does not exist
          if (box !== null) {
            return res.sendSigned(409, 'Box already exists');
          }

          Box.create(user, req.params.box);
        }, {
          status: 'Box created'
        });
      },

      /**
       * Deletes (erases) a box.
       *
       * @param {Object} request
       * @param {Object} response
       */
      erase: function erase(req, res) {
        pssst.handle(req, res, function handle(user, box) {

          // Assert box name is allowed
          if (Box.isBlocked(req.params.box)) {
            return res.sendSigned(403, 'Box name restricted');
          }

          Box.erase(user, req.params.box);
        }, {
          status: 'Box deleted'
        });
      },

      /**
       * Pushes a message to a box.
       *
       * @param {Object} request
       * @param {Object} response
       */
      push: function push(req, res) {
        pssst.handle(req, res, function handle(user, box) {

          // Intentionally forget sender
          delete req.body.from;

          box.push(req.body);
        }, {
          verify: req.body.from,
          status: 'Message sent'
        });
      },

      /**
       * Pulls a message from a box.
       *
       * @param {Object} request
       * @param {Object} response
       */
      pull: function pull(req, res) {
        pssst.handle(req, res, function handle(user, box) {
          pssst.respond(req, res, user, box.pull());
        });
      }
    }
  }
}
