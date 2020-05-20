package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.master_thesis.client.data.Construction;
import com.master_thesis.client.data.DefaultPublicData;
import com.master_thesis.client.data.HomomorphicHashData;
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


@Component("hash")
public class HomomorphicHash {

    protected PublicParameters publicParameters;
    private DefaultPublicData defaultPublicData;
    private static final Logger log = (Logger) LoggerFactory.getLogger(HomomorphicHash.class);
    private final SecureRandom random;

    @Autowired
    public HomomorphicHash(PublicParameters publicParameters, DefaultPublicData defaultPublicData) {
        this.publicParameters = publicParameters;
        this.defaultPublicData = defaultPublicData;
        random = new SecureRandom();
    }

    /**
     * This is the share secret function from the Homomorphic Hash construction.
     *
     * @param secret the input is the secret that should be shared.
     * @return An object with data that should be sent.
     */
    public HomomorphicHashData shareSecret(BigInteger secret) {
        // Find the publicly available information used for this computation.
        int substationID = publicParameters.getSubstationID();
        BigInteger fieldBase = publicParameters.getFieldBase(substationID);
        BigInteger generator = publicParameters.getGenerator(substationID);

        // Get a random nonce value, using a built in java pseudo random number generator (PRNG).
        BigInteger nonce = BigInteger.valueOf(random.nextLong()).mod(fieldBase);
        log.info("base: {}, generator: {}, secret: {}, nonce: {}", fieldBase, generator, secret, nonce);

        // The client proof, tau, is computed.
        BigInteger proofComponent = hash(fieldBase, secret.add(nonce), generator);

        // We generate a polynomial of order t. The numerical value of t is retrieved inside the function.
        Function<Integer, BigInteger> polynomial = generatePolynomial(secret, fieldBase);

        // We retrieve the list of servers that will be used in the computation.
        List<Server> servers = publicParameters.getServers();

        // A range of 1 - the number of servers are created to make use that the input to the polynomial is unique
        // and that the Lagrange Basis Coefficient will be integer.
        Set<BigInteger> polynomialInput = IntStream.range(1,servers.size() + 1).mapToObj(BigInteger::valueOf).collect(Collectors.toSet());
        Iterator<BigInteger> iteratorPolyInput = polynomialInput.iterator();

        // Here we create a map (dict in python) that relates each server to its share.
        Map<URI, BigInteger> shares = new HashMap<>();
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
            shares.put(server.getUri().resolve(Construction.HASH.getEndpoint()), share);
        });

        // Store the data that is needed later in the construction in an object and return it.
        // - The shares will be sent to the server
        // - The proof component is sent to the verifier
        // - The nonce is sent to a trusted party (the coordinator).
        return new HomomorphicHashData(shares, proofComponent, nonce);
    }


    /**
     * This function creates a polynomial that will take an input and return the result.
     * @param secret This will be the result for input = 0.
     * @param field The polynomial is computed mod field.
     * @return A function that can take 1 value as input.
     */
    protected Function<Integer, BigInteger> generatePolynomial(BigInteger secret, BigInteger field) {
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
            BigInteger res = secret;
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

    public BigInteger hash(BigInteger field, BigInteger input, BigInteger g) {
        return g.modPow(input, field);
    }

    /**
     * In this function a Lagrange Basis Coefficient is computed.
     * @param currentValue The value for which to compute the Lagrange cofficient.
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
