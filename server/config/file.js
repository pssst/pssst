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

// Static files
module.exports = function(root) {

  var fs = require('fs');
  var mime = require('mime');

  var enc = 'utf8';
  var dir = __dirname + '/../' + root + '/';

  mime.default_type = 'text/plain';

  return function(req, res) {
    var file = req.params.file;
    var type = mime.lookup(file);

    fs.readFile(dir + file, enc, function(err, dat) {
      if (!err) {
        res.setHeader('content-type', type);
        res.sign(200, dat);
      } else {
        res.sign(404, 'File not found');
      }
    });
  }
}
