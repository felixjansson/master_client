package com.master_thesis.client.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigInteger;
import java.net.URI;
import java.util.Map;

public class LinearSignatureData {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private Map<URI, ServerData> serverData;
    private NonceData nonceData;
    private VerifierData verifierData;
    private int fid;
    private int substationID;
    private int clientID;

    public LinearSignatureData(Map<URI, ServerData> shares, BigInteger nonce) {
        nonceData = new NonceData(nonce);
        serverData = shares;
        verifierData = new VerifierData();
    }

    public LinearSignatureData setFid(int fid) {
        serverData.values().forEach(val -> val.setFid(fid));
        nonceData.setFid(fid);
        verifierData.setFid(fid);
        this.fid = fid;
        return this;
    }

    public LinearSignatureData setClientID(int clientID) {
        serverData.values().forEach(val -> val.setId(clientID));
        nonceData.setId(clientID);
        verifierData.setId(clientID);
        this.clientID = clientID;
        return this;
    }


    public void setSubstationID(int substationID) {
        serverData.values().forEach(val -> val.setSubstationID(substationID));
        nonceData.setSubstationID(substationID);
        verifierData.setSubstationID(substationID);
        this.substationID = substationID;
    }

    public int getFid() {
        return fid;
    }

    public int getClientID() {
        return clientID;
    }

    public int getSubstationID() {
        return substationID;
    }

    public Map<URI, ServerData> getServerData() {
        return serverData;
    }

    public NonceData getNonceData() {
        return nonceData;
    }

    public VerifierData getVerifierData() {
        return verifierData;
    }

    public void setVerifierData(BigInteger primE, BigInteger s, BigInteger x) {
        verifierData.setFidPrime(primE);
        verifierData.setsShare(s);
        verifierData.setX(x);
    }

    public static class ServerData extends ComputationData {
        private BigInteger secretShare;

        public ServerData(BigInteger secretShare) {
            super(Construction.LINEAR);
            this.secretShare = secretShare;
        }

        public BigInteger getSecretShare() {
            return secretShare;
        }
    }

    public static class NonceData extends ComputationData {
        private BigInteger nonce;

        public NonceData(BigInteger nonce) {
            super(Construction.LINEAR);
            this.nonce = nonce;
        }

        public BigInteger getNonce() {
            return nonce;
        }
    }

    public static class VerifierData extends ComputationData {

        private BigInteger fidPrime;
        private BigInteger sShare;
        private BigInteger x;

        public VerifierData() {
            super(Construction.LINEAR);
        }


        public void setFidPrime(BigInteger fidPrime) {
            this.fidPrime = fidPrime;
        }

        public BigInteger getFidPrime() {
            return fidPrime;
        }

        public void setsShare(BigInteger sShare) {
            this.sShare = sShare;
        }

        public BigInteger getsShare() {
            return sShare;
        }

        public void setX(BigInteger x) {
            this.x = x;
        }

        public BigInteger getX() {
            return x;
        }
    }

    public static class PublicData {
        private BigInteger N, NRoof, fidPrime, g, g1;
        private BigInteger[] h, sk;

        public BigInteger getN() {
            return N;
        }

        public void setN(BigInteger n) {
            N = n;
        }

        public BigInteger getNRoof() {
            return NRoof;
        }

        public void setNRoof(BigInteger NRoof) {
            this.NRoof = NRoof;
        }

        public BigInteger getFidPrime() {
            return fidPrime;
        }

        public void setFidPrime(BigInteger fidPrime) {
            this.fidPrime = fidPrime;
        }

        public BigInteger getG() {
            return g;
        }

        public void setG(BigInteger g) {
            this.g = g;
        }

        public BigInteger getG1() {
            return g1;
        }

        public void setG1(BigInteger g1) {
            this.g1 = g1;
        }

        public BigInteger[] getH() {
            return h;
        }

        public void setH(BigInteger[] h) {
            this.h = h;
        }

        public BigInteger[] getSk() {
            return sk;
        }

        public void setSk(BigInteger[] sk) {
            this.sk = sk;
        }

        @Override
        public String toString() {
            try {
                return objectMapper.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                return "ERROR: JsonProcessingException => PublicData {}";
            }
        }
    }

}
