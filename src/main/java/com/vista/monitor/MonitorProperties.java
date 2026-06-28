package com.vista.monitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vista.monitor")
public class MonitorProperties {
    private String model = "gptoss";
    private long intervalSeconds = 60;
    private boolean autoStart = false;
    private double rxmerFloor = 32.0;
    private double flapCeiling = 8;
    private double t3Ceiling = 3;

    public String getModel() { return model; }
    public void setModel(String v) { this.model = v; }
    public long getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(long v) { this.intervalSeconds = v; }
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean v) { this.autoStart = v; }
    public double getRxmerFloor() { return rxmerFloor; }
    public void setRxmerFloor(double v) { this.rxmerFloor = v; }
    public double getFlapCeiling() { return flapCeiling; }
    public void setFlapCeiling(double v) { this.flapCeiling = v; }
    public double getT3Ceiling() { return t3Ceiling; }
    public void setT3Ceiling(double v) { this.t3Ceiling = v; }
}