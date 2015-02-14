using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Serialization;
using pssst.Api;
using pssst.Api.Interface;

namespace pssst.Cli
{
    class Program
    {
        static void Main(string[] args)
        {
            if (args.Length < 2)
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
            client.Configure("http://192.168.0.101", 62421);

            return client;
        }

        private static User LoadUser(string username)
        {
            string filename = username + ".pssst";
            
            User user = DeserializeData<User>(filename);

            return user;
        }

        private static T DeserializeData<T>(string filename) where T: class
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
