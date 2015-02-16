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

import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import name.pssst.api.Pssst;
import name.pssst.api.PssstException;
import name.pssst.api.internal.entity.RsaData;

/**
 * Internal class for API calls.
 */
public final class RequestProvider {
    private static final String MIME_TYPE_TEXT = "text/plain";
    private static final String MIME_TYPE_JSON = "application/json";
    private static final String MIME_TYPE_DATA = "application/octet-stream";

    private static final String HEADER_CONTENT_TYPE = "content-type";
    private static final String HEADER_CONTENT_HASH = "content-hash";

    private static final String API_ENCODING = "UTF_8";
    private static final long API_GRACE_TIME = 30;
    private static final int API_VERSION = 1;

    private final KeyStorage mKeyStorage;

    /**
     * Constructs a new request provider.
     * @param keyStorage Key storage
     */
    public RequestProvider(KeyStorage keyStorage) {
        mKeyStorage = keyStorage;
    }

    /**
     * Returns the key storage.
     * @return Text
     */
    public final KeyStorage getKeyStorage() {
        return mKeyStorage;
    }

    /**
     * Checks the server time.
     * @return Success
     * @throws PssstException
     */
    public static boolean checkServerTime() throws PssstException {
        final long serverTime = Long.parseLong(requestUrl("time").getText());
        final long systemTime = (System.currentTimeMillis() / 1000);

        return (Math.abs(serverTime - systemTime) <= API_GRACE_TIME);
    }

    /**
     * Creates a simple unverified URL request and returns its body.
     * @param path URL Path
     * @return Response
     * @throws PssstException
     */
    public static Response requestUrl(String path) throws PssstException {
        final Response response = new Request(Request.Method.GET, getUrl(path)).execute();

        // Assert response is valid
        if (response.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new PssstException("Not Found");
        }

        return response;
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

        final URL url;
        final String content;
        final Request request;

        // Check for empty path
        if (path != null && !path.isEmpty()) {
            url = getApiUrl(user, path);
        } else {
            url = getApiUrl(user);
        }

        // Check for empty body
        if (data != null) {
            content = encodeJson(data);
        } else {
            content = "";
        }

        request = new Request(method, url, content);
        request.setHeader(HEADER_CONTENT_TYPE, getMimeType(data));
        request.setHeader(HEADER_CONTENT_HASH, buildContentHash(content));

        final Response response = request.execute();

        // Assert response could be verified
        if (!checkContentHash(response.getBytes(), response.getHeader(HEADER_CONTENT_HASH))) {
            throw new PssstException("Verification failed");
        }

        // Assert response is really empty
        if (!response.isEmpty() && response.getStatusCode() == HttpURLConnection.HTTP_NO_CONTENT) {
            throw new PssstException("Response not empty");
        }

        // Assert response is not an error
        if (!response.isEmpty() && response.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new PssstException(response.getText());
        }

        return response;
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
     * Returns the web server URL.
     * @param path User path
     * @return URL
     * @throws PssstException
     */
    private static URL getUrl(String path) throws PssstException {
        try {
            return new URL(String.format("%s/%s", Pssst.getServer(), path));
        } catch (MalformedURLException e) {
            throw new PssstException("URL invalid", e);
        }
    }

    /**
     * Returns the API call URL.
     * @param user User name
     * @param path User path
     * @return URL
     * @throws PssstException
     */
    private static URL getApiUrl(String user, String path) throws PssstException {
        return getUrl(String.format("%s/%s/%s", API_VERSION, user, path));
    }

    /**
     * Returns the API call URL.
     * @param user User name
     * @return URL
     * @throws PssstException
     */
    private static URL getApiUrl(String user) throws PssstException {
        return getUrl(String.format("%s/%s", API_VERSION, user));
    }

    /**
     * Returns the MIME type.
     * @param data Request data
     * @return MIME type
     */
    private static String getMimeType(Object data) {
        if (data instanceof String) {
            return MIME_TYPE_TEXT;
        } else if (data instanceof JSONObject) {
            return MIME_TYPE_JSON;
        } else {
            return MIME_TYPE_DATA;
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
            final RsaData rsaData = mKeyStorage.getUserKey().sign(data.getBytes(API_ENCODING));
            final String signature = new String(Base64.encode(rsaData.getSignature()), API_ENCODING);

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
        if ((Math.abs(rsaData.getTimestamp() - timestamp) > API_GRACE_TIME)) {
            return false;
        }

        // Check if response data is correct
        if (!api.verify(data, rsaData)) {
            return false;
        }

        return true;
    }
}
