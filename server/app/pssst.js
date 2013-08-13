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

module.exports = function(app) {

  var User = require('./pssst.user.js');
  var Box  = require('./pssst.box.js');

  var db = app.get('db');

  var block = app.get('config').name;

  // Return user
  User.get = function(req, res, ignore) {
    var name = req.params.user;

    if (!new RegExp('^[a-z0-9]{2,63}$').test(name)) {
      throw {status: 400, message: 'User name invalid'};
    }

    var user = db.load(name);

    if (user && User.deleted(user)) {
      throw {status: 410, message: 'User was deleted'};
    }

    if (!user && !ignore) {
      throw {status: 404, message: 'User not found'};
    }

    return user;
  }

  // Return box
  Box.get = function(req, res, ignore) {
    var name = req.params.box || 'all';

    if (!new RegExp('^[a-z0-9]{2,63}$').test(name)) {
      throw {status: 400, message: 'Box name invalid'};
    }

    var user = User.get(req, res);

    if (User.deleted(user)) {
      throw {status: 410, message: 'User was deleted'};
    }

    var box = Box.find(user, name);

    if (!box && !ignore) {
      throw {status: 404, message: 'Box not found'};
    }

    return box;
  }

  // End request
  function end(req, res, user, dat) {
    var name = req.params.user;

    if (db.save(name, user)) {
      throw {status: 500, message: 'Request failed'};
    }

    if (dat instanceof String) {
      res.sign(201, dat);
    } else if (dat) {
      res.sign(200, dat);
    } else {
      res.sign(204);
    }
  }

  // Route facades
  return {


    // User routes
    user: {

      // Create new user document
      create: function(req, res) {
        req.verify(req.body.key);

        if (User.blocked(block, req.params.user)) {
          throw {status: 403, message: 'User name restricted'};
        }

        if (User.get(req, res, true)) {
          throw {status: 409, message: 'User already exists'};
        }

        end(req, res, User.create(req.body.key), 'User created');
      },

      // Delete user
      delete: function(req, res) {
        req.verify(req.params.user);

        var user = User.get(req, res);

        User.delete(user);

        end(req, res, user, 'User deleted');
      },

      // Return user key
      find: function(req, res) {
        res.sign(200, User.get(req, res).key);
      }
    },


    // Box routes
    box: {

      // Create user box
      create: function(req, res) {
        req.verify(req.params.user);

        var user = User.get(req, res);

        if (Box.blocked(req.params.box)) {
          throw {status: 403, message: 'Box name restricted'};
        }

        if (Box.get(req, res, true)) {
          throw {status: 409, message: 'Box already exists'};
        }

        Box.create(user, req.params.box);

        end(req, res, user, 'Box created');
      },

      // Delete user box
      delete: function(req, res) {
        req.verify(req.params.user);

        var user = User.get(req, res);

        if (Box.blocked(req.params.box)) {
          throw {status: 403, message: 'Box name restricted'};
        }

        Box.delete(user, req.params.box);

        end(req, res, user, 'Box deleted');
      },

      // Push message into box
      push: function(req, res) {
        req.verify(req.body.meta.from);

        var box = Box.get(req, res);
        box.push(req.body);

        end(req, res, box.user, 'Message sent');
      },

      // Pull message from box
      pull: function(req, res) {
        req.verify(req.params.user);

        var box = Box.get(req, res);
        var body = box.pull();

        end(req, res, box.user, body);
      }
    }
  }
}
