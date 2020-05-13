package com.master_thesis.client.SanityCheck.Linear;

import com.master_thesis.client.data.ComputationData;
import com.master_thesis.client.data.Construction;

import java.math.BigInteger;

public class LinearClientData extends ComputationData {

    private BigInteger fidPrime;
    private BigInteger sShare;
    private BigInteger x;

    public LinearClientData() {
        super(Construction.LINEAR);
    }

    public BigInteger getFidPrime() {
        return fidPrime;
    }

    public void setFidPrime(BigInteger fidPrime) {
        this.fidPrime = fidPrime;
    }

    public BigInteger getsShare() {
        return sShare;
    }

    public void setsShare(BigInteger sShare) {
        this.sShare = sShare;
    }

    public BigInteger getX() {
        return x;
    }

    public void setX(BigInteger x) {
        this.x = x;
    }


}
