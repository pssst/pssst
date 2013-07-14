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

// Get User
function user(req, res, fn) {
  db.get(req.params.id, function(err, doc) {
    if (!err) {
      fn(doc);
    } else {
      res.send(404, 'User doesn\'t exist');
    }
  });
}

// Get Box
function box(req, res, user, fn) {
  if (req.params.box == null) {
    req.params.box = 'all';
  }

  if (req.params.box in user.boxes) {
    fn(user.boxes[req.params.box]);
  } else {
    res.send(404, 'Box doesn\'t exist');
  }
}

module.exports = function pssst(config, db) {
  return {
    create: function create(req, res) {
      if (!new RegExp('^[a-z0-9]{2,63}$').test(req.params.id)) {
        return res.send(400, 'User name invalid');
      }

      for (i=0; i<config.names.length; i++) {
        if (new RegExp(config.names[i]).test(req.params.id)) {
          return res.send(400, 'User name restricted');
        }
      }

      db.get(req.params.id, function(err, doc) {
        if (!err) {
          return res.send(409, 'User already exists');
        }
      })

      db.save(req.params.id, {
          'boxes': {'all': []},
          'key': req.body.key
        }, function(err, doc) {
          if (err) {
            console.error(err);
            res.send(500, err);
          } else {
            res.send(201);
          }
      });
    },

    find: function find(req, res) {
      user(req, res, function(user) {
        res.json(200, user.key);
      });
    },

    push: function push(req, res) {
      user(req, res, function(user) {
        box(req, res, user, function(box) {
          box.push(req.body);
          res.send(201);
        })
      });
    },

    pull: function pull(req, res) {
      user(req, res, function(user) {
        box(req, res, user, function(box) {
          var body = box.shift();
          res.json(200, body);
        })
      });
    }
  };
}
