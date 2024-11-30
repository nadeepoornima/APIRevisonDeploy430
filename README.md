# API Updater
Used to bulk update APIs with a given role/s for devportal visibility restriction [1]. \
Compatible with wso2am-4.1.0. \
[1] https://apim.docs.wso2.com/en/4.1.0/design/advanced-topics/control-api-visibility-and-subscription-availability-in-developer-portal/#control-api-visibility-in-the-developer-portal

# Recommendations
Back up the APIM_DB and SHARED_DB before the execution of the client. 

# Building the Client 
Clone the repository. \
execute ```mvn clean install``` from inside the **/customAPIpublisher** directory. \
After successful build use the created **Update-Client-1.0-SNAPSHOT-jar-with-dependencies.jar** inside **/customAPIpublisher/target** directory.

# Steps to follow

1. Copy the **client-truststore.jks**  file resides **<APIM_HOME>/repository/resources/security** directory and place it inside the apiUpdate client stored location 
2. Please note that ,all three files; **client-truststore.jks, Update-Client-1.0-SNAPSHOT-jar-with-dependencies.jar, logging.properties and the config.properties** files  should be stored in the same location along with **/logs** folder.

A sample directory structure would look like follows.
```
└── /WorkingDirectory
    ├── Update-Client-1.0-SNAPSHOT-jar-with-dependencies.jar
    ├── logging.properties
    ├── client-truststore.jks
    ├── config.properties
    └── /logs
        └── app.log
```
3. Modify the **config.properties** file accordingly - refer the sample config.properties file attached

```
# Trust-store configurations
TRUSTSTORE.PATH = client-truststore.jks
TRUSTSTORE.PASSWORD = wso2carbon

# Max API limit to be updated . Default 1000
MAX.API.LIMIT = 1000

# tenant admin configurations
ADMIN.USERNAME = admin
ADMIN.PASSWORD = admin

# Publisher REST API configurations
PUBLISHER.REST.URL = https://localhost:9443/api/am/publisher/v3/apis

# configure thread sleep time in miliseconds between API Update
API.REDEPLOY.THREAD.SLEEP.TIME = 1000

# Configure The role list you need to restrict with
# Ex : DEVPORTAL.RESTRICTED.ROLE.LIST = [restrictRole,abcrole,xyzrole]
DEVPORTAL.RESTRICTED.ROLE.LIST = []

# Configure The role list you need to remove restriction
# Ex : DEVPORTAL.RESTRICTED.ROLE.LIST.TO.REMOVE = [restrictRole,abcrole]
DEVPORTAL.RESTRICTED.ROLE.LIST.TO.REMOVE = []

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

Following is a sample app.log after execution 

```
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Starting API Update ***** 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Number Of APIs : 10 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService |
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Starting Processing API with ID :8dbf65b7-1243-4e6b-a748-c525b5fe65c2 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** API : test|/test|1 is defined in APISkipList. Hence Skipping this API  
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Finished Processing API with Id : 8dbf65b7-1243-4e6b-a748-c525b5fe65c2 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService |  
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Starting Processing API with ID :3a53af6f-0607-4f92-bdb1-ee5f4c48fb95 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** API : test1|/t1|1 is in PUBLISHED State. Proceeding with Update. 
2024-11-24 00:26:32 | com.sample.utilities.RestRequest handleUpdateResponse | com.sample.utilities.RestRequest | API updated successfully. 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** API visibility and roles updated successfully. 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Revision Count for API : test1|/t1|1 is : 5 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Revision Count for API is 5. Deleting Oldest Revision. 
2024-11-24 00:26:32 | com.sample.utilities.RestRequest deleteRevision | com.sample.utilities.RestRequest | Successfully deleted revision ID: a8c5d790-5c54-42b9-aabf-db32f1b14553 for API ID: 3a53af6f-0607-4f92-bdb1-ee5f4c48fb95 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** New Revision created with id : 5bf1727e-deda-4721-83ff-b97905554b71 for API : test1|/t1|1 
2024-11-24 00:26:32 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** New Revision going to be deployed with payload : [{"vhost":"localhost","displayOnDevportal":true,"name":"Default"}, {"vhost":"localhost","displayOnDevportal":true,"name":"Production Gateway"}] 
2024-11-24 00:26:33 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Completed Updating API : test1|/t1|1 
2024-11-24 00:26:33 | com.sample.updater.UpdateService main | com.sample.updater.UpdateService | ***** Finished Processing API with Id : 3a53af6f-0607-4f92-bdb1-ee5f4c48fb95 
```


