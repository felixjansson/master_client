package com.master_thesis.client.data;

import ch.qos.logback.classic.Logger;
import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
    @Value("${warmup_runs}")
    private int warmupRuns;

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
            fieldBase = generatePrime(fieldBase_bits);
        }
        return fieldBase;
    }

    public void setFieldBase(BigInteger fieldBase) {
        this.fieldBase = fieldBase;
    }

    public BigInteger getGenerator() {
        if (generator == null) {
            generator = generatePrime(generator_bits);
        }
        return generator;
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
        return "servers=" + numberOfServers +
                ", fieldBase bits=" + fieldBase_bits +
                ", generator bits=" + generator_bits +
                ", tSecure=" + tSecure +
                ", k bits=" + PRIME_BIT_LENGTH +
                ", k' bits=" + PRIME_BIT_LENGTH_PRIME +
                ", runTimes=" + runTimes +
                ", rsa bits=" + RSA_BIT_LENGTH +
                ", skip runs=" + warmupRuns;
    }

    public String toCSVString() {
        return  construction +
                "," + numberOfServers +
                "," + fieldBase_bits +
                "," + generator_bits +
                "," + tSecure +
                "," + PRIME_BIT_LENGTH +
                "," + PRIME_BIT_LENGTH_PRIME +
                "," + runTimes +
                "," + RSA_BIT_LENGTH +
                "," + warmupRuns;
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
                generatePrimeUpTo(PRIME_BIT_LENGTH),
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
        Set<String> settings = Set.of("servers", "fieldbasebits", "generatorbits", "tsecure", "kbits", "k'bits", "runtimes", "rsabits", "skipruns", "fb", "gb");
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
                case "tsecure":
                    this.settSecure(scanner.nextInt());
                    break;
                case "servers":
                    this.setNumberOfServers(scanner.nextInt());
                    break;
                case "kbits":
                    PRIME_BIT_LENGTH = scanner.nextInt();
                    generateLinearSignatureData();
                    break;
                case "k'bits":
                    PRIME_BIT_LENGTH_PRIME = scanner.nextInt();
                    generateLinearSignatureData();
                    break;
                case "gb":
                case "generatorbits":
                    generator_bits = scanner.nextInt();
                    generator = null;
                    getGenerator();
                    break;
                case "fb":
                case "fieldbasebits":
                    fieldBase_bits = scanner.nextInt();
                    fieldBase = null;
                    getFieldBase();
                    break;
                case "runtimes":
                    runTimes = scanner.nextInt();
                    break;
                case "rsabits":
                    RSA_BIT_LENGTH = scanner.nextInt();
                    rsaNPrime = null;
                    getRSASecretPrimes();
                    break;
                case "skipruns":
                    warmupRuns = scanner.nextInt();
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

    private BigInteger generateSafePrime(int bits) {
        String[] command = new String[]{"openssl", "prime", "-generate", "-safe", "-bits", Integer.toString(bits)};
        BigInteger safePrime = invokeOpenSSL(command);
        if (safePrime.bitLength() != bits)
            throw new RuntimeException("Prime has " + safePrime.bitLength() + " bits but expected " + bits + " bits.");
        return safePrime;
    }

    /**
     * @param bits at most this many bits will be used in the prime.
     * @return a prime of at most #bits size.
     */
    private BigInteger generatePrimeUpTo(int bits) {
        return new BigInteger(bits, 16, random);
    }

    /**
     * @param bits the exact number of bits in the requested prime.
     * @return a prime of exactly #bits.
     */
    private BigInteger generatePrime(int bits) {
        String[] command = new String[]{"openssl", "prime", "-generate", "-bits", Integer.toString(bits)};
        BigInteger prime = invokeOpenSSL(command);
        if (prime.bitLength() != bits)
            throw new RuntimeException("Prime has " + prime.bitLength() + " bits but expected " + bits + " bits.");
        return prime;
    }

    @SneakyThrows
    private BigInteger invokeOpenSSL(String[] command) {
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

    public void updateValues(ApplicationArguments args) {
        args.getOptionNames().forEach(option -> {
            if (args.getOptionValues(option).size() == 0)
                return;
            String value = args.getOptionValues(option).get(0);
            switch (option) {
                case "user-tag":
                    user = value;
                    break;
                case "runs":
                    runTimes = Integer.parseInt(value);
                    break;
                case "construction":
                    construction = Integer.parseInt(value);
                    break;
                case "t_secure":
                    tSecure = Integer.parseInt(value);
                    break;
                case "numberOfServers":
                    numberOfServers = Integer.parseInt(value);
                    break;
                case "k":
                    PRIME_BIT_LENGTH = Integer.parseInt(value);
                    break;
                case "k_prime":
                    PRIME_BIT_LENGTH_PRIME = Integer.parseInt(value);
                    break;
                case "fieldBase_bits":
                    fieldBase_bits = Integer.parseInt(value);
                    break;
                case "generator_bits":
                    generator_bits = Integer.parseInt(value);
                    break;
                case "RSA_BIT_LENGTH":
                    RSA_BIT_LENGTH = Integer.parseInt(value);
                    break;
                case "warmup_runs":
                    warmupRuns = Integer.parseInt(value);
                    break;
            }
        });

    }

    public int getWarmupRuns() {
        return warmupRuns;
    }
}
