/*
  Pssst!

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see http://www.gnu.org/licenses/.

  Christian Uhsat <christian@uhsat.de>
*/

module.exports = function(app, pssst) {

  // Pssst! create user
  app.post('/user/:user', pssst.create);

  // Pssst! find user
  app.get('/user/:user/key', pssst.find);

  // Pssst! push message
  app.put('/user/:user/:box?*', pssst.push);

  // Pssst! pull message
  app.get('/user/:user/:box?*', pssst.pull);

  // Pssst! static files
  app.get('/:file', pssst.static);

  // All other
  app.get('*', function route(req, res) {
    res.send(404);
  });
}
