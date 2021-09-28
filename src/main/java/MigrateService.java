import model.AppKeyMapping;
import model.AppRegister;
import model.KeyMapInit;
import model.OauthAppDetails;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.DBConnection;
import utilities.RestRequest;
import utilities.ReadConfigFile;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static utilities.RestRequest.*;

public class MigrateService {

    static String pingKmDcrUrl = "https://localhost:9031/pf-ws/rest/oauth/clients";
    static String pingKmUsername = "Administraton";
    static String pingKmPassword = "Admin@123";
    static String residentDcrUrl = "https://localhost:9444/keymanager-operations/dcr/register";
    static String residentTokenUrl = "https://localhost:9444/oauth2/token";
    static String residentusername = "admin";
    static String residentpassword = "admin";
    static String publisherRestUrl = "https://localhost:9444/api/am/publisher/v1/apis";
    static String enableAppMigration = "true";
    static String enableApiMigration = "true";
    static String enableApiRedeploy = "true";
    static String gatewayEnvironmentList = "Production and Sandbox,Production and Sandbox 2";

    public static void main(String arg[]) {

        ReadConfigFile configs = new ReadConfigFile();
        pingKmDcrUrl = configs.getProperty("PINGKM.DCR.URL");
        pingKmUsername = configs.getProperty("PINGKM.USERNAME");
        pingKmPassword = configs.getProperty("PINGKM.PASSWORD");
        residentDcrUrl = configs.getProperty("RESIDENTKM.DCR.URL");
        residentusername = configs.getProperty("RESIDENTKM.USERNAME");
        residentpassword = configs.getProperty("RESIDENTKM.PASSWORD");
        residentTokenUrl = configs.getProperty("RESIDENTKM.TOKEN.URL");
        publisherRestUrl = configs.getProperty("PUBLISHER.REST.URL");
        enableAppMigration = configs.getProperty("RUN.APPLICATION.MIGRATION");
        enableApiMigration = configs.getProperty("RUN.API.MIGRATION");
        enableApiRedeploy = configs.getProperty("RUN.API.REDEPLOY");
        gatewayEnvironmentList = configs.getProperty("API.GATEWAY.ENVIRONMENT.LIST");

        if (StringUtils.equalsIgnoreCase("true", enableAppMigration)) {
            System.out.println(" ............ Starting Application data migration ............ ");
//        initializeComplete(true);
            List<KeyMapInit> keyMapInitList;
            keyMapInitList = getKeyMapInitList();
            System.out.println(" ............ Migrating " + keyMapInitList.size() + " Application entries ............ ");

            int appId;
            String appType;
            String pingKMUUID = getKmUUID("PingFederate");
            String residentKMUUID = getKmUUID("default");
            String keyType;
            AppRegister appRegister;
            AppKeyMapping appKeyMapping;
            String clientID;
            OauthAppDetails kmOauthAppDetails;

            for (int i = 0; i < keyMapInitList.size(); i++) {

                appId = keyMapInitList.get(i).getId();
                keyType = keyMapInitList.get(i).getkeyType();
                clientID = keyMapInitList.get(i).getclientID();
                appRegister = getAppRegisterData(appId,keyType);
                appKeyMapping = getAppKeyMappingData(appId,keyType);
                appType = getAppType(appId);
                if ((StringUtils.equalsIgnoreCase("secured", appType) || StringUtils.equalsIgnoreCase("public", appType))
                        && clientID != null && StringUtils.isNotEmpty(clientID)){

                    String initialInput = appRegister.getinputs();
                    JSONParser parser = new JSONParser();
                    JSONObject updatedInputJson = null;
                    try {
                        updatedInputJson = (JSONObject) parser.parse(initialInput);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    //Resident KM flow
                    JSONObject updatedResidentKmInputJson = updatedInputJson;
                    kmOauthAppDetails = getOauthAppDetails(clientID);
                    String grantResidentString = "";
                    Boolean ExistOauthAPP = false;
                    JSONObject adsResidentPropJson = new JSONObject();
                    if (kmOauthAppDetails != null) {
                        ExistOauthAPP = true;
                        if (!updatedResidentKmInputJson.isEmpty()) {
                            if (kmOauthAppDetails.getgrantTypes() != null && !kmOauthAppDetails.getgrantTypes().isEmpty()) {
                                grantResidentString = kmOauthAppDetails.getgrantTypes().replace(" ", ",");
                            } else if (kmOauthAppDetails.getgrantTypes() == null) {
                                grantResidentString = null;
                            }
                        }
                        updatedResidentKmInputJson.put("grant_types",grantResidentString);
                        updatedResidentKmInputJson.put("callback_url",kmOauthAppDetails.getcallbackUrls());
                        adsResidentPropJson.put("application_access_token_expiry_time", kmOauthAppDetails.getappTokenExp());
                        adsResidentPropJson.put("user_access_token_expiry_time", kmOauthAppDetails.getuserTokenExp());
                        adsResidentPropJson.put("refresh_token_expiry_time", kmOauthAppDetails.getrefreshTokenExp());
                        adsResidentPropJson.put("id_token_expiry_time", kmOauthAppDetails.getidTokenExp());
                        updatedResidentKmInputJson.put("additionalProperties", adsResidentPropJson.toJSONString());

//                        AppRegister residentKmAppRegister = appRegister;
//                        residentKmAppRegister.setinputs(updatedResidentKmInputJson.toJSONString());
//                        residentKmAppRegister.setkeyManager(residentKMUUID);

                        //update the Db with residentKmAppregister
//                        Boolean updateStatus = updateAppRegisterData(appId,keyType,residentKmAppRegister);
//                        System.out.println(" ... Updated resident registration entry ... : appID = " + appId +
//                                ",  key type = " + keyType + ",  update status = " + updateStatus);
                    } else {
                        System.out.println(" ... Unable to find Oauth APP details from IDN_OAUTH_CONSUMER_APPS ..." +
                                " : appID = " + appId + ",  client-ID = " + clientID);
                    }

                    //Resident Key-mapping update
                    JSONObject appAttributesJson = new JSONObject();
                    if (ExistOauthAPP) {
                        appAttributesJson = getAppAttributes(appId);
                        JSONObject residentAppInfo = new JSONObject();
                        residentAppInfo.put("clientId",clientID);
                        residentAppInfo.put("clientName",kmOauthAppDetails.getappName());
                        residentAppInfo.put("callBackURL",kmOauthAppDetails.getcallbackUrls());
                        residentAppInfo.put("clientSecret", kmOauthAppDetails.getclientSecret());
                        //remove the additional \ lines
                        updatedResidentKmInputJson.put("additionalProperties", adsResidentPropJson);
                        updatedResidentKmInputJson.put("redirect_uris",updatedResidentKmInputJson.get("callback_url"));
                        updatedResidentKmInputJson.remove("callback_url");
                        residentAppInfo.put("parameters",updatedResidentKmInputJson);
                        residentAppInfo.put("isSaasApplication",true);
                        residentAppInfo.put("appAttributes", appAttributesJson);
                        residentAppInfo.put("tokenType", "JWT");
                        AppKeyMapping residentAppKeyMap = appKeyMapping;
                        residentAppKeyMap.setappInfo(residentAppInfo);
                        residentAppKeyMap.setkeyManager(residentKMUUID);

                        //update the Db with residentAppKeyMap
                        Boolean updateStatuskm = updateKeyMappingData(appId,keyType,residentAppKeyMap);
                        System.out.println(" ... Updated resident key-mapping entry ... : appID = " + appId + ",  key type = "
                                + keyType + ",  update Status = " + updateStatuskm);
                    }



                    //Ping KM flow
//                    JSONObject updatedPingKmInputJson = updatedInputJson;
                    JSONObject pingKmResponse;
                    pingKmResponse = RestRequest.getOauthClientDetails(pingKmDcrUrl,
                            clientID, pingKmUsername,pingKmPassword, false);

                    String grantPingString = "";
                    String callbackPingString = "";
                    Boolean executePingFlow = false;
                    if (!pingKmResponse.isEmpty() && pingKmResponse.containsKey("failures")) {
                        System.out.println(" ... Error extracting app details from Ping-KM ... : appID = " + appId + "," +
                                "  key type = " + keyType + ",  Ping-Response = " + pingKmResponse.get("failures"));
                    } else {
//                        if (!updatedPingKmInputJson.isEmpty() && !pingKmResponse.isEmpty()) {
                        if (!pingKmResponse.isEmpty()) {
                            executePingFlow = true;
//                            if (pingKmResponse.get("grantTypes") instanceof ArrayList) {
//                                ArrayList<String> grants = (ArrayList<String>) pingKmResponse.get("grantTypes");
//                                grantPingString = String.join(",",grants);
//                            }
                            if (pingKmResponse.get("redirectUris") instanceof ArrayList) {
                                ArrayList<String> callbacks = (ArrayList<String>) pingKmResponse.get("redirectUris");
                                callbackPingString = String.join(",",callbacks);
                            }
                        }
//                        updatedPingKmInputJson.put("grant_types",grantPingString);
//                        updatedPingKmInputJson.put("callback_url",callbackPingString);
//                        JSONObject adsPingPropJson = new JSONObject();
//                        adsPingPropJson.put("bypassApprovalPage", pingKmResponse.get("bypassApprovalPage").toString());
//                        adsPingPropJson.put("restrictedResponseTypes", pingKmResponse.get("restrictedResponseTypes"));
//                        adsPingPropJson.put("clientAuthnType", pingKmResponse.get("clientAuthnType"));
//                        adsPingPropJson.put("restrictedScopes", pingKmResponse.get("restrictedScopes"));
//                        updatedPingKmInputJson.put("additionalProperties", adsPingPropJson.toJSONString());
//
//                        AppRegister pingKmAppRegister = appRegister;
//                        pingKmAppRegister.setinputs(updatedPingKmInputJson.toString());
//                        pingKmAppRegister.setkeyManager(pingKMUUID);
//                        pingKmAppRegister.setwfRef(UUID.randomUUID().toString());
//
//                        //Insert the Db with pingKmAppRegister
//                        Boolean insertStatus = insertAppRegisterData(pingKmAppRegister);
//                        System.out.println(" ... Inserted pingKM registration entry ... : appID = " + appId + "," +
//                                "  key type = " + keyType + ",  insert status = " + insertStatus);
                    }


                    //PingKM Key-mapping insert
                    if (executePingFlow) {
                        JSONObject pingAppInfo = new JSONObject();
                        pingAppInfo.put("clientId",clientID);
                        pingAppInfo.put("clientName",pingKmResponse.get("name"));
                        pingAppInfo.put("callBackURL", callbackPingString);
                        pingAppInfo.put("clientSecret", kmOauthAppDetails.getclientSecret());
                        pingAppInfo.put("isSaasApplication", false);
                        pingAppInfo.put("appAttributes",appAttributesJson);
                        //update parameters section
                        JSONObject pingParam = new JSONObject();
                        pingParam.put("client_name",pingKmResponse.get("name"));
                        pingParam.put("client_id",clientID);
                        String grantTypesPing = "";
                        if (pingKmResponse.get("grantTypes") instanceof ArrayList) {
                            ArrayList<String> grants = (ArrayList<String>) pingKmResponse.get("grantTypes");
                            grantTypesPing = String.join(" ", grants);
                        }
                        pingParam.put("grant_types",grantTypesPing);
                        JSONObject pingAddiProps = new JSONObject();
                        pingAddiProps.put("name",pingKmResponse.get("name"));
                        pingAddiProps.put("clientId",clientID);
                        pingAddiProps.put("redirectUris",pingKmResponse.get("redirectUris"));
                        pingAddiProps.put("grantTypes",pingKmResponse.get("grantTypes"));
                        pingAddiProps.put("description",pingKmResponse.get("description"));
                        pingAddiProps.put("clientAuthnType",pingKmResponse.get("clientAuthnType"));
                        pingAddiProps.put("restrictedResponseTypes",pingKmResponse.get("restrictedResponseTypes"));
                        pingAddiProps.put("restrictScopes",pingKmResponse.get("restrictScopes"));
                        pingAddiProps.put("restrictedScopes",pingKmResponse.get("restrictedScopes"));
                        pingAddiProps.put("bypassApprovalPage",pingKmResponse.get("bypassApprovalPage"));
                        pingAddiProps.put("forceSecretChange",pingKmResponse.get("forceSecretChange"));
                        pingParam.put("additionalProperties",pingAddiProps);
                        pingAppInfo.put("parameters",pingParam);

                        AppKeyMapping pingAppKeyMap = appKeyMapping;
                        pingAppKeyMap.setappInfo(pingAppInfo);
                        pingAppKeyMap.setkeyManager(pingKMUUID);
                        pingAppKeyMap.setUUID(UUID.randomUUID().toString());


                        //insert the db with pingAppKeyMap
                        Boolean insertStatusping = insertKeyMappingData(pingAppKeyMap);
                        System.out.println(" ... Inserted pingKM key-mapping entry ... : appID = " + appId + "" +
                                ",  key type = " + keyType + ",  insert Status = " + insertStatusping);
                    }


                }
            }
//        initializeComplete(false);

            System.out.println(" ............ Application data migration completed ............ ");
        } else {
            System.out.println(" ............ Application data migration not enabled ............ ");
        }

        if (StringUtils.equalsIgnoreCase("true", enableApiMigration) ||
                StringUtils.equalsIgnoreCase("true", enableApiRedeploy)) {
            System.out.println(" ............ Starting API data migration ............ ");

            JSONObject clientDetails = registerClient(residentDcrUrl, residentusername, residentpassword);
            String clientId = null;
            String clientSecret = null;
            if (!(clientDetails == null) && !clientDetails.isEmpty()) {
                clientId = (String) clientDetails.get("client_id");
                clientSecret = (String) clientDetails.get("client_secret");
            }

            JSONObject tokenDetails = getToken(residentTokenUrl, clientId, clientSecret,
                    residentusername, residentpassword);
            String accessToken = null;
            if (!(tokenDetails == null) && !tokenDetails.isEmpty()) {
                accessToken = (String) tokenDetails.get("access_token");
            }

            ArrayList<JSONObject> apiDetailsArray = getAPIList(publisherRestUrl, accessToken);
            String apiId;
            JSONObject apiData;
            ArrayList<String> gatewayEnvironments = new ArrayList<>();
            String[] gatewayEnvList = gatewayEnvironmentList.split(",");
            for (String gatewayEnv : gatewayEnvList) {
                if (StringUtils.isNotEmpty(gatewayEnv) && StringUtils.isNotEmpty(gatewayEnv.trim())) {
                    gatewayEnvironments.add(gatewayEnv.trim());
                }
            }
            if ((apiDetailsArray != null) && !apiDetailsArray.isEmpty()) {
                System.out.println(" ............ Migrating " + apiDetailsArray.size() +" APIs ............ ");
                for (JSONObject apiDetails : apiDetailsArray) {
                    apiId = (String) apiDetails.get("id");
                    String apiName = (String) apiDetails.get("name");
                    String apiStatus = null;
                    if (apiDetails.get("lifeCycleStatus") instanceof String) {
                        apiStatus = (String) apiDetails.get("lifeCycleStatus");
                    }

                    if (StringUtils.equalsIgnoreCase("true", enableApiMigration)) {
                        apiData = getApiDetails(publisherRestUrl, accessToken, apiId);
                        Boolean toUpdate = false;
                        if (apiData == null) {
                            System.out.println(" ISSUE : Error while extracting API details ..." +
                                    " skipping update for API-ID : " + apiId + " API name : " + apiName);
                        }
                        if ((apiData != null) && apiData.get("additionalProperties") instanceof JSONObject) {
                            JSONObject additionalProps = ((JSONObject) apiData.get("additionalProperties"));
                            if (additionalProps.containsKey("basic_auth_handler") &&
                                    additionalProps.get("basic_auth_handler").toString().equals("enable")) {
                                toUpdate = true;
                                System.out.println(" ............ API is configured with Basic-Auth......... " + apiId);
                            }
                        }
                        ArrayList<String> securityTypes;
                        if (toUpdate && apiData.get("securityScheme") instanceof ArrayList) {
                            securityTypes = (ArrayList<String>) apiData.get("securityScheme");
                            if (!securityTypes.contains("basic_auth")) {
                                securityTypes.add("basic_auth");
                                apiData.put("securityScheme", securityTypes);

                            }
                        }
                        if ((apiData != null) && apiData.containsKey("enableStore")) {
                            apiData.put("enableStore", true);
                        }
                        if ((apiData != null) && apiData.containsKey("gatewayEnvironments")) {

                            apiData.put("gatewayEnvironments", gatewayEnvironments);
                            System.out.println(" ............ Updating API ......... " + apiId);
                            Boolean updateStatus = updateApi(publisherRestUrl, accessToken, apiId, apiData.toJSONString());
                            if (!updateStatus) {
                                System.out.println(" ISSUE : Error while updating the API ... " + apiData.toJSONString());
                            }
                        }
                    }

                    if (StringUtils.equalsIgnoreCase("true", enableApiRedeploy) &&
                            StringUtils.equalsIgnoreCase("PUBLISHED",apiStatus)) {
                        System.out.println(" ............ Redeploying API ......... " + apiId);
                        Boolean redeployStatus = reDeployApi(publisherRestUrl, accessToken, apiId);
                        if (!redeployStatus) {
                            System.out.println(" ISSUE : Error while redeploying the API ... " + apiName);
                        }
                    }
                }
            }

            Boolean deleteStatus = deleteClient(residentDcrUrl, clientId, residentusername, residentpassword);
            if (!deleteStatus) {
                System.out.println(" ISSUE : Error while deleting the OAuth client ... ");
            }

            System.out.println(" ............ API data migration completed ............ ");
        } else {
            System.out.println(" ............ API data migration not enabled ............ ");
        }

    }

    private static List<KeyMapInit> getKeyMapInitList() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet results = null;
        String key_map_init_sql = "SELECT APPLICATION_ID,CONSUMER_KEY,KEY_TYPE FROM AM_APPLICATION_KEY_MAPPING" +
                " WHERE KEY_MANAGER = 'Resident Key Manager'";
        List<KeyMapInit> key_mapInit_list = new ArrayList<KeyMapInit>();

        try {
            conn = DBConnection.createNewDBconnection();
            stmt = conn.createStatement();
            results = stmt.executeQuery(key_map_init_sql);
            while (results.next()) {
                KeyMapInit key_mapInit = new KeyMapInit();
                key_mapInit.setId(results.getInt("APPLICATION_ID"));
                key_mapInit.setclientID(results.getString("CONSUMER_KEY"));
                key_mapInit.setkeyType(results.getString("KEY_TYPE"));
                key_mapInit_list.add(key_mapInit);
            }
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in getKeyMapInitList database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { results.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getKeyMapInitList database transaction ... ");
                e.printStackTrace();
            }
            try { stmt.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getKeyMapInitList database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getKeyMapInitList database transaction ... ");
                e.printStackTrace();
            }
        }
        return key_mapInit_list;
    }

