package com.sample.updater;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import com.sample.utilities.ReadConfigFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sample.utilities.RestRequest.*;

public class UpdateService {

    private static final Logger LOGGER = Logger.getLogger(UpdateService.class.getName());

    public static void main(String[] args) {
        ReadConfigFile configs = new ReadConfigFile();
        String maxApiLimitProp = configs.getProperty("MAX.API.LIMIT");
        int maxApiLimit = (maxApiLimitProp != null) ? Integer.parseInt(maxApiLimitProp) : 2000;
        String adminusername = configs.getProperty("ADMIN.USERNAME");
        String adminpassword = configs.getProperty("ADMIN.PASSWORD");
        String publisherRestUrl = configs.getProperty("PUBLISHER.REST.URL");
        String visibilityRestrictRoleList = configs.getProperty("DEVPORTAL.RESTRICTED.ROLE.LIST");
        String [] visibilityRestrictRoleListArray = getArrayFromString(visibilityRestrictRoleList);
        String visibilityRestrictRoleListToRemove = configs.getProperty("DEVPORTAL.RESTRICTED.ROLE.LIST.TO.REMOVE");
        String [] visibilityRestrictRoleListToRemoveArray = getArrayFromString(visibilityRestrictRoleListToRemove);
        String apiSkipList = configs.getProperty("API.SKIP.LIST");
        String [] apiSkipListArray = getArrayFromString(apiSkipList);
        String apiExplicitUpdateList = configs.getProperty("EXPLICIT.API.UPDATE.LIST");
        String [] apiExplicitUpdateListArray = getArrayFromString(apiExplicitUpdateList);
        boolean enableExplicitUpdateMode = Boolean.parseBoolean(configs.getProperty("ENABLE.EXPLICIT.API.UPDATE.MODE"));
        long sleepTime = Long.parseLong(configs.getProperty("API.REDEPLOY.THREAD.SLEEP.TIME"));

        LOGGER.info("***** Starting API Update *****");

        // Create Basic accessToken by Encoding admin credentials
        String accessToken = Base64.getEncoder().encodeToString((adminusername + ":"
                + adminpassword).getBytes(StandardCharsets.UTF_8));

        // Get API List by calling /api/am/publisher/v3/apis
        ArrayList<JSONObject> apiDetailsArray = getAPIList(publisherRestUrl, accessToken, maxApiLimit);

        // Check if Running on Explicit API Update Mode
        //if (enableExplicitUpdateMode){
            //if (apiExplicitUpdateListArray.length==0){
                LOGGER.log(Level.SEVERE, "***** Running in Explicit API Update Mode , " +
                        "But No APIs defined at EXPLICIT.API.UPDATE.LIST ");
                LOGGER.log(Level.SEVERE, "Stopping Execution Of Client");
                //System.exit(0);
           // } else {
                LOGGER.info("***** Running in Explicit API Update Mode , Hence only the listed APIs will be updated ");
                //apiDetailsArray = getFilteredApiDetailsList(apiDetailsArray,apiExplicitUpdateListArray);
                //LOGGER.info("***** Found " +apiDetailsArray.size()+ " APIs out of " +apiExplicitUpdateListArray.length+
        // " listed under EXPLICIT.API.UPDATE.LIST");
            //}
       // }

        if (apiDetailsArray != null && !apiDetailsArray.isEmpty()) {
            LOGGER.info("***** Number Of APIs : " + apiDetailsArray.size());
            LOGGER.info("");

            // Iterate through each API in the list
            for (JSONObject apiDetails : apiDetailsArray) {
                String apiId = (String) apiDetails.get("id");
                LOGGER.info("***** Starting Processing API with ID :" + apiId);
                String apiName = (String) apiDetails.get("name");
                String apiContext = (String) apiDetails.get("context");
                String apiVersion = (String) apiDetails.get("version");
                String apiStatus = (String) apiDetails.get("lifeCycleStatus");

                // If defined in apiSkipList skip this API
                if (Arrays.asList(apiSkipListArray).contains(apiId)) {
                    LOGGER.info("***** API : " + apiName + "|" + apiContext + "|" + apiVersion + " is defined in " +
                            "APISkipList. Hence Skipping this API ");
                    LOGGER.info("***** Finished Processing API with Id : " + apiId);
                    LOGGER.info("");
                    continue;
                }
                // We only update the APIs which are in published state
                if (apiStatus.equalsIgnoreCase("CREATED")) {
                    LOGGER.info("***** API : " + apiName + "|" + apiContext + "|" + apiVersion + " is in CREATED " +
                            " State. Currently in state: " + apiStatus + " - Skipping this API.");
                } else {
                    LOGGER.info("***** API : " + apiName + "|" + apiContext + "|" + apiVersion + " is in not in " +
                            "CREATED State. Proceeding with Update.");

                    try {
                        // Get API details by passing the apiId by calling /api/am/publisher/v3/apis/<apiId>
                        JSONObject apiDetailsByApiId = getAPIDetailsByApiId(publisherRestUrl, accessToken, apiId);

                        // Check if devportal visibility is PUBLIC
                        String apiVisibility = (String) apiDetailsByApiId.get("visibility");

                        // Update API visibility in object
                        boolean isUpdated = updateVisibilityAndRoles(apiDetailsByApiId, apiVisibility,
                                visibilityRestrictRoleListArray,
                                visibilityRestrictRoleListToRemoveArray);

                        if (isUpdated) {
                            // Call update API
                            if (updateApi(publisherRestUrl, accessToken, apiId, apiDetailsByApiId)) {
                                LOGGER.info("***** API visibility and roles updated successfully.");
                            } else {
                                LOGGER.warning("***** Failed to update API visibility and roles.");
                            }
                        }

                        // Get revision list by passing API ID
                        ArrayList<JSONObject> apiRevisionArray = getRevisionListByApiId(publisherRestUrl, accessToken,
                                apiId);
                        LOGGER.info("***** Revision Count for API : " + apiName + "|" + apiContext + "|"
                                + apiVersion + " is : " + apiRevisionArray.size());

                        // Handle Revision Deployment
                        if (apiRevisionArray.size() < 5) {
                            handleRevisionCreationAndDeployment(publisherRestUrl, accessToken, apiId,
                                    apiName, apiContext, apiVersion, apiRevisionArray, sleepTime);
                        } else {
                            LOGGER.info("***** Revision Count for API is 5. Deleting Oldest Revision.");

                            // Find revision to delete
                            String revisionIdToDelete = findRevisionToDelete(apiRevisionArray);

                            // Delete revision and get remaining revisions
                            ArrayList<JSONObject> remainingApiRevisionArray = deleteRevision(publisherRestUrl,
                                    accessToken, apiId, revisionIdToDelete);

                            // Handle new revision creation and deployment
                            handleRevisionCreationAndDeployment(publisherRestUrl, accessToken, apiId, apiName,
                                    apiContext, apiVersion, remainingApiRevisionArray, sleepTime);
                        }
                        LOGGER.info("***** Completed Updating API : " + apiName + "|" + apiContext + "|" + apiVersion);

                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error occurred while processing API with ID: " + apiId, e);
                        LOGGER.log(Level.SEVERE, "Stopping Execution Of Client");
                        System.exit(0);
                    }
                }

                LOGGER.info("***** Finished Processing API with Id : " + apiId);
                LOGGER.info("");

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Thread sleep interrupted", e);
                }
            }
        } else {
            LOGGER.info("***** No APIs Found to Update . API updating completed ");
        }

