package com.master_thesis.client;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

@Component
@Qualifier("Dummy")
public class DummySecretShare implements ClientSecretSharing {

    @Override
    @SneakyThrows
    public List<ShareObject> shareSecret(int secret, List<URI> servers) {
        List<ShareObject> shares = new LinkedList<>();
//        Math magic
        int share = secret / servers.size();
        for (URI server : servers) {
            JSONObject information = new JSONObject();
            information.put("share", share);
            secret -= share;
            shares.add(new ShareObject(server, information));
        }

        shares.get(0).information.put("share", share + secret);
//        end of math magic

        return shares;

    }
}
