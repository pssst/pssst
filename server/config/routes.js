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

module.exports = function register(app, pssst) {

  // Pssst! create user
  app.post('/user/:id', pssst.create);

  // Pssst! find user
  app.get('/user/:id/key', pssst.find);

  // Pssst! push message
  app.put('/user/:id/:box?*', pssst.push);

  // Pssst! pull message
  app.get('/user/:id/:box?*', pssst.pull);

  // Public directory
  app.get('/:file', function public(req, res) {
    res.sendfile(req.params.file, {'root': 'public'}, function() {
      res.send(404);
    });
  });

  // All other
  app.get('*', function route(req, res) {
    res.send(404, 'You messed something up, right?');
  });
}
