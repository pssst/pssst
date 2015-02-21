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
using System.Configuration;
using System.IO;
using System.Xml.Serialization;
using pssst.Api.Pcl;
using pssst.Api.Pcl.Interface;

namespace pssst.Cli
{
    internal class Program
    {
        private static void Main(string[] args)
        {
            if (args.Length < 2)
                return;

            if (!Configure())
                return;

            string command = args[0];
            string username = args[1];

            switch (command.ToLower())
            {
                case "create":
                    CreateUser(username);
                    break;

                case "pull":
                    PullMessage(username);
                    break;

                case "push":
                    PushMessage(username, args[2], args[3]);
                    break;
            }
        }

        private static Uri _server;

        private static bool Configure()
        {
            string server;

            try
            {
                server = ConfigurationManager.AppSettings["server"];
            }
            catch (ConfigurationErrorsException e)
            {
                Console.WriteLine(e.Message);
                return false;
            }

            if (string.IsNullOrEmpty(server))
                return false;

            try
            {
                _server = new Uri(server);
            }
            catch (UriFormatException e)
            {
                Console.WriteLine(e.Message);
                return false;
            }

            return true;
        }

        private static void CreateUser(string username)
        {
            IPssstClient client = CreateClient();

            User newUser = client.CreateUser(username);

            SerializeData(newUser, newUser.Name + ".pssst");
        }

        private static void PullMessage(string username)
        {
            User user = LoadUser(username);

            if (user == null)
                return;

            IPssstClient client = CreateClient();
            ReceivedMessageBody? message = client.ReceiveMessage(user);

            if (!message.HasValue)
                return;

            Console.WriteLine("From: '{0}' received at '{1}': '{2}'",
                message.Value.head.user,
                message.Value.head.time,
                message.Value.body);
        }

        private static void PushMessage(string username, string receivername, string message)
        {
            User user = LoadUser(username);

            if (user == null)
                return;

            IPssstClient client = CreateClient();

            User receiver = client.GetUser(receivername);

            if (receiver == null)
                return;

            client.SendMessage(user, receiver, message);
        }

        private static IPssstClient CreateClient()
        {
            IPssstClient client = new PssstClient();
            client.Configure(_server);

            return client;
        }

        private static User LoadUser(string username)
        {
            string filename = username + ".pssst";

            User user = DeserializeData<User>(filename);

            return user;
        }

        private static T DeserializeData<T>(string filename) where T : class
        {
            if (!File.Exists(filename))
                return null;

            T result = null;

            using (FileStream stream = new FileStream(filename, FileMode.Open))
            {
                result = new XmlSerializer(typeof(T)).Deserialize(stream) as T;
            }

            return result;
        }

        private static void SerializeData(object data, string filename)
        {
            using (TextWriter writer = new StreamWriter(filename))
            {
                new XmlSerializer(data.GetType()).Serialize(writer, data);
            }
        }
    }
}