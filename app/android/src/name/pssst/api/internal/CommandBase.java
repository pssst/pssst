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
package name.pssst.api.internal;

import android.net.http.AndroidHttpClient;
import android.util.Pair;

import name.pssst.api.Pssst;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Base64;

import java.net.URI;

/**
 * Abstract base class for commands.
 * @author Christian & Christian
 */
public abstract class CommandBase implements Command {
    protected static final String USERAGENT = "Pssst " + Pssst.VERSION;
    protected static final Integer PROTOCOL = 1;
    protected KeyStore store;

    /**
     * Initializes the instance with an store object.
     * @param store key store
     */
    protected CommandBase(KeyStore store) {
        this.store = store;
    }

    /**
     * Should return the commands result.
     * @param param parameter
     * @return Result
     * @throws Exception
     */
    public abstract Object execute(Object param) throws Exception;

    /**
     * Returns the result of an API request (signed and verified).
     * @param request request method
     * @param path request path
     * @param data request data
     * @return response body
     * @throws Exception
     */
    protected final String requestApi(HttpRequestBase request, String path, JSONObject data) throws Exception {
        AndroidHttpClient client = AndroidHttpClient.newInstance(USERAGENT);

        try {
            assertState();

            boolean hasBody = (data != null);

            String body = parseJSON(data);
            String hash = buildContentHash(body);

            request.setURI(new URI(getUserPath(path)));
            request.setHeader("content-type", hasBody ? "application/json" : "text/plain");
            request.setHeader("content-hash", hash);

            if (hasBody) {
                setBody(request, body);
            }

            HttpResponse response = client.execute(request);

            body = getBody(response, "US-ASCII");
            hash = response.getFirstHeader("content-hash").getValue();

            Pair<Long, byte[]> h = parseContentHash(hash);

            Key api = Key.fromPublicKey(store.loadKey(Pssst.getApiAddress()));

            if (!api.verify(body.getBytes("US-ASCII"), h.first, h.second)) {
                throw new Exception("Verification failed");
            }

            int code = getCode(response);

            if (!(code == 200 || code == 204)) {
                throw new Exception(body);
            }

            return body;
        } finally {
            client.close();
        }
    }

    /**
     * Returns the result of an URL request (without any checks).
     * @param path request path
     * @return response body
     * @throws Exception
     */
    protected final String requestUrl(String path) throws Exception {
        AndroidHttpClient client = AndroidHttpClient.newInstance(USERAGENT);

        try {
            HttpResponse response = client.execute(new HttpGet(getFilePath(path)));

            if (getCode(response) != 200) {
                throw new Exception("Not Found");
            }

            return getBody(response, "UTF-8");
        } finally {
            client.close();
        }
    }

    /**
     * Asserts a valid state.
     * @throws Exception
     */
    private void assertState() throws Exception {
        if (!store.getDirectory().exists()) {
            throw new Exception("User was deleted");
        }
    }

    /**
     * Returns the JSON object as string.
     * @param json JSON object
     * @return string
     */
    private String parseJSON(JSONObject json) {
        String data = "";

        if (json != null) {
            data = json.toString();
            data = data.replace("\\/", "/"); // Remove invalid masking
        }

        return data;
    }

    /**
     * Returns the parsed content hash header.
     * @param header header
     * @return timestamp and signature
     * @throws Exception
     */
    private Pair<Long, byte[]> parseContentHash(String header) throws Exception {
        if (header == null || !header.matches("^[0-9]+; ?[A-Za-z0-9\\+/]+=*$")) {
            throw new Exception("Verification failed");
        }

        String[] t = header.split(";", 2);

        Long timestamp = Long.parseLong(t[0]);
        byte[] signature = Base64.decode(t[1]);

        return new Pair<Long, byte[]>(timestamp, signature);
    }

    /**
     * Returns a new valid content hash header.
     * @param body body
     * @return content hash
     * @throws Exception
     */
    private String buildContentHash(String body) throws Exception {
        Pair<Long, byte[]> hash = store.getKey().sign(body.getBytes("US-ASCII"));

        Long timestamp = hash.first;
        byte[] signature = Base64.encode(hash.second);

        return String.format("%s; %s", timestamp, new String(signature, "US-ASCII"));
    }

    /**
     * Returns the valid server path for an user.
     * @param user user
     * @return path
     */
    private String getUserPath(String user) {
        return String.format("%s/%s/%s", Pssst.getApiAddress(), PROTOCOL, user);
    }

    /**
     * Returns the valid server path for a file.
     * @param file file
     * @return path
     */
    private String getFilePath(String file) {
        return String.format("%s/%s", Pssst.getApiAddress(), file);
    }

    /**
     * Sets the request body.
     * @param request request
     * @param body body
     * @throws Exception
     */
    private void setBody(HttpRequest request, String body) throws Exception {
        ((HttpEntityEnclosingRequest)request).setEntity(new StringEntity(body, "US-ASCII"));
    }

    /**
     * Returns the responses body.
     * @param response response
     * @param encoding encoding
     * @return body
     * @throws Exception
     */
    private String getBody(HttpResponse response, String encoding) throws Exception {
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            return EntityUtils.toString(response.getEntity(), encoding);
        }

        return "";
    }

    /**
     * Returns the responses code.
     * @param response response
     * @return code
     */
    private int getCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }
}
