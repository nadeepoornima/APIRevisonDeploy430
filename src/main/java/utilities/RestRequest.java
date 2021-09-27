package utilities;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
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
import java.util.Base64;
import java.util.List;

public class RestRequest {

    static String KEY_STORE_PATH = "client-truststore.jks";
    static String KEY_STORE_PASSWORD = "wso2carbon";

    public static JSONObject getOauthClientDetails(
            String kmDcrUrl, String clientID, String userName, String password, Boolean isResident) {

            String URL = kmDcrUrl + "/" + clientID;
            JSONObject responseJson =  new JSONObject();
        HttpClientBuilder httpBuilder = getBuilder();

            try (CloseableHttpClient httpClient = httpBuilder.build()) {
                HttpGet httpget = new HttpGet(URL);
                String credentials = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
                httpget.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
                if (isResident) {
                    httpget.addHeader("X-WSO2-Tenant", "carbon.super");
                }
                try (CloseableHttpResponse response = httpClient.execute(httpget)) {
                    HttpEntity entity = response.getEntity();
                    String responseString = EntityUtils.toString(entity);
                    if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        JSONParser parser = new JSONParser();
                        responseJson = (JSONObject) parser.parse(responseString);
                    } else if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                        JSONParser parser = new JSONParser();
                        responseJson = (JSONObject) parser.parse(responseString);
                    } else {
                        System.out.println(" ISSUE : Error in getOauthClientDetails REST request ... " + URL);
                    }
                    EntityUtils.consume(entity);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        return responseJson;
    }

    public static ArrayList<JSONObject> getAPIList(String url, String accessToken) {

        ArrayList<JSONObject> apiDetailsList = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "?limit=700";

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                        JSONObject responseJson = (JSONObject) parser.parse(responseString);
//                JSONObject responseJson = new Gson().fromJson(responseString, (Type) new JSONObject());
                if (responseJson.get("list") instanceof ArrayList) {
                    apiDetailsList = (ArrayList<JSONObject>) responseJson.get("list");
                }
            } else {
                System.out.println(" ISSUE : Error in getAPIList REST request ... " + url);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return apiDetailsList;
    }

    public static JSONObject registerClient(String url, String username, String password) {

        JSONObject clientDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);

            String jsonPayload="{\"client_name\": \"migration_client_app\"," +
                    "\"grant_types\": [\"refresh_token\",\"password\",\"client_credentials\"]," +
                    "\"ext_application_owner\": \"admin\",\"ext_user_token_lifetime\": \"43200\"}";
            HttpEntity stringEntity = new StringEntity(jsonPayload, ContentType.APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                JSONParser parser = new JSONParser();
                clientDetails = (JSONObject) parser.parse(responseString);
            } else {
                System.out.println(" ISSUE : Error in registerClient REST request ... " + url);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return clientDetails;
    }

    public static JSONObject getToken(String url, String clientId, String clientSecret,
                                      String username, String password) {

        JSONObject tokenDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        String credentials = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
            List <NameValuePair> namevaluePairs = new ArrayList <NameValuePair>();
            namevaluePairs.add(new BasicNameValuePair("grant_type", "password"));
            namevaluePairs.add(new BasicNameValuePair("username", username));
            namevaluePairs.add(new BasicNameValuePair("password", password));
            namevaluePairs.add(new BasicNameValuePair("scope", "apim:api_view apim:api_import_export" +
                    " apim:api_product_import_export apim:api_create apim:api_publish"));

            httpPost.setEntity(new UrlEncodedFormEntity(namevaluePairs, HTTP.UTF_8));
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                tokenDetails = (JSONObject) parser.parse(responseString);
            } else {
                System.out.println(" ISSUE : Error in getToken REST request ... " + url);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return tokenDetails;
    }

    public static Boolean deleteClient(String url, String clientId,  String username, String password) {

        Boolean successStatus = false;
        HttpClientBuilder httpBuilder = getBuilder();
        String credentials = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        url = url + "/" + clientId;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpDelete httpDelete = new HttpDelete(url);
            httpDelete.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials);
            CloseableHttpResponse response = httpClient.execute(httpDelete);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                successStatus = true;
            } else {
                System.out.println(" ISSUE : Error in deleteClient REST request ... " + url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return successStatus;
    }

    public static JSONObject getApiDetails(String url, String accessToken, String apiId) {

        JSONObject apiDetails = null;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiId;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpGet httpget = new HttpGet(url);
            httpget.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpget);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (entity != null && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                JSONParser parser = new JSONParser();
                 apiDetails = (JSONObject) parser.parse(responseString);
            } else {
                System.out.println(" ISSUE : Error in getApiDetails REST request ... " + url +
                        "  response : " + responseString);
            }
            EntityUtils.consume(entity);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return apiDetails;
    }



    public static Boolean updateApi(String url, String accessToken, String apiId, String apiData) {

        Boolean successState = false;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/" + apiId;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPut httpPut = new HttpPut(url);
            httpPut.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            HttpEntity stringEntity = new StringEntity(apiData, ContentType.APPLICATION_JSON);
            httpPut.setEntity(stringEntity);
            CloseableHttpResponse response = httpClient.execute(httpPut);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                successState = true;
            } else {
                System.out.println(" ISSUE : Error in updateApi REST request ... response : " + responseString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return successState;
    }


    public static Boolean reDeployApi(String url, String accessToken, String apiId) {

        Boolean successState = false;
        HttpClientBuilder httpBuilder = getBuilder();
        url = url + "/change-lifecycle?action=Publish&apiId=" + apiId;

        try (CloseableHttpClient httpClient = httpBuilder.build()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                successState = true;
            } else {
                System.out.println(" ISSUE : Error in reDeployApi REST request ... response : " + responseString);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return successState;
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
