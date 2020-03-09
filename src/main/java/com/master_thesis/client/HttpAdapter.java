package com.master_thesis.client;


import cc.redberry.rings.bigint.BigInteger;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Component
public class HttpAdapter {

    private ObjectMapper objectMapper;
    private static final Logger log = (Logger) LoggerFactory.getLogger(HttpAdapter.class);

    public HttpAdapter() {
        this.objectMapper = new ObjectMapper();
    }


    public void sendShare(URI uri, SecretShare information) {
        boolean sending = true;
        while (sending) {
            try {
                String jsonObject = objectMapper.writeValueAsString(information);
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonObject)).build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
                sending = false;
                log.debug("To {}: {}", uri, jsonObject);
            } catch (InterruptedException | IOException e) {
                log.info("Failed to send to {}: {}", uri, e.getMessage());
                e.printStackTrace();
            }
        }
    }


    @SneakyThrows
    public JsonNode registerClient() {
        URI uri = URI.create("http://localhost:4000/api/client/register");
        HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), JsonNode.class);
    }

    @SneakyThrows
    public BigInteger getFieldBase(int transformatorID) {
        URI uri = URI.create("http://localhost:4000/api/setup/fieldBase/" + transformatorID);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return new BigInteger(response.body());
    }

    @SneakyThrows
    public BigInteger getGenerator(int transformatorID) {
        URI uri = URI.create("http://localhost:4000/api/setup/generator/" + transformatorID);
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
    public JsonNode listClients(int transformatorID) {
        URI uri = URI.create("http://localhost:4000/api/client/list");
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(), JsonNode.class);
    }

    @SneakyThrows
    public int getTSecurity(int transformatorID) {
        URI uri = URI.create("http://localhost:4000/api/setup/t-security/" + transformatorID);
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return Integer.parseInt(response.body());
    }
}
