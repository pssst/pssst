/*
  Pssst!

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  You should have received a copy of the GNU General Public License
  along with this program. If not, see http://www.gnu.org/licenses/.

  Christian & Christian <pssst@pssst.name>
*/

module.exports = function() {

  var fs = require('fs');
  var ursa = require('ursa');
  var crypto = require('crypto');

  var enc = 'utf8';
  var sha = 'sha256';
  var prv = __dirname + '/../config/key';
  var pub = __dirname + '/../static/key';

  if (!fs.existsSync(prv) || !fs.existsSync(pub)) {

    // Generate keys
    var key = ursa.generatePrivateKey(2048);

    fs.writeFileSync(prv, key.toPrivatePem(enc));
    fs.writeFileSync(pub, key.toPublicPem(enc));
  }

  // Load PEMs
  var prv = fs.readFileSync(prv, {encoding: enc});
  var pub = fs.readFileSync(pub, {encoding: enc});

  // Create keys
  var key = {
    private: ursa.createPrivateKey(prv, undefined, enc),
    public: ursa.createPublicKey(pub, enc)
  }

  if (!ursa.isPrivateKey(key.private)){
    throw 'Private key invalid';
  }

  if (!ursa.isPublicKey(key.public)) {
    throw 'Public key invalid';
  }

  if (!ursa.matchingPublicKeys(key.private, key.public)) {
    throw 'Keys do not match';
  }

  // Generate Timestamp
  function timestamp() {
    return Number((new Date).getTime() / 1000).toFixed(0);
  }

  // Generate HMAC
  function hmac(data, sec) {
    if (sec == undefined) {
      sec = timestamp();
    }

    mac = crypto.createHmac(sha, sec.toString());
    mac.update(data);

    return {sec: sec, mac: mac.digest('base64')};
  }


  // Crypto object
  return {
    key: key,

    // Sign data
    sign: function(data) {
      if (data instanceof Object) {
        data = JSON.stringify(data);
      }

      var mac = hmac(data);
      var sig = key.private.hashAndSign(sha, mac.mac, 'base64', 'base64')

      return {sec: mac.sec, sig: sig};
    },

    // Verify data
    verify: function(data, sec, sig, pem) {
      if (data instanceof Object) {
        data = JSON.stringify(data);
      }

      var sec = parseInt(sec, 10);
      var mac = hmac(data, sec);
      var now = timestamp();

      if ((now + 3) < sec || sec < (now - 3)) {
        return false;
      }

      var key = ursa.createPublicKey(pem, enc);

      return key.hashAndVerify(sha, mac.mac, sig, 'base64');
    }
  }
}
