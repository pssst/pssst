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
using System.Net.Http;
using System.Threading.Tasks;
using System.IO;
using System.Net;
using System.Threading;
using pssst.Api.Pcl.Interface;
using System.Linq;
using Rhino.Mocks;
using Newtonsoft.Json;
using NUnit.Framework;

namespace pssst.Api.Pcl.Tests.Unit
{
    /// <summary>
    /// Summary description for CommunicationTests
    /// </summary>
    [TestFixture]
    public class CommunicationTests
    {
        #region CreateUser Tests

        [Test]
        public void CreateUser_ValidUserData_SendsPublicKeyToCorrectUri()
        {
            // Arrange
            User user = new User("Sender", string.Empty, string.Empty, string.Empty, "MyPublicKey");

            Uri server = new Uri("http://api.pssst.name");
            Uri uri = new Uri(server, string.Format("/1/{0}", user.Name));
                        
            string content = string.Format("{{\"key\":\"{0}\"}}", user.PublicKey);
            
            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(null, HttpStatusCode.Accepted);

            var crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub(x => x.SignData(null, new Keypair(), 0))
                .IgnoreArguments()
                .Return(new byte[7]);

            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), crypto);

            // Act
            communication.CreateUser(server, user);
            var message = ((FakeHttpMessageHandler)fakeHttpMessageHandler).Request;

