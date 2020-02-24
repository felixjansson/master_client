package com.master_thesis.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


@Component
public class HttpAdapter {

    private ObjectMapper objectMapper;

    public HttpAdapter() {
        this.objectMapper = new ObjectMapper();
    }

    @SneakyThrows
    public void sendShare(URI uri, SecretShare information) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .setHeader("User-Agent", "Java 11 HttpClient Bot") // add request header
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(information))).build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }


    @SneakyThrows
    public JsonNode registerClient(){
        URI uri = URI.create("http://localhost:4000/api/client/register");
        HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        return objectMapper.readValue(response.body(),JsonNode.class);
    }

}
