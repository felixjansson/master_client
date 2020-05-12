package com.master_thesis.client.data;

import ch.qos.logback.classic.Logger;
import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Set;

@Component
@PropertySource("classpath:local.properties")
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

    @Value("${RSA_BIT_LENGTH}")
    private int RSA_BIT_LENGTH;


    private BigInteger fieldBase;
    private BigInteger generator;
    private BigInteger rsaNPrime;
    private BigInteger rsaN;
    @Value("${user-tag}")
    private String user;

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
                generatePrime(PRIME_BIT_LENGTH),
                generateRandomBigInteger(NRoof),
                generateRandomBigInteger(NRoof),
                generateHVector(1, NRoof), pqRoof);
    }

    private BigInteger generateRandomBigInteger(BigInteger modulo){
        return new BigInteger(modulo.bitLength(), random).mod(modulo);
    }

    private BigInteger[] generateSafePrimePair(int bitLength) {
        BigInteger p, q;
        p = generateSafePrime(bitLength);
        q = p.subtract(BigInteger.ONE).divide(BigInteger.TWO);
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

    public int getFieldBase_bits() {
        return fieldBase_bits;
    }

    public int getGenerator_bits() {
        return generator_bits;
    }

    public int getPRIME_BIT_LENGTH() {
        return PRIME_BIT_LENGTH;
    }

    public int getPRIME_BIT_LENGTH_PRIME() {
        return PRIME_BIT_LENGTH_PRIME;
    }

    public int getRSA_BIT_LENGTH() {
        return RSA_BIT_LENGTH;
    }

    public BigInteger[] getRSASecretPrimes() {
        if (rsaN == null || rsaNPrime == null)
            generateRSAPrimes(getFieldBase());
        return new BigInteger[] {rsaN, rsaNPrime};
    }

    private BigInteger generateSafePrime(int bits){
        String[] command = new String[]{"openssl", "prime", "-generate", "-safe", "-bits", Integer.toString(bits / 2)};
        return invokeOpenSSL(command);
    }

    private BigInteger generatePrime(int bits){
        String[] command = new String[]{"openssl", "prime", "-generate", "-bits", Integer.toString(bits / 2)};
        return invokeOpenSSL(command);
    }

    @SneakyThrows
    private BigInteger invokeOpenSSL(String[] command){
        log.debug("Invoke openSSL with command: {}", Arrays.toString(command));
        Runtime runtime = Runtime.getRuntime();
        Process processOpenSSL = runtime.exec(command);
        processOpenSSL.waitFor();
        return new BigInteger(new BufferedReader(new InputStreamReader(processOpenSSL.getInputStream())).readLine());
    }

    void generateRSAPrimes(BigInteger fieldBase) {
        // Todo: We believe that if the rsa primes are lower than fieldbase there could be an issue but we do not remember why at the moment.
        fieldBase = BigInteger.ZERO;
//        if (fieldBase.bitLength() > RSA_BIT_LENGTH / 2)
//            throw new RuntimeException("FieldBase bit length is higher than RSA primes. RSA must be two times larger.");
        BigInteger[] pPair = generateConstrainedSafePrimePair(fieldBase, new BigInteger[]{});
        BigInteger[] qPair = generateConstrainedSafePrimePair(fieldBase, pPair);
        rsaNPrime = pPair[0].multiply(qPair[0]);
        rsaN = pPair[1].multiply(qPair[1]);
    }

    private BigInteger[] generateConstrainedSafePrimePair(BigInteger minValue, BigInteger[] forbiddenValues) {
        BigInteger[] pair;
        boolean isSmallerThanMinValue, isForbidden;
        do {
            pair = generateSafePrimePair(minValue);
            isSmallerThanMinValue = pair[1].max(minValue).equals(minValue);
            isForbidden = Arrays.equals(pair, forbiddenValues);
        } while (isForbidden || isSmallerThanMinValue);
        return pair;
    }

    private BigInteger[] generateSafePrimePair(BigInteger minValue) {
        BigInteger p, q;
        do {
            p = generateSafePrime(RSA_BIT_LENGTH);
        } while (p.compareTo(minValue) < 1);
        q = p.subtract(BigInteger.ONE).divide(BigInteger.TWO);
        return new BigInteger[]{q, p};
    }

    public String getUser() {
        return user;
    }
}
