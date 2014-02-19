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


// A simple file server.
//
// @param {String} document root
//
module.exports = function File(root) {

  // Required imports
  var fs   = require('fs');
  var mime = require('mime');
  var util = require('util');

  // Required constants
  var ENCODING = 'utf8';
  var MIMETYPE = 'text/plain';

  // Default MIME type if unknown
  mime.default_type = MIMETYPE;

  // Serves the requested file including MIME type.
  //
  // @param {Object} request
  // @param {Object} response
  //
  this.serve = function serve(req, res) {
    var file = req.params.file;
    var path = util.format('%s/../%s/%s', __dirname, root, file);

    // Get and send signed file content
    fs.readFile(path, ENCODING, function(err, data) {
      if (!err) {
        res.setHeader('content-type', mime.lookup(file));
        res.sendSigned(200, data);
      } else {
        res.sendSigned(404, 'File not found');
      }
    });
  };

  return this;
}
