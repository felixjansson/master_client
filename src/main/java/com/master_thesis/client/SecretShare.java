package com.master_thesis.client;

public class SecretShare {

    private int share;
    private int clientID;
    private int transformatorID;

    public SecretShare(int share, int clientID, int transformatorID) {
        this.share = share;
        this.clientID = clientID;
        this.transformatorID = transformatorID;
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
