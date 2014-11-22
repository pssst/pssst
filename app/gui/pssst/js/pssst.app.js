/**
 * Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
define(['js/pssst.api.js'], function (api) {
  return function (token) {
    var user = null;
    var box = 'box';

    var cli = api(token);
    var app = {
      /**
       * Calls the API.
       *
       * @param {String} the method
       * @param {Object} the parameters
       * @param {Function} callback
       */
      call: function call(method, params, callback) {
        cli.call(method, params, function call(err, val) {
          if (!err) {
            callback(val);
          } else {
            $('#error').html(err).fadeIn(200).delay(3000).fadeOut(200);

            if (method === 'login') {
              $('#login-create span').removeClass('fa-spin fa-spinner');
              $('#login-create span').addClass('fa-user');
              $('#login-create').prop('disabled', false);
            }
          }
        });
      },

      /**
       * Creates the Pssst instance.
       *
       * @param {Boolean} create user
       */
      login: function login(create) {
        var username = $.trim($('#username').val());
        var password = $.trim($('#password').val());
        var args = [create === true, username, password];

        if (username && password) {
          $('#login-dialog').removeClass('animated shake');

          if (create === true) {
            $('#login-create').prop('disabled', true);
            $('#login-create span').removeClass('fa-user');
            $('#login-create span').addClass('fa-spin fa-spinner');
          }

          app.call('login', args, function call(data) {
            if (data) {
              user = data;
              app.list();
              $('#write').focus();
              $('#login-dialog').modal('hide');
            } else {
              $('#login-dialog').addClass('animated shake');
            }
          });
        }
      },

      /**
       * Clears the Pssst instance.
       */
      logout: function logout() {
        app.call('logout', null, function call() {
          location.reload();
        });
      },

      /**
       * Sets the CLI version.
       */
      version: function version() {
        app.call('version', null, function call(data) {
          $('.version').text(data);
        });
      },

      /**
       * Deletes an user.
       */
      disable: function disable() {
        app.call('delete', null, function call() {
          app.logout();
        });
      },

      /**
       * Creates a box.
       */
      create: function create() {
        var boxname = $.trim($('#boxname').val());

        if (boxname) {
          app.call('create', [boxname], function call() {
            app.list(box = boxname);
            $('#boxname').val('');
          });
        }
      },

      /**
       * Deletes a box.
       */
      erase: function erase() {
        app.call('delete', [box], function call() {
          $('#box-' + box).remove();
          app.list(box = 'box');
        });
      },

      /**
       * Changes the active box.
       *
       * @param {String} the box name
       */
      change: function(boxname) {
        $('#user').html(user + '.' + boxname + ' <b class="caret"></b>');
        $('section').hide();
        $('#box-' + boxname).show();

        var all = $('.box span');
        var box = $('#boxname-' + boxname + ' span');

        all.removeClass('fa-folder-open').addClass('fa-folder');
        box.removeClass('fa-folder').addClass('fa-folder-open');

        document.title = 'Pssst - ' + user + '.' + boxname;
      },

      /**
       * Lists all boxes of an user.
       *
       * @param {String} the box to change to
       */
      list: function list(change) {
        app.call('list', null, function call(boxes) {
          $('#boxes').empty();

          boxes.forEach(function(box) {
            $('#boxes').append(Mustache.render(
              '<li>'
            + '  <a id="boxname-{{box}}" class="box" href="">'
            + '    <span class="fa fa-folder"></span>&nbsp;&nbsp;{{box}}'
            + '  </a>'
            + '</li>', {box: box}
            ));

            if ($('#box-' + box).length === 0) {
              $('#content').append(Mustache.render(
                '<section id="box-{{box}}"></section>', {box: box}
              ));
            }
          });

          $('.box').click(function() {
            app.change(box = $.trim($(this).text()));
          });

          app.change(change || box);
        });
      },

      /**
       * Pulls a message from a box.
       */
      pull: function pull() {
        app.call('pull', [box], function call(data) {
          if (data) {
            $('#box-' + box).append(Mustache.render(
              '<article class="panel panel-default">'
            + '  <div class="panel-body">'
            + '    {{#text}}'
            + '      {{.}}<br>'
            + '    {{/text}}'
            + '    <small class="text-muted">'
            + '      <span class="fa fa-clock-o"></span> {{user}}, {{time}}'
            + '    </small>'
            + '  </div>'
            + '</article>'
            , {
              text: data[2].split('\n'),
              user: 'pssst.' + data[0],
              time: new Date(data[1] * 1000)
            }));
            $('#box-' + box + ' article:last-child').fadeIn(200);
            $('html,body').animate({scrollTop: $(document).height()}, 'slow');
          }
        });
      },

      /**
       * Pushes a message into a box.
       */
      push: function push() {
        var receiver = $.trim($('#receiver').val());
        var message  = $.trim($('#message').val());

        if (receiver && message) {
          app.call('push', [[receiver], message], function call(data) {
            $('#receiver').val('');
            $('#message').val('');
          });
        }
      }
    };

    app.version();

    // Add token to all internal links
    $('body').on('click', 'a', function(event) {
      if ($(this).attr('href') === '') {
        $(this).attr('href', '#' + token);
      }
    });

    // Set app bindings
    $('#login-create').click(function() { app.login(true); });
    $('#login-exists').click(app.login);
    $('#logout').click(app.logout);
    $('#disable').click(app.disable);
    $('#create').click(app.create);
    $('#delete').click(app.erase);
    $('#send').click(app.push);
    $('.modal').modal({
      backdrop: 'static',
      keyboard: false,
      show: false
    }).on('keypress', function(e) {
      if (e.which === 13) {
        var button = $(this).find('.btn-primary:first');
        if (button.attr('id') !== 'send') {
          button.click();
        }
      }
    }).on('shown.bs.modal', function() {
      $(this).find('[autofocus]:first').focus();
    });

    // Set message pulling
    setInterval(function task() {
      if (user) app.pull();
    }, 2000);

    $('#login-dialog').modal('show');

    return this;
  };
});
