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

package name.pssst.api.internal;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;

/**
 * Internal class for API requests.
 */
public final class Request {
    private static final String HEADER_USER_AGENT = "user-agent";
    private static final int SOCKET_TIMEOUT = 60 * 1000;

    private final HttpURLConnection mConnection;
    private final String mContent;

    public static enum Method {
        POST, GET, PUT, DELETE
    }

    /**
     * Constructs a new request.
     * @param method Request method
     * @param url Request URL
     * @param content Request content
     * @throws PssstException
     */
    public Request(Method method, URL url, String content) throws PssstException {
        mContent = content;

        try {
            mConnection = (HttpURLConnection) url.openConnection();
            mConnection.addRequestProperty(HEADER_USER_AGENT, String.format("Pssst %s Android", Pssst.getVersion()));
            mConnection.setConnectTimeout(SOCKET_TIMEOUT);
            mConnection.setReadTimeout(SOCKET_TIMEOUT);
            mConnection.setUseCaches(false);
            mConnection.setDoInput(true);

            switch (method) {
                case POST:
                    mConnection.setRequestMethod("POST");
                    break;

                case GET:
                    mConnection.setRequestMethod("GET");
                    break;

                case PUT:
                    mConnection.setRequestMethod("PUT");
                    break;

                case DELETE:
                    mConnection.setRequestMethod("DELETE");
                    break;

                default:
                    throw new PssstException("Method unknown");
            }
        } catch (IOException e) {
            throw new PssstException("Connection failed", e);
        }
    }

    /**
     * Constructs a new request.
     * @param method Request method
     * @param url Request URL
     * @throws PssstException
     */
    public Request(Method method, URL url) throws PssstException {
        this(method, url, null);
    }

    /**
     * Set request header.
     * @param field Header field
     * @param value Header value
     */
    public void setHeader(String field, String value) {
        mConnection.addRequestProperty(field, value);
    }

    /**
     * Executes the request and returns the response.
     * @return Response
     * @throws PssstException
     */
    public Response execute() throws PssstException {
        try {
            if (mContent != null && !mContent.isEmpty()) {
                final byte[] body = mContent.getBytes();

                mConnection.setFixedLengthStreamingMode(body.length);
                mConnection.setDoOutput(true);

                final BufferedOutputStream stream = new BufferedOutputStream(mConnection.getOutputStream());

                stream.write(body);
                stream.flush();
                stream.close();
            }

            mConnection.connect();

            return new Response(mConnection);
        } catch (IOException e) {
            throw new PssstException("Connection failed", e);
        }
    }
}
