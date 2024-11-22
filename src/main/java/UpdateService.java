import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.ReadConfigFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import static utilities.RestRequest.*;

public class UpdateService {

    public static void main(String arg[]) throws InterruptedException {

        ReadConfigFile configs = new ReadConfigFile();

        String residentusername = configs.getProperty("ADMIN.USERNAME");
        String residentpassword = configs.getProperty("ADMIN.PASSWORD");
        String publisherRestUrl = configs.getProperty("PUBLISHER.REST.URL");
        String visibilityRestrictRole = configs.getProperty("DEVPORTAL.RESTRICTED.ROLE");
        long sleepTime = Long.parseLong(configs.getProperty("API.REDEPLOY.THREAD.SLEEP.TIME"));

        System.out.println("***** Starting API Update *****");
        //Create Basic accessToken by Encoding admin credentials
        String accessToken = Base64.getEncoder().encodeToString((residentusername + ":" + residentpassword).getBytes(StandardCharsets.UTF_8));
        //Get API List by calling /api/am/publisher/v3/apis
        ArrayList<JSONObject> apiDetailsArray = getAPIList(publisherRestUrl, accessToken);

        if ((apiDetailsArray != null) && !apiDetailsArray.isEmpty()) {
            System.out.println("***** Number Of APIs : " + apiDetailsArray.size());
            System.out.println();
            //Iterate through each API in the list
            for (JSONObject apiDetails : apiDetailsArray) {
                String apiId = (String) apiDetails.get("id");
                System.out.println("***** Starting Processing API with ID :" +apiId);
                String apiName = (String) apiDetails.get("name");
                String apiContext = (String) apiDetails.get("context");
                String apiVersion = (String) apiDetails.get("version");
                String apiStatus = (String) apiDetails.get("lifeCycleStatus");
                // we only update the apis which are at published state
                if (!apiStatus.equalsIgnoreCase("published")) {
                    System.out.println("***** API : " + apiName + "|" + apiContext + "|" + apiVersion +" is Not at PUBLISHED State . Currently at state : " + apiStatus + " Hence ignoring this API .");
                } else {
                    System.out.println("***** API : " + apiName + "|" + apiContext + "|" + apiVersion +" is at PUBLISHED State . Hence Proceed with Update .");
                    // Proceed to api update
                    // Get API details by passing the apiid by call /api/am/publisher/v3/apis/<apiId>
                    JSONObject apiDetailsByApiId = getAPIDetailsByApiId(publisherRestUrl,accessToken,apiId);
                    // Check if devportal visibility is PUBLIC
                    String apiVisibility = (String) apiDetailsByApiId.get("visibility");
                    if (apiVisibility.equalsIgnoreCase("public")){
                        //modify the apiDetailsByApiId object by setting the visibility as restricted
                        //handle visibility
                        apiDetailsByApiId.put("visibility", "RESTRICTED");
                        //handle visibleRoles
                        JSONArray visibleRoles = (JSONArray) apiDetailsByApiId.get("visibleRoles");
                        if (visibleRoles == null) {
                            visibleRoles = new JSONArray(); // initialize if empty
                        }
                        // Add the new role
                        if (!visibleRoles.contains(visibilityRestrictRole)) { // Check for duplicates
                            visibleRoles.add(visibilityRestrictRole);
                        }
                        apiDetailsByApiId.put("visibleRoles", visibleRoles);
                        // call update api
                        boolean isUpdated = updateApi(publisherRestUrl, accessToken, apiId, apiDetailsByApiId);
                        Thread.sleep(sleepTime);
                        if (isUpdated) {
                            System.out.println("***** API visibility and roles updated successfully.");
                        } else {
                            System.err.println("***** Failed to update API visibility and roles.");
                        }
                    }
                    if (apiVisibility.equalsIgnoreCase("restricted")){
                        //handle only visibleRoles
                        JSONArray visibleRoles = (JSONArray) apiDetailsByApiId.get("visibleRoles");
                        if (visibleRoles == null) {
                            visibleRoles = new JSONArray();
                        }
                        // Add the new role
                        if (!visibleRoles.contains(visibilityRestrictRole)) { // Check for duplicates
                            visibleRoles.add(visibilityRestrictRole);
                        }
                        apiDetailsByApiId.put("visibleRoles", visibleRoles);
                        // call update api
                        boolean isUpdated = updateApi(publisherRestUrl, accessToken, apiId, apiDetailsByApiId);
                        if (isUpdated) {
                            System.out.println("***** API visibility and roles updated successfully.");
                        } else {
                            System.out.println("***** Failed to update API visibility and roles.");
                        }
                    }
                    // get revisionList by passing ApiID
                    ArrayList<JSONObject> apiRevisionArray = getRevisionListByApiId(publisherRestUrl, accessToken, apiId);
                    System.out.println("***** Revision Count for API : " + apiName + "|" + apiContext + "|" + apiVersion +" is : " + apiRevisionArray.size());
                    if (apiRevisionArray.size() < 5){
                        // create newapirevision
                        JSONObject newApiRevision = createRevision(publisherRestUrl, accessToken, apiId);
                        String newApiRevisionId = (String)newApiRevision.get("id");
                        System.out.println("***** New Revision created with id : " + newApiRevisionId + " for API : "  + apiName + "|" + apiContext + "|" + apiVersion );
                        // create payload to deploy newApiRevision
                        ArrayList<JSONObject> deployRevisionPayload = buildDeployRevisionPayload(apiRevisionArray);
                        System.out.println("***** New Revision going to be deployed with payload : "+ deployRevisionPayload.toString());
                        // deploy newapirevision
                        ArrayList<JSONObject> newDeployedRevisionDetails = deployRevision(publisherRestUrl, accessToken, apiId,newApiRevisionId,deployRevisionPayload);
                        Thread.sleep(sleepTime);
                    } else {
                        System.out.println("***** Revision Count for API is 5 . Hence Deleting Oldest revision .");
                        // find revision to delete
                        String revisionIdToDelete = findRevisionToDelete(apiRevisionArray);
                        // Delete Revision and Get remaining Revisions
                        ArrayList<JSONObject> remainingApiRevisionArray = deleteRevision(publisherRestUrl, accessToken, apiId, revisionIdToDelete);
                        // create newapirevision
                        JSONObject newApiRevision = createRevision(publisherRestUrl, accessToken, apiId);
                        String newApiRevisionId = (String)newApiRevision.get("id");
                        System.out.println("***** New Revision created with id : " + newApiRevisionId + " for API : "  + apiName + "|" + apiContext + "|" + apiVersion );
                        // create payload to deploy newApiRevision
                        ArrayList<JSONObject> deployRevisionPayload = buildDeployRevisionPayload(remainingApiRevisionArray);
                        System.out.println("***** New Revision going to be deployed with payload : "+ deployRevisionPayload.toString());
                        // deploy newapirevision
                        ArrayList<JSONObject> newDeployedRevisionDetails = deployRevision(publisherRestUrl, accessToken, apiId, newApiRevisionId, deployRevisionPayload);
                        Thread.sleep(sleepTime);
                    }
                    System.out.println("***** Completed Updating API : "  + apiName + "|" + apiContext + "|" + apiVersion);
                }
                System.out.println("***** Finished Processing API with Id : "+ apiId);
                System.out.println();
                Thread.sleep(sleepTime);
            }
        }

        System.out.println("............ API updating completed ............ ");
    }

