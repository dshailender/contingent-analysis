package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BreakpointTier {
    private int tier;

    @JsonProperty("start_equity")
    private double startEquity;

    @JsonProperty("end_equity")
    private double endEquity;

    private double width;

    @JsonProperty("claimants_description")
    private String claimantsDescription;

    @JsonProperty("event_description")
    private String eventDescription;

    @JsonProperty("is_thereafter")
    private boolean isThereafter = false;

    public BreakpointTier() {
    }

    public BreakpointTier(int tier, double startEquity, double endEquity, double width,
                          String claimantsDescription, String eventDescription, boolean isThereafter) {
        this.tier = tier;
        this.startEquity = startEquity;
        this.endEquity = endEquity;
        this.width = width;
        this.claimantsDescription = claimantsDescription;
        this.eventDescription = eventDescription;
        this.isThereafter = isThereafter;
    }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public double getStartEquity() { return startEquity; }
    public void setStartEquity(double startEquity) { this.startEquity = startEquity; }

    public double getEndEquity() { return endEquity; }
    public void setEndEquity(double endEquity) { this.endEquity = endEquity; }

    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }

    public String getClaimantsDescription() { return claimantsDescription; }
    public void setClaimantsDescription(String claimantsDescription) { this.claimantsDescription = claimantsDescription; }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }

    public boolean isThereafter() { return isThereafter; }
    public void setThereafter(boolean thereafter) { isThereafter = thereafter; }
}

