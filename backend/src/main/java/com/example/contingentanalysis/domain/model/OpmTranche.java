package com.example.contingentanalysis.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpmTranche {
    private int tier;

    @JsonProperty("strike_low")
    private double strikeLow;

    @JsonProperty("strike_high")
    private double strikeHigh;

    @JsonProperty("call_low")
    private double callLow;

    @JsonProperty("call_high")
    private double callHigh;

    @JsonProperty("incremental_call")
    private double incrementalCall;

    public OpmTranche() {
    }

    public OpmTranche(int tier, double strikeLow, double strikeHigh, double callLow, double callHigh, double incrementalCall) {
        this.tier = tier;
        this.strikeLow = strikeLow;
        this.strikeHigh = strikeHigh;
        this.callLow = callLow;
        this.callHigh = callHigh;
        this.incrementalCall = incrementalCall;
    }

    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }

    public double getStrikeLow() { return strikeLow; }
    public void setStrikeLow(double strikeLow) { this.strikeLow = strikeLow; }

    public double getStrikeHigh() { return strikeHigh; }
    public void setStrikeHigh(double strikeHigh) { this.strikeHigh = strikeHigh; }

    public double getCallLow() { return callLow; }
    public void setCallLow(double callLow) { this.callLow = callLow; }

    public double getCallHigh() { return callHigh; }
    public void setCallHigh(double callHigh) { this.callHigh = callHigh; }

    public double getIncrementalCall() { return incrementalCall; }
    public void setIncrementalCall(double incrementalCall) { this.incrementalCall = incrementalCall; }
}

