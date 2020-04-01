package com.master_thesis.client.data;

import java.math.BigInteger;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class HomomorphicHashData {

    private Map<URI, ServerData> serverData;
    private NonceData nonceData;
    private VerifierData verifierData;


    public HomomorphicHashData(Map<URI, BigInteger> shares, BigInteger proofComponent, BigInteger nonce) {
        nonceData = new NonceData(nonce);
        verifierData = new VerifierData(proofComponent);
        serverData = new HashMap<>();
        shares.forEach((uri, secretShare) -> serverData.put(uri, new ServerData(secretShare)));
    }

    public HomomorphicHashData setFid(int fid) {
        serverData.values().forEach(val -> val.setFid(fid));
        nonceData.setFid(fid);
        verifierData.setFid(fid);
        return this;
    }

    public HomomorphicHashData setClientID(int clientID) {
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

    public Map<URI, ServerData> getServerData() {
        return serverData;
    }

    public NonceData getNonceData() {
        return nonceData;
    }

    public VerifierData getVerifierData() {
        return verifierData;
    }

    public static class ServerData extends ComputationData {
        private BigInteger secretShare;

        public ServerData(BigInteger secretShare) {
            super(Construction.HASH);
            this.secretShare = secretShare;
        }

        public BigInteger getSecretShare() {
            return secretShare;
        }
    }

    public static class NonceData extends ComputationData {
        private BigInteger nonce;

        public NonceData(BigInteger nonce) {
            super(Construction.HASH);
            this.nonce = nonce;
        }

        public BigInteger getNonce() {
            return nonce;
        }
    }

    public static class VerifierData extends ComputationData {
        private BigInteger proofComponent;

        public VerifierData(BigInteger clientProof) {
            super(Construction.HASH);
            this.proofComponent = clientProof;
        }

        public BigInteger getProofComponent() {
            return proofComponent;
        }
    }

}
