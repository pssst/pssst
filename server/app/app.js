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
 * Facade class for easy mapping.
 *
 * @param {Object} redis wrapper
 */
module.exports = function App(redis) {

  // Required classes
  var pssst = require('./pssst.js');
  var pssst = pssst(redis);

  /**
   * Adds the current epoch to a message.
   *
   * @param {Object} the message
   * @return {Object} the message
   */
  function addTime(message) {
    message.meta.time = Number((new Date).getTime() / 1000).toFixed(0);
    
    return message;
  }

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
          if (pssst.user.isDenied(req.params.user, config.deny)) {
            return res.sendSigned(403, 'User name denied');
          }

          // Assert user does not exist
          if (user !== null) {
            return res.sendSigned(409, 'User already exists');
          }

          user = pssst.user.create(req.body.key);

          pssst.respond(req, res, user, 'User created');
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
          pssst.user.disable(user);
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
          res.sendSigned(200, pssst.box.list(user));
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
          if (pssst.box.isBlocked(req.params.box)) {
            return res.sendSigned(403, 'Box name restricted');
          }

          // Assert user does not exist
          if (box !== null) {
            return res.sendSigned(409, 'Box already exists');
          }

          pssst.box.create(user, req.params.box);
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
          if (pssst.box.isBlocked(req.params.box)) {
            return res.sendSigned(403, 'Box name restricted');
          }

          pssst.box.erase(user, req.params.box);
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
          box.push(addTime(req.body));
        }, {
          verify: req.body.meta.name,
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
