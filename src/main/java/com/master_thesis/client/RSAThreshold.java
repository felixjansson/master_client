package com.master_thesis.client;

import cc.redberry.rings.bigint.BigInteger;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Map;

@Component("rsa")
public class RSAThreshold extends HomomorphicHash {
    private final static SecureRandom random = new SecureRandom();
    private final static BigInteger zero = BigInteger.ZERO;
    private final static BigInteger one = BigInteger.ONE;
    private final static BigInteger two = BigInteger.TWO;
    private BigInteger privateKey;
    private BigInteger publicKey;
    private BigInteger rsaN;
    private BigInteger fieldBase;
    private int securityThreshold;


    @Autowired
    public RSAThreshold(PublicParameters publicParameters) {
        super(publicParameters);
        generateRSAKeys(32);
    }

    @Override
    public Map<URI, SecretShare> shareSecret(int int_secret) {
        Map<URI, SecretShare> shares = super.shareSecret(int_secret);
        fieldBase = publicParameters.getFieldBase(publicParameters.getTransformatorID());
        securityThreshold = publicParameters.getSecurityThreshold();
        SimpleMatrix skv = generateSKVector();
        SimpleMatrix matrixOfClient = generateMatrixOfClient();
        SimpleMatrix skShares = matrixOfClient.mult(skv);
        putElementsInField(skShares);
        shares.forEach((uri, secretShare) -> {
            secretShare.setMatrixOfClient(matrixOfClient);
            secretShare.setSkShare(skShares);
        });

        return shares;
    }

    private void putElementsInField(SimpleMatrix skShares) {
        for (int i = 0; i < skShares.numRows(); i++) {
            skShares.set(i, 0, skShares.get(i) % fieldBase.intValue());
        }
    }

    private SimpleMatrix generateSKVector() {
        SimpleMatrix skv = new SimpleMatrix(securityThreshold, 1);
        for (int i = 1; i < securityThreshold; i++) {
            int value = getRandomElementInField();
            skv.set(i, 0, value);
        }
        skv.set(0, 0, privateKey.intValue());

        return skv;

    }

    // TODO: 2020-03-09 What should we do when 'm' or 't' is zero? 
    private SimpleMatrix generateMatrixOfClient() {
        int m = 5;//publicParameters.getServers().size();
        DMatrixRMaj dMatrixRMaj = new DMatrixRMaj(m, securityThreshold);
        boolean isFullRank = false;
        while ((m != 0 && securityThreshold != 0) && !isFullRank) {
            for (int row = 0; row < m; row++) {
                for (int col = 0; col < securityThreshold; col++) {
                    int value = getRandomElementInField();
                    dMatrixRMaj.set(row, col, value);
                }
            }
            int rank = SingularOps_DDRM.rank(dMatrixRMaj);
            isFullRank = rank == Math.min(securityThreshold, m);
        }
        return new SimpleMatrix(dMatrixRMaj);
    }

    private int getRandomElementInField() {
        return Math.abs(random.nextInt()) % fieldBase.intValue();
    }


    // TODO: 2020-03-04 What happens if our message >= rsaN? Verify that the rsaN is exactly n bits.
    private void generateRSAKeys(int n) {
        // TODO: 2020-03-04 Choose publicKey s.t. publicKey >> Combination(n,t)
        publicKey = new BigInteger("7"); // 2^16 + 1 = 65537, large enough?
        BigInteger nPrime = null;
        BigInteger qPrime = null;
        BigInteger pPrime = null;
        boolean isCoprime = false;
        while (nPrime == null || !isCoprime) {
            pPrime = BigInteger.probablePrime(n / 2, random);
            qPrime = BigInteger.probablePrime(n / 2, random);
            nPrime = pPrime.multiply(qPrime);
            isCoprime = gcd(nPrime, publicKey).equals(one);
        }

        BigInteger p = pPrime.multiply(two).add(one);
        BigInteger q = qPrime.multiply(two).add(one);
        rsaN = p.multiply(q);
        privateKey = publicKey.modInverse(nPrime);
    }


    private BigInteger gcd(BigInteger a, BigInteger b) {
        if (b.equals(zero)) {
            return a;
        }
        return gcd(b, a.mod(b));
    }

}
