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
    /**
     * Shows a error message.
     *
     * @param {String} the error message
     */
    function error(message) {
      $('.pssst #error').html(message).fadeIn(200).delay(3000).fadeOut(200);
    }

    /**
     * Changes the active box.
     *
     * @param {String} the box name
     */
    function change(boxname) {
      $('.pssst #user').html(user + '.' + boxname + ' <b class="caret"></b>');
      $('.pssst section').hide();
      $('.pssst #box-' + boxname).show();

      var all = $('.pssst .box span');
      var box = $('.pssst #boxname-' + boxname + ' span');

      all.removeClass('fa-folder-open').addClass('fa-folder');
      box.removeClass('fa-folder').addClass('fa-folder-open');

      document.title = 'Pssst - ' + user + '.' + boxname;
    }

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
            error(err);
          }
        });
      },

      /**
       * Creates the Pssst instance.
       *
       * @param {Boolean} create user
       */
      login: function login(create) {
        var username = $.trim($('.pssst #username').val());
        var password = $.trim($('.pssst #password').val());
        var args = [create === true, username, password];

        if (username && password) {
          $('.pssst #login-dialog').removeClass('animated shake');

          if (create === true) {
            $('.pssst #login-create').prop('disabled', true);
            $('.pssst #login-create span').removeClass('fa-user');
            $('.pssst #login-create span').addClass('fa-spin fa-spinner');
          }

          app.call('login', args, function call(data) {
            if (data) {
              user = data;
              app.list();
              $('.pssst #write').focus();
              $('.pssst #login-dialog').modal('hide');
            } else {
              $('.pssst #login-dialog').addClass('animated shake');
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
          $('.pssst .version').text(data);
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
        var boxname = $.trim($('.pssst #boxname').val());

        if (boxname) {
          app.call('create', [boxname], function call() {
            app.list(box = boxname);
            $('.pssst #boxname').val('');
          });
        }
      },

      /**
       * Deletes a box.
       */
      erase: function erase() {
        app.call('delete', [box], function call() {
          $('.pssst #box-' + box).remove();
          app.list(box = 'box');
        });
      },

      /**
       * Lists all boxes of an user.
       *
       * @param {String} the box to change to
       */
      list: function list(active) {
        app.call('list', null, function call(boxes) {
          $('.pssst #boxes').empty();

          boxes.forEach(function(box) {
            $('.pssst #boxes').append(Mustache.render(
              '<li>'
            + '  <a id="boxname-{{box}}" class="box" href="">'
            + '    <span class="fa fa-folder"></span>&nbsp;&nbsp;{{box}}'
            + '  </a>'
            + '</li>', {box: box}
            ));

            if ($('.pssst #box-' + box).length === 0) {
              $('.pssst #content').append(Mustache.render(
                '<section id="box-{{box}}"></section>', {box: box}
              ));
            }
          });

          $('.pssst .box').click(function() {
            change(box = $.trim($(this).text()));
          });

          change(active || box);
        });
      },

      /**
       * Pulls a message from a box.
       */
      pull: function pull() {
        app.call('pull', [box], function call(data) {
          if (data) {
            $('.pssst #box-' + box).append(Mustache.render(
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
            $('.pssst #box-' + box + ' article:last-child').fadeIn(200);
            $('html,body').animate({scrollTop: $(document).height()}, 'slow');
          }
        });
      },

      /**
       * Pushes a message into a box.
       */
      push: function push() {
        var receiver = $.trim($('.pssst #receiver').val());
        var message  = $.trim($('.pssst #message').val());

        if (receiver && message) {
          app.call('push', [[receiver], message], function call(data) {
            $('.pssst #receiver').val('');
            $('.pssst #message').val('');
          });
        }
      }
    };

    // Add token to all internal links
    $('.pssst').on('click', 'a', function(event) {
      if ($(this).attr('href') === '') {
        $(this).attr('href', '#' + token);
      }
    });

    $('.pssst #login-create').click(function() { app.login(true); });
    $('.pssst #login-exists').click(app.login);
    $('.pssst #logout').click(app.logout);
    $('.pssst #disable').click(app.disable);
    $('.pssst #create').click(app.create);
    $('.pssst #delete').click(app.erase);
    $('.pssst #send').click(app.push);
    $('.pssst .modal').modal({
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

    setInterval(function task() {
      if (user) app.pull();
    }, 2000);

    app.version();

    $('.pssst #login-dialog').modal('show');

    return this;
  };
});
