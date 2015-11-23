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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *
 * Cryptographical functions for signing and verifying data.
 */
module.exports = function Crypto() {

  // Required imports
  var fs = require('fs');
  var rsa = require('node-rsa');
  var crypto = require('crypto');

  // Required constants
  var GRACE = 30;

  var RSA_SIZE = 2048;
  var RSA_HASH = 'sha256';
  var RSA_FORMAT = 'pkcs1';
  var RSA_SCHEME = 'pkcs1-sha256';

  var ENCODING = 'base64';

  var ID_RSA = __dirname + '/../id_rsa';
  var ID_PUB = __dirname + '/../www/key';

  // Generate a reasonable strong RSA key
  if (!fs.existsSync(ID_RSA) || !fs.existsSync(ID_PUB)) {
    var key = new rsa({b: RSA_SIZE});

    fs.writeFileSync(ID_RSA, key.exportKey('private'));
    fs.writeFileSync(ID_PUB, key.exportKey('public'));
  }

  // Load private server key
  var key = new rsa(fs.readFileSync(ID_RSA), RSA_FORMAT, RSA_SCHEME);

  // Assert the key has a private part
  if (!key.isPrivate()){
    throw new Error('Key has no private part');
  }

  // Assert the key has a public part
  if (!key.isPublic()) {
    throw new Error('Key has no public part');
  }

  // Assert the key size is big enough
  if (key.getKeySize() < RSA_SIZE) {
    throw new Error('Key size too small');
  }

  /**
   * Returns the HMAC of the given data.
   *
   * @param {Object} the data
   * @param {Number} timestamp
   * @return {Object} timestamp and signature
   */
  function createHMAC(data, timestamp) {
    var hmac, timestamp = timestamp || now();

    // Calculate hash with final round
    hmac = crypto.createHmac(RSA_HASH, timestamp.toString());
    hmac.update(data.toString());

    return {
      timestamp: timestamp,
      signature: hmac.digest(ENCODING)
    };
  };

  /**
   * Returns the current timestamp.
   *
   * @return {Number} the timestamp
   */
  this.now = function now() {
    return Number((new Date()).getTime() / 1000).toFixed(0);
  }

  /**
   * Returns the signature of the given data.
   *
   * @param {Object} the data
   * @return {Object} timestamp and signature
   */
  this.sign = function sign(data) {
    if (data instanceof Object) {
      data = JSON.stringify(data);
    }

    var hmac = createHMAC(data);

    return {
      timestamp: hmac.timestamp,
      signature: key.sign(hmac.signature, ENCODING, ENCODING)
    };
  };

  /**
   * Returns if the given data could be verified.
   *
   * @param {Object} the data
   * @param {Object} the data HMAC
   * @param {String} the users public key (PEM format)
   * @return {Boolean} true if verified
   */
  this.verify = function verify(data, hmac, pem) {
    if (data instanceof Object) {
      data = JSON.stringify(data);
    }

    var time = parseInt(hmac.timestamp, 10);
    var sig = hmac.signature;

    // Assert the timestamp is within grace time
    if (Math.abs(time - now()) <= GRACE) {
      var hmac = createHMAC(data, time);

      try {
        return new rsa(pem).verify(hmac.signature, sig, ENCODING, ENCODING);
      } catch (err) {
        return false; // Possibly an OpenSSL error
      }
    }

    return false;
  };

  return this;
}()
