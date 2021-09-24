package model;

import org.json.simple.JSONObject;

public class AppKeyMapping {
    private String UUID;
    private int appId;
    private String clientID;
    private String keyType;
    private String state;
    private String createMode;
    private String keyManager;
    private JSONObject appInfo;

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public int getappId() {
        return appId;
    }

    public void setappId(int appId) {
        this.appId = appId;
    }

    public String getclientID() {
        return clientID;
    }

    public void setclientID(String clientID) {
        this.clientID = clientID;
    }

    public String getkeyType() {
        return keyType;
    }

    public void setkeyType(String keyType) {
        this.keyType = keyType;
    }

    public String getstate() {
        return state;
    }

    public void setstate(String state) {
        this.state = state;
    }

    public String getcreateMode() {
        return createMode;
    }

    public void setcreateMode(String createMode) {
        this.createMode = createMode;
    }

    public String getkeyManager() { return keyManager; }

    public void setkeyManager(String keyManager) { this.keyManager = keyManager; }

    public JSONObject getappInfo() { return appInfo; }

    public void setappInfo(JSONObject appInfo) { this.appInfo = appInfo; }
}