        LOGGER.info("............ API updating completed ............ ");
    }

    public static String findRevisionToDelete(ArrayList<JSONObject> apiRevisionArray) {
        String revisionIdToDelete = null;
        long oldestCreatedTime = Long.MAX_VALUE;

        for (JSONObject revision : apiRevisionArray) {
            long createdTime = (long) revision.get("createdTime");
            ArrayList<?> deploymentInfo = (ArrayList<?>) revision.get("deploymentInfo");

            if (deploymentInfo.isEmpty() && createdTime < oldestCreatedTime) {
                oldestCreatedTime = createdTime;
                revisionIdToDelete = (String) revision.get("id");
            }
        }
        return revisionIdToDelete;
    }

//    public static ArrayList<JSONObject> buildDeployRevisionPayload(ArrayList<JSONObject> apiRevisionArray) {
//        ArrayList<JSONObject> payload = new ArrayList<>();
//        for (JSONObject revision : apiRevisionArray) {
//            JSONArray deploymentInfo = (JSONArray) revision.get("deploymentInfo");
//            if (deploymentInfo != null) {
//                for (Object obj : deploymentInfo) {
//                    JSONObject deployment = (JSONObject) obj;
//                    JSONObject formattedDeployment = new JSONObject();
//                    formattedDeployment.put("name", deployment.get("name"));
//                    formattedDeployment.put("vhost", deployment.get("vhost"));
//                    formattedDeployment.put("displayOnDevportal", deployment.get("displayOnDevportal"));
//                    payload.add(formattedDeployment);
//                }
//            }
//        }
//        return payload;
//    }
public static ArrayList<JSONObject> buildDeployRevisionPayload(ArrayList<JSONObject> apiRevisionArray) {
    ArrayList<JSONObject> payload = new ArrayList<>();
    for (JSONObject revision : apiRevisionArray) {
        JSONArray deploymentInfo = (JSONArray) revision.get("deploymentInfo");
        if (deploymentInfo != null) {
            for (Object obj : deploymentInfo) {
                JSONObject deployment = (JSONObject) obj;
                JSONObject formattedDeployment = new JSONObject();

                String name = (String) deployment.get("name");
                formattedDeployment.put("name", name);

                // Check if name = "external"
                if ("external".equalsIgnoreCase(name)) {
                    //vhost value of external GW
                    formattedDeployment.put("vhost", "*******");
                } else if("internal".equalsIgnoreCase(name)) {
                    //vhost value of internal GW
                    formattedDeployment.put("vhost", "*****");
                }else {
                    formattedDeployment.put("vhost", deployment.get("vhost"));
                }

                formattedDeployment.put("displayOnDevportal", deployment.get("displayOnDevportal"));
                payload.add(formattedDeployment);
            }
        }
    }
    return payload;
}

    public static String[] getArrayFromString(String apiSkipList) {
        if (apiSkipList == null || apiSkipList.isEmpty()) {
            return new String[0];  // Return an empty array if input is invalid
        }
        apiSkipList = apiSkipList.trim();
        if (apiSkipList.startsWith("[") && apiSkipList.endsWith("]")) {
            // Remove the surrounding brackets
            apiSkipList = apiSkipList.substring(1, apiSkipList.length() - 1).trim();
        } else {
            throw new IllegalArgumentException("Input must be a list formatted with square brackets.");
        }
        // Split by comma and trim spaces from each element
        String[] array = apiSkipList.split(",");
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }
        return array;
    }

    public static ArrayList<JSONObject> getFilteredApiDetailsList(ArrayList<JSONObject> apiDetailsArray ,
                                                                  String[] apiExplicitUpdateListArray){
        // Convert the String array to a List
        ArrayList<String> stringList = new ArrayList<>(Arrays.asList(apiExplicitUpdateListArray));
        ArrayList<JSONObject> filteredArrayList = new ArrayList<>();
        // Filter the apiDetailsArray
        for (JSONObject jsonObject : apiDetailsArray) {
            String id = (String) jsonObject.get("id");
            if (stringList.contains(id)) {
                filteredArrayList.add(jsonObject);
            }
        }
        return filteredArrayList;
    }

    public static boolean updateVisibilityAndRoles(JSONObject apiDetails, String currentVisibility,
                                                   String[] visibilityRestrictRoles,
                                                   String[] rolesToRemove) {

        if (!"PRIVATE".equalsIgnoreCase(currentVisibility)) {
            boolean isUpdated = false;
            JSONArray visibleRoles = (JSONArray) apiDetails.get("visibleRoles");
            if (visibleRoles == null) {
                visibleRoles = new JSONArray(); // Initialize if null
            }

            // Add missing roles
            for (String role : visibilityRestrictRoles) {
                if (!visibleRoles.contains(role)) {
                    visibleRoles.add(role);
                    isUpdated = true;
                }
            }

            // Remove unwanted roles
            for (String role : rolesToRemove) {
                if (visibleRoles.contains(role)) {
                    visibleRoles.remove(role);
                    isUpdated = true;
                }
            }

            // Adjust visibility if no roles are present
            if (visibleRoles.isEmpty() && !"PUBLIC".equalsIgnoreCase(currentVisibility)) {
                apiDetails.put("visibility", "PUBLIC");
                isUpdated = true;
            } else if (!visibleRoles.isEmpty() && !"RESTRICTED".equalsIgnoreCase(currentVisibility)) {
                apiDetails.put("visibility", "RESTRICTED");
                isUpdated = true;
            }

            apiDetails.put("visibleRoles", visibleRoles);
            return isUpdated;
        } else {
            LOGGER.info("***** API visibility is set to PRIVATE . Hence do not update .");
            return false;
        }
    }

    private static void handleRevisionCreationAndDeployment(String publisherRestUrl, String accessToken,
                                                            String apiId, String apiName, String apiContext,
                                                            String apiVersion, ArrayList<JSONObject> apiRevisionArray,
                                                            long sleepTime) throws InterruptedException {
        // Create new API revision
        JSONObject newApiRevision = createRevision(publisherRestUrl, accessToken, apiId);
        String newApiRevisionId = (String) newApiRevision.get("id");
        LOGGER.info("***** New Revision created with id : " + newApiRevisionId + " for API : " +
                apiName + "|" + apiContext + "|" + apiVersion);

        // Create payload to deploy new API revision
        ArrayList<JSONObject> deployRevisionPayload = buildDeployRevisionPayload(apiRevisionArray);
        LOGGER.info("***** New Revision going to be deployed with payload : " + deployRevisionPayload.toString());

        // Deploy new API revision
        ArrayList<JSONObject> newDeployedRevisionDetails = deployRevision(publisherRestUrl, accessToken, apiId,
                newApiRevisionId, deployRevisionPayload);
        Thread.sleep(sleepTime);
    }

}
