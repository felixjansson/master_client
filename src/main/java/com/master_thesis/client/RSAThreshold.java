package com.master_thesis.client;

import cc.redberry.rings.bigint.BigInteger;
import ch.qos.logback.classic.Logger;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.SingularOps_DDRM;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = (Logger) LoggerFactory.getLogger(RSAThreshold.class);

    private static final int KEY_BIT_LENGTH = 12;

    private BigInteger privateKey;
    private BigInteger publicKey;
    private BigInteger rsaN;
    private BigInteger fieldBase;
    private int securityThreshold;
    private BigInteger qPrime;
    private BigInteger pPrime;
    private BigInteger rsaNPrime;


    @Autowired
    public RSAThreshold(PublicParameters publicParameters) {
        super(publicParameters);
    }

    @Override
    public Map<URI, SecretShare> shareSecret(int int_secret) {
        generateRSAPrimes(32);
        Map<URI, SecretShare> shares = super.shareSecret(int_secret);
        fieldBase = publicParameters.getFieldBase(publicParameters.getTransformatorID());
        securityThreshold = publicParameters.getSecurityThreshold();
        SimpleMatrix matrixOfClient = generateMatrixOfClient();
        generateRSAKeys(BigInteger.valueOf(Math.round(matrixOfClient.rows(0, matrixOfClient.numCols()).determinant())));
        SimpleMatrix skv = generateSKVector();
        log.info("\nA: \n{}\nSKV:\n{}", matrixOfClient, skv);
        SimpleMatrix skShares = matrixOfClient.mult(skv);

        shares.forEach((uri, secretShare) -> {
            secretShare.setMatrixOfClient(matrixOfClient);
            secretShare.setSkShare(skShares); // TODO: 2020-03-11 Could be limited to 1 share to each server if they collaborate
            secretShare.setRsaN(rsaN);
            secretShare.setPublicKey(publicKey.intValue());
        });

        return shares;
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
        int m = publicParameters.getServers().size();
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
    void generateRSAPrimes(int n) {
        // TODO: 2020-03-04 Choose publicKey s.t. publicKey >> Combination(n,t)
        rsaNPrime = null;
        qPrime = BigInteger.valueOf(11);
        pPrime = BigInteger.valueOf(41);
        //pPrime = BigInteger.probablePrime(n / 2, random);
        //qPrime = BigInteger.probablePrime(n / 2, random);
        rsaNPrime = pPrime.multiply(qPrime);

        BigInteger p = pPrime.multiply(two).add(one);
        BigInteger q = qPrime.multiply(two).add(one);
        rsaN = p.multiply(q);
    }

    private void generateRSAKeys(BigInteger determinant) {
        publicKey = new BigInteger(KEY_BIT_LENGTH, 16, random); // 2^16 + 1 = 65537, large enough?
        while (!determinant.gcd(publicKey).equals(one)) {
            publicKey = new BigInteger(KEY_BIT_LENGTH, 16, random); // 2^16 + 1 = 65537, large enough?
        }
        privateKey = publicKey.modInverse(rsaNPrime);
        log.info("Keys: pk: {} sk: {}", publicKey, privateKey);
    }

}
