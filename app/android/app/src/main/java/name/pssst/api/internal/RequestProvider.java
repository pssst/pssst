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

import android.net.http.AndroidHttpClient;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.api.internal.entity.RsaData;

/**
 * Internal class for API calls.
 */
public final class RequestProvider {
    private static final String USER_AGENT = String.format("Pssst %s Android", Pssst.getVersion());
    private static final long GRACE_TIME = 30;
    private static final int VERSION = 1;

    private final KeyStorage mKeyStorage;

    /**
     * Constructs a new request provider.
     * @param keyStorage Key storage
     */
    public RequestProvider(KeyStorage keyStorage) {
        mKeyStorage = keyStorage;
    }

    /**
     * Returns the grace time.
     * @return Grace time
     */
    public static long getGraceTime() {
        return GRACE_TIME;
    }

    /**
     * Returns the key storage.
     * @return Text
     */
    public final KeyStorage getKeyStorage() {
        return mKeyStorage;
    }

    /**
     * Creates a simple unverified URL request and returns its body.
     * @param path URL Path
     * @return Response
     * @throws PssstException
     */
    public static Response requestUrl(String path) throws PssstException {
        final AndroidHttpClient client = AndroidHttpClient.newInstance(USER_AGENT);

        try {
            final HttpRequestBase request = new HttpGet(getUri(path));
            final Response response = new Response(client.execute(request));

            if (response.getStatusCode() != 200) {
                throw new PssstException("Not Found");
            }

            return response;
        } catch (IOException e) {
            throw new PssstException("Request failed", e);
        } finally {
            client.close();
        }
    }

    /**
     * Returns the verified API response.
     * @param method Request method
     * @param user User name
     * @param path User path
     * @param data Request data
     * @return Response
     * @throws PssstException
     */
    public final Response requestApi(Request.Method method, String user, String path, JSONObject data) throws PssstException {
        try {
            mKeyStorage.assertStorage();
        } catch (PssstException e) {
            throw new PssstException("User was deleted", e);
        }

        final AndroidHttpClient client = AndroidHttpClient.newInstance(USER_AGENT);

        try {
            final URI uri;
            final Request request;
            final String content;

            if (path != null && !path.isEmpty()) {
                uri = getApiUri(user, path);
            } else {
                uri = getApiUri(user);
            }

            if (data != null) {
                content = encodeJson(data);
            } else {
                content = "";
            }

            request = new Request(method, uri, content);
            request.setHeader("content-type", getMimeType(data));
            request.setHeader("content-hash", buildContentHash(content));

            final Response response = new Response(client.execute(request.getRequestBase()));

            if (!checkContentHash(response.getBytes(), response.getHeader("content-hash"))) {
                throw new PssstException("Verification failed");
            }

            if (response.getStatusCode() != 200 && response.getStatusCode() != 204) {
                throw new PssstException(response.getText());
            }

            return response;
        } catch (IOException e) {
            throw new PssstException("API connection failed", e);
        } finally {
            client.close();
        }
    }

    /**
     * Returns the API response.
     * @param method Request method
     * @param user User name
     * @param path User path
     * @return Response
     * @throws PssstException
     */
    public final Response requestApi(Request.Method method, String user, String path) throws PssstException {
        return requestApi(method, user, path, null);
    }

    /**
     * Returns the API response.
     * @param method Request method
     * @param user User name
     * @return Response
     * @throws PssstException
     */
    public final Response requestApi(Request.Method method, String user) throws PssstException {
        return requestApi(method, user, null, null);
    }

    /**
     * Returns the web server URI.
     * @param path User path
     * @return URI
     * @throws PssstException
     */
    private static URI getUri(String path) throws PssstException {
        try {
            return new URI(String.format("%s/%s", Pssst.getServer(), path));
        } catch (URISyntaxException e) {
            throw new PssstException("URI invalid", e);
        }
    }

    /**
     * Returns the API call URI.
     * @param user User name
     * @param path User path
     * @return URI
     * @throws PssstException
     */
    private static URI getApiUri(String user, String path) throws PssstException {
        return getUri(String.format("%s/%s/%s", VERSION, user, path));
    }

    /**
     * Returns the API call URI.
     * @param user User name
     * @return URI
     * @throws PssstException
     */
    private static URI getApiUri(String user) throws PssstException {
        return getUri(String.format("%s/%s", VERSION, user));
    }

    /**
     * Returns the MIME type.
     * @param data Request data
     * @return MIME type
     */
    private static String getMimeType(Object data) {
        if (data instanceof String) {
            return HTTP.PLAIN_TEXT_TYPE;
        } else if (data instanceof JSONObject) {
            return "application/json";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * Encodes the JSON data as string.
     * @param json JSON data
     * @return Encoded data
     */
    private static String encodeJson(JSONObject json) {
        return json.toString().replace("\\/", "/"); // Fix wrong encoding
    }

    /**
     * Returns the content hash header.
     * @param data Request data
     * @return Content hash
     * @throws PssstException
     */
    private String buildContentHash(String data) throws PssstException {
        try {
            final RsaData rsaData = mKeyStorage.getUserKey().sign(data.getBytes(HTTP.UTF_8));
            final String signature = new String(Base64.encode(rsaData.getSignature()), HTTP.UTF_8);

            return String.format("%s; %s", rsaData.getTimestamp(), signature);
        } catch (UnsupportedEncodingException e) {
            throw new PssstException("Encoding invalid" , e);
        }
    }

    /**
     * Returns if content hash could be verified.
     * @param data Content data
     * @param hash Content hash
     * @return Verification
     * @throws PssstException
     */
    private boolean checkContentHash(byte[] data, String hash) throws PssstException {
        if (hash == null || !hash.matches("^[0-9]+; ?[A-Za-z0-9\\+/]+=*$")) {
            return false;
        }

        final Key api = mKeyStorage.loadApiKey();
        final String[] token = hash.split(";", 2);
        final RsaData rsaData = new RsaData(Base64.decode(token[1]), Long.parseLong(token[0]));
        final long timestamp = (System.currentTimeMillis() / 1000);

        // Check if response time is with grace time
        if ((Math.abs(rsaData.getTimestamp() - timestamp) > GRACE_TIME)) {
            return false;
        }

        // Check if response data is correct
        if (!api.verify(data, rsaData)) {
            return false;
        }

        return true;
    }
}
