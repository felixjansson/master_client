package com.master_thesis.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;

public interface PublicParameters {

    List<URI> getServers();

    int getTransformatorID();

    JsonNode registerClient();

}
