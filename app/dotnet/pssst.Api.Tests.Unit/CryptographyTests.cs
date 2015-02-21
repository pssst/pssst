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
using System.Text;
using System.Collections.Generic;
using pssst.Api.Pcl.Interface;
using Rhino.Mocks;
using Org.BouncyCastle.Security;
using System.IO;
using Org.BouncyCastle.OpenSsl;
using Org.BouncyCastle.Crypto;
using NUnit.Framework;

namespace pssst.Api.Pcl.Tests.Unit
{
    /// <summary>
    /// Summary description for CryptographyTests
    /// </summary>
    [TestFixture]
    public class CryptographyTests
    {
        #region TestKeys

        private const string _senderPublicKey = @"-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAne3bABJkfPAMTWHETyOY
V0/p2eXy8ObauJ1NBCD2bHlBzvg9IxJKKr2C05gUD1AMjFWLc1rByUoUJOuIc097
TB4DBwSuRan/h3ztwfSBDVX3OHr0Q168TUh5g6hGjgYnrxCq/XWD6xsVFiysiDhn
Guz5Lw/Xr3iU4tfiZr7bcRMzZ1oQ7ktThqq64VOEeQzQVkSHJ/jfKurhYlDfuaSM
/sXr4nmygrCC0HjeGxgMQJGo3L9R2eqc4g4tZzVA7yJ52vjP8kXUibWNbmS0FjhU
Nw0OxioGgUMRROEkB2DFcsy5lMzoPRepraXjLWS8ODCT7eiAG64a9xDK44mriSxf
wqHPJ3lg5gqfMz7Z+0rnUBj+ED0Z4rrH4g9q4CpvjGCgBaMCqgzbKiu/AfZJJEXt
hXRkVPx4TeHNj3YHf8qRP2UIwX5ef10jPBHwFTrHQuZ0fjHQuc+fJkrKu2QqGcXp
r6nwhsfrGqIhH/6aEpQavfd1WR7FThgHxNXDGwd4OPemkRbzj5BEoeHv/x2Oqilt
fqlMt8Dr7mbdxCeEzfGYokx07y53ZN6qKrj1QSrbOf6cjiLHPew4OszOoYlUzmy8
kRMvK66TcQlat5eRbXYXARa3h1klm+VfVJh8v2WyutNCg2eLH1FJD3F6nq9mmze/
jozE6fjkDDDiGobyKp1hNxECAwEAAQ==
-----END PUBLIC KEY-----";

        private const string _senderPrivateKey = @"-----BEGIN PRIVATE KEY-----
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
jsi3x213/fmrdHuXRP7nSMqqNNGr
-----END PRIVATE KEY-----";

        private const string _receiverPublicKey = @"-----BEGIN PUBLIC KEY-----
MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAnWBBughqpnU6ch9Y05Gz
KGnv7f4htsh0XtMkCUN6rszaJdLIK3VqDcafbGSYAGOm1/BGg9i4I/S1OcNofD/k
rVnUkCD8xF4zNvy8uiS5RUINBLzlr04FvLAOj6N8wAXV9KoknP3GvjXx6zSeDCYj
F+yigr8KD5mlcqEJ5CrFg3Llcy8XeldgqfRgILL6T8kf3gc65P5f1CDDD+qEayxe
J5n95imFk8XVD5yAzBAxFdMkNAlVmG52koN2CActDReVdrFsDN/5zW8GZjnf+O3i
qe34kurje0gEHJsrWSzADEdnhgZMklZwspfdMFNJ3QCbmGzpzxCERZ9ecNNbj3fR
bHQRWbhJiov9d5FwtJABoUaPdZOtZX6/rkhL0Eb43DCCG6ptyi9mdrjijIIBXEj7
Y/YhySFzG0C3woCFpoo5MbfFUi4sFJJLP3VOdyp6N6DtfHW0nwS0KdDpbnhDcuon
xl/iYyhc2umTgC7rZRkOzD9/KFd5RkiZK17TR7BCN3ShLBekO+uhdfRtvPJkoohF
hNl+fTyiDkd+L2oc7RlQ535RJgOVapj3Iu7wrJ/YF3cGCrtTv60u+1wFHHqN1IQS
SDkpB81Q9ve8f6zRVRPULLg+qdEY034on1T0+kWYBqtj/AWZImV1aWcdLeq9h6mj
csLWX+4aB+DzlsF63eO++YECAwEAAQ==
-----END PUBLIC KEY-----";

