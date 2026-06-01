package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class Subscription implements Serializable {
    private Long subscriptionId;
    private String plan;
    private String startDate;
    private String endDate;
    private Boolean active;
    private Boolean pending;

    public Long getSubscriptionId() { return subscriptionId; }
    public String getPlan() { return plan; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public Boolean getActive() { return active; }
    public Boolean getPending() { return pending; }

    public boolean isPremium() {
        return plan != null && !plan.equalsIgnoreCase("FREE")
                && active != null && active;
    }
}
