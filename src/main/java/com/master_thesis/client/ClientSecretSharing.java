package com.master_thesis.client;

import java.net.URI;
import java.util.Map;

public interface ClientSecretSharing {

    Map<URI, SecretShare> shareSecret(int secret);

}
