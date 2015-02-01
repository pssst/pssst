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
 * Cryptographical functions for signing and verifying data.
 */
module.exports = function Crypto() {

  // Required imports
  var fs = require('fs');
  var ursa = require('ursa');
  var crypto = require('crypto');

  // Required constants
  var GRACE = 30;

  var BINARY = 'base64';
  var ENCODING = 'utf8';

  var RSA_SIZE = 4096;
  var RSA_HASH = 'sha512';

  var ID_RSA = __dirname + '/../id_rsa';
  var ID_PUB = __dirname + '/../www/key';

  // Generate reasonable strong RSA keys
  if (!fs.existsSync(ID_RSA) || !fs.existsSync(ID_PUB)) {
    var key = ursa.generatePrivateKey(RSA_SIZE);

    fs.writeFileSync(ID_RSA, key.toPrivatePem(ENCODING));
    fs.writeFileSync(ID_PUB, key.toPublicPem(ENCODING));
  }

  // Key bundle
  var key = {
    private: ursa.createPrivateKey(
      fs.readFileSync(ID_RSA, {encoding: ENCODING}), undefined, ENCODING
    ),

    public: ursa.createPublicKey(
      fs.readFileSync(ID_PUB, {encoding: ENCODING}), ENCODING
    )
  };

  // Assert valid private key
  if (!ursa.isPrivateKey(key.private)){
    throw new Error('Private key invalid');
  }

  // Assert valid public key
  if (!ursa.isPublicKey(key.public)) {
    throw new Error('Public key invalid');
  }

  // Assert matching key bundle
  if (!ursa.matchingPublicKeys(key.private, key.public)) {
    throw new Error('Key bundle invalid');
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

    hmac = crypto.createHmac(RSA_HASH, timestamp.toString());
    hmac.update(data.toString());

    return {
      timestamp: timestamp,
      signature: hmac.digest(BINARY)
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
    var prv = key.private;

    return {
      timestamp: hmac.timestamp,
      signature: prv.hashAndSign(RSA_HASH, hmac.signature, BINARY, BINARY)
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

    if (Math.abs(time - now()) <= GRACE) {
      var hmac = createHMAC(data, time);
      var pub = ursa.createPublicKey(pem, ENCODING);

      try {
        return pub.hashAndVerify(RSA_HASH, hmac.signature, sig, BINARY);
      } catch (err) {
        return false; // OpenSSL error
      }
    }

    return false;
  };

  return this;
}()