        private const string _receiverPrivateKey = @"-----BEGIN PRIVATE KEY-----
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
pxxEBgxJkrT/H2YqWyunKomHjnTRfcBO
-----END PRIVATE KEY-----";

        #endregion TestKeys

        #region EncryptedMessageData

        private byte[] _seed = new byte[] { };
        private const string _message = "Hello Test, this is a simple short message.";

        private const string _nonce = "63:31:72:0b:cb:46:95:2b:09:38:ff:05:5b:a6:51:00:44:f2:ec:30:25:78:9e:e4:88:b8:70:e6:90:e3:06:28:d4:bd:f5:ce:bf:59:4f:8f:1e:cc:3b:b3:c3:3f:db:8a";
        private const string _encryptedNonce = "54:55:6d:34:2f:42:38:30:59:2f:46:2b:6d:68:46:75:75:78:43:44:58:61:6e:58:4b:39:4d:48:42:7a:43:76:32:44:4b:62:38:54:70:38:54:49:31:74:55:48:75:68:30:69:47:59:55:36:4e:4d:7a:61:6e:36:6e:35:46:4e:51:66:58:47:66:4f:68:44:6e:43:50:35:2f:67:37:56:57:53:4f:34:47:4f:38:75:6f:32:68:34:6e:48:64:7a:59:4d:4b:57:64:38:37:61:45:4a:32:4b:6a:70:6a:49:70:4a:36:36:50:31:31:58:76:67:2f:32:4e:37:64:30:67:30:31:69:46:70:57:48:35:66:4e:66:39:77:34:74:2b:39:77:74:44:42:69:42:39:53:6a:36:53:42:45:72:2f:34:4e:47:79:44:69:4e:76:57:70:47:78:77:31:61:34:6e:6d:6d:55:72:46:35:78:52:52:70:39:6d:62:77:61:30:2f:6c:36:67:75:66:77:4b:2b:37:39:36:4d:4e:4d:54:52:50:46:51:44:34:30:71:39:35:77:71:35:2b:38:31:51:2f:36:74:49:42:2b:74:54:42:30:44:41:4a:6a:30:72:5a:4a:58:30:72:4b:39:66:41:38:4c:54:4e:43:78:6d:31:65:54:37:45:6f:44:62:61:6a:6d:4e:4f:68:36:38:58:4c:52:58:5a:2b:59:75:56:6a:38:49:45:5a:74:36:67:4e:32:75:70:45:36:4b:4d:30:7a:36:58:48:34:64:42:53:4e:69:51:49:4b:68:34:65:2b:57:2b:37:70:4d:33:53:52:62:6b:52:63:64:36:76:55:75:63:34:57:77:55:52:63:65:31:54:6c:47:35:43:49:6b:38:54:6b:2b:66:48:49:38:44:75:61:6f:62:53:78:64:4d:65:37:6f:6c:36:57:66:59:57:35:63:35:4b:76:36:65:2b:6f:57:77:76:58:4e:4c:5a:74:4c:33:7a:31:75:5a:6e:64:67:79:37:5a:76:46:59:75:44:71:42:4f:37:34:53:79:2f:79:66:59:30:68:75:42:4f:39:32:6e:61:32:36:59:69:37:4a:66:6a:58:5a:6e:4b:62:61:64:38:63:66:63:43:69:4b:4c:79:32:73:64:46:32:68:68:42:56:42:71:50:52:59:6b:66:33:6f:4b:6b:6c:75:6d:51:65:47:64:4b:6b:7a:74:32:69:63:62:47:67:4e:54:57:52:5a:73:77:55:48:78:61:2b:7a:67:5a:2f:63:41:6c:38:50:51:6a:53:49:50:4a:72:2b:52:77:43:72:45:47:4b:72:56:6b:78:62:4b:59:44:4e:51:68:44:77:37:6b:74:43:67:47:52:53:6d:4a:69:49:62:57:6a:67:59:79:33:31:55:67:4c:75:63:48:2f:50:64:65:6b:46:4e:46:58:2b:2b:71:63:58:32:6a:4c:51:57:33:4d:48:44:6a:70:61:79:2f:42:33:65:38:59:45:46:69:64:48:78:4a:70:75:69:63:51:31:46:75:64:31:53:57:69:4e:65:31:32:44:56:63:6c:45:70:48:78:63:35:6a:6f:49:56:75:45:62:57:45:68:66:65:51:63:79:71:72:4f:73:48:41:35:46:4f:66:74:44:51:46:4d:66:6f:52:50:73:4a:37:36:59:6e:4b:2b:6b:4d:75:67:32:49:38:3d";
        private const string _encryptedMessage = "48:37:55:4c:63:46:72:4f:48:37:75:77:46:7a:66:41:54:62:4d:71:4a:6f:67:55:39:79:50:49:47:5a:4a:75:64:63:56:45:77:43:74:6f:66:4c:4a:4c:56:50:32:63:37:51:5a:6a:66:50:68:72:78:67:3d:3d";