    public static String findRevisionToDelete(ArrayList<JSONObject> apiRevisionArray){
        String revisionIdToDelete = null;
        long oldestCreatedTime = Long.MAX_VALUE;

        for (JSONObject revision : apiRevisionArray) {
            // Get createdTime and deploymentInfo from the current revision
            long createdTime = (long) revision.get("createdTime");
            ArrayList<?> deploymentInfo = (ArrayList<?>) revision.get("deploymentInfo");
            // Check if deploymentInfo is empty and createdTime is older
            if (deploymentInfo.isEmpty() && createdTime < oldestCreatedTime) {
                oldestCreatedTime = createdTime;
                revisionIdToDelete = (String) revision.get("id");
            }
        }
        return revisionIdToDelete;
    }

    public static ArrayList<JSONObject> buildDeployRevisionPayload(ArrayList<JSONObject> apiRevisionArray){
        ArrayList<JSONObject> payload = new ArrayList<>();
        for (JSONObject revision : apiRevisionArray) {
            JSONArray deploymentInfo = (JSONArray) revision.get("deploymentInfo");
            if (deploymentInfo != null) {
                for (Object obj : deploymentInfo) {
                    JSONObject deployment = (JSONObject) obj;
                    JSONObject formattedDeployment = new JSONObject();
                    formattedDeployment.put("name", deployment.get("name"));
                    formattedDeployment.put("vhost", deployment.get("vhost"));
                    formattedDeployment.put("displayOnDevportal", deployment.get("displayOnDevportal"));
                    payload.add(formattedDeployment);
                }
            }
        }
        return payload;
    }

}
