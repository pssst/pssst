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
    /// <summary>
    /// Represents a pssst user.
    /// </summary>
    public sealed class User
    {
        /// <summary>
        /// Gets the name.
        /// </summary>
        public string Name { get; set; }

        public string Box { get; set; }

        public string Password { get; set; }

        /// <summary>
        /// Gets the private key.
        /// </summary>
        public string PrivateKey { get; set; }

        /// <summary>
        /// Gets the public key.
        /// </summary>
        public string PublicKey { get; set; }

        /// <summary>
        /// Initializes a new instance of the <see cref="User"/> class.
        /// </summary>
        public User()
        {
        }

        /// <summary>
        /// Initializes a new instance of the <see cref="User"/> class.
        /// </summary>
        /// <param name="name">The name.</param>
        /// <param name="box">The box.</param>
        /// <param name="password">The password.</param>
        /// <param name="privateKey">The private key.</param>
        /// <param name="publicKey">The public key.</param>
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

        /// <summary>
        /// Determines whether the specified <see cref="System.Object" />, 
        /// is equal to this instance.
        /// </summary>
        /// <param name="obj">The <see cref="System.Object" /> to compare with 
        /// this instance.</param>
        /// <returns>
        ///   <c>true</c> if the specified <see cref="System.Object" /> is 
        ///   equal to this instance; otherwise, <c>false</c>.
        /// </returns>
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

        /// <summary>
        /// Returns a hash code for this instance.
        /// </summary>
        /// <returns>
        /// A hash code for this instance, suitable for use in hashing 
        /// algorithms and data structures like a hash table.
        /// </returns>
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