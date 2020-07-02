package com.master_thesis.client;

import com.master_thesis.client.data.*;
import com.master_thesis.client.util.NoiseGenerator;
import com.master_thesis.client.util.PublicParameters;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component("dp")
public class DifferentialPrivacy {

    private NoiseGenerator noiseGenerator;
    private PublicParameters publicParameters;
    private Random random;
    private HomomorphicHash homomorphicHash;

    @Autowired
    public DifferentialPrivacy(PublicParameters publicParameters, NoiseGenerator noiseGenerator, HomomorphicHash homomorphicHash) {
        this.publicParameters = publicParameters;
        this.noiseGenerator = noiseGenerator;
        this.homomorphicHash = homomorphicHash;
        random = new SecureRandom();
    }

    /**
     * This is the share secret function from the Homomorphic Hash construction.
     *
     * @param secret the input is the secret that should be shared.
     * @return An object with data that should be sent.
     */
    public DifferentialPrivacyData shareSecret(BigInteger secret, int substationID) {
        // Find the publicly available information used for this computation.
        BigInteger fieldBase = publicParameters.getFieldBase(substationID);
        BigInteger generator = publicParameters.getGenerator(substationID);
        secret = noiseGenerator.addNoise(secret);

        // Get a random nonce value, using a built in java pseudo random number generator (PRNG).
        BigInteger nonce = BigInteger.valueOf(random.nextLong()).mod(fieldBase);

        // The client proof, tau, is computed.
        BigInteger proofComponent = homomorphicHash.hash(fieldBase, secret.add(nonce), generator);

        // We generate a polynomial of order t. The numerical value of t is retrieved inside the function.
        Function<Integer, BigInteger> polynomial = homomorphicHash.generatePolynomial(secret, fieldBase, substationID);

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
            share = share.multiply(homomorphicHash.computeLagrangeCoefficient(number, polynomialInput));
            // Store the result in the map.
            shares.put(server.getUri().resolve(Construction.DP.getEndpoint()), share);
        });

        // Store the data that is needed later in the construction in an object and return it.
        // - The shares will be sent to the server
        // - The proof component is sent to the verifier
        // - The nonce is sent to a trusted party (the coordinator).
        return new DifferentialPrivacyData(shares, proofComponent, nonce);
    }



}
