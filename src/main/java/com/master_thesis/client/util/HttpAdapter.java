package com.master_thesis.client.util;


import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.master_thesis.client.data.ComputationData;
import com.master_thesis.client.data.Server;
import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class HttpAdapter {

    private ObjectMapper objectMapper;
    private static final Logger log = (Logger) LoggerFactory.getLogger(HttpAdapter.class);

    public HttpAdapter() {
        this.objectMapper = new ObjectMapper();
    }

    @SneakyThrows
    public void sendServerShare(URI uri, Object information) {
        postRequest(uri, information);
    }

    @SneakyThrows
    public List<Server> getServers() {

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("http://localhost:4000/api/server/list"))
                .GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return new ObjectMapper().readValue(response.body(), new TypeReference<>() {
        });
    }

    @SneakyThrows
    public JsonNode registerClient() {
        URI uri = URI.create("http://localhost:4000/api/client/register");
        HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        log.info(response.body());
        return objectMapper.readValue(response.body(), JsonNode.class);
    }

    @SneakyThrows
    public BigInteger getFieldBase(int substationID) {
        URI uri = URI.create("http://localhost:4000/api/setup/fieldBase/" + substationID);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return new BigInteger(response.body());
    }

    @SneakyThrows
    public BigInteger getGenerator(int substationID) {
        URI uri = URI.create("http://localhost:4000/api/setup/generator/" + substationID);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return new BigInteger(response.body());
    }

    @SneakyThrows
    public void deleteClients() {
        URI uri = URI.create("http://localhost:4000/api/client");
        HttpRequest request = HttpRequest.newBuilder(uri).DELETE().build();
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
    }

    @SneakyThrows
    public JsonNode listClients(int substationID) {
        URI uri = URI.create("http://localhost:4000/api/client/list/" + substationID);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), JsonNode.class);
    }

    @SneakyThrows
    public int getTSecurity(int substationID) {
        URI uri = URI.create("http://localhost:4000/api/setup/t-security/" + substationID);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return Integer.parseInt(response.body());
    }

    @SneakyThrows
    public void sendNonce(Object nonceData) {
        URI uri = URI.create("http://localhost:4000/lastClient/newNonce");
        postRequest(uri, nonceData);
    }

    @SneakyThrows
    public void sendProofComponent(ComputationData clientProofData) {
        URI uri = URI.create("http://localhost:3000/api/client/" + clientProofData.getConstruction().getEndpoint());
        postRequest(uri, clientProofData);
    }

    @SneakyThrows
    private void postRequest(URI uri, Object body) {
        boolean sending = true;
        String jsonObject = objectMapper.writeValueAsString(body);

        while (sending) {
            try {
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonObject)).build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                sending = false;
                log.debug("To {}: {}", uri, jsonObject);
                if (!response.body().isEmpty()) {
                    log.debug("Got answer: {}", response.body());
                }
            } catch (InterruptedException | IOException e) {
                log.error("Failed to send to {}: {}", uri, e.getMessage());
                e.printStackTrace();
                Thread.sleep(2000);
            }
        }
    }

    public void updateFid(int substationID, int clientID, int fid) {
        URI uri = URI.create("http://localhost:4000/api/client/fid");
        Map<String, Integer> body = new HashMap<>();
        body.put("substationID", substationID);
        body.put("clientID", clientID);
        body.put("fid", fid);
        postRequest(uri, body);
    }
}
