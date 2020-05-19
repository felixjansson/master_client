package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.master_thesis.client.data.Construction;
import com.master_thesis.client.data.DefaultPublicData;
import com.master_thesis.client.data.LinearSignatureData;
import com.master_thesis.client.data.LinearSignatureData.PublicData;
import com.master_thesis.client.data.LinearSignatureData.ServerData;
import com.master_thesis.client.data.Server;
import com.master_thesis.client.util.PublicParameters;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class LinearSignature {
    private static final Logger log = (Logger) LoggerFactory.getLogger(LinearSignature.class);
    private PublicParameters publicParameters;
    private DefaultPublicData defaultPublicData;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public LinearSignature(PublicParameters publicParameters, DefaultPublicData defaultPublicData) {
        this.publicParameters = publicParameters;
        this.defaultPublicData = defaultPublicData;
    }

    /**
     * This is the share secret function from the Linear Signature construction.
     *
     * @param int_secret the input is the secret that should be shared.
     * @return An object with data that should be sent.
     */
    public LinearSignatureData shareSecret(int int_secret, int fid) {
        // Find the publicly available information used for this computation.
        int substationID = publicParameters.getSubstationID();
        PublicData data = publicParameters.getLinearPublicData(substationID, fid);
        BigInteger secret = BigInteger.valueOf(int_secret);

        // Get a random nonce value, using a built in java pseudo random number generator (PRNG).
        BigInteger nonce = BigInteger.valueOf(random.nextLong()).mod(data.getN());
        log.info("base: {}, secret: {}, nonce: {}", data.getN(), secret, nonce);

        // We generate a polynomial of order t. The numerical value of t is retrieved inside the function.
        Function<Integer, BigInteger> polynomial = generatePolynomial(int_secret, data.getN());

        // We retrieve the list of servers that will be used in the computation.
        List<Server> servers = publicParameters.getServers();

        // A range of 1 - the number of servers are created to make use that the input to the polynomial is unique
        // and that the Lagrange Basis Coefficient will be integer.
        Set<BigInteger> polynomialInput = IntStream.range(1, servers.size() + 1).mapToObj(BigInteger::valueOf).collect(Collectors.toSet());
        Iterator<BigInteger> iteratorPolyInput = polynomialInput.iterator();

        // Here we create a map (dict in python) that relates each server to its share.
        Map<URI, ServerData> shares = new HashMap<>();
        servers.forEach(server -> {
            BigInteger number = iteratorPolyInput.next();
            // Compute the polynomial with a unique value as input.
            BigInteger share = polynomial.apply(number.intValue());
            // Multiply it with the Lagrange Coefficient.
            if (defaultPublicData.isExternalLagrange()) {
                share = share.multiply(defaultPublicData.getLagrangeValue(number));
            } else {
                share = share.multiply(computeLagrangeCoefficient(number, polynomialInput));
            }
            // Store the result in the map.
            shares.put(server.getUri().resolve(Construction.LINEAR.getEndpoint()), new ServerData(share));
        });

        // Store the data that is needed later in the construction in an object and return it.
        // - The shares will be sent to the server
        // - The nonce is sent to a trusted party (the coordinator).
        return new LinearSignatureData(shares, nonce);
    }


    /**
     * This is the partial proof function from the Linear Signature construction.
     * @param data is the object containing the already computed secret shares.
     * @param secret is the secret of the client.
     * @return the data object, but now including the partial proof.
     */
    public LinearSignatureData partialProof(LinearSignatureData data, int secret) {

        // We retrieve the public information.
        PublicData publicData = publicParameters.getLinearPublicData(data.getSubstationID(), data.getFid());
        log.debug("PublicParameters: {}", publicData);
        BigInteger eN = publicData.getN().multiply(publicData.getFidPrime());
        BigInteger s = new BigInteger(eN.bitLength(), random).mod(eN);

        // Compute the xi,R with nonce and secret
        BigInteger xR = data.getNonceData().getNonce().add(BigInteger.valueOf(secret));

        // x^(eN) = {g^s * PRODUCT( h[j]^f[j,i] ) * g1^(xR)} mod nRoof
        BigInteger xeN = publicData.getG1().modPow(s, publicData.getNRoof())
                .multiply(publicData.getH()[data.getClientID()])
                .multiply(publicData.getG2().modPow(xR, publicData.getNRoof()))
                .mod(publicData.getNRoof());

        // Solve for x, using z as the inverse of eN in mod( totient(NRoof) )
        // (eN)z = 1 mod totient(NRoof) ==> (x^(eN))^z = x mod NRoof.
        BigInteger totientNRoof = publicData.getSk()[0].subtract(BigInteger.ONE).multiply(publicData.getSk()[1].subtract(BigInteger.ONE));
        BigInteger eNInverse = publicData.getN().multiply(publicData.getFidPrime()).modInverse(totientNRoof);

        // x = x^(eN)^(eNInverse) mod (NRoof)
        BigInteger x = xeN.modPow(eNInverse, publicData.getNRoof());

        // Add the computed value to the data object and return it, now including the partial proof (sigma).
        data.setVerifierData(publicData.getFidPrime(), s, x);
        return data;
    }

    /**
     * This function creates a polynomial that will take an input and return the result.
     * @param secret This will be the result for input = 0.
     * @param field The polynomial is computed mod field.
     * @return A function that can take 1 value as input.
     */
    protected Function<Integer, BigInteger> generatePolynomial(int secret, BigInteger field) {
        // Retrieve the t parameter for the construction.
        int t = publicParameters.getSecurityThreshold();

        StringBuilder logString = new StringBuilder("Polynomial used: ").append(secret);

        // We define a list of coefficients, a's.
        ArrayList<BigInteger> coefficients = new ArrayList<>();
        for (int i = 1; i <= t; i++) {
            BigInteger a;
            do {
                a = new BigInteger(field.bitLength(), random).mod(field);
            } while (a.equals(BigInteger.ZERO) || a.compareTo(field) >= 0);
            logString.append(" + ").append(a).append("x^").append(i);
            coefficients.add(a);
        }
        log.info(logString.toString());

        // Here we define the function that is returned.
        return (input) -> {
            // Note that everything here defines the polynomial.
            BigInteger bigIntInput = BigInteger.valueOf(input);
            // We begin with the secret as x_i + ...
            BigInteger res = BigInteger.valueOf(secret);
            // That is then continued by ... + a[i] * input^i + ...
            for (int i = 0; i < coefficients.size(); i++) {
                BigInteger coefficient = coefficients.get(i);
                BigInteger polynomial = bigIntInput.pow(i + 1);
                res = res.add(coefficient.multiply(polynomial));
            }
            // Finally, we can return the result.
            return res;
        };
    }

    /**
     * In this function a Lagrange Basis Coefficient is computed.
     * @param currentValue The value for which to compute the Lagrange coefficient.
     * @param potentialValues All input values to the polynomial.
     * @return The result of the computation. A single value.
     */
    public BigInteger computeLagrangeCoefficient(BigInteger currentValue, Set<BigInteger> potentialValues){
        // Compute the nominator, the product of all inputs except for the one that is the currentValue.
        BigInteger nominator = potentialValues.stream()
                .filter(x -> !x.equals(currentValue))
                .reduce(BigInteger.ONE, BigInteger::multiply);
        // Compute the denominator, the product of all inputs minus the current value, except for the input that is the currentValue.
        BigInteger denominator = potentialValues.stream()
                .filter(x -> !x.equals(currentValue))
                .reduce(BigInteger.ONE, (prev, x) -> prev.multiply(x.subtract(currentValue)));
        log.debug("beta values: {}/{} = {}", nominator, denominator, nominator.divideAndRemainder(denominator));
        // Return the nominator divided by the denominator.
        return nominator.divide(denominator);
    }

}
