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
using System.Linq;
using pssst.Api.Pcl.Interface;
using Rhino.Mocks;
using NUnit.Framework;

namespace pssst.Api.Pcl.Tests.Unit
{
    /// <summary>
    /// Summary description for PssstClientTests
    /// </summary>
    [TestFixture]
    public class PssstClientTests
    {
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

        private IPssstClient CreateClientWithDefaultStubs()
        {
            ICryptography crypto = MockRepository.GenerateStub<ICryptography>();
            ICommunication communication = MockRepository.GenerateStub<ICommunication>();

            IPssstClient client = new PssstClient(crypto, communication);

            return client;
        }

        #region Configure

        [Test]
        public void Configure_ProvideValidHost_HostIsSet()
        {
            // Arrange
            Uri expectedHost = new Uri("http://pssst.tests");

            IPssstClient client = new PssstClient();

            // Act
            client.Configure(expectedHost);

            // Assert
            Assert.AreEqual(expectedHost, client.Host);
        }

        [Test]
        public void Configure_ProvideHostAndPortSeperatly_HostIsSet()
        {
            // Arrange
            string host = "http://pssst.tests";
            int port = 80;

            Uri expectedUri = new Uri(string.Format("{0}:{1}", host, port));

            IPssstClient client = new PssstClient();

            // Act
            client.Configure(host, port);

            // Assert
            Assert.AreEqual(expectedUri, client.Host);
        }

        #endregion Configure

        #region CreateUser

        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void CreateUser_ProvideEmptyString_ThrowsArgumentException()
        {
            // Arrange
            IPssstClient client = new PssstClient();

            // Act
            client.CreateUser(string.Empty);

            // Assert
            // see method attribute
        }

        [Test]
        [ExpectedException(typeof(ArgumentException))]
        public void CreateUser_ProvideNull_ThrowsArgumentException()
        {
            // Arrange
            IPssstClient client = new PssstClient();

            // Act
            client.CreateUser(null);
               
            // Assert
            // see method attribute
        }

        [Test]
        public void CreateUser_ProvideValidUsernames_ReturnsUser()
        {
            // Arrange
            IList<string> validNames = new List<string>()
            {
                "pssst.username.box:p4ssW0rd",
                "pssst.username:p4ssW0rd",
                "pssst.un.bo:p",
                "pssst.usernameusernameusernameusernameusernameusernameusernameuserna.box:p4ssW0rd",
                "pssst.username.boxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxbo:p4ssW0rd",
                "pssst.username.box",
                "pssst.1username.box",
                "pssst.username.1box:password",
                "username.box:p4ssW0rd",
                "username:p4ssW0rd",
                "username.box",
                "username",
            };

            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            IList<User> result = new List<User>();

            foreach (var name in validNames)
            {
                result.Add(client.CreateUser(name));
            }

            // Assert
            Assert.IsFalse(result.Contains(null));
        }

        [Test]
        public void CreateUser_ProvideInvalidUsernames_ReturnsNull()
        {
            // Arrange
            IList<string> validNames = new List<string>()
            {
                "PSSST.username.box:password",
                "apssst.username.box:password",
                "psssta.username.box:password",
                "1pssst.username.box:password",
                "pssst1.username.box:password",
                "pssst.Username.box:password",
                "pssst.username.Box:password",
                "pssst.usernameusernameusernameusernameusernameusernameusernameusernam.box:p4ssW0rd",
                "pssst.username.boxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxboxbox:p4ssW0rd",
            };

            IPssstClient client = new PssstClient();

            // Act
            IList<User> result = new List<User>();

            foreach (var name in validNames)
            {
                result.Add(client.CreateUser(name));
            }

            // Assert
            Assert.IsFalse(result.Any(u => u != null));
        }

        [Test]
        public void CreateUser_ProvideValidUserName_ReturnsFilledUser()
        {
            // Arrange
            string expectedName = "username";
            string expectedBox = "box";
            string expectedPassword = "p4ssW0rd";

            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            User result = client.CreateUser(
                string.Format("pssst.{0}.{1}:{2}", expectedName, expectedBox, expectedPassword));

            // Assert
            Assert.AreEqual(expectedName, result.Name);
            Assert.AreEqual(expectedBox, result.Box);
            Assert.AreEqual(expectedPassword, result.Password);
        }

