package com.master_thesis.client;


import cc.redberry.rings.bigint.BigInteger;
import ch.qos.logback.classic.Logger;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class SecretShare {
    private static final Logger log = (Logger) LoggerFactory.getLogger(HomomorphicHash.class);

    private BigInteger share;
    private int clientID;
    private int transformatorID;
    private BigInteger proofComponent;
    private BigInteger nonce;
    private SimpleMatrix matrixOfClient;
    private SimpleMatrix skShare;
    private int publicKey;
    private int rsaN;

    public SecretShare(BigInteger share, BigInteger proofComponent, BigInteger nonce) {
        this.share = share;
        this.proofComponent = proofComponent;
        this.nonce = nonce;
    }

    public int getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(int publicKey) {
        this.publicKey = publicKey;
    }

    public int getRsaN() {
        return rsaN;
    }

    public void setRsaN(int rsaN) {
        this.rsaN = rsaN;
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

}
