package model;

public class OauthAppDetails {

    private String appName;
    private String clientSecret;
    private String grantTypes;
    private String callbackUrls;
    private long userTokenExp;
    private long appTokenExp;
    private long idTokenExp;
    private long refreshTokenExp;


    public String getappName() {
        return appName;
    }

    public void setappName(String appName) {
        this.appName = appName;
    }

    public String getclientSecret() {
        return clientSecret;
    }

    public void setclientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getgrantTypes() {
        return grantTypes;
    }

    public void setgrantTypes(String grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String getcallbackUrls() {
        return callbackUrls;
    }

    public void setcallbackUrls(String callbackUrls) {
        this.callbackUrls = callbackUrls;
    }

    public long getuserTokenExp() {
        return userTokenExp;
    }

    public void setuserTokenExp(long userTokenExp) {
        this.userTokenExp = userTokenExp;
    }

    public long getappTokenExp() {
        return appTokenExp;
    }

    public void setappTokenExp(long appTokenExp) {
        this.appTokenExp = appTokenExp;
    }

    public long getidTokenExp() {
        return idTokenExp;
    }

    public void setidTokenExp(long idTokenExp) {
        this.idTokenExp = idTokenExp;
    }

    public long getrefreshTokenExp() {
        return refreshTokenExp;
    }

    public void setrefreshTokenExp(long refreshTokenExp) {
        this.refreshTokenExp = refreshTokenExp;
    }

}

