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
using System.Text.RegularExpressions;
using pssst.Api.Interface;

namespace pssst.Api
{
    public sealed class PssstClient : IPssstClient
    {
        private const int KeyLength = 2048;

        public Uri Host { get; private set; }

        private readonly ICryptography cryptoProvider;
        private readonly ICommunication server;

        public PssstClient()
        {
            this.cryptoProvider = new Cryptography();
            this.server = new Communication();
        }

        public PssstClient(ICryptography cryptoProvider,
            ICommunication server)
            : this()
        {
            this.cryptoProvider = cryptoProvider;
            this.server = server;
        }

        public void Configure(Uri host)
        {
            Host = host;
        }

        public void Configure(string host, int port)
        {
            Configure(new Uri(String.Format("{0}:{1}", host, port)));
        }

        public User CreateUser(string userName)
        {
            if (string.IsNullOrEmpty(userName))
                throw new ArgumentException("Argument must not be null.", "userName");

            Match result = Regex.Match(userName, "^(pssst\\.)?([a-z0-9]{2,62})(\\.[a-z0-9]{2,62})?(:\\S*)?$");

            if (!result.Success)
                return null;

            string name = result.Groups[2].Value;

            string box = "box";

            if (!string.IsNullOrEmpty(result.Groups[3].Value))
                box = result.Groups[3].Value.Trim('.');

            string password = result.Groups[4].Value.Trim(':');

            Keypair keys = this.cryptoProvider.CreateKeyPair(KeyLength);

            User user = new User(name, box, password, keys.PrivateKey, keys.PublicKey);

            this.server.CreateUser(Host, user);

            return user;
        }

        public bool SendMessage(User sender, User receiver, string message)
        {
            if (sender == null || string.IsNullOrEmpty(sender.Name))
                return false;

            if (receiver == null || string.IsNullOrEmpty(receiver.Name))
                return false;

            if (string.IsNullOrEmpty(message))
                return false;

            MessageBody messageBody = this.cryptoProvider.EncryptMessage(
                receiver.PublicKey, message);

            messageBody.head = new MessageHead()
            {
                nonce = messageBody.head.nonce,
                user = sender.Name
            };

            this.server.SendMessage(Host, sender, receiver, messageBody);

            return true;
        }

        public User GetUser(string userName)
        {
            if (string.IsNullOrEmpty(userName))
                return null;

            string publicKey = this.server.GetPublicKey(Host, userName);

            if (string.IsNullOrEmpty(publicKey))
                return null;

            User result = new User(
                userName, string.Empty, string.Empty, string.Empty, publicKey);

            return result;
        }

        public ReceivedMessageBody? ReceiveMessage(User user)
        {
            if (user == null)
                throw new ArgumentNullException("user");

            ReceivedMessageBody? receivedMessage = this.server.ReceiveMessage(Host, user);

            if (!receivedMessage.HasValue)
                return null;

            ReceivedMessageBody message = receivedMessage.Value;

            message.body = this.cryptoProvider.DecryptMessage(
                new Keypair()
                {
                    PublicKey = user.PublicKey,
                    PrivateKey = user.PrivateKey
                },
                message);

            return message;
        }
    }
}