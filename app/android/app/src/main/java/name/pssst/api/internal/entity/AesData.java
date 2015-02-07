/*
 * Copyright (C) 2013-2015  Christian & Christian  <hello@pssst.name>
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
 */

package name.pssst.api.internal.entity;

/**
 * Internal AES data storage class.
 */
public final class AesData {
    protected final byte[] mData;
    protected final byte[] mNonce;

    /**
     * Constructs a new Data storage from the data and nonce.
     * @param data Data
     * @param nonce Nonce
     */
    public AesData(byte[] data, byte[] nonce) {
        mData = data;
        mNonce = nonce;
    }

    /**
     * Returns the data.
     * @return Data
     */
    public final byte[] getData() {
        return mData;
    }

    /**
     * Returns the nonce.
     * @return Nonce
     */
    public final byte[] getNonce() {
        return mNonce;
    }
}
