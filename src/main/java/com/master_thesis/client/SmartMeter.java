package com.master_thesis.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.util.List;

@SpringBootApplication
public class SmartMeter {

    public static void main(String[] args) {
        SpringApplication.run(SmartMeter.class, args);
    }

    private Reader reader;
    private ClientSecretSharing clientSecretSharing;
    private PublicParameters pp;

    @Autowired
    public SmartMeter(Reader reader, ClientSecretSharing clientSecretSharing, PublicParameters pp) {
        this.pp = pp;
        this.reader = reader;
        this.clientSecretSharing = clientSecretSharing;
    }

    @Autowired
    public void run(HttpAdapter httpAdapter) {
        int val = reader.readValue();
        List<URI> servers = pp.getServers();
        List<ShareObject> shares = clientSecretSharing.shareSecret(val, servers);
        for (ShareObject share : shares) {
            httpAdapter.sendShare(share.destination, share.information);
        }
    }



}
