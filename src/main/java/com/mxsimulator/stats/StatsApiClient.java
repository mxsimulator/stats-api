package com.mxsimulator.stats;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class StatsApiClient {
    private final HttpClient httpClient;
    private final String BASE_URL = "https://mxsimulator.com/servers/";
    private final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    public StatsApiClient() {
        httpClient = HttpClient.newBuilder().build();
    }

    public Map<String, LocalDateTime> getAllServers() throws IOException, InterruptedException {
        Map<String, LocalDateTime> servers = new HashMap<>();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());
        Document doc = Jsoup.parse(response.body());
        Element body = doc.body();
        Element table = body.select("table.laptimes").first();
        Elements rows = table.select("tr");
        for (int i = 1; i < rows.size(); i++) {
            Elements cols = rows.get(i).select("td");
            // TODO parse track /riders/length
            servers.put(
                    cols.get(0).select("a").first().html(),
                    LocalDateTime.parse(cols.get(1).select("noscript").first().html(), TIMESTAMP_FORMAT)
            );
        }

        return servers;
    }

    public String getServerRaces(String serverUrl) {
        return String.format("%s/%s.html",
                BASE_URL,
                serverUrl
        );
    }

    public String getServerRace(String serverUrl, Integer raceId) {
        return String.format("%s/%s/%d.html",
                BASE_URL,
                serverUrl,
                raceId);
    }
}