            // Assert
            Assert.AreEqual(uri, message.RequestUri);
            Assert.AreEqual(HttpMethod.Post, message.Method);
            Assert.AreEqual(content, ((FakeHttpMessageHandler)fakeHttpMessageHandler).RequestContent);
            Assert.AreEqual("application/json", message.Content.Headers.ContentType.MediaType);
        }

        [Test]
        public void CreateUser_ValidUserData_AddsSignatureToMessage()
        {
            // Arrange
            Uri server = new Uri("http://api.pssst.name");
            User user = new User("Sender", string.Empty, string.Empty, string.Empty, "MyPublicKey");

            byte[] signature = new byte[] { 0, 1, 2, 3, 4, 5, 6 };
            string expectedHash = Convert.ToBase64String(signature);

            var crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub(x => x.SignData(null, new Keypair(), 0))
                .IgnoreArguments()
                .Return(signature);

            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(null, HttpStatusCode.Accepted);
            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), crypto);

            // Act
            communication.CreateUser(server, user);
            var message = ((FakeHttpMessageHandler)fakeHttpMessageHandler).Request;
            var contentHash = message.Headers.GetValues("content-hash").First();
            
            // Assert
            string[] contentHashValues = contentHash.Split(new char[] { ';' }, StringSplitOptions.RemoveEmptyEntries);
            Assert.AreEqual(2, contentHashValues.Length);
            long.Parse(contentHashValues[0].Trim());
            Assert.AreEqual(expectedHash, contentHashValues[1].Trim());
        }

        [Test]
        public void CreateUser_ValidUserData_SendsKeyAsJson()
        {
            // Arrange
            string publicKey = "KeyWith\nLineBreak";
            string expectedJson = "{\"key\":\"KeyWith\\nLineBreak\"}";

            User user = new User("Sender", string.Empty, string.Empty, string.Empty, publicKey);

            Uri server = new Uri("http://api.pssst.name");
            Uri uri = new Uri(server, string.Format("/1/{0}", user.Name));

            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(null, HttpStatusCode.Accepted);

            var crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub(x => x.SignData(null, new Keypair(), 0))
                .IgnoreArguments()
                .Return(new byte[7]);

            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), crypto);

            // Act
            communication.CreateUser(server, user);
            var message = ((FakeHttpMessageHandler)fakeHttpMessageHandler).Request;

            // Assert
            Assert.AreEqual(expectedJson, ((FakeHttpMessageHandler)fakeHttpMessageHandler).RequestContent);
        }

        #endregion CreateUser Tests

        #region SendMessage Tests

        [Test]
        public void SendMessage_SendMessageWithNoBoxSpecified_SendsMessageToServer()
        {
            // Arrange
            User sender = new User("Sender", string.Empty, string.Empty, string.Empty, string.Empty);
            User receiver = new User("Receiver", string.Empty, string.Empty, string.Empty, string.Empty);

            Uri server = new Uri("http://api.pssst.name");
            Uri uri = new Uri(server, string.Format("/1/{0}/box", receiver.Name));

            var crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub(x => x.SignData(null, new Keypair(), 0))
                .IgnoreArguments()
                .Return(new byte[] { 0, 1, 2, 3, 4, 5, 6 });

            MessageBody messageBody = new MessageBody()
            {
                body = "Hallo Test",
                head = new MessageHead()
                {
                    user = "Sender",
                    nonce = "Nonce"
                }
            };

            string expectedContent = string.Format(
                "{{\"head\":{{\"user\":\"{0}\",\"nonce\":\"{1}\"}},\"body\":\"{2}\"}}",
                messageBody.head.user,
                messageBody.head.nonce,
                messageBody.body);

            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(null, HttpStatusCode.Accepted);
            
            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), crypto);

            // Act
            communication.SendMessage(server, sender, receiver, messageBody);
            var message = ((FakeHttpMessageHandler)fakeHttpMessageHandler).Request;

            // Assert
            Assert.AreEqual(uri, message.RequestUri);
            Assert.AreEqual(HttpMethod.Put, message.Method);
            Assert.AreEqual(expectedContent, ((FakeHttpMessageHandler)fakeHttpMessageHandler).RequestContent);
            Assert.AreEqual("application/json", message.Content.Headers.ContentType.MediaType);
        }

        [Test]
        public void SendMessage_SendValidMessage_AddsSignatureToMessage()
        {
            // Arrange
            Uri server = new Uri("http://api.pssst.name");
            User sender = new User("Sender", string.Empty, string.Empty, string.Empty, string.Empty);
            User receiver = new User("Receiver", string.Empty, string.Empty, string.Empty, string.Empty);

            byte[] signature = new byte[] { 0, 1, 2, 3, 4, 5, 6 };
            string expectedHash = Convert.ToBase64String(signature);

            var crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub(x => x.SignData(null, new Keypair(), 0))
                .IgnoreArguments()
                .Return(signature);

            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(null, HttpStatusCode.Accepted);
            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), crypto);

            // Act
            communication.SendMessage(server, sender, receiver, new MessageBody());
            var message = ((FakeHttpMessageHandler)fakeHttpMessageHandler).Request;
            var contentHash = message.Headers.GetValues("content-hash").First();

            // Assert
            string[] contentHashValues = contentHash.Split(new char[] { ';' }, StringSplitOptions.RemoveEmptyEntries);
            Assert.AreEqual(2, contentHashValues.Length);
            long.Parse(contentHashValues[0].Trim());
            Assert.AreEqual(expectedHash, contentHashValues[1].Trim());
        }

        #endregion SendMessage Tests

        #region GetPublicKey Tests

        [Test]
        public void GetPublicKey_RetrieveKeyOfExistingUser_SendsExpectedMessage()
        {
            // Arrange
            Uri server = new Uri("http://api.pssst.name");
            string requestedUser = "Receiver";
            Uri expectedUri = new Uri(server, string.Format("/1/{0}/key", requestedUser));

            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(null, HttpStatusCode.Accepted);
            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), null);

            // Act
            communication.GetPublicKey(server, requestedUser);
            var message = ((FakeHttpMessageHandler)fakeHttpMessageHandler).Request;

            // Assert
            Assert.AreEqual(expectedUri, message.RequestUri);
            Assert.AreEqual(HttpMethod.Get, message.Method);
            Assert.IsNull(((FakeHttpMessageHandler)fakeHttpMessageHandler).RequestContent);
        }

        [Test]
        public void GetPublicKey_RetrieveKeyOfExistingUser_ReturnsExpectedKey()
        {
            // Arrange
            Uri server = new Uri("http://api.pssst.name");
            string requestedUser = "Receiver";
            string expectedKey = "TestKey";

            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(expectedKey, HttpStatusCode.Accepted);
            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), null);

            // Act
            string result = communication.GetPublicKey(server, requestedUser);

            // Assert
            Assert.AreEqual(expectedKey, result);
        }

        #endregion GetPublicKey Tests

        [Test]
        [ExpectedException(typeof(ArgumentNullException))]
        public void ReceiveMessage_CallWithoutUserSpecified_ThrowsArgumentNullException()
        {
            // Arrange
            Uri server = new Uri("http://api.pssst.name");
            ICommunication communication = new HttpCommunication(null, null);

            // Act
            communication.ReceiveMessage(server, null);

            // Assert
            // See Method Attribute
        }

        [Test]
        [ExpectedException(typeof(ArgumentNullException))]
        public void ReceiveMessage_CallWithoutServerSpecified_ThrowsArgumentNullException()
        {
            // Arrange
            User user = new User("User", string.Empty, string.Empty, string.Empty, string.Empty);
            ICommunication communication = new HttpCommunication(null, null);

            // Act
            communication.ReceiveMessage(null, user);

            // Assert
            // See Method Attribute
        }

        [Test]
        public void ReceiveMessage_CallWithValidServerAndUser_CallsExpectedUri()
        {
            // Arrange
            Uri server = new Uri("http://api.pssst.name");
            User user = new User("Sender", string.Empty, string.Empty, string.Empty, string.Empty);
            Uri expectedUri = new Uri(server, string.Format("/1/{0}", user.Name));

            var crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub(x => x.SignData(null, new Keypair(), 0))
                .IgnoreArguments()
                .Return(new byte[7]);

            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(JsonConvert.SerializeObject(new ReceivedMessageBody()), HttpStatusCode.Accepted);

            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), crypto);

            // Act
            communication.ReceiveMessage(server, user);
            var message = ((FakeHttpMessageHandler)fakeHttpMessageHandler).Request;

            // Assert
            Assert.AreEqual(expectedUri, message.RequestUri);
            Assert.AreEqual(HttpMethod.Get, message.Method);            
        }

        [Test]
        public void ReceiveMessage_ServerHasOneMessageForUser_ReturnsMessage()
        {
            // Arrange
            Uri server = new Uri("http://api.pssst.name");
            User user = new User("Sender", string.Empty, string.Empty, string.Empty, string.Empty);

            ReceivedMessageBody encryptedMessage = new ReceivedMessageBody()
            {
                body = "MessageText",
                head = new ReceivedMessageHead()
                {
                    nonce = "Nonce",
                    user = "Sender",
                    time = 123456789
                }
            };

            var crypto = MockRepository.GenerateStub<ICryptography>();
            crypto.Stub(x => x.SignData(null, new Keypair(), 0))
                .IgnoreArguments()
                .Return(new byte[7]);

            HttpMessageHandler fakeHttpMessageHandler = FakeHttpMessageHandler.GetHttpMessageHandler(JsonConvert.SerializeObject(encryptedMessage), HttpStatusCode.Accepted);

            ICommunication communication = new HttpCommunication(new HttpClient(fakeHttpMessageHandler), crypto);

            // Act
            ReceivedMessageBody? messageBody = communication.ReceiveMessage(server, user);

            // Assert
            Assert.AreEqual(encryptedMessage, messageBody.Value);
        }
    }

    

    public class FakeHttpMessageHandler : HttpMessageHandler
    {
        private HttpResponseMessage _response;

        public static HttpMessageHandler GetHttpMessageHandler(string content, HttpStatusCode httpStatusCode)
        {
            var memStream = new MemoryStream();

            var sw = new StreamWriter(memStream);
            sw.Write(content);
            sw.Flush();
            memStream.Position = 0;

            var httpContent = new StreamContent(memStream);

            var response = new HttpResponseMessage()
            {
                StatusCode = httpStatusCode,
                Content = httpContent
            };

            var messageHandler = new FakeHttpMessageHandler(response);

            return messageHandler;
        }

        public HttpRequestMessage Request { get; private set; }

        public string RequestContent { get; private set; }

        public FakeHttpMessageHandler(HttpResponseMessage response)
        {
            _response = response;
        }

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            if (request.Method == HttpMethod.Get && request.Content != null)
                throw new Exception();

            var tcs = new TaskCompletionSource<HttpResponseMessage>();

            Request = request;

            if (Request.Content != null)
                RequestContent = request.Content.ReadAsStringAsync().Result;

            tcs.SetResult(_response);

            return tcs.Task;
        }
    }

}
