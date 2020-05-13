package com.master_thesis.client.SanityCheck.Linear;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;

@Component
public class LinearSignatureServer {

    /**
     * This is the partial Eval function from the Linear Signatures based construction.
     *
     * @param shares a list of all secret shares that is given to this server.
     * @return The sum of all shares (y_j)
     */
    public BigInteger partialEval(List<BigInteger> shares) {
        return shares.stream().reduce(BigInteger.ZERO, BigInteger::add);
    }
}