        #endregion EncryptedMessageData

        private const string _messageSignature = "84:b8:a7:4c:9c:94:5b:17:c7:0f:67:fc:be:2a:50:ad:59:38:6d:2e:9b:42:c0:1d:0b:a4:69:93:8b:18:fe:06:86:61:11:da:cc:97:c0:65:57:7a:1d:82:33:a0:2d:fe:0b:4f:84:c9:4c:90:2e:86:b4:6a:0b:2b:f8:92:a3:11:d1:e7:a7:7a:a9:2f:6b:52:98:7e:4e:ce:8b:74:df:5a:0f:ea:0c:25:a8:b8:1b:3e:71:02:c2:8e:62:73:0a:e9:74:66:c4:9c:9d:f4:6e:15:ea:94:f3:50:95:17:ff:8f:12:7e:cf:8b:90:93:60:88:f2:f9:6e:63:95:d2:cd:ea:5e:92:2b:d6:20:5f:cf:58:54:4f:c7:1b:02:56:fd:ac:2d:2b:d5:23:6e:b4:43:70:b6:0c:14:1a:8c:01:7e:b3:b7:2e:5a:5b:f5:63:83:d1:10:e3:c7:76:b6:37:c9:71:04:fd:68:82:8a:cb:c1:04:d7:51:b1:df:20:1c:12:d6:3a:26:71:8e:29:e9:17:34:45:a5:53:4b:37:a9:0d:20:f2:c4:53:a3:fe:e4:e9:4b:60:9f:76:ba:54:12:e3:1f:33:bd:ef:04:c9:99:3b:07:40:7d:42:62:4d:91:da:b5:88:2b:be:ed:e3:0a:02:ea:e4:3d:a4:ef:45:4d:90:04:f2:16:d6:fd:7f:9a:5f:d6:67:61:bc:0b:68:01:e8:c2:06:9a:05:ff:47:a4:3e:7f:5d:d2:6c:97:40:bc:d6:e0:fc:f0:9b:f0:0a:c7:f1:8f:1d:32:07:ef:e9:55:73:22:21:b9:80:31:e3:7b:32:f4:45:e6:e6:b2:97:fb:5e:0e:33:2c:75:e6:aa:ea:7d:68:ec:c7:a6:22:cf:18:35:88:f6:f1:c8:fe:e7:85:be:4b:cb:b6:61:ef:b6:13:b6:33:9b:74:34:b1:17:88:fc:d5:7c:01:7e:21:7c:cf:de:b1:73:a4:1e:27:aa:1f:1c:97:00:8f:de:db:a3:c3:b8:9c:15:df:f0:0a:03:a9:2b:ec:1c:2a:8b:2f:0f:9f:f3:f4:52:de:c7:08:9d:24:5d:70:00:5d:5c:eb:10:72:6a:cf:76:b0:d3:e7:57:40:21:d1:12:a2:6c:f7:1d:d8:cd:91:44:f1:58:7c:15:c6:f9:36:dd:cc:c3:45:77:80:59:b3:95:56:60:41:34:ee:c8:9d:60:1a:70:76:02:b0:39:0b:6b:1d:5f:e1:11:76:1c:37:21:41:54:91:4d:a9:d2:23:49:63:2e:f5:49:53:d2:18:3e:07:e3:25:d6:a2:17:1e:9d:2b:a1:6d:2d:be:b7:5d:d4:59:9f:d5:d5:b4:46:86";
        private const long _signatureTimestamp = 1418501091;

