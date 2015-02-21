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
    /// Represents a keypair containing a public and a private key.
    /// </summary>
    public struct Keypair
    {
        /// <summary>
        /// Gets or sets the public key.
        /// </summary>
        public string PublicKey { get; set; }

        /// <summary>
        /// Gets or sets the private key.
        /// </summary>
        public string PrivateKey { get; set; }
    }
}