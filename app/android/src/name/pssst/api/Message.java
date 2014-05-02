// Copyright (C) 2013-2014  Christian & Christian  <hello@pssst.name>
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.
package name.pssst.api;

import name.pssst.api.exception.ApiException;

import java.util.Date;

/**
 * Pssst message value class.
 * @author Christian & Christian
 */
public final class Message {
    private Name name;
    private String text;
    private Date time;

    /**
     * Initializes the message from a pull command.
     * @param user sender
     * @param text text
     * @param time arrival
     * @throws ApiException
     */
    public Message(String user, String text, long time) throws ApiException {
        this.name = new Name(user);
        this.text = text;
        this.time = new Date(time * 1000);
    }

    /**
     * Initializes the message for a push command.
     * @param user receiver
     * @param text text
     * @throws ApiException
     */
    public Message(String user, String text) throws ApiException {
        this.name = new Name(user);
        this.text = text;
        this.time = null;
    }

    /**
     * Returns the senders name.
     * @return name
     */
    public final Name getName() {
        return name;
    }

    /**
     * Returns the message text.
     * @return text
     */
    public final String getText() {
        return text;
    }

    /**
     * Returns the arrival time.
     * @return time
     */
    public final Date getTime() {
        return time;
    }
}
