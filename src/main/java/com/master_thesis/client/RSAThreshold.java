package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
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

    public ShareInformation shareSecret(int int_secret) {
        // Collect public information
        int substationID = publicParameters.getSubstationID();
        BigInteger base = publicParameters.getFieldBase(substationID);
        BigInteger generator = publicParameters.getGenerator(substationID);
        securityThreshold = publicParameters.getSecurityThreshold();
        BigInteger fieldBase = new BigInteger(base.toString());
        BigInteger secret = BigInteger.valueOf(int_secret);
        List<Server> servers = publicParameters.getServers();
        Set<Integer> serverIDs = servers.stream().map(Server::getServerID).collect(Collectors.toSet());

        // Compute proofComponent (tau), create a polynomial and nonce
        BigInteger nonce = BigInteger.valueOf(random.nextLong());
        BigInteger proofComponent = hash(base, secret.add(nonce), generator);
        Function<Integer, BigInteger> polynomial = generatePolynomial(int_secret, fieldBase);

        log.info("base: {}, generator: {}, secret: {}, nonce: {}", base, generator, secret, nonce);

        //RSA components
        generateRSAPrimes(fieldBase);
        SimpleMatrix matrixOfClient = generateMatrixOfClient(fieldBase);
        generateRSAKeys(BigInteger.valueOf(Math.round(matrixOfClient.rows(0, matrixOfClient.numCols()).determinant())));
        SimpleMatrix skv = generateSKVector(fieldBase);
        SimpleMatrix skShares = matrixOfClient.mult(skv);

        // Pack the information which should be sent.
        HashMap<URI, ServerShare> map = new HashMap<>();
        servers.forEach(server -> {
            // Compute the server's share
            BigInteger share = polynomial.apply(server.getServerID());
            share = share.multiply(BigInteger.valueOf(beta(server.getServerID(), serverIDs)));
            map.put(server.getUri(), new ServerShare(share, proofComponent, matrixOfClient, skShares, rsaN, publicKey.intValue()));
        });
        return new ShareInformation(map, nonce, proofComponent);
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

    public int beta(int serverID, Set<Integer> serverIDs) {
        return (int) Math.round(serverIDs.stream().mapToDouble(Integer::doubleValue).reduce(1f, (prev, j) -> {
            if (j == serverID) {
                return prev;
            } else {
                return prev * (j / (j - serverID));
            }
        }));
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
