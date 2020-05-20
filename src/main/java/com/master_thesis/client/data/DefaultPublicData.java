package com.master_thesis.client.data;

import ch.qos.logback.classic.Logger;
import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@PropertySource("classpath:local.properties")
public class DefaultPublicData {

    @Value("${number_of_servers}")
    private int numberOfServers;
    //    private BigInteger localFieldBase = BigInteger.valueOf(2011);
    @Value("${fieldbase_bits}")
    private int fieldBaseBits;
    @Value("${generator_bits}")
    private int generatorBits;
    @Value("${t_secure}")
    private int tSecure;
    private LinearSignatureData.PublicData linearSignatureData;

    private final SecureRandom random = new SecureRandom();
    private static final Logger log = (Logger) LoggerFactory.getLogger(DefaultPublicData.class);
    //    This is k hat \hat{k}
    @Value("${k_hat}")
    private int linearHatPrimeBitSize;
    //    This is k
    @Value("${k}")
    private int linearPrimeBitSize;
    @Value("${runs}")
    private int runTimes;
    @Value("${construction}")
    private int construction;

    @Value("${rsa_bit_size}")
    private int rsaBitSize;


    private BigInteger fieldBase;
    private BigInteger generator;
    private BigInteger rsaNPrime;
    private BigInteger rsaN;
    @Value("${user_tag}")
    private String user;
    @Value("${warmup_runs}")
    private int warmupRuns;
    @Value("${external_lagrange}")
    private boolean externalLagrange;
    private Map<BigInteger, BigInteger> lagrangeMap;


