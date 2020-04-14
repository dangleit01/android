package de.nextbill.client.model;

public class VersionDTO {

    private String currentVersion;
    private String newestVersion;

    private Boolean breakingChangeForAndroid;

    private Boolean mailSentActive;

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getNewestVersion() {
        return newestVersion;
    }

    public void setNewestVersion(String newestVersion) {
        this.newestVersion = newestVersion;
    }

    public Boolean getBreakingChangeForAndroid() {
        return breakingChangeForAndroid;
    }

    public void setBreakingChangeForAndroid(Boolean breakingChangeForAndroid) {
        this.breakingChangeForAndroid = breakingChangeForAndroid;
    }

    public Boolean getMailSentActive() {
        return mailSentActive;
    }

    public void setMailSentActive(Boolean mailSentActive) {
        this.mailSentActive = mailSentActive;
    }
}
