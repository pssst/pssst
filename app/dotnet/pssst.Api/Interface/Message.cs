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
    /* 
     * !!! Attention: The property names of the structs in this class must not
     * be changed. They are used for serializing the data that is sent to the
     * server. Only if the protocol changes, the property names should be changed.
     */

    /// <summary>
    /// Represents the body of a message.
    /// </summary>
    public struct MessageBody
    {
        /// <summary>
        /// Gets or sets the head.
        /// </summary>
        public MessageHead head { get; set; }

        /// <summary>
        /// Gets or sets the body.
        /// </summary>
        public string body { get; set; }
    }

    /// <summary>
    /// Represents the head of a message.
    /// </summary>
    public struct MessageHead
    {
        /// <summary>
        /// Gets or sets the name of the sender.
        /// </summary>
        public string user { get; set; }

        /// <summary>
        /// Gets or sets the nonce.
        /// </summary>
        public string nonce { get; set; }
    }

    /// <summary>
    /// Represents the body of a received message.
    /// </summary>
    public struct ReceivedMessageBody
    {
        /// <summary>
        /// Gets or sets the head.
        /// </summary>
        public ReceivedMessageHead head { get; set; }

        /// <summary>
        /// Gets or sets the body.
        /// </summary>
        public string body { get; set; }
    }

    /// <summary>
    /// Represents the head of a received message.
    /// </summary>
    public struct ReceivedMessageHead
    {
        /// <summary>
        /// Gets or sets the name of the sender.
        /// </summary>
        public string user { get; set; }

        /// <summary>
        /// Gets or sets the nonce.
        /// </summary>
        public string nonce { get; set; }

        /// <summary>
        /// Gets or sets the time when this message was sent.
        /// </summary>
        public long time { get; set; }
    }
}