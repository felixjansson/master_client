package com.master_thesis.client.SanityCheck.HomomorphicHash;

import ch.qos.logback.classic.Logger;
import com.master_thesis.client.data.DefaultPublicData;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Stream;

@Component
public class HomomorphicHashVerifier {

    private final static Logger log = (Logger) LoggerFactory.getLogger(HomomorphicHashVerifier.class);
    protected DefaultPublicData publicParameters;


    @Autowired
    public HomomorphicHashVerifier(DefaultPublicData publicParameters) {
        this.publicParameters = publicParameters;
    }

    /**
     * This is the final Eval function from the Homomorphic Hash based construction
     *
     * @param partialResultInfo a stream of all the partial sums, given from the servers
     * @return the sum of the servers' partial sums
     */
    public BigInteger finalEval(Stream<BigInteger> partialResultInfo) {
        return partialResultInfo
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    /**
     * This is the final Proof function from the Homomorphic Hash based construction
     *
     * @param partialProofs a stream of the servers' partial proofs
     * @return the product of all the clients' partial proofs reduced to the field
     */
    public BigInteger finalProof(Stream<BigInteger> partialProofs) {
        return partialProofs
                .reduce(BigInteger.ONE, BigInteger::multiply)
                .mod(publicParameters.getFieldBase());
    }

    /**
     * This is the verify function from the Homomorphic based construction
     *
     * @param result       is given from the final Eval function
     * @param serverProof  is given from the final Proof function
     * @param clientProofs is a list of all clients' proofs (tau from paper)
     * @return true: if product of client proofs == hash of the final eval
     * AND if the product of clients' proofs == the server proof,
     * false: otherwise
     */
    public boolean verify(BigInteger result, BigInteger serverProof, List<BigInteger> clientProofs) {
        if (serverProof == null)
            return false;
        BigInteger fieldBase = publicParameters.getFieldBase();
//        Compute the product of all the clients' proofs
        BigInteger clientProof = clientProofs.stream().reduce(BigInteger.ONE, BigInteger::multiply).mod(fieldBase);
//        Compute the hash of the final result
        BigInteger resultProof = hash(result, fieldBase, publicParameters.getGenerator());
//        Check if the product of the clients' proofs are equal to the hash value of the final result
        boolean clientEqResult = clientProof.equals(resultProof);
//        Check if the product of the clients' proofs are equal to the final proof
        boolean clientEqServer = clientProof.equals(serverProof);
        if (!(clientEqResult && clientEqServer))
            log.info("clientProof: {}, resultProof: {}, serverProof:{}", clientProof, resultProof, serverProof);
        return clientEqResult && clientEqServer;
    }

    public BigInteger hash(BigInteger input, BigInteger fieldBase, BigInteger generator) {
        return generator.modPow(input, fieldBase);
    }

}