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
 * Internal RSA data storage class.
 */
public final class RsaData {
    protected final byte[] mSignature;
    protected final long mTimestamp;

    /**
     * Constructs a new RsaData storage from the signature and timestamp.
     * @param signature Signature
     * @param timestamp Timestamp
     */
    public RsaData(byte[] signature, long timestamp) {
        mSignature = signature;
        mTimestamp = timestamp;
    }

    /**
     * Returns the signature.
     * @return Signature
     */
    public final byte[] getSignature() {
        return mSignature;
    }

    /**
     * Returns the timestamp.
     * @return Timestamp
     */
    public final long getTimestamp() {
        return mTimestamp;
    }
}
