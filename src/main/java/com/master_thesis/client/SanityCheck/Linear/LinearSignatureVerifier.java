package com.master_thesis.client.SanityCheck.Linear;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.master_thesis.client.data.LinearSignatureData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Component
public class LinearSignatureVerifier {
    private static final Logger log = LoggerFactory.getLogger(LinearSignatureVerifier.class);


    /**
     * This is the final Eval function from the  Linear Signature based construction
     *
     * @param partialResultInfo a stream of all the partial sums, given from the servers
     * @return the sum of the servers' partial sums
     */
    public BigInteger finalEval(Stream<BigInteger> partialResultInfo) {
        return partialResultInfo
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    /**
     * This is the final Proof function from the Linear Signature based construction
     *
     * @param clientData is a list of the data sent from the clients'
     * @param publicData is the public available data for this construction
     * @return the sigma that contains s and xTilde
     */
    public LinearProofData finalProof(List<LinearClientData> clientData, LinearSignatureData.PublicData publicData) {
//        Compute the unique prime times N
        BigInteger eN = publicData.getN().multiply(publicData.getFidPrime());
        BigInteger nRoof = publicData.getNRoof();
//        Compute the sum of all partial s-shares
        BigInteger s = clientData.stream().map(LinearClientData::getsShare).reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger sPrime = s.subtract(s.mod(eN)).divide(eN);
//      We compute xTilde in three steps by computing the numerator, denominator and combining them
        BigInteger numerator = clientData.stream().map(LinearClientData::getX).reduce(BigInteger.ONE, BigInteger::multiply);
        BigInteger denominator = publicData.getG1().modPow(sPrime, nRoof).modInverse(nRoof);
//      Note that we use modulo inverse and thus we have to use multiplication and not division
        BigInteger xTilde = numerator.multiply(denominator).mod(nRoof);

        return new LinearProofData(s.mod(eN), xTilde);
    }

    /**
     * This is the verify function from the Linear Signature based construction
     *
     * @param linearResult is the result from the finalEval function
     * @param proofData    is the proof data computed from the finalProof function
     * @param publicData   is the public data for this construction
     * @param rn           is the sum of all the clients' nonce
     * @return true: if the hash of s times the product of all h times the hash of the result == xTilde to the power of eN
     * false otherwise
     */
    public boolean verify(BigInteger linearResult, LinearProofData proofData, LinearSignatureData.PublicData publicData, BigInteger rn) {
        BigInteger eN = publicData.getN().multiply(publicData.getFidPrime());

//        Check that the result and s are smaller than eN
        boolean inEN = linearResult.compareTo(eN) < 0 && proofData.getS().compareTo(eN) < 0;
        if (!inEN)
            return false;

        BigInteger nRoof = publicData.getNRoof();
//      Below we compute the lhs and rhs and check their equivalence.
//      lhs (clients' secret and nonce)
        BigInteger lhs = proofData.getXTilde().modPow(eN, nRoof);
//      xTilde contains all nonce from the clients. We include Rn to remove these nonce to receive the correct result
        lhs = lhs.multiply(publicData.getG2().modPow(rn, nRoof)).mod(nRoof);
//      rhs (the sum of clients' secret)
        BigInteger rhs = publicData.getG1().modPow(proofData.getS(), nRoof)
                .multiply(Arrays.stream(publicData.getH()).reduce(BigInteger.ONE, BigInteger::multiply)).mod(nRoof)
                .multiply(publicData.getG2().modPow(linearResult, nRoof))
                .mod(nRoof);

        boolean correctResult = lhs.equals(rhs);
        if (!correctResult)
            logError(lhs, rhs, linearResult, proofData, publicData);

        return correctResult;
    }

    /**
     * This function is used to log useful information when an error occurs
     */
    private void logError(BigInteger lhs, BigInteger rhs, BigInteger linearResult, LinearProofData proofData, LinearSignatureData.PublicData publicData) {
        ObjectMapper objectMapper = new ObjectMapper();
        log.error("Could not verify the Linear Signature result");
        log.error("lhs: {} != {} :rhs", lhs, rhs);
        log.error("linearResult: {}", linearResult);
        try {
            log.error("proofData: {}", objectMapper.writeValueAsString(proofData));
            log.error("publicData: {}", objectMapper.writeValueAsString(publicData));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}
