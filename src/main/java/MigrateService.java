import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import utilities.ReadConfigFile;
import java.util.ArrayList;
import static utilities.RestRequest.*;

public class MigrateService {

    public static void main(String arg[]) throws InterruptedException {

        ReadConfigFile configs = new ReadConfigFile();

        String residentDcrUrl = configs.getProperty("RESIDENTKM.DCR.URL");
        String residentusername = configs.getProperty("RESIDENTKM.USERNAME");
        String residentpassword = configs.getProperty("RESIDENTKM.PASSWORD");
        String residentTokenUrl = configs.getProperty("RESIDENTKM.TOKEN.URL");
        String publisherRestUrl = configs.getProperty("PUBLISHER.REST.URL");
        String enableApiRedeploy = configs.getProperty("RUN.API.REDEPLOY");
        long sleepTime = Long.parseLong(configs.getProperty("API.REDEPLOY.THREAD.SLEEP.TIME"));

        if (StringUtils.equalsIgnoreCase("true", enableApiRedeploy)) {
            System.out.println("............ Starting API redeploying ............ ");

            JSONObject clientDetails = registerClient(residentDcrUrl, residentusername, residentpassword);
            String clientId = null;
            String clientSecret = null;
            if (!(clientDetails == null) && !clientDetails.isEmpty()) {
                clientId = (String) clientDetails.get("clientId");
                clientSecret = (String) clientDetails.get("clientSecret");
            }

            JSONObject tokenDetails = getToken(residentTokenUrl, clientId, clientSecret,
                    residentusername, residentpassword);
            String accessToken = null;
            if (!(tokenDetails == null) && !tokenDetails.isEmpty()) {
                accessToken = (String) tokenDetails.get("access_token");
            }

            ArrayList<JSONObject> apiDetailsArray = getAPIList(publisherRestUrl, accessToken);
            String apiId;

            if ((apiDetailsArray != null) && !apiDetailsArray.isEmpty()) {
                System.out.println("............ Redeploying " + apiDetailsArray.size() + " APIs ............ ");
                for (JSONObject apiDetails : apiDetailsArray) {
                    apiId = (String) apiDetails.get("id");
                    String apiName = (String) apiDetails.get("name");
                    String apiStatus = null;
                    if (apiDetails.get("lifeCycleStatus") instanceof String) {
                        apiStatus = (String) apiDetails.get("lifeCycleStatus");
                        System.out.println("....... API " +apiName+ " is currently at state " + apiStatus);
                    }

                    if (StringUtils.equalsIgnoreCase("true", enableApiRedeploy) &&
                            StringUtils.equalsIgnoreCase("PUBLISHED", apiStatus)) {
                        System.out.println("....... Re-deploying " +apiName+ " has started ");
                            Boolean redeployStatus = reDeployApi(publisherRestUrl, accessToken, apiId);
                            if (!redeployStatus) {
                                System.out.println(" ISSUE : Error while redeploying the API ... " + apiName);
                            }else {
                                System.out.println("....... Re-deploying " +apiName+ " has finished ");
                            }
                    }
                    Thread.sleep(sleepTime);
                }
            }

            System.out.println("............ API redeploying completed ............ ");
        }
    }
}

