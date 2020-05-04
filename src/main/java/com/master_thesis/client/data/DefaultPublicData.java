package com.master_thesis.client.data;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;

@Component
@PropertySource("classpath:values.properties")
public class DefaultPublicData {

    @Value("${numberOfServers}")
    private int numberOfServers;
    //    private BigInteger localFieldBase = BigInteger.valueOf(2011);
    @Value("${fieldBase_bits}")
    private int fieldBase_bits;
    @Value("${generator_bits}")
    private int generator_bits;
    @Value("${t_secure}")
    private int tSecure;
    private LinearSignatureData.PublicData linearSignatureData;

    private final SecureRandom random = new SecureRandom();
    private static final Logger log = (Logger) LoggerFactory.getLogger(DefaultPublicData.class);
    @Value("${k}")
    private int PRIME_BIT_LENGTH = 64;
    @Value("${k_prime}")
    private int PRIME_BIT_LENGTH_PRIME = 64;
    @Value("${runs}")
    private int runTimes;
    @Value("${construction}")
    private int construction;
    private BigInteger fieldBase;
    private BigInteger generator;

    public int getRunTimes() {
        return runTimes;
    }

    public int getNumberOfServers() {
        return numberOfServers;
    }

    public void setNumberOfServers(int numberOfServers) {
        this.numberOfServers = numberOfServers;
    }

    public BigInteger getFieldBase() {
        if (fieldBase == null) {
            do {
                fieldBase = new BigInteger(fieldBase_bits, 16, random);
            } while (fieldBase.bitLength() != fieldBase_bits);
        }
        return fieldBase;
    }

    public void setFieldBase(BigInteger fieldBase) {
        this.fieldBase = fieldBase;
    }

    public BigInteger getGenerator() {
        if (generator == null) {
            do {
                generator = new BigInteger(generator_bits, 16, random);
            } while (generator.bitLength() != generator_bits);
        }
        return fieldBase;
    }

    public void setGenerator(BigInteger generator) {
        this.generator = generator;
    }

    public int gettSecure() {
        return tSecure;
    }

    public void settSecure(int tSecure) {
        this.tSecure = tSecure;
    }

    @Override
    public String toString() {
        return "Default values [" +
                " Servers: " + numberOfServers +
                ", FieldBase: " + fieldBase +
                ", Generator: " + generator +
                ", TSecure: " + tSecure +
                ", Prime bits: " + PRIME_BIT_LENGTH +
                " ]";
    }


    private BigInteger[] generateHVector(int numberOfClients, BigInteger nRoof) {
        BigInteger[] h = new BigInteger[numberOfClients];
        Arrays.fill(h, generateRandomBigInteger(nRoof));
        return h;
    }

    /**
     * This is the setup from the paper.
     */
    public void generateLinearSignatureData() {
        BigInteger[] pqRoof= generateSafePrimePair(PRIME_BIT_LENGTH);
        BigInteger NRoof = pqRoof[0].multiply(pqRoof[1]);
        BigInteger totientRoof = pqRoof[0].subtract(BigInteger.ONE).multiply(pqRoof[1].subtract(BigInteger.ONE));
        BigInteger[] pq;
        BigInteger N;
        int tries = 0;
        do {
            pq = generateSafePrimePair(PRIME_BIT_LENGTH_PRIME);
            N = pq[0].multiply(pq[1]);
            log.debug("Generating safe prime try: {}, totientRoof: {}, N: {}", ++tries, totientRoof, N);
        } while (!totientRoof.gcd(N).equals(BigInteger.ONE));
        this.linearSignatureData =  new LinearSignatureData.PublicData(N, NRoof,
                new BigInteger(PRIME_BIT_LENGTH, 16, random),
                generateRandomBigInteger(NRoof),
                generateRandomBigInteger(NRoof),
                generateHVector(1, NRoof), pqRoof);
    }

    private BigInteger generateRandomBigInteger(BigInteger modulo){
        return new BigInteger(modulo.bitLength(), random).mod(modulo);
    }

    private BigInteger[] generateSafePrimePair(int bitLength) {
        BigInteger p, q;
        do {
            p = new BigInteger(bitLength/2, 16, random);
            q = p.subtract(BigInteger.ONE).divide(BigInteger.TWO);
        } while (!q.isProbablePrime(16));
        return new BigInteger[]{q, p};
    }

    public void changeDefaultValues(Scanner scanner) {
        Set<String> settings = Set.of("fieldbase", "tsecure", "generator", "servers", "primebits");
        String input;
        do {
            do {
                System.out.println(this);
                System.out.print("What value should be changed? [back] to return. ");
                input = scanner.nextLine().toLowerCase().replaceAll("\\s", "");
                if (input.equals("back")) return;
            } while (!settings.contains(input));

            System.out.print("New value? ");
            switch (input) {
                case "fieldbase":
                    this.setFieldBase(scanner.nextBigInteger());
                    break;
                case "tsecure":
                    this.settSecure(scanner.nextInt());
                    break;
                case "generator":
                    this.setGenerator(scanner.nextBigInteger());
                    break;
                case "servers":
                    this.setNumberOfServers(scanner.nextInt());
                    break;
                case "primebits":
                    int bits = scanner.nextInt();
                    PRIME_BIT_LENGTH = bits;
                    PRIME_BIT_LENGTH_PRIME = bits;
                    generateLinearSignatureData();
                    break;
            }
            scanner.nextLine();

        } while (true);
    }

    public LinearSignatureData.PublicData getLinearSignatureData() {
        if (linearSignatureData == null) {
            generateLinearSignatureData();
        }
        return linearSignatureData;
    }

    public int getConstruction() {
        return construction;
    }
}
