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


// Pssst internal request handling. The available options are:
//
//   status = the status text to respond
//   verify = the name/key to verify
//
// @param {Object} database wrapper
//
module.exports = function Pssst(db) {
  var self = this;

  // Required static classes
  this.user = require('./pssst.user.js');
  this.box  = require('./pssst.box.js');

  // Gets the user from the database.
  //
  // @param {Object} request
  // @param {Object} response
  // @param {String} the user name
  // @param {Function} callback
  // @param {Boolean} strict handling
  //
  function getUser(req, res, name, callback, strict) {
    db.get(name, function get(err, user) {
      if (!err) {

        // Assert user is not disabled
        if (user !== null && this.user.isDisabled(user)) {
          return res.sendSigned(410, 'User was deleted');
        }

        // Assert user exists
        if (user === null && strict) {
          return res.sendSigned(404, 'User not found');
        }

        callback(null, user);
      } else {
        callback(err);
      }
    });
  }

  // Gets the box from the user (sync).
  //
  // @param {Object} request
  // @param {Object} response
  // @param {Object} the user
  // @param {String} the box name
  // @param {Boolean} strict handling
  // @return {Mixed} true if error else found box or null
  //
  function getBox(req, res, user, name, strict) {
    var box = this.box.find(user, name);

    // Assert box exists
    if (box === null && strict) {
      return res.sendSigned(404, 'Box not found');
    }

    return box;
  }

  // Persists the user and responds request.
  //
  // @param {Object} request
  // @param {Object} response
  // @param {Object} the user
  // @param {String} status text
  //
  this.respond = function respond(req, res, user, status) {
    db.set(req.params.user, user, function set(err) {
      if (err) {
        res.sendError(err);
      } else if (status) {
        res.sendSigned(200, status);
      } else {
        res.sendSigned(204);
      }
    })
  }

  // Handles a request.
  //
  // @param {Object} request
  // @param {Object} response
  // @param {Function} callback
  // @param {Object} handling options
  // @return {Boolean} true if error
  //
  this.handle = function handle(req, res, callback, options) {
    options = options || {};

    var username = req.params.user;

    // Assert valid user name
    if (!new RegExp('^[a-z0-9]{2,63}$').test(username)) {
      return res.sendSigned(400, 'User name invalid');
    }

    var boxname = req.params.box || 'box'; // Default box

    // Assert valid box name
    if (!new RegExp('^[a-z0-9]{2,63}$').test(boxname)) {
      return res.sendSigned(400, 'Box name invalid');
    }

    // Set verification method for request
    if (options.verify !== false) {
      var verifier = req.verify;
    } else {
      var verifier = function always(p, fn) {
        fn();
      };
    }

    // Should the request be handled strictly?
    var strict = (req.method !== 'POST');

    // Verify request
    verifier(options.verify || username, function verifier() {
      getUser(req, res, username, function getUser(err, user) {
        if (err) {
          return res.sendError(err);
        }

        if (user) {
          var box = getBox(req, res, user, boxname, strict);
        } else {
          var box = null;
        }

        // Request has been responded before
        if (box === true || callback(user, box)) {
          return;
        }

        // Request must be responded after
        if (typeof options.status === 'string') {
          self.respond(req, res, user, options.status);
        }
      }, strict);
    });
  }

  return this;
}
