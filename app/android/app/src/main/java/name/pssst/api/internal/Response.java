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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import name.pssst.api.PssstException;

/**
 * Internal class for API responses.
 */
public class Response {
    private final HttpResponse mResponse;
    private final HttpEntity mEntity;
    private String mContent = null;

    /**
     * Constructs a new response.
     * @param response Response
     */
    public Response(HttpResponse response) {
        mResponse = response;
        mEntity = response.getEntity();
    }

    /**
     * Returns the header data.
     * @param name Header name
     * @return Content hash
     */
    public final String getHeader(String name) {
        return mResponse.getFirstHeader(name).getValue();
    }

    /**
     * Returns the response status code.
     * @return Status code
     */
    public final int getStatusCode() {
        return mResponse.getStatusLine().getStatusCode();
    }

    /**
     * Returns if the response is empty.
     * @return Empty
     * @throws PssstException
     */
    public final boolean isEmpty() throws PssstException {
        return (mEntity == null || mEntity.getContentLength() == 0);
    }

    /**
     * Returns the response body.
     * @return Text
     * @throws PssstException
     */
    public final String getText() throws PssstException {
        return parserContent();
    }

    /**
     * Returns the response body as bytes.
     * @return Bytes
     * @throws PssstException
     */
    public final byte[] getBytes() throws PssstException {
        try {
            return parserContent().getBytes(HTTP.UTF_8);
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
            return new JSONObject(parserContent());
        } catch (JSONException e) {
            throw new PssstException("JSON invalid", e);
        }
    }

    /**
     * Returns the response buffered content.
     * @return Content
     * @throws PssstException
     */
    private String parserContent() throws PssstException {
        try {
            if (mContent == null && mEntity != null) {
                return mContent = EntityUtils.toString(mEntity, HTTP.UTF_8);
            } else if (mContent == null) {
                return mContent = "";
            } else {
                return mContent;
            }
        } catch (IOException e) {
            throw new PssstException("Encoding invalid", e);
        }
    }
}
