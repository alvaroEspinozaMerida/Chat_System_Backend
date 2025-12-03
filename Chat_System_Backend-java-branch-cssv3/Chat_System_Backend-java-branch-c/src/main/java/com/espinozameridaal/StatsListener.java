package com.espinozameridaal;

public interface StatsListener {
    void onStatsUpdated(double lastRttMs, double avgRttMs, double throughputMbps);
}