        #region Additional test attributes
        //
        // You can use the following additional attributes as you write your tests:
        //
        // Use ClassInitialize to run code before running the first test in the class
        // [ClassInitialize()]
        // public static void MyClassInitialize(TestContext testContext) { }
        //
        // Use ClassCleanup to run code after all tests in a class have run
        // [ClassCleanup()]
        // public static void MyClassCleanup() { }
        //
        // Use TestInitialize to run code before running each test 
        // [TestInitialize()]
        // public void MyTestInitialize() { }
        //
        // Use TestCleanup to run code after each test has run
        // [TestCleanup()]
        // public void MyTestCleanup() { }
        //
        #endregion

        private static byte[] HexStringToByteArray(string hex)
        {
            string[] hexValues = hex.Split(new char[] { ':' }, StringSplitOptions.RemoveEmptyEntries);

            byte[] result = new byte[hexValues.Length];

            for (int i = 0; i < hexValues.Length; i++)
            {
                result[i] = Convert.ToByte(hexValues[i], 16);
            }

            return result;
        }

        private static string ByteArrayToHexString(byte[] data)
        {
            StringBuilder hex = new StringBuilder();
            foreach (byte b in data)
                hex.AppendFormat("{0:x2}:", b);
            hex.Remove(hex.Length - 1, 1);
            return hex.ToString();
        }

        private static byte[] StringToByteArray(string data)
        {
            byte[] result = new byte[data.Length];

            for (int i = 0; i < data.Length; i++)
            {
                result[i] = Convert.ToByte(data[i]);
            }

            return result;
            //return Convert.FromBase64String(data);
        }
        
        private static string ByteArrayToStringTest(byte[] data)
        {
            StringBuilder result = new StringBuilder(data.Length);

            for (int i = 0; i< data.Length;i++)
            {
                result.Append(Convert.ToChar(data[i]));
            }

            return result.ToString();
        }

        private static string ByteArrayToString(byte[] data)
        {
            StringBuilder result = new StringBuilder(data.Length);

            for (int i = 0; i < data.Length; i++)
            {
                result.Append(Convert.ToChar(data[i]));
            }

            return result.ToString();
        }
        
        private Keypair ConvertStringToKeyPair(string privateKey, string publicKey)
        {
            Keypair result = new Keypair();

            result.PrivateKey = privateKey;
            result.PublicKey = publicKey;

            return result;
        }

