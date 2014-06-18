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
 * Server wrapper with SSL support.
 *
 * @param {Object} express app
 */
module.exports = function Server(app) {

  // Required imports
  var fs    = require('fs');
  var http  = require('http');
  var https = require('https');

  // Required constants
  var ENCODING = 'utf8';

  var key = __dirname + '/../config/pssst.key';
  var crt = __dirname + '/../config/pssst.crt';

  if (!fs.existsSync(crt)) {
    return http.createServer(app);
  } else {
    return https.createServer({
      key:  fs.readFileSync(key, ENCODING),
      cert: fs.readFileSync(crt, ENCODING)
    }, app);
  }
}
