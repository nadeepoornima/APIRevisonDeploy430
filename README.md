# custom_migration
Used to Republish all the APIs which are already at PUBLISHED state after migration. \
If you have multi tenancy need to run the migration client per each tenant. \
Tested in wso2am-3.2.0+1632415863879

# Steps to follow

1. Copy the **client-truststore.jks**  file resides **<APIM_HOME>/repository/resources/security** directory and place it inside the migration client stored location 
2. Please note that ,all three files; **client-truststore.jks, Migration-Client-1.0-SNAPSHOT-jar-with-dependencies.jar, and the config.properties** files  should be stored in the same location. 
3. Modify the **config.properties** file accordingly - refer the sample config.properties file attached 

```  
# Trust-store configurations 
# If you have setup a different client-truststore name in Gateway please change the name accordingly 
TRUSTSTORE.PATH = client-truststore.jks 
TRUSTSTORE.PASSWORD = <trust-store_password> 

# Residnet-KM configurations 
RESIDENTKM.DCR.URL = https://<KM_HOST_NAME>:<PORT>/client-registration/v0.17/register 
RESIDENTKM.TOKEN.URL = https://<KM_HOST_NAME>:<PORT>/oauth2/token 
RESIDENTKM.USERNAME = <TENANT_ADMIN_USERNAME> 
RESIDENTKM.PASSWORD = <TENANT_ADMIN_PASSWORD> 

# Publisher REST API configurations 
PUBLISHER.REST.URL = https://<PUBLISHER_HOST_NAME>:<PORT>/api/am/publisher/v1/apis 

# Migration client parameters 
# configure "true" 
RUN.API.REDEPLOY = true 

# configure thread sleep time between API redeploying 
API.REDEPLOY.THREAD.SLEEP.TIME = <THREAD_SLEEP_TIME_IN_MILISECONDS> 
```

4. Run the Client to re-publish all APIs with below command

`java -jar Migration-Client-1.0-SNAPSHOT-jar-with-dependencies.jar`

To get the terminal log to a different log file, execute the below command. 
`java -jar Migration-Client-1.0-SNAPSHOT-jar-with-dependencies.jar > migration_client_log_<tenant_Name>.txt`

You will see a similar log in terminal or migration_client_log_<tenant_Name>.txt as follows when migration client is started . 
```
............ Starting API redeploying ............ 
log4j:WARN No appenders could be found for logger (org.apache.http.client.protocol.RequestAddCookies).
log4j:WARN Please initialize the log4j system properly.
............ Redeploying 12 APIs ............ 
....... API Migrate1 is currently at state PUBLISHED
....... Re-deploying Migrate1 has started 
....... Re-deploying Migrate1 has finished 

```