        private static Keypair ExportKey(AsymmetricCipherKeyPair keyPair)
        {
            Keypair result = new Keypair();

            using (var writer = new StringWriter())
            {
                PemWriter pemWriter = new PemWriter(writer);
                pemWriter.Writer.NewLine = "\n";

                pemWriter.WriteObject(keyPair.Private);
                pemWriter.Writer.Flush();

                result.PrivateKey = writer.ToString();

                pemWriter.WriteObject(keyPair.Public);
                pemWriter.Writer.Flush();

                result.PublicKey = writer.ToString();
            }

            if (string.IsNullOrEmpty(result.PrivateKey)
                && string.IsNullOrEmpty(result.PublicKey))
                throw new Exception("Error while exporting the key.");

            return result;
        }

        #region CreateKeyPair

        [Test]
        public void CreateKeyPair_ValidLength_ResultContainsValidPrivateKey()
        {
            // Arrange
            ICryptography crypto = new Cryptography();

            // Act
            Keypair result = crypto.CreateKeyPair(1024);

            // Assert
            StringAssert.StartsWith("-----BEGIN RSA PRIVATE KEY-----\n", result.PrivateKey);
            StringAssert.EndsWith("\n-----END RSA PRIVATE KEY-----\n", result.PrivateKey);
        }

        [Test]
        public void CreateKeyPair_ValidLength_ResultContainsValidPublicKey()
        {
            // Arrange
            ICryptography crypto = new Cryptography();

            // Act
            Keypair result = crypto.CreateKeyPair(1024);

            // Assert
            StringAssert.StartsWith("-----BEGIN PUBLIC KEY-----\n", result.PublicKey);
            StringAssert.EndsWith("\n-----END PUBLIC KEY-----\n", result.PublicKey);
        }

        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void CreateKeyPair_LengthShorterThan1024_ThrowsArgumentException()
        {
            // Arrange
            ICryptography crypto = new Cryptography();

            // Act
            Keypair result = crypto.CreateKeyPair(1023);

            // Assert
            // see method attribute
        }

        #endregion CreateKeyPair

        #region EncryptMessage

        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void EncryptMessage_InvalidReceiverKey_ThrowsArgumentException()
        {
            // Arrange
            ICryptography crypto = new Cryptography();

            // Act
            crypto.EncryptMessage(null, "Hello Test!");

            // Assert
            // see method attribute
        }

        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void EncryptMessage_InvalidMessage_ThrowsArgumentException()
        {
            // Arrange
            ICryptography crypto = new Cryptography();

            // Act
            crypto.EncryptMessage("receiver", null);

            // Assert
            // see method attribute
        }

        [Test]
        public void EncryptMessage_ShortMessageToEncrypt_ReturnsEncryptedNonce()
        {
            // Arrange
            byte[] seed = HexStringToByteArray(_nonce);
            byte[] expectedEncryptedNonce = HexStringToByteArray(_encryptedNonce);

            SecureRandom random = MockRepository.GenerateStub<SecureRandom>();
            random.Stub<SecureRandom>(x => x.GenerateSeed(48))
                .Return(seed);

            ICryptography crypto = new Cryptography(random);

            // Act
            MessageBody message = crypto.EncryptMessage(_receiverPublicKey, _message);

            var encryptedNonce = StringToByteArray(message.head.nonce);

            // Assert
            // Because of the encryption algorithm the encrypted nonce should
            // never be the same.
            CollectionAssert.AreNotEqual(expectedEncryptedNonce, encryptedNonce);
        }

        [Test]
        public void EncryptMessage_ShortMessageToEncrypt_ReturnsEncryptedMessage()
        {
            // Arrange
            byte[] seed = HexStringToByteArray(_nonce);
            byte[] expectedEncryptedMessage = HexStringToByteArray(_encryptedMessage);
            
            SecureRandom random = MockRepository.GenerateStub<SecureRandom>();
            random.Stub<SecureRandom>(x => x.GenerateSeed(48))
                .Return(seed);

            ICryptography crypto = new Cryptography(random);

            // Act
            MessageBody message = crypto.EncryptMessage(_receiverPublicKey, _message);

            var encryptedMessage = StringToByteArray(message.body);
            
            // Assert
            CollectionAssert.AreEqual(expectedEncryptedMessage, encryptedMessage);
        }

