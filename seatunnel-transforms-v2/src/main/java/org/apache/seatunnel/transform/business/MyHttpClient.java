package org.apache.seatunnel.transform.business;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.Map;

/**
 * 我的Http Client
 */
public class MyHttpClient {
    private final CloseableHttpClient client;
    private final String apiPath;
    private final Map<String, String> header;
    private final Map<String, Object> body;
    private final String parse;

    public MyHttpClient(String apiPath, Map<String, String> header, Map<String, Object> body, String parse) {
        this.apiPath = apiPath;
        this.header = header;
        this.body = body;
        this.parse = parse;
        this.client = HttpClients.createDefault();
    }

    public CloseableHttpClient getClient() {
        return client;
    }
}
