package com.master_thesis.client;

import java.net.URI;
import java.util.List;

public interface ClientSecretSharing {

    List<ShareObject> shareSecret(int secret, List<URI> servers);

}
