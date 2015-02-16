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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

import name.pssst.api.PssstException;

/**
 * Internal class for API responses.
 */
public final class Response {
    private static final String API_ENCODING = "UTF_8";
    private static final int BUFFER_SIZE = 8192;

    private HttpURLConnection mConnection;
    private String mBody = "";

    /**
     * Constructs a new response.
     * @param connection HTTP/S connection
     * @throws PssstException
     */
    public Response(HttpURLConnection connection) throws PssstException {
        mConnection = connection;

        try {
            try {
                mBody = readStream(connection.getInputStream());
            } catch (IOException e) {
                mBody = readStream(connection.getErrorStream());
                throw new PssstException(mBody, e);
            } finally {
                connection.disconnect();
            }
        } catch (IOException e) {
            throw new PssstException("Connection failed", e);
        }
    }

    /**
     * Returns if the response is empty.
     * @return Empty
     * @throws PssstException
     */
    public final boolean isEmpty() throws PssstException {
        return mBody.isEmpty();
    }

    /**
     * Returns the header data.
     * @param field Header field
     * @return Header data
     */
    public final String getHeader(String field) {
        return mConnection.getHeaderField(field);
    }

    /**
     * Returns the response status code.
     * @return Status code
     * @throws PssstException
     */
    public final int getStatusCode() throws PssstException {
        try {
            return mConnection.getResponseCode();
        } catch (IOException e) {
            throw new PssstException("Connection failed", e);
        }
    }

    /**
     * Returns the response body.
     * @return Text
     * @throws PssstException
     */
    public final String getText() throws PssstException {
        return mBody;
    }

    /**
     * Returns the response body as bytes.
     * @return Bytes
     * @throws PssstException
     */
    public final byte[] getBytes() throws PssstException {
        try {
            return mBody.getBytes(API_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new PssstException("Encoding invalid", e);
        }
    }

    /**
     * Returns the response body as JSON.
     * @return JSON object
     * @throws PssstException
     */
    public final JSONObject getJson() throws PssstException {
        try {
            return new JSONObject(mBody);
        } catch (JSONException e) {
            throw new PssstException("JSON invalid", e);
        }
    }

    /**
     * Returns the stream data as a string.
     * @param inputStream Input stream
     * @return Stream data
     * @throws PssstException
     * @throws IOException
     */
    private String readStream(InputStream inputStream) throws PssstException, IOException {
        final BufferedInputStream stream;
        final BufferedReader reader;
        final StringBuilder buffer;

        try {
            stream = new BufferedInputStream(inputStream);
            reader = new BufferedReader(new InputStreamReader(stream, API_ENCODING));
            buffer = new StringBuilder();

            int count; char[] chunk = new char[BUFFER_SIZE];

            while ((count = reader.read(chunk)) != -1) {
                buffer.append(chunk, 0, count);
            }

            reader.close();
            stream.close();

            return buffer.toString();
        } catch (UnsupportedEncodingException e) {
            throw new PssstException("Encoding invalid", e);
        }
    }
}
