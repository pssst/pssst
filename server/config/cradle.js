/*
  Pssst! Einfach. Sicher.

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see http://www.gnu.org/licenses/.

  Christian Uhsat <christian@uhsat.de>
*/

var cradle = require('cradle');
var config = require('./config.json');

module.exports = function connect() {

  // Setting up the connection
  couchdb = new(cradle.Connection)(config.db.host, config.db.port, { auth: {
    username: config.db.user,
    password: config.db.pass
  }})

  // Setting up the database
  db = couchdb.database(config.db.name);
  db.exists(function exists(err, exists) {
    if (err) {
      console.error(err);
      process.exit(1);
    }

    if (!exists) {
      db.create();
    }
  });

  // Testing
  db.save('.log', {'start': new Date().toString()}, function(err, doc) {
    if (err) {
      console.error(err);
      process.exit(1);
    }
  });

  return db;
}
