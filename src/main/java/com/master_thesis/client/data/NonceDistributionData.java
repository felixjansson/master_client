package com.master_thesis.client.data;

import java.math.BigInteger;
import java.net.URI;
import java.util.Map;

public class NonceDistributionData {
    private Map<URI, ServerData> serverData;
    private VerifierData verifierData;


    public NonceDistributionData(Map<URI, ServerData> serverData, BigInteger proofComponent) {
        this.serverData = serverData;
        verifierData = new VerifierData(proofComponent);
    }

    public NonceDistributionData setFid(int fid) {
        serverData.values().forEach(val -> val.setFid(fid));
        verifierData.setFid(fid);
        return this;
    }

    public NonceDistributionData setClientID(int clientID) {
        serverData.values().forEach(val -> val.setId(clientID));
        verifierData.setId(clientID);
        return this;
    }

    public void setSubstationID(int substationID) {
        serverData.values().forEach(val -> val.setSubstationID(substationID));
        verifierData.setSubstationID(substationID);
    }

    public Map<URI, ServerData> getServerData() {
        return serverData;
    }

    public ComputationData getVerifierData() {
        return verifierData;
    }

    public static class ServerData extends ComputationData {
        private BigInteger secretShare;
        private BigInteger nonceShare;

        public ServerData(BigInteger secretShare, BigInteger nonceShare) {
            super(Construction.NONCE);
            this.secretShare = secretShare;
            this.nonceShare = nonceShare;
        }

        public BigInteger getSecretShare() {
            return secretShare;
        }

        public BigInteger getNonceShare() {
            return nonceShare;
        }
    }

    public static class VerifierData extends ComputationData {
        private BigInteger proofComponent;

        public VerifierData(BigInteger clientProof) {
            super(Construction.NONCE);
            this.proofComponent = clientProof;
        }

        public BigInteger getProofComponent() {
            return proofComponent;
        }
    }
}
