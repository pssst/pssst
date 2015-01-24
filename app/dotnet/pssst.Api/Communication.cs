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
using System.Linq;
using System.Net.Http;
using Newtonsoft.Json;
using pssst.Api.Interface;

namespace pssst.Api
{
    public sealed class Communication : ICommunication
    {
        private const string RelativeUriCreateUser = "/1/{0}";
        private const string RelativeUriSendMessage = "/1/{0}/{1}";
        private const string RelativeUriFind = "/1/{0}/key";
        private const string RelativeUriReceiveMessage = "/1/{0}";

        private readonly ICryptography _cryptography;

        // Todo: Dispose HttpClient
        private HttpClient _http;

        public Communication()
        {
            _http = new HttpClient();
            _cryptography = new Cryptography();
        }

        public Communication(HttpClient http, ICryptography cryptography)
        {
            _http = http;
            _cryptography = cryptography;
        }

        public void CreateUser(Uri server, User user)
        {
            Uri requestUri = new Uri(server, string.Format(RelativeUriCreateUser, user.Name));

            string content = string.Format("{{\"key\":\"{0}\"}}", user.PublicKey);

            // Newline must be escaped to handle it correct in JSON
            content = content.Replace("\n", "\\n");

            HttpRequestMessage message = CreateMessage(requestUri, HttpMethod.Post, content, user);

            HttpResponseMessage response = _http.SendAsync(message).Result;
        }

        public void SendMessage(Uri server, User sender, User receiver, MessageBody message)
        {
            Uri requestUri = new Uri(server, string.Format(RelativeUriSendMessage, receiver.Name, "box"));

            string content = PrepareContent(message);

            HttpRequestMessage httpMessage = CreateMessage(requestUri, HttpMethod.Put, content, sender);

            HttpResponseMessage response = _http.SendAsync(httpMessage).Result;
        }

        public ReceivedMessageBody? ReceiveMessage(Uri server, User user)
        {
            if (server == null)
                throw new ArgumentNullException("server");

            if (user == null)
                throw new ArgumentNullException("user");

            Uri requestUri = new Uri(server, string.Format(RelativeUriReceiveMessage, user.Name));

            HttpRequestMessage httpMessage = CreateMessage(requestUri, HttpMethod.Get, string.Empty, user);

            HttpResponseMessage response = _http.SendAsync(httpMessage).Result;

            string responseString = response.Content.ReadAsStringAsync().Result;

            ReceivedMessageBody result = JsonConvert.DeserializeObject<ReceivedMessageBody>(responseString);

            return result;
        }

        public string GetPublicKey(Uri server, string user)
        {
            Uri requestUri = new Uri(server, string.Format(RelativeUriFind, user));

            HttpRequestMessage httpMessage = CreateMessage(requestUri, HttpMethod.Get);

            HttpResponseMessage response = _http.SendAsync(httpMessage).Result;

            string publicKey = response.Content.ReadAsStringAsync().Result;

            return publicKey;
        }

        private string PrepareContent(object content)
        {
            if (content == null)
            {
                // return {} if no content is set
                return String.Empty;
            }

            string result = JsonConvert.SerializeObject(content);

            return result;
        }

        private long GetUnixTimestamp()
        {
            DateTime epoch = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
            long timestamp = Convert.ToInt64((DateTime.Now.ToUniversalTime() - epoch).TotalSeconds);

            return timestamp;
        }

        private HttpRequestMessage CreateMessage(Uri requestUri, HttpMethod method, string content, User sender)
        {
            if (content == null)
                content = string.Empty;

            string contentHash = CreateContentHash(sender, content);

            HttpRequestMessage message = CreateMessage(requestUri, method);

            if (!string.IsNullOrEmpty(content))
            {
                message.Content = new StringContent(content);
                message.Content.Headers.ContentType = new System.Net.Http.Headers.MediaTypeHeaderValue("application/json");
            }

            message.Headers.Add("content-hash", contentHash);

            return message;
        }

        private HttpRequestMessage CreateMessage(Uri requestUri, HttpMethod method)
        {
            HttpRequestMessage message = new HttpRequestMessage(method, requestUri);

            return message;
        }

        private string CreateContentHash(User sender, string content)
        {
            long timestamp = GetUnixTimestamp();

            string signature = CreateSignature(sender, content, timestamp);

            string contentHash = string.Format("{0}; {1}", timestamp, signature);

            return contentHash;
        }

        private string CreateSignature(User sender, string content, long timestamp)
        {
            byte[] signature = _cryptography.SignData(
                content,
                new Keypair()
                {
                    PrivateKey = sender.PrivateKey,
                    PublicKey = sender.PublicKey
                },
                timestamp);

            return Convert.ToBase64String(signature);
        }
    }
}