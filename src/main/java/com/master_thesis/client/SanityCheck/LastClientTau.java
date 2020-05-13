package com.master_thesis.client.SanityCheck;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

@Component
public class LastClientTau {

    private static final Logger log = (Logger) LoggerFactory.getLogger(LastClientTau.class);
    private ObjectMapper objectMapper = new ObjectMapper();


    BigInteger computeLastTau(BigInteger nonceSum, BigInteger fieldBase, BigInteger generator) {
        return hash(computeNonceInverse(nonceSum, new BigInteger[]{fieldBase}), fieldBase, generator);
    }

    public BigInteger computeNonceInverse(BigInteger nonceSum, BigInteger[] fieldPrimes) {
        BigInteger fieldBase = Arrays.stream(fieldPrimes).reduce(BigInteger.ONE, BigInteger::multiply);
        BigInteger totient = eulerTotient(fieldPrimes);
        BigDecimal sum = new BigDecimal(nonceSum);
        BigDecimal tot = new BigDecimal(totient);
        BigInteger ceil = sum.divide(tot, RoundingMode.CEILING).toBigInteger();
        return totient.multiply(ceil).subtract(nonceSum).mod(fieldBase);
    }


    private BigInteger eulerTotient(BigInteger prime) {
        if (!prime.isProbablePrime(16)) {
            throw new RuntimeException("No prime, no totient");
        }
        return prime.subtract(BigInteger.ONE);
    }

    private BigInteger eulerTotient(BigInteger[] sk) {
        return Arrays.stream(sk).map(this::eulerTotient).reduce(BigInteger.ONE, BigInteger::multiply);
    }

    public BigInteger hash(BigInteger input, BigInteger fieldBase, BigInteger generator) {
        return generator.modPow(input, fieldBase);
    }

    public BigInteger getRn(BigInteger nonceSum, BigInteger[] sk) {
        return computeNonceInverse(nonceSum, sk);
    }
}
