# API Updater
Used to Update All APIs with a given role for devportal visibility restriction [1]. \
Compatible with wso2am-4.1.0. \

# Steps to follow

1. Copy the **client-truststore.jks**  file resides **<APIM_HOME>/repository/resources/security** directory and place it inside the apiUpdate client stored location 
2. Please note that ,all three files; **client-truststore.jks, Update-Client-1.0-SNAPSHOT-jar-with-dependencies.jar, logging.properties and the config.properties** files  should be stored in the same location along with **/logs** folder. 
3. Modify the **config.properties** file accordingly - refer the sample config.properties file attached 

```
# Trust-store configurations
TRUSTSTORE.PATH = client-truststore.jks
TRUSTSTORE.PASSWORD = wso2carbon
# tenant admin configurations
ADMIN.USERNAME = admin
ADMIN.PASSWORD = admin
# Publisher REST API configurations
PUBLISHER.REST.URL = https://localhost:9443/api/am/publisher/v3/apis
# configure thread sleep time between API Update
API.REDEPLOY.THREAD.SLEEP.TIME = 1000
# Configure The role you need to restrict with
DEVPORTAL.RESTRICTED.ROLE = restrictRole
# Configure the API ids you want to skip as comma seperated array
# Ex : API.SKIP.LIST = [a62ca2a7-a1d2-4919-9f5c-642e36d07099,352a7d6c-5bec-4964-b059-850ac6c95006]
API.SKIP.LIST = []
# Set this to true if you need to run the client in Explicit API update Mode . By Default its false 
# When The Explicit API update Mode is enabled , it will update the APIs defined in EXPLICIT.API.UPDATE.LIST
# Will be useful for testing the client flow only for 1 or 2 APIs 
ENABLE.EXPLICIT.API.UPDATE.MODE = false
# Configure the API ids you want to explicitly update as comma seperated array
# Ex : EXPLICIT.API.UPDATE.LIST = [a62ca2a7-a1d2-4919-9f5c-642e36d07099,352a7d6c-5bec-4964-b059-850ac6c95006]
EXPLICIT.API.UPDATE.LIST = []
```
4. logging.properties file

```
# Root logger configuration
handlers = java.util.logging.FileHandler
.level = INFO

# FileHandler configuration
java.util.logging.FileHandler.level = ALL
java.util.logging.FileHandler.pattern = logs/app.log
java.util.logging.FileHandler.append = true
java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter

# SimpleFormatter custom format
java.util.logging.SimpleFormatter.format = %1$tF %1$tT | %2$s | %3$s | %5$s %n
```
# How to Run

**java -Djava.util.logging.config.file=logging.properties -jar Update-Client-1.0-SNAPSHOT-jar-with-dependencies.jar config.properties**

**app.log** file will be created under **/logs** directory . 

Folllowing is a sample app.log after execution 

```
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** Starting API Update ***** 
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** Number Of APIs : 10 
2024-11-24 00:26:32 | UpdateService main | UpdateService |
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** Starting Processing API with ID :8dbf65b7-1243-4e6b-a748-c525b5fe65c2 
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** API : test|/test|1 is defined in APISkipList. Hence Skipping this API  
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** Finished Processing API with Id : 8dbf65b7-1243-4e6b-a748-c525b5fe65c2 
2024-11-24 00:26:32 | UpdateService main | UpdateService |  
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** Starting Processing API with ID :3a53af6f-0607-4f92-bdb1-ee5f4c48fb95 
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** API : test1|/t1|1 is in PUBLISHED State. Proceeding with Update. 
2024-11-24 00:26:32 | utilities.RestRequest handleUpdateResponse | utilities.RestRequest | API updated successfully. 
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** API visibility and roles updated successfully. 
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** Revision Count for API : test1|/t1|1 is : 5 
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** Revision Count for API is 5. Deleting Oldest Revision. 
2024-11-24 00:26:32 | utilities.RestRequest deleteRevision | utilities.RestRequest | Successfully deleted revision ID: a8c5d790-5c54-42b9-aabf-db32f1b14553 for API ID: 3a53af6f-0607-4f92-bdb1-ee5f4c48fb95 
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** New Revision created with id : 5bf1727e-deda-4721-83ff-b97905554b71 for API : test1|/t1|1 
2024-11-24 00:26:32 | UpdateService main | UpdateService | ***** New Revision going to be deployed with payload : [{"vhost":"localhost","displayOnDevportal":true,"name":"Default"}, {"vhost":"localhost","displayOnDevportal":true,"name":"Production Gateway"}] 
2024-11-24 00:26:33 | UpdateService main | UpdateService | ***** Completed Updating API : test1|/t1|1 
2024-11-24 00:26:33 | UpdateService main | UpdateService | ***** Finished Processing API with Id : 3a53af6f-0607-4f92-bdb1-ee5f4c48fb95 
```


