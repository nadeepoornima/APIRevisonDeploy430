package com.sample.utilities;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestRequest {

    private static final Logger LOGGER = Logger.getLogger(RestRequest.class.getName());
    private static String KEY_STORE_PATH = "client-truststore.jks";
    private static String KEY_STORE_PASSWORD = "wso2carbon";

    public static ArrayList<JSONObject> getAPIList(String url, String accessToken, int maxApiLimit) {
        ArrayList<JSONObject> apiDetailsList = new ArrayList<>();
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "?limit=" + maxApiLimit;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                handleResponse(response, apiDetailsList);
            }

        } catch (IOException | ParseException e) {
            LOGGER.log(Level.SEVERE, "Error during getAPIList for URL: " + url, e);
        }

        return apiDetailsList;
    }

    private static void handleResponse(CloseableHttpResponse response, ArrayList<JSONObject> apiDetailsList) throws IOException, ParseException {
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        try {
            // deploy revision gives 201
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED ) {
                JSONParser parser = new JSONParser();
                Object parsedResponse = parser.parse(responseString);
                // this if handles getAPIList , getRevisionListByApiId , deleteRevision
                if (parsedResponse instanceof JSONObject) {
                    JSONObject responseJson = (JSONObject) parser.parse(responseString);
                    if (responseJson.get("list") instanceof ArrayList) {
                        apiDetailsList.addAll((ArrayList<JSONObject>) responseJson.get("list"));
                    }
                // this handles deployRevision
                } else if (parsedResponse instanceof ArrayList) {
                    apiDetailsList.addAll((ArrayList<JSONObject>)parsedResponse);
                } else {
                    LOGGER.log(Level.WARNING, "Unexpected JSON response type.");
                }
            } else {
                LOGGER.log(Level.WARNING, "Unexpected response for getAPIList: " +
                        response.getStatusLine() + ", Response: " + responseString);
            }
        } finally {
            EntityUtils.consume(entity); // Ensure the entity is fully consumed
        }
    }

    public static JSONObject getAPIDetailsByApiId(String url, String accessToken, String apiId) {
        JSONObject apiDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiId;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                apiDetails = handleSingleResponse(response);
            }

        } catch (IOException | ParseException e) {
            LOGGER.log(Level.SEVERE, "Error during getAPIDetailsByApiId for API ID: " + apiId, e);
        }

        return apiDetails;
    }

    private static JSONObject handleSingleResponse(CloseableHttpResponse response) throws IOException, ParseException {
        HttpEntity entity = response.getEntity();
        String responseString = EntityUtils.toString(entity);
        JSONObject result = null;

        try {
            // createRevision gives 201
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                JSONParser parser = new JSONParser();
                result = (JSONObject) parser.parse(responseString);
            } else {
                LOGGER.log(Level.WARNING, "Unexpected response for API details: " +
                        response.getStatusLine() + ", Response: " + responseString);
            }
        } finally {
            EntityUtils.consume(entity);
        }

        return result;
    }

    public static boolean updateApi(String url, String accessToken, String apiId, JSONObject apiDetailsByApiId) {
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiId;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPut httpPut = new HttpPut(url);
            httpPut.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            httpPut.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            httpPut.setEntity(new StringEntity(apiDetailsByApiId.toJSONString(), "UTF-8"));

            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                return handleUpdateResponse(response);
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error updating API ID: " + apiId, e);
        }

        return false;
    }

    private static boolean handleUpdateResponse(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        String responseBody = EntityUtils.toString(entity, "UTF-8");

        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                LOGGER.log(Level.INFO, "API updated successfully.");
                return true;
            } else {
                LOGGER.log(Level.WARNING, "Failed to update API. Status: " + statusCode + ", Response: " + responseBody);
                return false;
            }
        } finally {
            EntityUtils.consume(entity);
        }
    }

    // Similar updates to other methods (getRevisionListByApiId, createRevision, deployRevision, deleteRevision)
    public static ArrayList<JSONObject> getRevisionListByApiId(String url, String accessToken, String apiId) {
        ArrayList<JSONObject> revisionDetails = new ArrayList<>();
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiId + "/revisions";

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                handleResponse(response, revisionDetails);
            }

        } catch (IOException | ParseException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving revisions for API ID: " + apiId, e);
        }

        return revisionDetails;
    }

    public static JSONObject createRevision(String url, String accessToken, String apiId) {
        JSONObject newRevisionDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiId + "/revisions";

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            httpPost.setEntity(new StringEntity("{\"description\":\"added visibility restriction\"}", ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                newRevisionDetails = handleSingleResponse(response);
            }

        } catch (IOException | ParseException e) {
            LOGGER.log(Level.SEVERE, "Error creating revision for API ID: " + apiId, e);
        }

        return newRevisionDetails;
    }

    public static ArrayList<JSONObject> deployRevision(String url, String accessToken, String apiId, String revisionId, ArrayList<JSONObject> deployRevisionPayload) {
        ArrayList<JSONObject> deployedRevisionDetails = new ArrayList<>();
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiId + "/deploy-revision?revisionId=" + revisionId;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            httpPost.setEntity(new StringEntity(deployRevisionPayload.toString(), ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                handleResponse(response, deployedRevisionDetails);
            }

        } catch (IOException | ParseException e) {
            LOGGER.log(Level.SEVERE, "Error deploying revision for API ID: " + apiId + ", Revision ID: " + revisionId, e);
        }

        return deployedRevisionDetails;
    }

    public static ArrayList<JSONObject> deleteRevision(String url, String accessToken, String apiId, String revisionId) {
        ArrayList<JSONObject> remainingRevisions = new ArrayList<>();
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiId + "/revisions/" + revisionId;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpDelete httpDelete = new HttpDelete(url);
            httpDelete.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);

            try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
                handleResponse(response, remainingRevisions);
                LOGGER.log(Level.INFO, "Successfully deleted revision ID: " + revisionId + " for API ID: " + apiId);
            }

        } catch (IOException | ParseException e) {
            LOGGER.log(Level.SEVERE, "Error deleting revision ID: " + revisionId + " for API ID: " + apiId, e);
        }

        return remainingRevisions;
    }


    protected static HttpClientBuilder getBuilder() {
        KeyStore keyStore;
        HttpClientBuilder httpBuilder = null;
        ReadConfigFile configs = new ReadConfigFile();
        KEY_STORE_PATH = configs.getProperty("TRUSTSTORE.PATH");
        KEY_STORE_PASSWORD = configs.getProperty("TRUSTSTORE.PASSWORD");

        try {
            keyStore = KeyStore.getInstance("jks");
            try (InputStream keyStoreInput = new FileInputStream(KEY_STORE_PATH)) {
                keyStore.load(keyStoreInput, KEY_STORE_PASSWORD.toCharArray());
            }

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());

            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(
                    sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            httpBuilder = HttpClientBuilder.create();
            httpBuilder.setSSLSocketFactory(sslConnectionFactory);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", sslConnectionFactory)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();

            HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
            httpBuilder.setConnectionManager(ccm);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error configuring HttpClientBuilder", e);
        }

        return httpBuilder;
    }
}

