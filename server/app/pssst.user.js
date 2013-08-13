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

// Static user object
module.exports = {

  // Test name
  blocked: function(name, block) {
    return new RegExp(block).test(name);
  },

  // Create user
  create: function(key) {
    return {
      'key': key,
      'box': {
        'all': []
      }
    }
  },

  // Delete user
  delete: function(user) {
    user.box = null;
    user.key = null;
  },

  // Test deleted user
  deleted: function(user) {
    return user.key == null;
  }
}