        #endregion EncryptMessage

        #region DecryptMessage

        //[Test]
        //[ExpectedException(typeof(ArgumentException))]
        //public void DecryptMessage_NoKeySpecified_ThrowsArgumentException()
        //{
        //    // Arrange
        //    ICryptography crypto = new Cryptography();
        //    MessageBody message = new MessageBody()
        //    {
        //        data = _encryptedMessage,
        //        meta = new MessageMeta()
        //        {
        //            once = _encryptedNonce
        //        }
        //    };

        //    // Act
        //    crypto.DecryptMessage(null, message);

        //    // Assert
        //    // see method attribute
        //}

        [Test]
        public void DecryptMessage_NoMessageToDecrypt_ReturnsEmptyString()
        {
            // Arrange
            ICryptography crypto = new Cryptography();

            Keypair keyPair = ConvertStringToKeyPair(_receiverPrivateKey, _receiverPublicKey);

            // Act
            string decryptedMessage = crypto.DecryptMessage(keyPair, new ReceivedMessageBody());

            // Assert
            Assert.AreEqual(string.Empty, decryptedMessage);
        }

        [Test]
        public void DecryptMessage_CompleteMessageToDecrypt_ReturnsDecryptedMessage()
        {
            // Arrange
            ICryptography crypto = new Cryptography();

            byte[] messageText = HexStringToByteArray(_encryptedMessage);
            byte[] nonce = HexStringToByteArray(_encryptedNonce);

            ReceivedMessageBody message = new ReceivedMessageBody()
            {
                body = ByteArrayToString(messageText),
                head = new ReceivedMessageHead()
                {
                    nonce = ByteArrayToString(nonce)
                }
            };

            Keypair keyPair = ConvertStringToKeyPair(_receiverPrivateKey, _receiverPublicKey);

            // Act
            string decryptedMessage = crypto.DecryptMessage(keyPair, message);

            // Assert
            Assert.AreEqual(_message, decryptedMessage);
        }

        #endregion DecryptMessage

        #region SignData

        [Test]
        public void SignData_DataSetNull_ReturnsEmptyArray()
        {
            // Arrange
            ICryptography crypto = new Cryptography();
            Keypair keyPair = ConvertStringToKeyPair(_senderPrivateKey, _senderPublicKey);
            long timestamp = 0;

            // Act
            byte[] result = crypto.SignData(null, keyPair, timestamp);

            // Assert
            Assert.AreEqual(0, result.Length);
        }

        [Test]
        public void SignData_EmptyStringAsData_ReturnsSignature()
        {
            // Arrange
            ICryptography crypto = new Cryptography();
            Keypair keyPair = ConvertStringToKeyPair(_senderPrivateKey, _senderPublicKey);
            long timestamp = 0;

            // Act
            byte[] result = crypto.SignData(string.Empty, keyPair, timestamp);

            // Assert
            Assert.AreNotEqual(0, result.Length);
        }

        [Test]
        public void SignData_DataToSign_ReturnsExpectedSignature()
        {
            // Arrange
            ICryptography crypto = new Cryptography();
            Keypair keyPair = ConvertStringToKeyPair(_senderPrivateKey, _senderPublicKey);
            byte[] expectedSignature = HexStringToByteArray(_messageSignature);
            
            // Act
            byte[] result = crypto.SignData(_message, keyPair, _signatureTimestamp);

            // Assert
            CollectionAssert.AreEqual(expectedSignature, result);
        }

        /*
        [Test]
        public void SignData_NoDataToSign_ReturnsEmptyArray()
        {
            // Arrange

            // Act

            // Assert
        }
         */

        #endregion SignData
    }
}
