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

package name.pssst.api;

/**
 * Custom Pssst API exception.
 */
public class PssstException extends Exception {

    /**
     * Constructs a new empty Exception.
     */
    public PssstException() {
        super();
    }

    /**
     * Constructs a new Exception with a message.
     * @param message Exception message
     */
    public PssstException(String message) {
        super(message);
    }

    /**
     * Constructs a new Exception from a throwable source.
     * @param throwable Exception source
     */
    public PssstException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Constructs a new Exception with a message from a throwable source.
     * @param message Exception message
     * @param throwable Exception source
     */
    public PssstException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
