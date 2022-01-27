package com.mxsimulator.stats;

import java.io.IOException;

public class StatsApiExample {
    public static void main(String... args) throws IOException, InterruptedException {
        StatsApiClient statsApiClient = new StatsApiClient();
        statsApiClient.getAllServers();
    }
}
