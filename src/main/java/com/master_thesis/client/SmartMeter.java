package com.master_thesis.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.configurationprocessor.json.JSONObject;

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
    private PublicParameters publicParameters;
    private int clientID;
    private int transformatorID;
    private HttpAdapter httpAdapter;

    @Autowired
    public SmartMeter(Reader reader, ClientSecretSharing clientSecretSharing, PublicParameters publicParameters, HttpAdapter httpAdapter) {
        this.publicParameters = publicParameters;
        this.reader = reader;
        this.clientSecretSharing = clientSecretSharing;
        this.httpAdapter = httpAdapter;
        JsonNode jsonNode = publicParameters.registerClient();
        this.clientID = jsonNode.get("clientID").asInt();
        this.transformatorID = jsonNode.get("transformatorID").asInt();
    }

    @Autowired
    public void run() {
        int val = reader.readValue();
        List<URI> servers = publicParameters.getServers();
        List<Integer> shares = clientSecretSharing.shareSecret(val, servers.size());
        HashMap<URI, SecretShare> destinationMap = zipToMap(servers, shares);
        destinationMap.forEach(httpAdapter::sendShare);
    }

    private HashMap<URI, SecretShare> zipToMap(List<URI> uris, List<Integer> shares) {
        Iterator<URI> uriIter = uris.iterator();
        Iterator<Integer> shareIter = shares.iterator();
        HashMap<URI, SecretShare> map = new HashMap<>();

        while (uriIter.hasNext() && shareIter.hasNext()) {
            map.put(uriIter.next(), new SecretShare(shareIter.next(), clientID, transformatorID, 0));
        }

        if (uriIter.hasNext() || shareIter.hasNext()) {
            throw new RuntimeException("Zip was not finished correctly");
        }

        return map;
    }



}
