package com.example.musicstreamingapp.model;

import java.io.Serializable;
import java.util.List;

public class PlanInfo implements Serializable {
    private String plan;
    private String name;
    private Long priceVnd;
    private List<String> features;

    public String getPlan() { return plan; }
    public String getName() { return name; }
    public Long getPriceVnd() { return priceVnd; }
    public List<String> getFeatures() { return features; }
}
