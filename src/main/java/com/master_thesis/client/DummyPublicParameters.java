package com.master_thesis.client;

import cc.redberry.rings.bigint.BigInteger;
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
import static cc.redberry.rings.Rings.Z;


@Component
@Qualifier("Dummy")
public class DummyPublicParameters implements PublicParameters {

    private HttpAdapter httpAdapter;

    @Autowired
    public DummyPublicParameters(HttpAdapter httpAdapter) {
        this.httpAdapter = httpAdapter;
    }

    @SneakyThrows
    public List<Server> getServers() {

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("http://localhost:4000/api/server/list"))
                .GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        List<Server> servers = new ObjectMapper().readValue(response.body(), new TypeReference<>() {
        });
        return servers;
    }

    @Override
    public int getTransformatorID() {
        return 0;

    }



    @Override
    public BigInteger getFieldBase() { // TODO: 2020-02-24 Ask Dr.Internet
        return Z.valueOf(17);
        //        return  Z.getOne().shiftLeft(107).decrement();
    }

    @Override
    public int getSecurityThreshold() { // TODO: 2020-02-26 Ask Dr. Internet
        return 2;
    }

}
