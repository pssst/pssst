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

package name.pssst.api.entity;

import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import name.pssst.api.PssstException;

/**
 * Pssst message.
 */
public final class Message {
    protected final byte[] mData;
    protected final String mUser;
    protected final long mTime;

    /**
     * Constructs a new Message from the data, user name and time.
     * @param data Data bytes
     * @param user User name
     * @param time Timestamp
     */
    public Message(byte[] data, String user, long time) {
        mData = data;
        mUser = user;
        mTime = time;
    }

    /**
     * Returns the message data as text.
     * @return Message text
     * @throws UnsupportedEncodingException
     */
    public final String getText() throws UnsupportedEncodingException {
        return new String(mData, HTTP.UTF_8);
    }

    /**
     * Returns the full canonical user name.
     * @return User name
     * @throws PssstException
     */
    public final String getUsername() throws PssstException {
        return new Name(mUser).toString();
    }

    /**
     * Returns the timestamp the message was pushed.
     * @return Timestamp
     */
    public final Date getTimestamp() {
        return new Date(mTime * 1000);
    }
}