    // Used for CSV prints
    @Value("${read_mode}")
    private String readMode;
    @Value("${secret_bits}")
    private int secret_bits;


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
            fieldBase = generatePrime(fieldBaseBits);
        }
        return fieldBase;
    }

    public void setFieldBase(BigInteger fieldBase) {
        this.fieldBase = fieldBase;
    }

    public BigInteger getGenerator() {
        if (generator == null) {
            generator = generatePrime(generatorBits);
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
                ", fieldBase bits=" + fieldBaseBits +
                ", generator bits=" + generatorBits +
                ", tSecure=" + tSecure +
                ", k hat bits=" + linearHatPrimeBitSize +
                ", k bits=" + linearPrimeBitSize +
                ", runTimes=" + runTimes +
                ", rsa bits=" + rsaBitSize +
                ", skip runs=" + warmupRuns +
                ", external lagrange=" + externalLagrange +
                ", read mode=" + readMode +
                ", secret bits=" + secret_bits;
    }

    public String toCSVString() {
        return construction +
                "," + numberOfServers +
                "," + fieldBaseBits +
                "," + generatorBits +
                "," + tSecure +
                "," + linearHatPrimeBitSize +
                "," + linearPrimeBitSize +
                "," + runTimes +
                "," + rsaBitSize +
                "," + warmupRuns +
                "," + externalLagrange +
                "," + readMode +
                "," + secret_bits;
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
        BigInteger[] pqRoof = generateSafePrimePair(linearHatPrimeBitSize);
        BigInteger NRoof = pqRoof[0].multiply(pqRoof[1]);
        BigInteger totientRoof = pqRoof[0].subtract(BigInteger.ONE).multiply(pqRoof[1].subtract(BigInteger.ONE));
        BigInteger[] pq;
        BigInteger N;
        int tries = 0;
        do {
            pq = generateSafePrimePair(linearPrimeBitSize);
            N = pq[0].multiply(pq[1]);
            log.debug("Generating safe prime try: {}, totientRoof: {}, N: {}", ++tries, totientRoof, N);
        } while (!totientRoof.gcd(N).equals(BigInteger.ONE));
        this.linearSignatureData =  new LinearSignatureData.PublicData(N, NRoof,
                generatePrimeUpTo(linearPrimeBitSize),
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
        Set<String> settings = Set.of("servers", "fieldbasebits", "generatorbits", "tsecure", "k_hat", "k", "runtimes", "rsabits", "skipruns", "externallagrange", "el", "fb", "gb");
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
                case "k_hat":
                    linearHatPrimeBitSize = scanner.nextInt();
                    generateLinearSignatureData();
                    break;
                case "k":
                    linearPrimeBitSize = scanner.nextInt();
                    generateLinearSignatureData();
                    break;
                case "gb":
                case "generatorbits":
                    generatorBits = scanner.nextInt();
                    generator = null;
                    getGenerator();
                    break;
                case "fb":
                case "fieldbasebits":
                    fieldBaseBits = scanner.nextInt();
                    fieldBase = null;
                    getFieldBase();
                    break;
                case "runtimes":
                    runTimes = scanner.nextInt();
                    break;
                case "rsabits":
                    rsaBitSize = scanner.nextInt();
                    rsaNPrime = null;
                    getRSASecretPrimes();
                    break;
                case "skipruns":
                    warmupRuns = scanner.nextInt();
                    break;
                case "externallagrange":
                case "el":
                    externalLagrange = scanner.nextBoolean();
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

    public int getFieldBaseBits() {
        return fieldBaseBits;
    }

    public int getGeneratorBits() {
        return generatorBits;
    }

    public int getLinearHatPrimeBitSize() {
        return linearHatPrimeBitSize;
    }

    public int getLinearPrimeBitSize() {
        return linearPrimeBitSize;
    }

    public int getRsaBitSize() {
        return rsaBitSize;
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
        int runs = 100;
        int max_runs = runs;
        do {
            pair = generateSafePrimePair(minValue);
            isSmallerThanMinValue = pair[1].max(minValue).equals(minValue);
            isForbidden = Arrays.equals(pair, forbiddenValues);
            runs--;
            if (runs == 0) {
                System.err.println("Failed to generate safe primes within " + max_runs + " tries. Exiting.");
                System.out.println("false,"+toCSVString()+",Can not generate safe prime pairs of this size.");
                System.exit(15);
            }
        } while (isForbidden || isSmallerThanMinValue);
        return pair;
    }

    private BigInteger[] generateSafePrimePair(BigInteger minValue) {
        BigInteger p, q;
        do {
            p = generateSafePrime(rsaBitSize);
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

    public boolean isExternalLagrange() {
        return externalLagrange;
    }

    public BigInteger getLagrangeValue(BigInteger currentValue) {
        if (lagrangeMap == null)
            generateLagrangeMap();

        return lagrangeMap.get(currentValue);
    }

    private void generateLagrangeMap() {
        lagrangeMap = new HashMap<>();
        int m = getNumberOfServers();
        Set<BigInteger> servers = IntStream.rangeClosed(1, m).mapToObj(BigInteger::valueOf).collect(Collectors.toSet());
        servers.forEach(x -> lagrangeMap.put(x, computeLagrangeCoefficient(x, servers)));
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
//        BigInteger fieldbase = publicParameters.getFieldBase(0);
//
//        BigInteger result = potentialValues.stream().filter(x -> !x.equals(currentValue))
//                .reduce(BigInteger.ONE, (suc, x) -> suc.multiply(
//                        x.multiply(x.subtract(currentValue).modInverse(fieldbase)
//                        ).mod(fieldbase)));
//
        BigInteger nominator = potentialValues.stream()
                .filter(x -> !x.equals(currentValue))
                .reduce(BigInteger.ONE, BigInteger::multiply);
        // Compute the denominator, the product of all inputs minus the current value, except for the input that is the currentValue.
        BigInteger denominator = potentialValues.stream()
                .filter(x -> !x.equals(currentValue))
                .reduce(BigInteger.ONE, (prev, x) -> prev.multiply(x.subtract(currentValue)));
        log.debug("beta values: {}/{} = {}", nominator, denominator, nominator.divideAndRemainder(denominator));
//         Return the nominator divided by the denominator.
//        log.info("{} with mod. {} without mod.", result, nominator.divide(denominator));
        return nominator.divide(denominator);
    }
}
