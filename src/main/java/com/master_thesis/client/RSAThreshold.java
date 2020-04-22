package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.master_thesis.client.data.Construction;
import com.master_thesis.client.data.RSAThresholdData;
import com.master_thesis.client.data.RSAThresholdData.NonceData;
import com.master_thesis.client.data.RSAThresholdData.ServerData;
import com.master_thesis.client.data.RSAThresholdData.VerifierData;
import com.master_thesis.client.data.Server;
import com.master_thesis.client.util.PublicParameters;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SingularOps_DDRM;
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


    private static final int KEY_BIT_LENGTH = 12;
    private static final int RSA_PRIME_BIT_LENGTH = 12;

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

    public RSAThresholdData shareSecret(int int_secret) {
        // Collect public information
        int substationID = publicParameters.getSubstationID();
        BigInteger fieldBase = publicParameters.getFieldBase(substationID);
        BigInteger generator = publicParameters.getGenerator(substationID);
        securityThreshold = publicParameters.getSecurityThreshold();
        BigInteger secret = BigInteger.valueOf(int_secret);
        List<Server> servers = publicParameters.getServers();
        if (servers.isEmpty())
            throw new RuntimeException("No servers available, the computation can not be performed");
        Set<Integer> serverIDs = servers.stream().map(Server::getServerID).collect(Collectors.toSet());

        // Compute proofComponent (tau), create a polynomial and nonce
        BigInteger nonce = BigInteger.valueOf(random.nextLong()).mod(fieldBase);
        BigInteger proofComponent = hash(fieldBase, secret.add(nonce), generator);
        Function<Integer, BigInteger> polynomial = generatePolynomial(int_secret, fieldBase);

        log.info("base: {}, generator: {}, secret: {}, nonce: {}", fieldBase, generator, secret, nonce);

        //RSA components
        generateRSAPrimes(fieldBase);
        SimpleMatrix matrixOfClient = generateMatrixOfClient(fieldBase);
        generateRSAKeys(BigInteger.valueOf(Math.round(matrixOfClient.rows(0, matrixOfClient.numCols()).determinant())));
        SimpleMatrix skv = generateSKVector(fieldBase);
        SimpleMatrix skShares = matrixOfClient.mult(skv);

        // Pack the information which should be sent.
        Set<Integer> polynomialInput = IntStream.range(1, servers.size() + 1).boxed().collect(Collectors.toSet());
        Iterator<Integer> iteratorPolyInput = polynomialInput.iterator();
        HashMap<URI, ServerData> shares = new HashMap<>();
        servers.forEach(server -> {
            // Compute the server's share
            int number = iteratorPolyInput.next();
            BigInteger share = polynomial.apply(number);
            share = share.multiply(beta(number, polynomialInput));
            shares.put(server.getUri().resolve(Construction.RSA.getEndpoint()), new ServerData(share, proofComponent, matrixOfClient, skShares, rsaN));
        });
        return new RSAThresholdData(shares, new VerifierData(proofComponent, publicKey), new NonceData(nonce));
    }

    protected Function<Integer, BigInteger> generatePolynomial(int secret, BigInteger field) {

        StringBuilder logString = new StringBuilder("Polynomial used: ").append(secret);
        ArrayList<BigInteger> coefficients = new ArrayList<>();
        for (int i = 1; i <= securityThreshold; i++) {
            BigInteger a;
            do {
                a = new BigInteger(field.bitLength(), random).mod(field);
            } while (a.equals(BigInteger.ZERO) || a.compareTo(field) >= 0);
            logString.append(" + ").append(a).append("x^").append(i);
            coefficients.add(a);
        }
        log.info(logString.toString());

        return (serverID) -> {
            BigInteger serverIDBIG = BigInteger.valueOf(serverID);
            BigInteger res = BigInteger.valueOf(secret);
            for (int i = 0; i < coefficients.size(); i++) {
                BigInteger coefficient = coefficients.get(i);
                BigInteger polynomial = serverIDBIG.pow(i + 1);
                res = res.add(coefficient.multiply(polynomial));
            }
            return res;
        };
    }

    public BigInteger hash(BigInteger field, BigInteger input, BigInteger g) {
        return g.modPow(input, field);
    }

    public BigInteger beta(int currentValue, Set<Integer> potentialValues) {
        BigInteger cv = BigInteger.valueOf(currentValue);
        BigInteger nominator = potentialValues.stream().map(BigInteger::valueOf)
                .filter(x -> !x.equals(cv))
                .reduce(BigInteger.ONE, BigInteger::multiply);
        BigInteger denominator = potentialValues.stream().map(BigInteger::valueOf)
                .filter(x -> !x.equals(cv))
                .reduce(BigInteger.ONE, (prev, x) -> prev.multiply(x.subtract(cv)));
        log.debug("beta values: {}/{} = {}", nominator, denominator, nominator.divideAndRemainder(denominator));
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
        DMatrixRMaj dMatrixRMaj = new DMatrixRMaj(m, securityThreshold);
        boolean isFullRank = false;
        while ((m != 0 && securityThreshold != 0) && !isFullRank) {
            for (int row = 0; row < m; row++) {
                for (int col = 0; col < securityThreshold; col++) {
                    int value = getRandomElementInField(fieldBase);
                    dMatrixRMaj.set(row, col, value);
                }
            }
            int rank = SingularOps_DDRM.rank(dMatrixRMaj);
            isFullRank = rank == Math.min(securityThreshold, m);
        }
        return new SimpleMatrix(dMatrixRMaj);
    }

    private int getRandomElementInField(BigInteger fieldBase) {
        return Math.abs(random.nextInt()) % fieldBase.intValue();
    }


    void generateRSAPrimes(BigInteger fieldBase) {
        BigInteger[] pPair = generateConstrainedSafePrimePair(fieldBase, new BigInteger[]{});
        BigInteger[] qPair = generateConstrainedSafePrimePair(fieldBase, pPair);
        rsaNPrime = pPair[0].multiply(qPair[0]);
        rsaN = pPair[1].multiply(qPair[1]);
    }

    private BigInteger[] generateConstrainedSafePrimePair(BigInteger minValue, BigInteger[] forbiddenValues) {
        BigInteger[] pair;
        boolean isSmallerThanMinValue, isForbidden;
        do {
            pair = generateSafePrimePair();
            isSmallerThanMinValue = pair[1].max(minValue).equals(minValue);
            isForbidden = Arrays.equals(pair, forbiddenValues);
        } while (isForbidden || isSmallerThanMinValue);
        return pair;
    }

    private BigInteger[] generateSafePrimePair() {
        BigInteger p, q;
        do {
            p = new BigInteger(RSA_PRIME_BIT_LENGTH, 16, random);
            q = p.subtract(BigInteger.ONE).divide(BigInteger.TWO);
        } while (!q.isProbablePrime(16));
        return new BigInteger[]{q, p};
    }

    private void generateRSAKeys(BigInteger determinant) {
        BigInteger rsaNPrimeTwo = rsaNPrime.multiply(BigInteger.TWO);
        do {
            // Generate a public key with gcd=1 with determinant and 2p'q'
            publicKey = new BigInteger(KEY_BIT_LENGTH, 16, random);
        } while (!determinant.gcd(publicKey).equals(one) || !rsaNPrimeTwo.gcd(publicKey).equals(BigInteger.ONE));
        privateKey = publicKey.modInverse(rsaNPrimeTwo);
    }

}
