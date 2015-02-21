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
    /// Defines the interface of a pssst client that provides encrypted communication
    /// with a pssst server.
    /// </summary>
    public interface IPssstClient
    {
        /// <summary>
        /// Gets the pssst server.
        /// </summary>
        Uri Host { get; }

        /// <summary>
        /// Sets the needed configuration for communicating with the server.
        /// </summary>
        /// <param name="host">The host.</param>
        void Configure(Uri host);

        /// <summary>
        /// Sets the needed configuration for communicating with the server.
        /// </summary>
        /// <param name="host">The host uri.</param>
        /// <param name="port">The port of the host.</param>
        void Configure(string host, int port);

        /// <summary>
        /// Creates a new user.
        /// </summary>
        /// <param name="userName">The name of the user.</param>
        /// <returns>The new user.</returns>
        User CreateUser(string userName);

        /// <summary>
        /// Gets the public key of an user from the server.
        /// </summary>
        /// <param name="userName">The Name of the user.</param>
        /// <returns>The retrieved user information.</returns>
        User GetUser(string userName);

        /// <summary>
        /// Sends a message to the specified receiver.
        /// </summary>
        /// <param name="sender">The sender of the message.</param>
        /// <param name="receiver">The receiver of the message.</param>
        /// <param name="message">The message.</param>
        /// <returns>
        /// A boolean indicating whethter the message was succesfully sent.
        /// </returns>
        bool SendMessage(User sender, User receiver, string message);

        /// <summary>
        /// Receives a message for the user.
        /// </summary>
        /// <param name="user">The user.</param>
        /// <returns>
        /// The received message or null if there was no message on the server.
        /// </returns>
        ReceivedMessageBody? ReceiveMessage(User user);
    }
}