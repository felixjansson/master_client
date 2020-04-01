package com.master_thesis.client.data;

import ch.qos.logback.classic.Logger;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.Map;

public class RSAThresholdData {

    private Map<URI, ServerData> serverData;
    private NonceData nonceData;
    private VerifierData verifierData;

    public RSAThresholdData(Map<URI, ServerData> serverData, VerifierData verifierData, NonceData nonceData) {
        this.serverData = serverData;
        this.nonceData = nonceData;
        this.verifierData = verifierData;
    }

    public RSAThresholdData setFid(int fid) {
        serverData.values().forEach(val -> val.setFid(fid));
        nonceData.setFid(fid);
        verifierData.setFid(fid);
        return this;
    }

    public RSAThresholdData setClientID(int clientID) {
        serverData.values().forEach(val -> val.setId(clientID));
        nonceData.setId(clientID);
        verifierData.setId(clientID);
        return this;
    }

    public void setSubstationID(int substationID) {
        serverData.values().forEach(val -> val.setSubstationID(substationID));
        nonceData.setSubstationID(substationID);
        verifierData.setSubstationID(substationID);
    }

    public Map<URI, RSAThresholdData.ServerData> getServerData() {
        return serverData;
    }

    public RSAThresholdData.NonceData getNonceData() {
        return nonceData;
    }

    public RSAThresholdData.VerifierData getVerifierData() {
        return verifierData;
    }

    public static class ServerData extends ComputationData {

        private static final Logger log = (Logger) LoggerFactory.getLogger(ServerData.class);
        private BigInteger share, proofComponent, rsaN;
        private SimpleMatrix matrixOfClient, skShare;


        public ServerData(BigInteger share, BigInteger proofComponent, SimpleMatrix matrixOfClient, SimpleMatrix skShare, BigInteger rsaN) {
            super(Construction.RSA);
            this.share = share;
            this.proofComponent = proofComponent;
            this.rsaN = rsaN;
            this.matrixOfClient = matrixOfClient;
            this.skShare = skShare;
        }

        public BigInteger getShare() {
            return share;
        }

        public BigInteger getProofComponent() {
            return proofComponent;
        }

        public BigInteger getRsaN() {
            return rsaN;
        }

        public byte[] getMatrixOfClient() {
            return getObjectAsByteArray(matrixOfClient);
        }

        public byte[] getSkShare() {
            return getObjectAsByteArray(skShare);
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

    public static class NonceData extends ComputationData {
        private BigInteger nonce;

        public NonceData(BigInteger nonce) {
            super(Construction.RSA);
            this.nonce = nonce;
        }

        public BigInteger getNonce() {
            return nonce;
        }
    }

    public static class VerifierData extends ComputationData {
        private BigInteger proofComponent;
        private BigInteger publicKey;

        public VerifierData(BigInteger proofComponent, BigInteger publicKey) {
            super(Construction.RSA);
            this.proofComponent = proofComponent;
            this.publicKey = publicKey;
        }

        public BigInteger getProofComponent() {
            return proofComponent;
        }

        public BigInteger getPublicKey() {
            return publicKey;
        }
    }


}
