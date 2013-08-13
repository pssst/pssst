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

module.exports = function(config) {

  var cradle = require('cradle');

  // Init db
  con = new(cradle.Connection)(
    config.db.host,
    config.db.port,
    config.db
  );

  // Load db
  db = con.database(config.db.name);
  db.exists(function(err, exists) {
    if (err) {
      throw err.reason;
    }

    if (!exists) {
      db.create();
    }
  });

  // Test db
  db.save('_touch', [], function(err, doc) {
    if (err) {
      throw err.reason;
    }
  });

  return {
    load: function(id) {
      var ret = null;

      db.get(id, function(err, doc) {
        if (!err) {
          ret = doc;
        }
      });

      return ret;
    },

    save: function(id, val) {
      var ret = null;

      db.save(id, val, function(err, doc) {
        if (err) {
          ret = err;
        }
      });

      return ret;
    }
  }
}