        [Test]
        public void CreateUser_ProvideUserAndBoxWithoutPassword_ReturnsUser()
        {
            // Arrange
            string expectedName = "username";
            string expectedBox = "box";
            string expectedPassword = string.Empty;

            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            User result = client.CreateUser(
                string.Format("pssst.{0}.{1}", expectedName, expectedBox));

            // Assert
            Assert.AreEqual(expectedName, result.Name);
            Assert.AreEqual(expectedBox, result.Box);
            Assert.AreEqual(expectedPassword, result.Password);
        }

        [Test]
        public void CreateUser_ProvideUserWithoutBoxAndPassword_ReturnsUserWithDefaultBox()
        {
            // Arrange
            string expectedName = "username";
            string expectedBox = "box";
            string expectedPassword = string.Empty;

            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            User result = client.CreateUser(
                string.Format("pssst.{0}", expectedName));

            // Assert
            Assert.AreEqual(expectedName, result.Name);
            Assert.AreEqual(expectedBox, result.Box);
            Assert.AreEqual(expectedPassword, result.Password);
        }

        [Test]
        public void CreateUser_ProvideValidUser_CreatesKeyPair()
        {
            // Arrange
            string expectedPublicKey = "1234";
            string expectedPrivateKey = "4321";

            ICryptography crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub<ICryptography>(x => x.CreateKeyPair(0))
                .IgnoreArguments()
                .Return(new Keypair() 
                    {
                        PrivateKey = expectedPrivateKey,
                        PublicKey = expectedPublicKey 
                    });

            var communication = MockRepository.GenerateStub<ICommunication>();

            IPssstClient client = new PssstClient(crypto, communication);

            // Act
            User result = client.CreateUser("pssst.user");

            // Assert
            Assert.AreEqual(expectedPrivateKey, result.PrivateKey);
            Assert.AreEqual(expectedPublicKey, result.PublicKey);
        }

        [Test]
        public void CreateUser_ProvideValidUser_CreatesUserAtServer()
        {
            // Arrange
            Uri expectedHost = new Uri("http://pssst.tests");
            
            string expectedPublicKey = "1234";
            string expectedPrivateKey = "4321";

            ICryptography crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub<ICryptography>(x => x.CreateKeyPair(0))
                .IgnoreArguments()
                .Return(new Keypair()
                {
                    PrivateKey = expectedPrivateKey,
                    PublicKey = expectedPublicKey
                });

            ICommunication communication = MockRepository.GenerateMock<ICommunication>();
            communication.Expect(x => x.CreateUser(null, null))
                .IgnoreArguments();

            IPssstClient client = new PssstClient(crypto, communication);
            client.Configure(expectedHost);

            // Act
            User result = client.CreateUser("pssst.user");

            // Assert
            var arguments = communication.GetArgumentsForCallsMadeOn(
                x => x.CreateUser(null, new User(string.Empty, string.Empty, string.Empty, string.Empty, string.Empty)));

            communication.VerifyAllExpectations();
            Assert.AreEqual(expectedHost, arguments[0][0]);
            Assert.AreEqual(result, arguments[0][1]);
        }

        #endregion CreateUser

        #region SendMessage

        [Test]
        public void SendMessage_NoSenderDefined_ReturnsFalse()
        {
            // Arrange
            User receiver = new User(
                "Receiver", string.Empty, string.Empty, string.Empty, string.Empty);
            string message = "Hello Test";
            
            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            bool result = client.SendMessage(null, receiver, message);

            // Assert
            Assert.IsFalse(result);
        }

        [Test]
        public void SendMessage_EmptySenderName_ReturnsFalse()
        {
            // Arrange
            User sender = new User(
                string.Empty, string.Empty, string.Empty, string.Empty, string.Empty);
            User receiver = new User(
                "Receiver", string.Empty, string.Empty, string.Empty, string.Empty);
            string message = "Hello Test";
            
            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            bool result = client.SendMessage(sender, receiver, message);

            // Assert
            Assert.IsFalse(result);
        }

        [Test]
        public void SendMessage_NoReceiverDefined_ReturnsFalse()
        {
            // Arrange
            User sender = new User(
                "Sender", string.Empty, string.Empty, string.Empty, string.Empty);
            string message = "Hello Test";

            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            bool result = client.SendMessage(sender, null, message);

            // Assert
            Assert.IsFalse(result);
        }

