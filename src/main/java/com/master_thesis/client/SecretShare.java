package com.master_thesis.client;

public class SecretShare {

    private int share;
    private int clientID;
    private int transformatorID;

    public int getProofComponent() {
        return proofComponent;
    }

    public void setProofComponent(int proofComponent) {
        this.proofComponent = proofComponent;
    }

    private int proofComponent;

    public SecretShare(int share, int clientID, int transformatorID, int proofComponent) {
        this.share = share;
        this.clientID = clientID;
        this.transformatorID = transformatorID;
        this.proofComponent = proofComponent;
    }

    public int getShare() {
        return share;
    }

    public void setShare(int share) {
        this.share = share;
    }

    public int getClientID() {
        return clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public int getTransformatorID() {
        return transformatorID;
    }

    public void setTransformatorID(int transformatorID) {
        this.transformatorID = transformatorID;
    }
}
