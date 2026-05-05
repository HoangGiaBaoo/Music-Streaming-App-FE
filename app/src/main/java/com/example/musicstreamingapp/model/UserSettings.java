package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class UserSettings implements Serializable {
    public Long settingsId;
    public Boolean dataSaver;
    public Boolean pushNotifications;
    public String streamQualityWifi;
    public Boolean privateSession;
    public Boolean personalizedAds;

    public boolean isDataSaver() { return Boolean.TRUE.equals(dataSaver); }
    public boolean isPushNotifications() { return Boolean.TRUE.equals(pushNotifications); }
    public boolean isPrivateSession() { return Boolean.TRUE.equals(privateSession); }
    public boolean isPersonalizedAds() { return Boolean.TRUE.equals(personalizedAds); }
    public String resolveQuality() {
        return streamQualityWifi == null ? "AUTO" : streamQualityWifi;
    }
}
