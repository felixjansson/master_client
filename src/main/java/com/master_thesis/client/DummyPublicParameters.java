package com.master_thesis.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component
@Qualifier("Dummy")
public class DummyPublicParameters implements PublicParameters {

    private HttpAdapter httpAdapter;

    @Autowired
    public DummyPublicParameters(HttpAdapter httpAdapter) {
        this.httpAdapter = httpAdapter;
    }

    @SneakyThrows
    public List<URI> getServers() {

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("http://localhost:4000/api/server/list/uri"))
                .GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        List<URI> servers = new ObjectMapper().readValue(response.body(), new TypeReference<>() {
        });
        return servers;
    }

    @Override
    public int getTransformatorID() {
        return 0;

    }


    public JsonNode registerClient() {
        return httpAdapter.registerClient();
    }

}
