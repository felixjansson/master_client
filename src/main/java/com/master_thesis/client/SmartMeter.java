package com.master_thesis.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class SmartMeter {

    public static void main(String[] args) {
        SpringApplication.run(SmartMeter.class, args);
    }

    private Reader reader;
    private ClientSecretSharing clientSecretSharing;
    private int clientID;
    private int transformatorID;
    private HttpAdapter httpAdapter;

    @Autowired
    public SmartMeter(Reader reader, @Qualifier("hash") ClientSecretSharing clientSecretSharing, HttpAdapter httpAdapter) {
        this.reader = reader;
        this.clientSecretSharing = clientSecretSharing;
        this.httpAdapter = httpAdapter;
        JsonNode jsonNode = httpAdapter.registerClient();
        this.clientID = jsonNode.get("clientID").asInt();
        this.transformatorID = jsonNode.get("transformatorID").asInt();
    }

    @Autowired
    public void run() {
        int val = reader.readValue();
        Map<URI, SecretShare> shareMap = clientSecretSharing.shareSecret(val);
        shareMap.values().forEach(secretShare -> secretShare.setClientID(clientID).setTransformatorID(transformatorID));
        shareMap.forEach(httpAdapter::sendShare);
    }


}