        [Test]
        public void SendMessage_EmptyReceiverName_ReturnsFalse()
        {
            // Arrange
            User sender = new User(
                "Sender", string.Empty, string.Empty, string.Empty, string.Empty);
            User receiver = new User(
                string.Empty, string.Empty, string.Empty, string.Empty, string.Empty);
            string message = "Hello Test";
            
            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            bool result = client.SendMessage(sender, receiver, message);

            // Assert
            Assert.IsFalse(result);
        }

        [Test]
        public void SendMessage_NoMessageDefined_ReturnsFalse()
        {
            // Arrange
            User sender = new User(
                "Sender", string.Empty, string.Empty, string.Empty, string.Empty);
            User receiver = new User(
                "Receiver", string.Empty, string.Empty, string.Empty, string.Empty);
            
            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            bool result = client.SendMessage(sender, receiver, null);

            // Assert
            Assert.IsFalse(result);
        }

        [Test]
        public void SendMessage_ValidDataProvided_MessageGetsEncryptedAndReturnsTrue()
        {
            // Arrange
            User sender = new User(
                "Sender", string.Empty, string.Empty, string.Empty, string.Empty);
            User receiver = new User(
                "Receiver", string.Empty, string.Empty, string.Empty, "1234");

            string message = "Hello Test!";

            ICryptography crypto = MockRepository.GenerateMock<ICryptography>();
            crypto.Expect(x => x.EncryptMessage(receiver.PublicKey, message))
                .Return(new MessageBody());

            ICommunication communication = MockRepository.GenerateStub<ICommunication>();

            IPssstClient client = new PssstClient(crypto, communication);

            // Act
            bool result = client.SendMessage(sender, receiver, message);

            // Assert
            crypto.VerifyAllExpectations();
            Assert.IsTrue(result);
        }

        [Test]
        public void SendMessage_ValidDataProvided_MessageGetsSendToServerAndReturnsTrue()
        {
            // Arrange
            Uri expectedHost = new Uri("http://pssst.tests");

            User sender = new User(
                "Sender", string.Empty, string.Empty, string.Empty, string.Empty);
            User receiver = new User(
                "Receiver", string.Empty, string.Empty, string.Empty, "1234");

            string message = "Hello Test!";

            ICryptography crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub<ICryptography>(x => x.EncryptMessage(null, null))
                .IgnoreArguments()
                .Return(new MessageBody());

            MessageBody messageBody = new MessageBody()
            {
                head = new MessageHead()
                {
                    user = sender.Name
                }
            };

            ICommunication communication = MockRepository.GenerateMock<ICommunication>();
            communication.Expect(x => x.SendMessage(expectedHost, sender, receiver, messageBody));

            IPssstClient client = new PssstClient(crypto, communication);

            client.Configure(expectedHost);

            // Act
            bool result = client.SendMessage(sender, receiver, message);

            // Assert
            communication.VerifyAllExpectations();
            Assert.IsTrue(result);
        }

        #endregion SendMessage

        #region ReceiveMessage

        [Test]
        [ExpectedException(typeof(ArgumentNullException))]
        public void ReceiveMessage_NoReceiverDefined_ThrowsArgumentException()
        {
            // Arrange
            IPssstClient client = CreateClientWithDefaultStubs();

            // Act
            ReceivedMessageBody? result = client.ReceiveMessage(null);

            // Assert
            // See method Attribute
        }

        [Test]
        public void ReceiveMessage_ServerHasOneMessageForUser_ReturnsTheMessage()
        {
            // Arrange
            Uri host = new Uri("http://pssst.tests");
            User user = new User("Receiver", string.Empty, string.Empty, string.Empty, string.Empty);

            ReceivedMessageBody expectedMessage = new ReceivedMessageBody()
            {
                body = "Message Text",
                head = new ReceivedMessageHead()
                {
                    nonce = "nonce",
                    time = 123456,
                    user = "Sender"
                }
            };

            ICommunication communication = MockRepository.GenerateStub<ICommunication>();
            communication.Stub(x => x.ReceiveMessage(host, user))
                .IgnoreArguments()
                .Return(expectedMessage);

            ICryptography crypto = MockRepository.GenerateMock<ICryptography>();
            crypto.Expect(x => x.DecryptMessage(new Keypair(), new ReceivedMessageBody()))
                .IgnoreArguments()
                .Return(expectedMessage.body);            

            IPssstClient client = new PssstClient(crypto, communication);

            // Act
            ReceivedMessageBody? result = client.ReceiveMessage(user);

            // Assert
            Assert.AreEqual(expectedMessage, result.Value);
        }

