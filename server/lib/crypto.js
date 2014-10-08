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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Cryptographical functions for signing and verifying data.
 */
module.exports = function Crypto() {

  // Required imports
  var fs = require('fs');
  var ursa = require('ursa');
  var crypto = require('crypto');

  // Required constants
  var APP = __dirname + '/../app/pssst.key';
  var WWW = __dirname + '/../www/key';

  // Generate reasonable strong RSA keys
  if (!fs.existsSync(APP) || !fs.existsSync(WWW)) {
    var key = ursa.generatePrivateKey(4096);

    fs.writeFileSync(APP, key.toPrivatePem('utf8'));
    fs.writeFileSync(WWW, key.toPublicPem('utf8'));
  }

  // Key bundle
  var key = {
    private: ursa.createPrivateKey(
      fs.readFileSync(APP, {encoding: 'utf8'}), undefined, 'utf8'
    ),

    public: ursa.createPublicKey(
      fs.readFileSync(WWW, {encoding: 'utf8'}), 'utf8'
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

  // Assert matching private/public key pair
  if (!ursa.matchingPublicKeys(key.private, key.public)) {
    throw new Error('Private/Public key invalid');
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

    hmac = crypto.createHmac('sha512', timestamp.toString());
    hmac.update(data.toString());

    return {
      timestamp: timestamp,
      signature: hmac.digest('base64')
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
    var key = key.private;

    return {
      timestamp: hmac.timestamp,
      signature: key.hashAndSign('sha512', hmac.signature, 'base64', 'base64')
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

    if (Math.abs(time - now()) <= 30) {
      var hmac = createHMAC(data, time);
      var key = ursa.createPublicKey(pem, 'utf8');

      return key.hashAndVerify('sha512', hmac.signature, sig, 'base64');
    } else {
      return false;
    }
  };

  return this;
}()
