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
    /// Describes the interface of the communication module that provides methods
    /// to communicate with the pssst server.
    /// </summary>
    public interface ICommunication
    {
        /// <summary>
        /// Creates a new user on the server.
        /// </summary>
        /// <param name="server">
        /// The server on which the user should be crated.
        /// </param>
        /// <param name="user">The user that should be created.</param>
        void CreateUser(Uri server, User user);

        /// <summary>
        /// Sends a message from one user (sender) to another (receiver).
        /// </summary>
        /// <param name="server">The server which should be used.</param>
        /// <param name="sender">The sender of the message.</param>
        /// <param name="receiver">The receiver of the message.</param>
        /// <param name="message">The encrypted message.</param>
        void SendMessage(Uri server, User sender, User receiver, MessageBody message);

        /// <summary>
        /// Receives a message for the user.
        /// </summary>
        /// <param name="server">The server which should be used.</param>
        /// <param name="user">The user.</param>
        /// <returns>
        /// The encrypted message or null if the server has no message for the
        /// user.
        /// </returns>
        ReceivedMessageBody? ReceiveMessage(Uri server, User user);

        /// <summary>
        /// Gets the public key of a user.
        /// </summary>
        /// <param name="server">The server which should be used.</param>
        /// <param name="user">
        /// The user whose public key should be retrieved.
        /// </param>
        /// <returns>
        /// The public key of the user.
        /// </returns>
        string GetPublicKey(Uri server, string user);
    }
}