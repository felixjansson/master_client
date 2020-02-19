package com.master_thesis.client;

import lombok.SneakyThrows;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import java.net.URI;

public class ShareObject {

    URI destination;
    JSONObject information;

    public ShareObject(URI destination, JSONObject information) {
        this.destination = destination;
        this.information = information;
    }

    public URI getDestination() {
        return destination;
    }

    @SneakyThrows
    public int getShare() {
        return information.getInt("share");
    }
}
