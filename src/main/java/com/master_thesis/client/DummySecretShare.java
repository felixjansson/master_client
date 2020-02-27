package com.master_thesis.client;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Component
@Qualifier("Dummy")
public class DummySecretShare implements ClientSecretSharing {

    private PublicParameters publicParameters;

    @Autowired
    public DummySecretShare(PublicParameters publicParameters) {
        this.publicParameters = publicParameters;
    }

    @Override
    @SneakyThrows
    public Map<URI, SecretShare> shareSecret(int secret) {
        List<Integer> shares = new LinkedList<>();
        int servers = publicParameters.getServers().size();
//        Math magic
        int share = secret / servers;
        for (int i = 0; i < servers-1; i++) {
            secret -= share;
            shares.add(share);
        }
        shares.add(secret);
        return new HashMap<>();
    }
}
