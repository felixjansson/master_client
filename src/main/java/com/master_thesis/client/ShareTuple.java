package com.master_thesis.client;


import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class ShareTuple {
    
    final Map<URI,BigInteger> shares;
    final Integer proofComponent;
    final BigInteger nonce;

    public ShareTuple(Map<URI,BigInteger> shares, Integer proofComponent, BigInteger nonce) {
        this.shares = shares;
        this.proofComponent = proofComponent;
        this.nonce = nonce;
    }
}
