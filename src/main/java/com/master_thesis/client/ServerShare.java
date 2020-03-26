package com.master_thesis.client;


import cc.redberry.rings.bigint.BigInteger;
import ch.qos.logback.classic.Logger;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ServerShare {
    private static final Logger log = (Logger) LoggerFactory.getLogger(ServerShare.class);

    private BigInteger share;
    private int clientID;
    private int substationID;
    private BigInteger proofComponent;
    private SimpleMatrix matrixOfClient;
    private SimpleMatrix skShare;
    private int publicKey;
    private BigInteger rsaN;
    private int fid;

    public ServerShare(BigInteger share, BigInteger proofComponent) {
        this.share = share;
        this.proofComponent = proofComponent;
    }

    public int getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(int publicKey) {
        this.publicKey = publicKey;
    }

    public BigInteger getRsaN() {
        return rsaN;
    }

    public void setRsaN(BigInteger rsaN) {
        this.rsaN = rsaN;
    }

    public BigInteger getShare() {
        return share;
    }

    public ServerShare setShare(BigInteger share) {
        this.share = share;
        return this;
    }

    public int getClientID() {
        return clientID;
    }

    public ServerShare setClientID(int clientID) {
        this.clientID = clientID;
        return this;
    }

    public int getSubstationID() {
        return substationID;
    }

    public ServerShare setSubstationID(int substationID) {
        this.substationID = substationID;
        return this;
    }

    public BigInteger getProofComponent() {
        return proofComponent;
    }

    public ServerShare setProofComponent(BigInteger proofComponent) {
        this.proofComponent = proofComponent;
        return this;
    }

    public byte[] getMatrixOfClient() {
        return getObjectAsByteArray(matrixOfClient);
    }

    public void setMatrixOfClient(SimpleMatrix matrixOfClient) {
        this.matrixOfClient = matrixOfClient;
    }

    public byte[] getSkShare() {
        return getObjectAsByteArray(skShare);
    }

    public void setSkShare(SimpleMatrix skShare) {
        this.skShare = skShare;
    }

    private byte[] getObjectAsByteArray(Object o) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(o);
            oos.flush();
            oos.close();
            baos.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return baos.toByteArray();
    }

    public int getFid() {
        return fid;
    }

    public void setFid(int fid) {
        this.fid = fid;
    }
}
