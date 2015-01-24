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

namespace pssst.Api.Interface
{
    public sealed class User
    {
        public string Name { get; private set; }

        public string Box { get; private set; }

        public string Password { get; private set; }

        public string PrivateKey { get; private set; }

        public string PublicKey { get; private set; }

        public User(string name,
            string box,
            string password,
            string privateKey,
            string publicKey)
        {
            Name = name;
            Box = box;
            Password = password;
            PrivateKey = privateKey;
            PublicKey = publicKey;
        }

        public override bool Equals(object obj)
        {
            User userObj = obj as User;

            if (userObj == null)
                return false;

            return (Name == userObj.Name)
                && (Box == userObj.Box)
                && (Password == userObj.Password)
                && (PrivateKey == userObj.PrivateKey)
                && (PublicKey == userObj.PublicKey);
        }

        public override int GetHashCode()
        {
            unchecked // Overflow is fine, just wrap
            {
                int hash = 17;

                if (Name != null)
                    hash = hash * 23 + Name.GetHashCode();
                if (Box != null)
                    hash = hash * 23 + Box.GetHashCode();
                if (Password != null)
                    hash = hash * 23 + Password.GetHashCode();
                if (PrivateKey != null)
                    hash = hash * 23 + PrivateKey.GetHashCode();
                if (PublicKey != null)
                    hash = hash * 23 + PublicKey.GetHashCode();

                return hash;
            }
        }
    }
}