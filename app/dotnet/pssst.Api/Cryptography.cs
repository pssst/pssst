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
 **/

using System;
using System.IO;
using System.Text;
using Org.BouncyCastle.Crypto;
using Org.BouncyCastle.Crypto.Digests;
using Org.BouncyCastle.Crypto.Generators;
using Org.BouncyCastle.Crypto.Macs;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Signers;
using Org.BouncyCastle.OpenSsl;
using Org.BouncyCastle.Security;
using pssst.Api.Interface;

namespace pssst.Api
{
    /// <summary>
    /// Provides cryptographic functionality using the BouncyCastle libraray.
    /// </summary>
    public sealed class Cryptography : ICryptography
    {
        private const int MinimumKeyLength = 1024;

        private readonly SecureRandom _random;

        /// <summary>
        /// Initializes a new instance of the <see cref="Cryptography"/> class.
        /// </summary>
        public Cryptography()
        {
            _random = new SecureRandom();
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="Cryptography"/> class.
        /// </summary>
        /// <param name="random">The random.</param>
        public Cryptography(SecureRandom random)
        {
            _random = random;
        }

        /// <summary>
        /// Creates a new RSA keypair with the given length. The minimum length
        /// of the key must be 1024.
        /// </summary>
        /// <param name="keyLength">The Length of the key (min 1024).</param>
        /// <returns>
        /// The new keypair.
        /// </returns>
        public Keypair CreateKeyPair(int keyLength)
        {
            if (keyLength < MinimumKeyLength)
                throw new ArgumentException("The length is to short.", "keyLength");

            KeyGenerationParameters parameters = new KeyGenerationParameters(
                _random,
                keyLength);

            IAsymmetricCipherKeyPairGenerator keyGenerator = new RsaKeyPairGenerator();

            keyGenerator.Init(parameters);

            AsymmetricCipherKeyPair keyPair = keyGenerator.GenerateKeyPair();

            Keypair result = ExportKey(keyPair);

            return result;
        }

        private Keypair ExportKey(AsymmetricCipherKeyPair keyPair)
        {
            Keypair result = new Keypair();

            result.PrivateKey = ConvertKeyToString(keyPair.Private);
            result.PublicKey = ConvertKeyToString(keyPair.Public);

            if (string.IsNullOrEmpty(result.PrivateKey)
                && string.IsNullOrEmpty(result.PublicKey))
                throw new Exception("Error while exporting the key.");

            return result;
        }

        private string ConvertKeyToString(AsymmetricKeyParameter key)
        {
            string result;

            using (var writer = new StringWriter())
            {
                PemWriter pemWriter = new PemWriter(writer);
                pemWriter.Writer.NewLine = "\n";

                pemWriter.WriteObject(key);
                pemWriter.Writer.Flush();

                result = writer.ToString();
            }

            return result;
        }

        private AsymmetricKeyParameter ImportKeyPair(Keypair keyPair)
        {
            // Todo: Handling of encrypted key's doesnt' work until now

            //StringReader strReader = new StringReader(keyPair.PrivateKey + keyPair.PublicKey);
            StringReader strReader = new StringReader(keyPair.PrivateKey);

            PemReader reader = null;

            reader = new PemReader(strReader);

            object key = reader.ReadObject();

            AsymmetricKeyParameter privateKeyParameter = null;

            if (key is AsymmetricCipherKeyPair)
            {
                privateKeyParameter = ((AsymmetricCipherKeyPair)key).Private;
            }
            else
            {
                privateKeyParameter = (AsymmetricKeyParameter)key;
            }

            return privateKeyParameter;
        }

        /// <summary>
        /// Encrypts the message with the receivers public key. The message is
        /// encrypted using AES/CFB8/NoPadding and a salt. After this the salt
        /// is encrypted using RSA/ECB/OAEPWithSHA1AndMGF1Padding algorithm.
        /// </summary>
        /// <param name="receiverKey">The receivers public key.</param>
        /// <param name="message">The message to encrypt.</param>
        /// <returns>
        /// The encrypted message.
        /// </returns>
        public MessageBody EncryptMessage(string receiverKey, string message)
        {
            if (string.IsNullOrEmpty(receiverKey))
                throw new ArgumentException("Argument must not be null or empty.", "receiverKey");

            if (string.IsNullOrEmpty(message))
                throw new ArgumentException("Argument must not be null or empty.", "message");

            byte[] seed = _random.GenerateSeed(48);

            byte[] password = CreatePassword(seed);
            byte[] salt = CreateSalt(seed);

            byte[] encryptedMessage = EncryptData(message, password, salt);

            byte[] encryptedSeed = EncryptNonce(seed, receiverKey);

            MessageHead meta = new MessageHead();
            meta.nonce = Convert.ToBase64String(encryptedSeed);

            MessageBody body = new MessageBody();
            body.head = meta;
            body.body = Convert.ToBase64String(encryptedMessage);

            return body;
        }

        /// <summary>
        /// Decrypts the message using the private key.
        /// </summary>
        /// <param name="keyPair">The key pair.</param>
        /// <param name="message">The encrypted message.</param>
        /// <returns>
        /// The decrypted message
        /// </returns>
        public string DecryptMessage(Keypair keyPair, ReceivedMessageBody message)
        {
            if (string.IsNullOrEmpty(message.body))
                return string.Empty;

            byte[] encryptedNonce = Convert.FromBase64String(message.head.nonce);
            byte[] decryptedNonce = DecryptNonce(encryptedNonce, keyPair);

            byte[] encryptedData = Convert.FromBase64String(message.body);
            string decryptedData = DecryptData(encryptedData, decryptedNonce);

            return decryptedData;
        }

        private byte[] CreatePassword(byte[] seed)
        {
            byte[] password = new byte[32];

            Array.Copy(seed, 0, password, 0, 32);

            return password;
        }

        private byte[] CreateSalt(byte[] seed)
        {
            byte[] salt = new byte[16];

            Array.Copy(seed, 32, salt, 0, 16);

            return salt;
        }

        private byte[] EncryptData(string data, byte[] password, byte[] salt)
        {
            IBufferedCipher cipher = CipherUtilities.GetCipher("AES/CFB8/NoPadding");

            cipher.Init(true, new ParametersWithIV(ParameterUtilities.CreateKeyParameter("AES", password), salt));

            byte[] dataArray = Encoding.ASCII.GetBytes(data);

            byte[] result = cipher.DoFinal(dataArray);

            return result;
        }

        private string DecryptData(byte[] encryptedData, byte[] nonce)
        {
            byte[] password = CreatePassword(nonce);
            byte[] salt = CreateSalt(nonce);

            IBufferedCipher cipher = CipherUtilities.GetCipher("AES/CFB8/NoPadding");

            cipher.Init(false, new ParametersWithIV(ParameterUtilities.CreateKeyParameter("AES", password), salt));

            byte[] decryptedData = cipher.DoFinal(encryptedData);

            string result = Encoding.ASCII.GetString(decryptedData);

            return result;
        }

        private byte[] EncryptNonce(byte[] nonce, string publicKey)
        {
            StringReader strReader = new StringReader(publicKey);

            PemReader reader = null;
            reader = new PemReader(strReader);

            RsaKeyParameters key = (RsaKeyParameters)reader.ReadObject();

            IBufferedCipher rsa = CipherUtilities.GetCipher("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            rsa.Init(true, key);
            byte[] encryptedCode = rsa.DoFinal(nonce);

            return encryptedCode;
        }

        private byte[] DecryptNonce(byte[] encryptedNonce, Keypair keyPair)
        {
            IBufferedCipher rsa = CipherUtilities.GetCipher("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
            rsa.Init(false, ImportKeyPair(keyPair));
            byte[] decryptedSeed = rsa.DoFinal(encryptedNonce);

            return decryptedSeed;
        }

        private AsymmetricCipherKeyPair ConvertKey(string key)
        {
            StringReader strReader = new StringReader(key);

            PemReader reader = null;
            reader = new PemReader(strReader);

            AsymmetricCipherKeyPair privateKeyParameter = (AsymmetricCipherKeyPair)reader.ReadObject();

            return privateKeyParameter;
        }

        /// <summary>
        /// Creates the signature for the data using the given keypair. The
        /// signature is created by computing a SHA512 hash and signing the hash
        /// with the private key.
        /// </summary>
        /// <param name="data">The data for which the signature should be created.</param>
        /// <param name="keypair">The keypair.</param>
        /// <param name="timestamp">The timestamp.</param>
        /// <returns>
        /// The signature.
        /// </returns>
        public byte[] SignData(string data, Keypair keypair, long timestamp)
        {
            if (data == null)
                return new byte[0];

            byte[] hashedData = ComputeHash(data, timestamp);

            // sign the hashed message with the private key

            byte[] signature = CreateSignature(hashedData, keypair);

            return signature;
        }

        private byte[] ComputeHash(string data, long timestamp)
        {
            byte[] key = Encoding.ASCII.GetBytes(timestamp.ToString());

            // init hmac
            IMac hmac = new HMac(new Sha512Digest());
            hmac.Init(new KeyParameter(key));

            // create sha512 hmac of the message

            byte[] message = Encoding.ASCII.GetBytes(data);
            hmac.BlockUpdate(message, 0, message.Length);

            byte[] hashedMessage = new byte[hmac.GetMacSize()];
            hmac.DoFinal(hashedMessage, 0);

            return hashedMessage;
        }

        /// <summary>
        /// Computes the signature of the given <see cref="data"/> using
        /// PKCS1-v1_5 Alogrithm and hashing the data with SHA256.
        /// </summary>
        /// <param name="data">The data for which the signature should be computed.</param>
        /// <returns>The signature.</returns>
        private byte[] CreateSignature(byte[] data, Keypair keypair)
        {
            ISigner signer = new RsaDigestSigner(new Sha512Digest());

            signer.Init(true, ImportKeyPair(keypair));
            signer.BlockUpdate(data, 0, data.Length);

            byte[] sig = signer.GenerateSignature();

            return sig;
        }
    }
}