        [Test]
        public void ReceiveMessage_ServerHasNoMessageForUser_ReturnsNull()
        {
            // Arrange
            Uri host = new Uri("http://pssst.tests");
            User user = new User("Receiver", string.Empty, string.Empty, string.Empty, string.Empty);

            ICommunication communication = MockRepository.GenerateStub<ICommunication>();
            communication.Stub(x => x.ReceiveMessage(host, user))
                .IgnoreArguments()
                .Return(null);

            IPssstClient client = new PssstClient(null, communication);

            // Act
            ReceivedMessageBody? result = client.ReceiveMessage(user);

            // Assert
            Assert.IsFalse(result.HasValue);
        }

        [Test]
        public void ReceiveMessage_ReceivedMessageGetsDecryptedWithCorrectKeys_ReturnsDecryptedMessage()
        {
            // Arrange
            Uri host = new Uri("http://pssst.tests");
            User user = new User("Receiver", string.Empty, string.Empty, "PrivateKey", "PublicKey");
            Keypair expectedKeyPair = new Keypair() { PrivateKey = user.PrivateKey, PublicKey = user.PublicKey };

            ReceivedMessageBody encryptedMessage = new ReceivedMessageBody() { body = "encryptedMessage" };
            ReceivedMessageBody decryptedMessage = new ReceivedMessageBody() { body = "decryptedMessage" };

            ICommunication communication = MockRepository.GenerateStub<ICommunication>();
            communication.Stub(x => x.ReceiveMessage(host, user))
                .IgnoreArguments()
                .Return(encryptedMessage);

            ICryptography crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub(x => x.DecryptMessage(expectedKeyPair, encryptedMessage))
                .Return(decryptedMessage.body);

            IPssstClient client = new PssstClient(crypto, communication);

            // Act
            ReceivedMessageBody? result = client.ReceiveMessage(user);

            // Assert
            Assert.AreEqual(decryptedMessage.body, result.Value.body);
        }

        #endregion ReceiveMessage

        #region GetUser

        [Test]
        public void GetUser_ProvideExistingUserName_ReturnsUserWithPublicKey()
        {
            // Arrange
            Uri expectedHost = new Uri("http://pssst.tests");
            string expectedUserName = "Receiver";
            string expectedPublicKey = "1234";

            ICryptography crypto = MockRepository.GenerateStub<ICryptography>();

            ICommunication communicaton = MockRepository.GenerateStub<ICommunication>();
            communicaton.Stub<ICommunication>(x => x.GetPublicKey(expectedHost, expectedUserName))
                .Return(expectedPublicKey);
            
            IPssstClient client = new PssstClient(crypto, communicaton);

            client.Configure(expectedHost);

            // Act
            User result = client.GetUser(expectedUserName);

            // Assert
            Assert.AreEqual(expectedUserName, result.Name);
            Assert.AreEqual(expectedPublicKey, result.PublicKey);
        }

        [Test]
        public void GetUser_ProvideNotExistingUserName_ReturnsNull()
        {
            // Arrange
            Uri expectedHost = new Uri("http://pssst.tests");
            string expectedUserName = "Receiver";
            string expectedPublicKey = "1234";

            ICryptography crypto = MockRepository.GenerateStub<ICryptography>();

            ICommunication communicaton = MockRepository.GenerateStub<ICommunication>();
            communicaton.Stub<ICommunication>(x => x.GetPublicKey(expectedHost, expectedUserName))
                .Return(expectedPublicKey);

            IPssstClient client = new PssstClient(crypto, communicaton);

            client.Configure(expectedHost);

            // Act
            User result = client.GetUser("DifferentUserName");

            // Assert
            Assert.IsNull(result);
        }

        [Test]
        public void GetUser_ProvideNoUserName_ReturnsNull()
        {
            // Arrange
            Uri expectedHost = new Uri("http://pssst.tests");
            string expectedUserName = "Receiver";
            string expectedPublicKey = "1234";

            ICryptography crypto = MockRepository.GenerateStub<ICryptography>();

            ICommunication communicaton = MockRepository.GenerateStub<ICommunication>();
            communicaton.Stub<ICommunication>(x => x.GetPublicKey(expectedHost, expectedUserName))
                .IgnoreArguments()
                .Return(expectedPublicKey);

            IPssstClient client = new PssstClient(crypto, communicaton);

            client.Configure(expectedHost);

            // Act
            User result = client.GetUser(string.Empty);

            // Assert
            Assert.IsNull(result);
        }

        #endregion GetUser       
    }
}
