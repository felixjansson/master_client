package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.master_thesis.client.data.Construction;
import com.master_thesis.client.data.RSAThresholdData;
import com.master_thesis.client.data.RSAThresholdData.NonceData;
import com.master_thesis.client.data.RSAThresholdData.ServerData;
import com.master_thesis.client.data.RSAThresholdData.VerifierData;
import com.master_thesis.client.data.Server;
import com.master_thesis.client.util.PublicParameters;
import org.ejml.simple.SimpleMatrix;
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

@Component("rsa")
public class RSAThreshold {
    private final static SecureRandom random = new SecureRandom();
    private final static BigInteger one = BigInteger.ONE;
    private static final Logger log = (Logger) LoggerFactory.getLogger(RSAThreshold.class);

    private BigInteger privateKey;
    private BigInteger publicKey;
    private BigInteger rsaN;
    private int securityThreshold;
    private BigInteger rsaNPrime;
    private PublicParameters publicParameters;



    @Autowired
    public RSAThreshold(PublicParameters publicParameters) {
        this.publicParameters = publicParameters;
    }

    /**
     * This is the share secret function from the Threshold Signature construction.
     *
     * @param secret the input is the secret that should be shared.
     * @return An object with data that should be sent.
     */
    public RSAThresholdData shareSecret(BigInteger secret, int substationID) {
        // Find the publicly available information used for this computation.
        BigInteger fieldBase = publicParameters.getFieldBase(substationID);
        BigInteger generator = publicParameters.getGenerator(substationID);
        securityThreshold = publicParameters.getSecurityThreshold(substationID);

        // Here we generate the primes, matrix and secret/public keys that will be used in this computation.
        BigInteger[] rsaNValues = publicParameters.getRsaN(substationID);
        rsaN = rsaNValues[0];
        rsaNPrime = rsaNValues[1];
        SimpleMatrix matrixOfClient = generateMatrixOfClient(fieldBase);
        BigInteger determinant = BigInteger.valueOf(Math.round(matrixOfClient.rows(0, matrixOfClient.numCols()).determinant()));
        if (determinant.equals(BigInteger.ZERO))
            throw new RuntimeException("ERROR: The determinant of matrixOfClient is zero");
        generateRSAKeys(determinant);
        SimpleMatrix skv = generateSKVector(fieldBase);
        SimpleMatrix skShares = matrixOfClient.mult(skv);

        // *** The rest of the function is very similar to Homomorphic Hash Secret Share. ***

        // Get a random nonce value, using a built in java pseudo random number generator (PRNG).
        BigInteger nonce = BigInteger.valueOf(random.nextLong()).mod(fieldBase);
        log.info("base: {}, generator: {}, secret: {}, nonce: {}", fieldBase, generator, secret, nonce);

        // The client proof, tau, is computed.
        BigInteger proofComponent = hash(fieldBase, secret.add(nonce), generator);

        // We generate a polynomial of order t. The numerical value of t is retrieved inside the function.
        Function<Integer, BigInteger> polynomial = generatePolynomial(secret, fieldBase, substationID);

        // We retrieve the list of servers that will be used in the computation.
        List<Server> servers = publicParameters.getServers();
        if (servers.isEmpty())
            throw new RuntimeException("No servers available, the computation can not be performed");

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
            share = share.multiply(computeLagrangeCoefficient(number, polynomialInput));
            // Store the result in the map together with the RSA information.
            shares.put(server.getUri().resolve(Construction.RSA.getEndpoint()), new ServerData(share, proofComponent, matrixOfClient, skShares, rsaN));
        });

        // Store the data that is needed later in the construction in an object and return it.
        // - The shares will be sent to the server
        // - The verifier data (proof component Tau and RSA public key) is sent to the verifier
        // - The nonce is sent to a trusted party (the coordinator).
        return new RSAThresholdData(shares, new VerifierData(proofComponent, publicKey), new NonceData(nonce));
    }

    /**
     * This function creates a polynomial that will take an input and return the result.
     *
     * @param secret This will be the result for input = 0.
     * @param field  The polynomial is computed mod field.
     * @return A function that can take 1 value as input.
     */
    protected Function<Integer, BigInteger> generatePolynomial(BigInteger secret, BigInteger field, int substationID) {
        // Retrieve the t parameter for the construction.
        int t = publicParameters.getSecurityThreshold(substationID);

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
     *
     * @param currentValue    The value for which to compute the Lagrange cofficient.
     * @param potentialValues All input values to the polynomial.
     * @return The result of the computation. A single value.
     */
    public BigInteger computeLagrangeCoefficient(BigInteger currentValue, Set<BigInteger> potentialValues) {
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

    private SimpleMatrix generateSKVector(BigInteger fieldBase) {
        SimpleMatrix skv = new SimpleMatrix(securityThreshold, 1);
        for (int i = 1; i < securityThreshold; i++) {
            int value = getRandomElementInField(fieldBase);
            skv.set(i, 0, value);
        }
        skv.set(0, 0, privateKey.intValue());
        return skv;
    }

    // TODO: 2020-03-09 What should we do when 'm' or 't' is zero? 
    private SimpleMatrix generateMatrixOfClient(BigInteger fieldBase) {
        int m = publicParameters.getServers().size();
        SimpleMatrix matrixOfClient = null;
        BigInteger determinant = BigInteger.ZERO;
        double[][] internalMatrix = new double[m][securityThreshold];
        boolean isFullRank = false;
        while ((m != 0 && securityThreshold != 0) && !isFullRank || determinant.equals(BigInteger.ZERO)) {
            for (int row = 0; row < m; row++) {
                for (int col = 0; col < securityThreshold; col++) {
                    int value = getRandomElementInField(fieldBase);
                    internalMatrix[row][col] = value;
                }
            }
            int rank = rankOfMatrix(Arrays.stream(internalMatrix).map(double[]::clone).toArray(double[][]::new));
            isFullRank = rank == Math.min(securityThreshold, m);
            matrixOfClient = new SimpleMatrix(internalMatrix);
            determinant = BigInteger.valueOf(Math.round(matrixOfClient.rows(0, matrixOfClient.numCols()).determinant()));
        }
        log.debug("Determinant of matrix: {}", determinant);
        return matrixOfClient;
    }

    private int getRandomElementInField(BigInteger fieldBase) {
        return Math.abs(random.nextInt()) % fieldBase.intValue();
    }

    private void generateRSAKeys(BigInteger determinant) {
        BigInteger rsaNPrimeTwo = rsaNPrime.multiply(BigInteger.TWO);
        do {
            // Generate a public key with gcd=1 with determinant and 2p'q'
            publicKey = new BigInteger(rsaNPrimeTwo.bitLength(), 16, random);
        } while ((!determinant.gcd(publicKey).equals(one) || !rsaNPrimeTwo.gcd(publicKey).equals(BigInteger.ONE)));
        privateKey = publicKey.modInverse(rsaNPrimeTwo);
    }

    private void swap(double[][] mat, int row1, int row2, int col) {
        for (int i = 0; i < col; i++) {
            double temp = mat[row1][i];
            mat[row1][i] = mat[row2][i];
            mat[row2][i] = temp;
        }
    }

    private int rankOfMatrix(double[][] mat) {
        int rank = mat[0].length;
        for (int row = 0; row < rank; row++) {
            if (mat[row][row] != 0) {
                for (int col = 0; col < mat.length; col++) {
                    if (col != row) {
                        double mult = mat[col][row] / mat[row][row];
                        for (int i = 0; i < rank; i++)
                            mat[col][i] -= mult * mat[row][i];
                    }
                }
            } else {
                boolean reduce = true;
                for (int i = row + 1; i < mat.length; i++) {
                    if (mat[i][row] != 0) {
                        swap(mat, row, i, rank);
                        reduce = false;
                        break;
                    }
                }
                if (reduce) {
                    rank--;
                    for (int i = 0; i < mat.length; i++)
                        mat[i][row] = mat[i][rank];
                }
                row--;
            }
        }
        return rank;
    }

}
