package com.master_thesis.client.SanityCheck.RSA;

import ch.qos.logback.classic.Logger;
import com.master_thesis.client.data.DefaultPublicData;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

@Component
public class RSAThresholdVerifier {

    private final static Logger log = (Logger) LoggerFactory.getLogger(RSAThresholdVerifier.class);
    private DefaultPublicData publicParameters;

    @Autowired
    public RSAThresholdVerifier(DefaultPublicData publicParameters) {
        this.publicParameters = publicParameters;
    }

    /**
     * This is the final Eval function from the RSA Threshold based construction
     * @param partialResultInfo a stream of all the partial sums, given from the servers
     * @return the sum of the servers' partial sums
     */
    public BigInteger finalEval(Stream<BigInteger> partialResultInfo) {
        return partialResultInfo
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    /**
     * This is the final Proof function from the RSA Threshold based construction
     * @param rsaProofComponents is a collection with the required RSA components used
     * @param substationID an identifier for the substation which this computation is related to
     * @param lastClientProof is the Rn from the trusted third-party
     * @return the final proof component (sigma)
     */
    public BigInteger finalProof(Collection<RSAOutgoingData.ProofData> rsaProofComponents, int substationID, BigInteger lastClientProof) {
        if (rsaProofComponents.isEmpty())
            return null;
//        Compute the product of all servers' proof components to the power of its public key
        return rsaProofComponents.stream()
//                Combine the partial signatures
                .map(rsaProofComponent -> {
                    try {
//                        Compute the signature that corresponds to the secret
                        BigInteger encryptedRSAProof = clientFinalProof(
                                rsaProofComponent.getPublicKey(),
                                rsaProofComponent.getClientProof(),
                                rsaProofComponent.getRsaProofComponent(),
                                rsaProofComponent.getRsaN(),
                                rsaProofComponent.getRsaDeterminant());
//                        Compute the power of sigma_i to pk
                        BigInteger res = encryptedRSAProof.modPow(rsaProofComponent.getPublicKey(), rsaProofComponent.getRsaN());
                        return res;
                    } catch (Exception e) {
                        log.info(e.getMessage());
                    }
                    return null;
                })
//                Compute the product of all partial signatures^pk
                .reduce(lastClientProof, BigInteger::multiply)
                .mod(publicParameters.getFieldBase());
    }


    /**
     * This is the verify function from the RSA Threshold based construction
     * @param substationID an identifier for the substation which this computation is related to
     * @param result is given from the final Eval function
     * @param serverProof is given from the final Proof function
     * @param clientProofs is a list of all clients' proofs (tau from paper)
     * @return true: if product of client proofs == hash of the final eval
     *          AND if the product of clients' proofs == the server proof,
     *          false: otherwise
     */
    public boolean verify(int substationID, BigInteger result, BigInteger serverProof, List<BigInteger> clientProofs) {
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

    /**
     * This function computes sigma roof (i.e. solves Equation 2.9)
     * @param pk is rsa public key
     * @param clientProof is the clientProof in use
     * @param serverProofs is the partial signatures from the servers
     * @param rsaN the modulo used in the rsa signature
     * @param determinant is the determinant of the matrix A_is
     * @return the sigma roof
     */
    private BigInteger clientFinalProof(BigInteger pk, BigInteger clientProof, BigInteger[] serverProofs, BigInteger rsaN, double determinant) {
//      Compute the product of all server proofs
        BigInteger partial = Arrays.stream(serverProofs).reduce(ONE, BigInteger::multiply).mod(rsaN);
//        Preprocess the determinant
        BigInteger det = BigInteger.valueOf(Math.round(determinant));
//        Collect the sign of the determinant
        BigInteger detSign = det.divide(det.abs());
//        Compute alpha and beta with EEA
        BigInteger[] eeaResult = extendedEuclideanAlgorithm(det.multiply(BigInteger.TWO), pk);
        BigInteger alpha = eeaResult[0].multiply(detSign);
        BigInteger beta = eeaResult[1];
//        Compute the clients' rsa proof component
        BigInteger sigmaRoofAlpha = partial.modPow(alpha, rsaN);
        BigInteger tauBeta = clientProof.modPow(beta, rsaN);
        return sigmaRoofAlpha.multiply(tauBeta).mod(rsaN);
    }

    /**
     * Returns the Bézout coefficients and gcd.
     * All input will be mapped to positive numbers by absolute value function.
     * The return coefficient can be multiplied by the original sign of that parameter to restore correct sign.
     *
     * @return [coefficient of a, coefficient of b, gcd].
     */
    public static BigInteger[] extendedEuclideanAlgorithm(BigInteger a, BigInteger b) {
        //output "Bézout coefficients:", (s[1], t[1])
        //output "greatest common divisor:", r[1]
        //output "quotients by the gcd:", (t[0], s[0])
        BigInteger aPos = a.abs();
        BigInteger bPos = b.abs();
        BigInteger[] s = new BigInteger[]{ZERO, ONE};
        BigInteger[] t = new BigInteger[]{ONE, ZERO};
        BigInteger[] r = new BigInteger[]{bPos.max(aPos), bPos.min(aPos)};

        while (!r[0].equals(ZERO)) {
            BigInteger quotient = r[1].divide(r[0]);
            r = internalEEA(r, quotient);
            s = internalEEA(s, quotient);
            t = internalEEA(t, quotient);
        }

        if (bPos.max(aPos).equals(bPos)) {
            return new BigInteger[]{s[1], t[1], r[1]};
        } else {
            return new BigInteger[]{t[1], s[1], r[1]};
        }
    }

    private static BigInteger[] internalEEA(BigInteger[] values, BigInteger q) {
        return new BigInteger[]{values[1].subtract(q.multiply(values[0])), values[0]};
    }

}
