package com.master_thesis.client;

import java.net.URI;
import java.util.List;

public interface PublicParameters {

    List<URI> getServers();

    int getTransformatorID();

    int registerClient();

}
