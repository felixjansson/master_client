package com.master_thesis.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.List;

@Component
@Qualifier("Dummy")
public class DummyPublicParameters implements PublicParameters {

    private HttpAdapter httpAdapter;

    @Autowired
    public DummyPublicParameters(HttpAdapter httpAdapter) {
        this.httpAdapter = httpAdapter;
    }

    public List<URI> getServers() {
        List<URI> servers = new LinkedList<>();
        int[] ports = new int[]{2000, 2001};
        for (int port : ports) {
            servers.add(URI.create(String.format("http://localhost:%d/api/client-share", port)));
        }
        return servers;
    }

    @Override
    public int getTransformatorID() {
        return 0;
    }


    public int registerClient(){
        return  httpAdapter.registerClient();
    }

}
