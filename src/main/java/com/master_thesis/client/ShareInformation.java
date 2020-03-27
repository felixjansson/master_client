package com.master_thesis.client;


import java.math.BigInteger;
import java.net.URI;
import java.util.Map;

public class ShareInformation {

    private final Map<URI, ServerShare> serverShares;
    private final BigInteger nonce;
    private int fid;
    private int substationID;
    private int clientID;

    public ShareInformation(Map<URI, ServerShare> serverShares, BigInteger nonce) {
        this.serverShares = serverShares;
        this.nonce = nonce;
    }

    private ShareInformation(ShareInformation shareInformation) {
        this.nonce = shareInformation.nonce;
        this.serverShares = null;
        this.fid = shareInformation.fid;
        this.substationID = shareInformation.substationID;
        this.clientID = shareInformation.clientID;
    }

    public ShareInformation removeServerShare() {
        return new ShareInformation(this);
    }

    public Map<URI, ServerShare> getServerShares() {
        return serverShares;
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public int getFid() {
        return fid;
    }

    public ShareInformation setFid(int fid) {
        serverShares.values().forEach(secretShare -> secretShare.setFid(fid));
        this.fid = fid;
        return this;
    }

    public int getSubstationID() {
        return substationID;
    }

    public ShareInformation setSubstationID(int substationID) {
        serverShares.values().forEach(secretShare -> secretShare.setSubstationID(substationID));
        this.substationID = substationID;
        return this;
    }

    public int getClientID() {
        return clientID;
    }

    public ShareInformation setClientID(int clientID) {
        serverShares.values().forEach(secretShare -> secretShare.setClientID(clientID));
        this.clientID = clientID;
        return this;
    }

}
