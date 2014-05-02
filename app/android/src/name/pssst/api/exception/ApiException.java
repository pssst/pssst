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
package name.pssst.api.exception;

/**
 * API exception.
 * @author Christian & Christian
 */
public final class ApiException extends Exception {
    /**
     * Initializes the base exception.
     * @param message message
     * @param exception exception
     */
    public ApiException(String message, Exception exception) {
        super(message, exception);
    }

    /**
     * Initializes the base exception.
     * @param message message
     */
    public ApiException(String message) {
        super(message);
    }
}
