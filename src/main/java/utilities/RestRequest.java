package utilities;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.*;
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
import java.security.cert.CertificateException;
import java.util.ArrayList;

public class RestRequest {

    static String KEY_STORE_PATH = "client-truststore.jks";
    static String KEY_STORE_PASSWORD = "wso2carbon";

    //completed
    public static ArrayList<JSONObject> getAPIList(String url, String accessToken) {

        ArrayList<JSONObject> apiDetailsList = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "?limit=700";

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                JSONObject responseJson = (JSONObject) parser.parse(responseString);
                if (responseJson.get("list") instanceof ArrayList) {
                    apiDetailsList = (ArrayList<JSONObject>) responseJson.get("list");
                }
            } else {
                System.out.println("***** ERROR : Error in getAPIList REST request ... " + url + " Response : " + responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return apiDetailsList;
    }

    //completed
    public static JSONObject getAPIDetailsByApiId(String url, String accessToken, String apiid) {

        JSONObject apiDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiid;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                apiDetails = (JSONObject) parser.parse(responseString);
            } else {
                System.out.println("***** ERROR : Error in getAPIDetails REST request ... " + url + " Response : " + responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return apiDetails;
    }

    //completed
    public static boolean updateApi(String url, String accessToken, String apiid, JSONObject apiDetailsByApiId) {

        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiid;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPut httpPut = new HttpPut(url);
            httpPut.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            httpPut.addHeader("Content-Type", "application/json");
            //set payload
            StringEntity entity = new StringEntity(apiDetailsByApiId.toJSONString(), "UTF-8");
            httpPut.setEntity(entity);
            try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
                // Handle the response
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                if (statusCode == 200) {
                    System.out.println("***** API updated successfully...");
                    return true;
                } else {
                    System.out.println("***** Failed to update API. HTTP Status: " + statusCode);
                    System.out.println("***** Response: " + responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // completed
    public static ArrayList<JSONObject> getRevisionListByApiId(String url, String accessToken, String apiid) {

        ArrayList<JSONObject> revisionDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiid + "/revisions";

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                JSONObject responseJson = (JSONObject) parser.parse(responseString);
                if (responseJson.get("list") instanceof ArrayList) {
                    revisionDetails = (ArrayList<JSONObject>) responseJson.get("list");
                }
            } else {
                System.out.println("***** ERROR : Error in getRevisionListByApiId REST request ... " + url + " Response : " + responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return revisionDetails;
    }

    //completed
    public static JSONObject createRevision(String url, String accessToken, String apiid) {

        JSONObject newRevisionDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiid + "/revisions";

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            //set payload
            String jsonPayload = "{\"description\":\"added visibility restriction\"}";
            HttpEntity stringEntity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED || response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                newRevisionDetails = (JSONObject) parser.parse(responseString);
            } else {
                System.out.println("***** Error : Error in createRevision REST request ... " + url + " Response : " + responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return newRevisionDetails;
    }

    //completed
    public static ArrayList<JSONObject> deployRevision(String url, String accessToken, String apiid, String revisionid, ArrayList<JSONObject> deployRevisionPayload) {

        ArrayList<JSONObject> newDeployedRevisionDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiid + "/deploy-revision?revisionId=" + revisionid;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            //set payload
            String jsonPayload = deployRevisionPayload.toString();
            HttpEntity stringEntity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && ( response.getStatusLine().getStatusCode() == HttpStatus.SC_OK || response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED )) {
                JSONParser parser = new JSONParser();
                newDeployedRevisionDetails = (ArrayList<JSONObject>) parser.parse(responseString);
            } else {
                System.out.println("***** Error : Error in deployRevision REST request ... " + url + " Response : " + responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return newDeployedRevisionDetails;
    }

    // completed
    public static ArrayList<JSONObject> deleteRevision(String url, String accessToken, String apiid, String revisionid) {

        ArrayList<JSONObject> remainingRevisionList = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiid + "/revisions/" + revisionid;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpDelete httpDelete = new HttpDelete(url);
            httpDelete.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpDelete);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                JSONObject responseJson = (JSONObject) parser.parse(responseString);
                System.out.println("***** Deleted revision with id : "+ revisionid + " of API with id : "+ apiid);
                if (responseJson.get("list") instanceof ArrayList) {
                    remainingRevisionList = (ArrayList<JSONObject>) responseJson.get("list");
                }
            } else {
                System.out.println("***** ERROR : Error in getAPIList REST request ... " + url + " Response : " + responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return remainingRevisionList;
    }

    protected static HttpClientBuilder getBuilder() {
        KeyStore keyStore;
        HttpClientBuilder httpBuilder = null;
        ReadConfigFile configs = new ReadConfigFile();
        KEY_STORE_PATH = configs.getProperty("TRUSTSTORE.PATH");
        KEY_STORE_PASSWORD = configs.getProperty("TRUSTSTORE.PASSWORD");
        try {
            keyStore = KeyStore.getInstance("jks");
            InputStream keyStoreInput = new FileInputStream(KEY_STORE_PATH);
            keyStore.load(keyStoreInput, KEY_STORE_PASSWORD.toCharArray());

            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, KEY_STORE_PASSWORD.toCharArray());

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(
                    keyManagerFactory.getKeyManagers(),
                    trustManagerFactory.getTrustManagers(),
                    new SecureRandom());

            httpBuilder = HttpClientBuilder.create();
            SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(
                    sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            httpBuilder.setSSLSocketFactory(sslConnectionFactory);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("https", sslConnectionFactory)
                    .register("http", new PlainConnectionSocketFactory())
                    .build();
            HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
            httpBuilder.setConnectionManager(ccm);
        } catch (KeyStoreException | IOException | CertificateException |
                 NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException e) {
            e.printStackTrace();
        }
        return httpBuilder;
    }

}
