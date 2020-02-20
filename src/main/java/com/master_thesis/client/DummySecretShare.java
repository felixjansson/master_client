package com.master_thesis.client;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
@Qualifier("Dummy")
public class DummySecretShare implements ClientSecretSharing {

    @Override
    @SneakyThrows
    public List<Integer> shareSecret(int secret, int servers) {
        List<Integer> shares = new LinkedList<>();
//        Math magic
        int share = secret / servers;
        for (int i = 0; i < servers-1; i++) {
            secret -= share;
            shares.add(share);
        }
        shares.add(secret);
        return shares;
    }
}
