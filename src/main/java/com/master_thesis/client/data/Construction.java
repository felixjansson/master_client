package com.master_thesis.client.data;

public enum Construction {
    RSA("RSA Threshold Scheme", "rsa-data"), HASH("Homomorphic Hash", "hash-data"), LINEAR("Linear Homomorphic Signatures", "linear-data");


    private final String name;
    private final String endpoint;

    Construction(String name, String endpoint) {
        this.name = name;
        this.endpoint = endpoint;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

}
