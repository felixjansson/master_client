package com.master_thesis.client.data;

public abstract class ComputationData {

    private final Construction construction;
    private int fid, id, substationID;

    protected ComputationData(Construction construction) {
        this.construction = construction;
    }

    public int getFid() {
        return fid;
    }

    public void setFid(int fid) {
        this.fid = fid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSubstationID() {
        return substationID;
    }

    public void setSubstationID(int substationID) {
        this.substationID = substationID;
    }

    public Construction getConstruction() {
        return construction;
    }

}
