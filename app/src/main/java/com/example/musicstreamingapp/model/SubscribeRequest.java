package com.example.musicstreamingapp.model;

import java.io.Serializable;

public class SubscribeRequest implements Serializable {
    private String plan;
    public SubscribeRequest(String plan) { this.plan = plan; }
    public String getPlan() { return plan; }
}