    private static String getAppType(Integer appId) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet results = null;
        String appType = null;
        String app_Attr_sql = "SELECT VALUE FROM AM_APPLICATION_ATTRIBUTES WHERE APPLICATION_ID = "
                + appId + " AND NAME = 'Application Type'";

        try {
            conn = DBConnection.createNewDBconnection();
            stmt = conn.createStatement();
            results = stmt.executeQuery(app_Attr_sql);
            while (results.next()) {
                appType = results.getString("VALUE");
            }
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in getAppType database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { results.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppType database transaction ... ");
                e.printStackTrace();
            }
            try { stmt.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppType database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppType database transaction ... ");
                e.printStackTrace();
            }
        }
        return appType;
    }

    private static String getKmUUID(String kmType) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet results = null;
        String kmUUID = null;
        String km_UUID_sql = "SELECT UUID FROM AM_KEY_MANAGER WHERE TYPE = '" + kmType + "'";
        try {
            conn = DBConnection.createNewDBconnection();
            stmt = conn.createStatement();
            results = stmt.executeQuery(km_UUID_sql);
            while (results.next()) {
                kmUUID = results.getString("UUID");
            }
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in getKmUUID database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { results.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getKmUUID database transaction ... ");
                e.printStackTrace();
            }
            try { stmt.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getKmUUID database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getKmUUID database transaction ... ");
                e.printStackTrace();
            }
        }
        return kmUUID;
    }

    private static OauthAppDetails getOauthAppDetails(String clientId) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet results = null;
        OauthAppDetails oauthAppDetails = new OauthAppDetails();
        String app_oauth_app_data_sql = "SELECT APP_NAME, CONSUMER_SECRET, CALLBACK_URL, GRANT_TYPES," +
                " USER_ACCESS_TOKEN_EXPIRE_TIME, APP_ACCESS_TOKEN_EXPIRE_TIME, REFRESH_TOKEN_EXPIRE_TIME, " +
                "ID_TOKEN_EXPIRE_TIME FROM IDN_OAUTH_CONSUMER_APPS WHERE CONSUMER_KEY = '" + clientId + "'";
        try {
            conn = DBConnection.createNewDBconnection();
            stmt = conn.createStatement();
            results = stmt.executeQuery(app_oauth_app_data_sql);
            while (results.next()) {
                oauthAppDetails.setappName(results.getString("APP_NAME"));
                oauthAppDetails.setclientSecret(results.getString("CONSUMER_SECRET"));
                oauthAppDetails.setcallbackUrls(results.getString("CALLBACK_URL"));
                oauthAppDetails.setgrantTypes(results.getString("GRANT_TYPES"));
                oauthAppDetails.setuserTokenExp(results.getLong("USER_ACCESS_TOKEN_EXPIRE_TIME"));
                oauthAppDetails.setappTokenExp(results.getLong("APP_ACCESS_TOKEN_EXPIRE_TIME"));
                oauthAppDetails.setrefreshTokenExp(results.getLong("REFRESH_TOKEN_EXPIRE_TIME"));
                oauthAppDetails.setidTokenExp(results.getLong("ID_TOKEN_EXPIRE_TIME"));
            }
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in getOauthAppDetails database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { results.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getOauthAppDetails database transaction ... ");
                e.printStackTrace();
            }
            try { stmt.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getOauthAppDetails database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getOauthAppDetails database transaction ... ");
                e.printStackTrace();
            }
        }
        return oauthAppDetails;
    }

    private static AppRegister getAppRegisterData(Integer appId, String keyType) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet results = null;
        AppRegister appRegister = new AppRegister();
        String app_register_data_sql = "SELECT * FROM AM_APPLICATION_REGISTRATION WHERE APP_ID = " + appId +
                " AND TOKEN_TYPE = '" + keyType + "'";
        try {
            conn = DBConnection.createNewDBconnection();
            stmt = conn.createStatement();
            results = stmt.executeQuery(app_register_data_sql);
            while (results.next()) {
                appRegister.setsubscriberId(results.getInt("SUBSCRIBER_ID"));
                appRegister.setwfRef(results.getString("WF_REF"));
                appRegister.setappId(results.getInt("APP_ID"));
                appRegister.settokenType(results.getString("TOKEN_TYPE"));
                appRegister.settokenScope(results.getString("TOKEN_SCOPE"));
                appRegister.setinputs(results.getString("INPUTS"));
                appRegister.setallowedDomains(results.getString("ALLOWED_DOMAINS"));
                appRegister.setvalidityPeriod(results.getLong("VALIDITY_PERIOD"));
                appRegister.setkeyManager(results.getString("KEY_MANAGER"));
            }
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in getAppRegisterData database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { results.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppRegisterData database transaction ... ");
                e.printStackTrace();
            }
            try { stmt.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppRegisterData database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppRegisterData database transaction ... ");
                e.printStackTrace();
            }
        }
        return appRegister;
    }

    private static AppKeyMapping getAppKeyMappingData(Integer appId, String keyType) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet results = null;
        AppKeyMapping appKeyMapping = new AppKeyMapping();
        String app_key_mapping_data_sql = "SELECT * FROM AM_APPLICATION_KEY_MAPPING WHERE APPLICATION_ID = " + appId +
                " AND KEY_TYPE = '" + keyType + "'";
        try {
            conn = DBConnection.createNewDBconnection();
            stmt = conn.createStatement();
            results = stmt.executeQuery(app_key_mapping_data_sql);
            while (results.next()) {
                appKeyMapping.setUUID(results.getString("UUID"));
                appKeyMapping.setappId(results.getInt("APPLICATION_ID"));
                appKeyMapping.setclientID(results.getString("CONSUMER_KEY"));
                appKeyMapping.setkeyType(results.getString("KEY_TYPE"));
                appKeyMapping.setstate(results.getString("STATE"));
                appKeyMapping.setcreateMode(results.getString("CREATE_MODE"));
                appKeyMapping.setkeyManager(results.getString("KEY_MANAGER"));
            }
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in getAppKeyMappingData database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { results.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppKeyMappingData database transaction ... ");
                e.printStackTrace();
            }
            try { stmt.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppKeyMappingData database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppKeyMappingData database transaction ... ");
                e.printStackTrace();
            }
        }
        return appKeyMapping;
    }

    private static JSONObject getAppAttributes(Integer appId) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet results = null;
        JSONObject appAttributes = new JSONObject();
        String app_Attr_sql = "SELECT * FROM AM_APPLICATION_ATTRIBUTES WHERE APPLICATION_ID = "
                + appId ;
        try {
            conn = DBConnection.createNewDBconnection();
            stmt = conn.createStatement();
            results = stmt.executeQuery(app_Attr_sql);
            while (results.next()) {
                if (StringUtils.equals(results.getString("NAME")," Application Owner Email")){
                    appAttributes.put(" Application Owner Email",results.getString("VALUE"));
                } else if (StringUtils.equals(results.getString("NAME"),"Application Owner")){
                    appAttributes.put("Application Owner",results.getString("VALUE"));
                } else if (StringUtils.equals(results.getString("NAME"),"Application Type")){
                    appAttributes.put("Application Type",results.getString("VALUE"));
                } else if (StringUtils.equals(results.getString("NAME"),"Restricted Scopes")){
                    appAttributes.put("Restricted Scopes",results.getString("VALUE"));
                }
            }
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in getAppType database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { results.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppType database transaction ... ");
                e.printStackTrace();
            }
            try { stmt.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppType database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in getAppType database transaction ... ");
                e.printStackTrace();
            }
        }
        return appAttributes;
    }

    private static boolean updateAppRegisterData(Integer appId, String keyType, AppRegister residentKmAppRegister) {
        Connection conn = null;
        PreparedStatement ps = null;
        Boolean sucessStatus = false;
//        String initialize_query = "SET FOREIGN_KEY_CHECKS=0";
        String initialize_query = "ALTER TABLE AM_APPLICATION_REGISTRATION NOCHECK CONSTRAINT ALL";
        String update_app_reg_data_sql = "UPDATE AM_APPLICATION_REGISTRATION SET INPUTS=?,KEY_MANAGER=?" +
                " WHERE APP_ID=" + appId + " AND TOKEN_TYPE='" + keyType + "'";
        try {
            conn = DBConnection.createNewDBconnection();
            ps = conn.prepareStatement(initialize_query);
            ps.execute();
            ps = conn.prepareStatement(update_app_reg_data_sql);
            ps.setString(1, residentKmAppRegister.getinputs());
            ps.setString(2, residentKmAppRegister.getkeyManager());
            ps.executeUpdate();
            sucessStatus = true;
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in updateAppRegisterData database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { ps.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in updateAppRegisterData database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in updateAppRegisterData database transaction ... ");
                e.printStackTrace();
            }
        }
        return sucessStatus;
    }

    private static boolean insertAppRegisterData(AppRegister pingKmAppRegister) {
        Connection conn = null;
        PreparedStatement ps = null;
        Boolean sucessStatus = false;
//        String initialize_query = "SET FOREIGN_KEY_CHECKS=0";
        String initialize_query = "ALTER TABLE AM_APPLICATION_REGISTRATION NOCHECK CONSTRAINT ALL";
        String insert_app_reg_data_sql = "INSERT INTO AM_APPLICATION_REGISTRATION (SUBSCRIBER_ID,WF_REF,APP_ID," +
                "TOKEN_TYPE,TOKEN_SCOPE,INPUTS,ALLOWED_DOMAINS,VALIDITY_PERIOD,KEY_MANAGER)" +
                " VALUES (?,?,?,?,?,?,?,?,?)";
        try {
            conn = DBConnection.createNewDBconnection();
            ps = conn.prepareStatement(initialize_query);
            ps.execute();
            ps = conn.prepareStatement(insert_app_reg_data_sql);
            ps.setInt(1, pingKmAppRegister.getsubscriberId());
            ps.setString(2, pingKmAppRegister.getwfRef());
            ps.setInt(3, pingKmAppRegister.getappId());
            ps.setString(4, pingKmAppRegister.gettokenType());
            ps.setString(5, pingKmAppRegister.gettokenScope());
            ps.setString(6, pingKmAppRegister.getinputs());
            ps.setString(7, pingKmAppRegister.getallowedDomains());
            ps.setLong(8, pingKmAppRegister.getvalidityPeriod());
            ps.setString(9, pingKmAppRegister.getkeyManager());
            ps.execute();
            sucessStatus = true;
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in insertAppRegisterData database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { ps.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in insertAppRegisterData database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in insertAppRegisterData database transaction ... ");
                e.printStackTrace();
            }
        }
        return sucessStatus;
    }


    private static boolean updateKeyMappingData(Integer appId, String keyType, AppKeyMapping residentAppKeyMap) {
        Connection conn = null;
        PreparedStatement ps = null;
        Boolean sucessStatus = false;
//        String initialize_query = "SET FOREIGN_KEY_CHECKS=0";
        String initialize_query = "ALTER TABLE AM_APPLICATION_KEY_MAPPING NOCHECK CONSTRAINT ALL";
        String update_app_attr_data_sql = "UPDATE AM_APPLICATION_KEY_MAPPING SET APP_INFO = ?, KEY_MANAGER = ?" +
                " WHERE APPLICATION_ID = " + appId + " AND KEY_TYPE = '" + keyType + "'";
        try {
            conn = DBConnection.createNewDBconnection();
            ps = conn.prepareStatement(initialize_query);
            ps.execute();
            ps = conn.prepareStatement(update_app_attr_data_sql);
            String appInfo = residentAppKeyMap.getappInfo().toJSONString();
            if (StringUtils.isNotEmpty(appInfo)) {
//                ps.setBinaryStream(1, new ByteArrayInputStream(appInfo.getBytes(StandardCharsets.UTF_8)));
                ps.setBinaryStream(1, new ByteArrayInputStream(appInfo.getBytes()));
            } else {
                ps.setBinaryStream(1, null);
            }
            ps.setString(2, residentAppKeyMap.getkeyManager());
            ps.executeUpdate();
            sucessStatus = true;
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in updateKeyMappingData database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { ps.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in updateKeyMappingData database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in updateKeyMappingData database transaction ... ");
                e.printStackTrace();
            }
        }
        return sucessStatus;
    }


    private static boolean insertKeyMappingData(AppKeyMapping pingAppKeyMap) {
        Connection conn = null;
        PreparedStatement ps = null;
        Boolean sucessStatus = false;
//        String initialize_query = "SET FOREIGN_KEY_CHECKS=0";
        String initialize_query = "ALTER TABLE AM_APPLICATION_KEY_MAPPING NOCHECK CONSTRAINT ALL";
        String insert_app_attr_data_sql = "INSERT INTO AM_APPLICATION_KEY_MAPPING (UUID,APPLICATION_ID,CONSUMER_KEY," +
                "KEY_TYPE,STATE,CREATE_MODE,KEY_MANAGER,APP_INFO) VALUES (?,?,?,?,?,?,?,?)";
        try {
            conn = DBConnection.createNewDBconnection();
            ps = conn.prepareStatement(initialize_query);
            ps.execute();
            ps = conn.prepareStatement(insert_app_attr_data_sql);
            ps.setString(1, pingAppKeyMap.getUUID());
            ps.setInt(2, pingAppKeyMap.getappId());
            ps.setString(3, pingAppKeyMap.getclientID());
            ps.setString(4, pingAppKeyMap.getkeyType());
            ps.setString(5, pingAppKeyMap.getstate());
            ps.setString(6, pingAppKeyMap.getcreateMode());
            ps.setString(7, pingAppKeyMap.getkeyManager());
            String appInfo = pingAppKeyMap.getappInfo().toJSONString();
            if (StringUtils.isNotEmpty(appInfo)) {
                ps.setBinaryStream(8, new ByteArrayInputStream(appInfo.getBytes()));
            } else {
                ps.setBinaryStream(8, null);
            }
            ps.execute();
            sucessStatus = true;
        } catch (SQLException ex) {
            System.out.println(" ISSUE : Error in insertKeyMappingData database transaction ... ");
            ex.printStackTrace();
        } finally {
            try { ps.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in insertKeyMappingData database transaction ... ");
                e.printStackTrace();
            }
            try { conn.close(); } catch (Exception e) {
                System.out.println(" ISSUE : Error in insertKeyMappingData database transaction ... ");
                e.printStackTrace();
            }
        }
        return sucessStatus;
    }

//    private static void initializeComplete(Boolean isInitializing) {
//        Connection conn = null;
//        Statement stmt = null;
//        String dropIndex_query = "DROP INDEX IDX_AAKM_CK ON AM_APPLICATION_KEY_MAPPING";
//        String createIndex_query = "CREATE INDEX IDX_AAKM_CK ON AM_APPLICATION_KEY_MAPPING (CONSUMER_KEY)";
//        try {
//            conn = DBConnection.createNewDBconnection();
//            stmt = conn.createStatement();
//            if (isInitializing) {
//                stmt.execute(dropIndex_query);
//            } else {
//                stmt.execute(createIndex_query);
//            }
//        } catch (SQLException ex) {
//            System.out.println(" ISSUE : Error in initializeComplete database transaction ... ");
//            ex.printStackTrace();
//        } finally {
//            try { stmt.close(); } catch (Exception e) {
//                System.out.println(" ISSUE : Error in initializeComplete database transaction ... ");
//                e.printStackTrace();
//            }
//            try { conn.close(); } catch (Exception e) {
//                System.out.println(" ISSUE : Error in initializeComplete database transaction ... ");
//                e.printStackTrace();
//            }
//        }
//    }


}

