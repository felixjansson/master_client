package com.master_thesis.client;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;

@Component("rsa")
public class RSAThreshold extends HomomorphicHash {
    private final static SecureRandom random = new SecureRandom();
    private final static BigInteger one = BigInteger.ONE;

    private static final int KEY_BIT_LENGTH = 12;
    private static final int RSA_PRIME_BIT_LENGTH = 12;

    private BigInteger privateKey;
    private BigInteger publicKey;
    private BigInteger rsaN;
    private int securityThreshold;
    private BigInteger rsaNPrime;


    @Autowired
    public RSAThreshold(PublicParameters publicParameters) {
        super(publicParameters);
    }

    @Override
    public ShareInformation shareSecret(int int_secret) {
        ShareInformation sharesInfo = super.shareSecret(int_secret);
        Map<URI, ServerShare> shares = sharesInfo.getServerShares();
        BigInteger fieldBase = publicParameters.getFieldBase(publicParameters.getSubstationID());
        securityThreshold = publicParameters.getSecurityThreshold();
        generateRSAPrimes(fieldBase);
        SimpleMatrix matrixOfClient = generateMatrixOfClient(fieldBase);
        generateRSAKeys(BigInteger.valueOf(Math.round(matrixOfClient.rows(0, matrixOfClient.numCols()).determinant())));
        SimpleMatrix skv = generateSKVector(fieldBase);
        SimpleMatrix skShares = matrixOfClient.mult(skv);

        shares.forEach((uri, secretShare) -> {
            secretShare.setMatrixOfClient(matrixOfClient);
            secretShare.setSkShare(skShares);
            secretShare.setRsaN(rsaN);
            secretShare.setPublicKey(publicKey.intValue());
        });

        return sharesInfo;
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
