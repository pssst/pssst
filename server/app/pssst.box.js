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

// Static box object
module.exports = {

  // Test name
  blocked: function(name) {
    return new RegExp("^(all)|(key)$").test(name);
  },

  // Create box
  create: function(user, name) {
    user.box[name] = [];
  },

  // Delete box
  delete: function(user, name) {
    delete user.box[name];
  },

  // Return box
  find: function(user, name) {
    return !(name in user.box) ? null : {

      // User object
      user: user,

      // Save message
      push: function(data) {
        user.box[name].push(data);
      },

      // Load message
      pull: function() {
        return user.box[name].shift();
      }
    };
  }
}
