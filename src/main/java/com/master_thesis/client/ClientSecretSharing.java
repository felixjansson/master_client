package com.master_thesis.client;

import java.util.List;

public interface ClientSecretSharing {

    List<Integer> shareSecret(int secret, int servers);

}
