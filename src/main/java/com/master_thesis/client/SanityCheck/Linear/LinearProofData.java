package com.master_thesis.client.SanityCheck.Linear;

import java.math.BigInteger;

public class LinearProofData {


    private BigInteger s;
    private BigInteger xTilde;

    public LinearProofData(BigInteger s, BigInteger xTilde) {

        this.s = s;
        this.xTilde = xTilde;
    }

    public BigInteger getS() {
        return s;
    }

    public BigInteger getXTilde() {
        return xTilde;
    }

}
