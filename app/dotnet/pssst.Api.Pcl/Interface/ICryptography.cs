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

namespace pssst.Api.Pcl.Interface
{
    /// <summary>
    /// Describes the interface of a module providing cryptographic functionality
    /// for the pssst messaging.
    /// </summary>
    public interface ICryptography
    {
        /// <summary>
        /// Creates a new keypair with the given length.
        /// </summary>
        /// <param name="keyLength">Length of the key.</param>
        /// <returns>The new keypair.</returns>
        Keypair CreateKeyPair(int keyLength);

        /// <summary>
        /// Encrypts the message with the receivers public key.
        /// </summary>
        /// <param name="receiverKey">The receivers public key.</param>
        /// <param name="message">The message to encrypt.</param>
        /// <returns>The encrypted message.</returns>
        MessageBody EncryptMessage(string receiverKey, string message);

        /// <summary>
        /// Decrypts the message using the private key.
        /// </summary>
        /// <param name="keyPair">The key pair.</param>
        /// <param name="message">The encrypted message.</param>
        /// <returns>The decrypted message</returns>
        string DecryptMessage(Keypair keyPair, ReceivedMessageBody message);

        /// <summary>
        /// Creates the signature for the data using the given keypair.
        /// </summary>
        /// <param name="data">
        /// The data for which the signature should be created.
        /// </param>
        /// <param name="keypair">The keypair.</param>
        /// <param name="timestamp">The timestamp.</param>
        /// <returns>The signature.</returns>
        byte[] SignData(string data, Keypair keypair, long timestamp);
    }
}