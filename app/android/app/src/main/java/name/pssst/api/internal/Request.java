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

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;

import java.io.UnsupportedEncodingException;
import java.net.URI;

import name.pssst.api.PssstException;

/**
 * Internal class for API requests.
 */
public class Request {
    private final HttpRequestBase mRequest;

    public static enum Method {
        POST, GET, PUT, DELETE
    }

    /**
     * Constructs a new request.
     * @param method Request method
     * @param uri Request URI
     * @param content Request content
     * @throws PssstException
     */
    public Request(Method method, URI uri, String content) throws PssstException {
        switch (method) {
            case POST:
                mRequest = new HttpPost(uri);
                break;

            case GET:
                mRequest = new HttpGet(uri);
                break;

            case PUT:
                mRequest = new HttpPut(uri);
                break;

            case DELETE:
                mRequest = new HttpDelete(uri);
                break;

            default:
                throw new PssstException("Method unknown");
        }

        // Sets the body for POST and PUT requests
        if (mRequest instanceof HttpEntityEnclosingRequest && content != null) {
            final HttpEntityEnclosingRequest request = (HttpEntityEnclosingRequest) mRequest;

            try {
                request.setEntity(new StringEntity(content, HTTP.UTF_8));
            } catch (UnsupportedEncodingException e) {
                throw new PssstException("Encoding invalid", e);
            }
        }
    }

    /**
     * Constructs a new request.
     * @param method Request method
     * @param uri Request URI
     * @throws PssstException
     */
    public Request(Method method, URI uri) throws PssstException {
        this(method, uri, null);
    }

    /**
     * Returns the internal request object.
     * @return Request object
     */
    public HttpRequestBase getRequestBase() {
        return mRequest;
    }

    /**
     * Set request header.
     * @param name Header name
     * @param value Header value
     */
    public void setHeader(String name, String value) {
        mRequest.setHeader(name, value);
    }
}
