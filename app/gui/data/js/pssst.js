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
 *
 * Returns an instance.
 *
 * @param {String} the token
 * @return {Object} instance
 */
function Pssst(token) {
  var user = null, box = 'box';

  // Local server communication
  var server = {
    api: {

      // Local server communication key
      key: CryptoJS.enc.Hex.parse(token.substring(0, 64)),

      // Local server communication IV
      iv: CryptoJS.enc.Hex.parse(token.substring(64)),

      /**
       * Returns the encrypted data (UTF-8 to Base64).
       *
       * @param {String} decrypted data
       * @return {String} encrypted data
       */
      encrypt: function encrypt(data) {
        data = CryptoJS.AES.encrypt(data, server.api.key, {iv:server.api.iv});
        data = data.toString();

        return data;
      },

      /**
       * Returns the decrypted data (Base64 to UTF-8).
       *
       * @param {String} encrypted data
       * @return {String} decrypted data
       */
      decrypt: function decrypt(data) {
        data = CryptoJS.AES.decrypt(data, server.api.key, {iv:server.api.iv});
        data = CryptoJS.enc.Utf8.stringify(data);

        return data;
      },

      /**
       * Calls the local server.
       *
       * @param {String} the method name
       * @param {Object} the method arguments
       * @param {Function} callback
       */
      call: function call(method, args, callback) {
        var param = JSON.stringify({'method': method, 'args': args || []});

        $.post('/call', {'params': server.api.encrypt(param)}, function(val) {
          try {
            callback(null, JSON.parse(server.api.decrypt(val)));
          } catch (err) {
            callback(err);
          }
        }, 'text').fail(function(jqxhr, status, err) {
          callback(server.api.decrypt(jqxhr.responseText));
        });
      }
    },

    /**
     * Calls the API.
     *
     * @param {String} the method name
     * @param {Object} the method arguments
     * @param {Function} callback
     */
    call: function call(method, args, callback) {
      $('.pssst #throbber span').addClass('fa-spin');

      server.api.call(method, args, function call(err, val) {
        $('.pssst #throbber span').removeClass('fa-spin');

        if (!err) {
          callback(val);
        } else {
          flash('error', err);
        }
      });
    },

    /**
     * Creates a Pssst instance.
     *
     * @param {Boolean} create user
     */
    login: function login(create) {
      var username = $.trim($('.pssst #username').val());
      var password = $.trim($('.pssst #password').val());
      var args = [create === true, username, password];

      if (username && password) {
        server.call('login', args, function call(data) {
          if (data) {
            user = data;
            $('.pssst #dialog-login').modal('hide');
            $('.pssst #receiver').focus();
            server.list();
          } else {
            flash('error', 'Login failed');
          }
        });
      }
    },

    /**
     * Clears a Pssst instance.
     */
    logout: function logout() {
      server.call('logout', null, function call() {
        location.reload();
      });
    },

    /**
     * Sets the CLI version.
     */
    version: function version() {
      server.call('version', null, function call(data) {
        $('.pssst .version').text(data);
      });
    },

    /**
     * Deletes an user.
     */
    disable: function disable() {
      server.call('delete', null, function call() {
        server.logout();
      });
    },

    /**
     * Creates a box.
     */
    create: function create() {
      var boxname = $('.pssst #boxname').val();

      if ($.trim(boxname) !== '') {
        server.call('create', [boxname], function call() {
          server.list();
          load(box = boxname);

          flash('info', 'Box created');
          $('.pssst #boxname').val('');
        });
      }
    },

    /**
     * Deletes a box.
     */
    erase: function erase() {
      server.call('delete', [box], function call() {
        $('.pssst #messages-' + box).remove();

        server.list();
        load(box = 'box');

        flash('info', 'Box deleted');
      });
    },

    /**
     * Lists all boxes of an user.
     */
    list: function list() {
      server.call('list', null, function call(boxes) {
        $('.pssst #boxes').empty();

        boxes.forEach(function(box) {
          $('.pssst #boxes').append(render(
            '<li>' +
              '<a id="box-{{box}}" class="box fa fa-folder" href="">' +
                '&nbsp;&nbsp;{{box}}' +
              '</a>' +
            '</li>'
          , {
            box: box
          }));

          if ($('.pssst #messages-' + box).length === 0) {
            $('.pssst #content').append(render(
              '<section id="messages-{{box}}"></section>'
            , {
              box: box
            }));
          }
        });

        $('.pssst .box').click(function() {
          load(box = $.trim($(this).text()));
        });

        load(box);
      });
    },

    /**
     * Pushes a message into a box.
     */
    push: function push() {
      var receiver = $.trim($('.pssst #receiver').val());
      var message  = $.trim($('.pssst #message').val());

      if (receiver && message) {
        server.call('push', [[receiver], message], function call(data) {
          flash('info', 'Message pushed');
          $('.pssst #receiver').val('');
          $('.pssst #message').val('');
        });
      }
    },

    /**
     * Pulls a message from a box.
     */
    pull: function pull() {
      server.call('pull', [box], function call(data) {
        if (data) {
          $('.pssst #messages-' + box).append(render(
            '<article class="well">' +
              '<span>{{text}}</span><br/><small>- {{user}} {{time}}</small>' +
            '</article>'
          , {
            text: data[2].replace(/\n/g, '<br/>'),
            user: 'pssst.' + data[0],
            time: new Date(data[1] * 1000)
          }));

          $('.pssst #messages-' + box + ' article:last-child').fadeIn(350);

          $("html, body").animate({scrollTop: $(document).height()}, "slow");
        }
      });
    }
  };

  /**
   * Renders a template.
   *
   * @param {String} the template
   * @param {Object} the template data
   */
  function render(template, data) {
    for (k in data) {
      template = template.replace(new RegExp('{{' + k + '}}', 'gi'), data[k]);
    }

    return template;
  }

  /**
   * Shows a flash message.
   *
   * @param {String} the flash type
   * @param {String} the flash text
   */
  function flash(type, text) {
    $('.pssst #flash-' + type).text(text).fadeIn().delay(3000).fadeOut();
  }

  /**
   * Loads the current box.
   *
   * @param {String} the box name
   */
  function load(box) {
    $('.pssst #user').html(user + '.' + box + ' <b class="caret"></b>');

    $('.pssst section').finish().hide();
    $('.pssst #messages-' + box).finish().fadeIn(350);

    var all = $('.pssst .box');
    var box = $('.pssst #box-' + box);

    all.removeClass('fa-folder-open').addClass('fa-folder');
    box.removeClass('fa-folder').addClass('fa-folder-open');
  };

  // Add token to all internal links
  $('.pssst').on('click', 'a', function(event) {
    var link = $(this).attr('href');

    if (link === '') {
      $(this).attr('href', '#' + token);
    }
  });

  // Bind Return key
  $('.pssst #username, .pssst #password').keypress(function(e) {
    if (e.which == 13) {
      $('.pssst #login-exists').click();
    }
  });

  // Bind Return key
  $('.pssst #receiver').keypress(function(e) {
    if (e.which == 13) {
      $('.pssst #push').click();
    }
  });

  // Bind elements
  $('.pssst #login-create').click(function() { server.login(true); });
  $('.pssst #login-exists').click(server.login);
  $('.pssst #logout').click(server.logout);
  $('.pssst #disable').click(server.disable);
  $('.pssst #create').click(server.create);
  $('.pssst #delete').click(server.erase);
  $('.pssst #push').click(server.push);
  $('.pssst .modal').modal({
    backdrop: 'static',
    keyboard: false,
    show: false
  });

  $('.pssst #dialog-login').modal('show');

  server.version();

  setInterval(function task() {
    if (user) server.pull();
  }, 2000);

  return this;
}

$(document).ready(function() {
  var token = location.hash.replace('#', '');

  if (token) {
    var singleton = Pssst(token);
  } else {
    location.href = 'about:blank';
  }
});
