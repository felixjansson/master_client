package com.master_thesis.client;

import cc.redberry.rings.bigint.BigInteger;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class PublicParameters {

    private HttpAdapter httpAdapter;

    @Autowired
    public PublicParameters(HttpAdapter httpAdapter) {
        this.httpAdapter = httpAdapter;
    }

    @SneakyThrows
    public List<Server> getServers() {

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("http://localhost:4000/api/server/list"))
                .GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        return new ObjectMapper().readValue(response.body(), new TypeReference<>() {
        });
    }

    public int getTransformatorID() {
        return 0;

    }

    public BigInteger getGenerator(int transformatorID) {
        return httpAdapter.getGenerator(transformatorID);
    }


    public BigInteger getFieldBase(int transformatorID) {
        return httpAdapter.getFieldBase(getTransformatorID());
    }

    public int getSecurityThreshold() {
        return httpAdapter.getTSecurity(getTransformatorID());
    }

}
