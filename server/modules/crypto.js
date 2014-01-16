/**
 * Pssst!
 * Copyright (C) 2013  Christian & Christian  <pssst@pssst.name>
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
 * Crypto functions for signing and verifying data.
 */
module.exports = function Crypto() {

  // Required imports
  var fs     = require('fs');
  var ursa   = require('ursa');
  var crypto = require('crypto');

  // Required constants
  var KEYSIZE  = 4096;
  var TIMESPAN = 10;
  var ENCODING = 'utf8';
  var HASH     = 'sha512';

  var prv = __dirname + '/../config/pssst.key';
  var pub = __dirname + '/../public/key';

  if (!fs.existsSync(prv) || !fs.existsSync(pub)) {

    // Generate reasonable strong RSA keys
    var rsa = ursa.generatePrivateKey(KEYSIZE);

    fs.writeFileSync(prv, rsa.toPrivatePem(ENCODING));
    fs.writeFileSync(pub, rsa.toPublicPem(ENCODING));
  }

  prv = fs.readFileSync(prv, {encoding: ENCODING});
  pub = fs.readFileSync(pub, {encoding: ENCODING});

  // Server key bundle
  var key = {
    private: ursa.createPrivateKey(prv, undefined, ENCODING),
    public:  ursa.createPublicKey(pub, ENCODING)
  };

  // Assert valid private key
  if (!ursa.isPrivateKey(key.private)){
    throw new Error('Private key invalid');
  }

  // Assert valid public key
  if (!ursa.isPublicKey(key.public)) {
    throw new Error('Public key invalid');
  }

  // Assert matching public/private key pair
  if (!ursa.matchingPublicKeys(key.private, key.public)) {
    throw new Error('Public/Private key invalid');
  }

  /**
   * Returns the current timestamp.
   *
   * @return {Number} the timestamp
   */
  function getTimestamp() {
    return Number((new Date).getTime() / 1000).toFixed(0);
  }

  /**
   * Returns the HMAC of the data.
   *
   * @param {Object} the data
   * @param {Number} timestamp
   * @return {Object} timestamp and HMAC
   */
  function buildHMAC(data, timestamp) {
    timestamp = timestamp || getTimestamp();

    hmac = crypto.createHmac(HASH, timestamp.toString());
    hmac.update(data.toString());

    return {
      timestamp: timestamp,
      hmac: hmac.digest('base64')
    };
  };

  /**
   * Returns the data signature.
   *
   * @param {Object} the data
   * @return {Object} timestamp and signature
   */
  this.sign = function sign(data) {
    if (data instanceof Object) {
      data = JSON.stringify(data);
    }

    var hmac = buildHMAC(data);
    var signature = key.private.hashAndSign(
      HASH, hmac.hmac, 'base64', 'base64'
    );

    return {
      timestamp: hmac.timestamp,
      signature: signature
    };
  };

  /**
   * Returns if data could be verified.
   *
   * @param {Object} the data
   * @param {Object} hmac of data
   * @param {String} user public key
   * @return {Boolean} true if verified
   */
  this.verify = function verify(data, hmac, pub) {
      if (data instanceof Object) {
        data = JSON.stringify(data);
      }

      // Calculate time window
      var now = getTimestamp();
      var min = now - (TIMESPAN / 2);
      var max = now + (TIMESPAN / 2);
      var timestamp = parseInt(hmac.timestamp, 10);

      if (min > timestamp || max < timestamp) {
        return false;
      }

      var key = ursa.createPublicKey(pub, ENCODING);
      var tmp = buildHMAC(data, timestamp);

      return key.hashAndVerify(
        HASH, tmp.hmac, hmac.signature, 'base64'
      );
  };
}
