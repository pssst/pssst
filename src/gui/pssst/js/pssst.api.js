/**
 * Copyright (C) 2013-2015  Christian & Christian  <hello@pssst.name>
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
 *
 *
 * Pssst API.
 *
 * @param {String} security token
 */
define(function (token) {
  /**
   * Pssst API class.
   *
   * @param {String} security token
   */
  return function (token) {

    // Extract key and iv from security token
    var key = CryptoJS.enc.Hex.parse(token.substring(0, 64));
    var iv = CryptoJS.enc.Hex.parse(token.substring(64));

    /**
     * Returns the encrypted data.
     *
     * @param {String} decrypted data (UTF-8)
     * @return {String} encrypted data (Base64)
     */
    function encrypt(data) {
      data = CryptoJS.AES.encrypt(data, key, {iv: iv});
      data = data.toString();

      return data;
    };

    /**
     * Returns the decrypted data.
     *
     * @param {String} encrypted data (Base64)
     * @return {String} decrypted data (UTF-8)
     */
    function decrypt(data) {
      data = CryptoJS.AES.decrypt(data, key, {iv: iv});
      data = CryptoJS.enc.Utf8.stringify(data);

      return data;
    };

    /**
     * Pssst API instance.
     */
    return {
      /**
       * Calls the local proxy server.
       *
       * @param {String} the request method
       * @param {Object} the request parameters
       * @param {Function} response callback
       */
      call: function call(method, params, callback) {
        var request = JSON.stringify({
          'method': method,
          'params': params || []
        });

        // Encrypted call (all data per JSON)
        $.post('/call', {'request': encrypt(request)}, function(val) {
          try {
            callback(null, JSON.parse(decrypt(val)));
          } catch (err) {
            callback(err);
          }
        }, 'text').fail(function(jqxhr, status, err) {
          callback(decrypt(jqxhr.responseText));
        });
      }
    };
  };
});
