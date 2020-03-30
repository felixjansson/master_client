package com.master_thesis.client;

import java.math.BigInteger;

public class VerifierInformation {

    private BigInteger clientProofComponent;
    private int clientID;
    private int substationID;
    private int fid;

    public VerifierInformation(ShareInformation shareInformation){
        this.clientProofComponent = shareInformation.getClientProofComponent();
        this.clientID = shareInformation.getClientID();
        this.substationID = shareInformation.getSubstationID();
        this.fid = shareInformation.getFid();
    }

    public BigInteger getClientProofComponent() {
        return clientProofComponent;
    }

    public void setClientProofComponent(BigInteger clientProofComponent) {
        this.clientProofComponent = clientProofComponent;
    }

    public int getClientID() {
        return clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public int getSubstationID() {
        return substationID;
    }

    public void setSubstationID(int substationID) {
        this.substationID = substationID;
    }

    public int getFid() {
        return fid;
    }

    public void setFid(int fid) {
        this.fid = fid;
    }

    @Override
    public String toString() {
        return "PartialClientInfo{" +
                "clientProofComponent=" + clientProofComponent +
                ", clientID=" + clientID +
                ", substationID=" + substationID +
                ", fid=" + fid +
                '}';
    }

}
