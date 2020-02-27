package com.master_thesis.client;


import cc.redberry.rings.bigint.BigInteger;

public class SecretShare {

    private BigInteger share;
    private int clientID;
    private int transformatorID;
    private BigInteger proofComponent;
    private BigInteger nonce;

    public SecretShare(BigInteger share, BigInteger proofComponent, BigInteger nonce) {
        this.share = share;
        this.proofComponent = proofComponent;
        this.nonce = nonce;
    }


    public BigInteger getShare() {
        return share;
    }

    public SecretShare setShare(BigInteger share) {
        this.share = share;
        return this;
    }

    public int getClientID() {
        return clientID;
    }

    public SecretShare setClientID(int clientID) {
        this.clientID = clientID;
        return this;
    }

    public int getTransformatorID() {
        return transformatorID;
    }

    public SecretShare setTransformatorID(int transformatorID) {
        this.transformatorID = transformatorID;
        return this;
    }

    public BigInteger getProofComponent() {
        return proofComponent;
    }

    public SecretShare setProofComponent(BigInteger proofComponent) {
        this.proofComponent = proofComponent;
        return this;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public SecretShare setNonce(BigInteger nonce) {
        this.nonce = nonce;
        return this;
    }
}
