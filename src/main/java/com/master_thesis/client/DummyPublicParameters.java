package com.master_thesis.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

@Component
@Qualifier("Dummy")
public class DummyPublicParameters implements PublicParameters {

    public List<URI> getServers() {
        List<URI> servers = new LinkedList<>();
        int[] ports = new int[]{1000, 2000};
        for (int port : ports) {
            servers.add(URI.create(String.format("http://localhost:%d/api/client-share", port)));
        }
        return servers;
    }

}
