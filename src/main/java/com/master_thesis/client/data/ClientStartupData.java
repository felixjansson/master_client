package com.master_thesis.client.data;

public class ClientStartupData {

    private int clientID;
    private int substationID;
    private int startFid;

    public ClientStartupData() {
        this.clientID = 0;
        this.substationID = 0;
        this.startFid = 1;
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

    public int getStartFid() {
        return startFid;
    }

    public void setStartFid(int startFid) {
        this.startFid = startFid;
    }
}
