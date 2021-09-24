package model;

public class AppRegister {
    private int subscriberId;
    private String wfRef;
    private int appId;
    private String tokenType;
    private String tokenScope;
    private String inputs;
    private String allowedDomains;
    private long validityPeriod;
    private String keyManager;

    public int getsubscriberId() {
        return subscriberId;
    }

    public void setsubscriberId(int subscriberId) {
        this.subscriberId = subscriberId;
    }

    public String getwfRef() {
        return wfRef;
    }

    public void setwfRef(String wfRef) {
        this.wfRef = wfRef;
    }

    public int getappId() {
        return appId;
    }

    public void setappId(int appId) {
        this.appId = appId;
    }

    public String gettokenType() {
        return tokenType;
    }

    public void settokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String gettokenScope() {
        return tokenScope;
    }

    public void settokenScope(String tokenScope) {
        this.tokenScope = tokenScope;
    }

    public String getinputs() {
        return inputs;
    }

    public void setinputs(String inputs) {
        this.inputs = inputs;
    }

    public String getallowedDomains() {
        return allowedDomains;
    }

    public void setallowedDomains(String allowedDomains) {
        this.allowedDomains = allowedDomains;
    }

    public long getvalidityPeriod() {
        return validityPeriod;
    }

    public void setvalidityPeriod(long validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public String getkeyManager() {
        return keyManager;
    }

    public void setkeyManager(String keyManager) {
        this.keyManager = keyManager;
    }
}
