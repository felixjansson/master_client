package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.master_thesis.client.data.*;
import com.master_thesis.client.util.HttpAdapter;
import com.master_thesis.client.util.NoiseGenerator;
import com.master_thesis.client.util.Reader;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigInteger;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class SmartMeter {

    private static final Logger log = (Logger) LoggerFactory.getLogger(SmartMeter.class);
    private Reader reader;
    private int fid;
    private int clientID;
    private RSAThreshold rsaThreshold;
    private HomomorphicHash homomorphicHash;
    private LinearSignature linearSignature;
    private HttpAdapter httpAdapter;
    private Scanner scanner;
    private int substationID;
    private ApplicationArguments args;
    private Collection<Construction> enabledConstructions = Stream.of(Construction.LINEAR).collect(Collectors.toSet());
    private long timer;
    private NoiseGenerator noiseGenerator;


    @Autowired
    public SmartMeter(ApplicationArguments args, Reader reader, RSAThreshold rsaThreshold, HomomorphicHash homomorphicHash, LinearSignature linearSignature, HttpAdapter httpAdapter, NoiseGenerator noiseGenerator) {
        this.args = args;
        this.reader = reader;
        this.rsaThreshold = rsaThreshold;
        this.homomorphicHash = homomorphicHash;
        this.linearSignature = linearSignature;
        this.httpAdapter = httpAdapter;
        this.noiseGenerator = noiseGenerator;

        if (args.containsOption("local") || args.containsOption("test")) {
            httpAdapter.toggleLocal();
        } else {
            register();
        }

    }

    public static void main(String[] args) {
        SpringApplication.run(SmartMeter.class, args);
    }

    @Autowired
    public void run() {

        if (args.containsOption("test")) {
            runTest();
            return;
        } else if (args.containsOption("dp")) {
            elementDP();
            return;
        }

        scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {

            System.out.printf("Client %s: [q]uit. [l]ist clients. [r]egister. \n          [d]elete all. [t]oggle construction. [s]end shares. \n          [local] toggles if servers are used. [default] to change default values\n          [test] to run locally with result output. [run many] to run many.\n          [1][2][3] to run a specific construction\n", clientID);

            String input = scanner.nextLine();
            log.debug("input: {}", input);
            switch (input.toLowerCase().replaceAll("\\s", "")) {
                case "r":
                    register();
                    break;
                case "d":
                    httpAdapter.deleteClients();
                    break;
                case "q":
                    running = false;
                    break;
                case "l":
                    System.out.println(listClients());
                    break;
                case "t":
                    toggleConstruction();
                    break;
                case "local":
                    httpAdapter.toggleLocal();
                    break;
                case "s":
                    readAndSendShare();
                    break;
                case "runmany":
                    runMany();
                    break;
                case "default":
                    httpAdapter.changeDefaultValues(scanner);
                    break;
                case "test":
                    httpAdapter.toggleTester();
                    break;
                default:
                    if ("123".contains(input)) {
                        runTest(Integer.parseInt(input));
                    } else {
                        log.info("unknown command: [{}]", input);
                    }
            }
        }
    }

    private void elementDP() {
        StringJoiner sj = new StringJoiner(",");
        sj.add(Double.toString(noiseGenerator.getEpsilon()));
        List<Integer> values;
        List<String> keys = new LinkedList<>(reader.getCSVKeys());
        keys.sort(null);

        for (String key : keys) {
            values = reader.readValuesMappedOnTimeFromCSV(key);
            int correct = values.stream().reduce(0, Integer::sum);


            if (noiseGenerator.getNoiseFunction().equals("gaussian")) {
                assert !values.isEmpty();
                noiseGenerator.computeGaussianVariance(values.stream().max(Integer::compareTo).get(), values.size());
            }

//            long correctWithNoise = noiseGenerator.addNoise(correct);

            BigInteger res = values.stream()
                    .map(x -> noiseGenerator.addNoise(x))
                    .map(BigInteger::valueOf)
                    .reduce(BigInteger.ZERO, BigInteger::add);
            sj.add(Integer.toString(correct));
//            sj.add(Long.toString(correctWithNoise));
            sj.add(res.toString());
        }
        System.out.println(sj.toString());
    }

    private void runTest() {
        runTest(httpAdapter.getConstruction());
    }

    private void runTest(int construction) {
        Map<Integer, Construction> constructionMap = Map.of(
                1, Construction.HASH,
                2, Construction.RSA,
                3, Construction.LINEAR);
        httpAdapter.updateLocalValues(constructionMap.get(construction));
        enabledConstructions.clear();
        enabledConstructions.add(constructionMap.get(construction));
        int runs = httpAdapter.getRunTimes();
        int checkpoint = runs / 20;
        int skipRuns = httpAdapter.getDefaultPublicData().getWarmupRuns();
        for (int i = 0; i < skipRuns; i++) {
            readAndSendShare();
        }
        timer = 0;
        for (int i = 0; i < checkpoint; i++) {
            readAndSendShare();
        }
        if (timer > 15000) {
            System.err.print("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + "5% done in " + timer/1000 + "s. Max total time: "
                    + timer * 20 / 1000 + "s. ==> ");
        }
        for (int i = checkpoint; i < runs; i++) {
            readAndSendShare();
        }
        System.err.println("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "]" + " Done in " + timer + "ms.");
        printCSVData(constructionMap.get(construction));
    }

    private void printCSVData(Construction construction) {
        StringJoiner sb = new StringJoiner(",");
        DefaultPublicData dpd = httpAdapter.getDefaultPublicData();
        sb.add(dpd.getUser());
        sb.add(construction.toString());
        sb.add(Boolean.toString(dpd.isExternalLagrange()));
        sb.add(reader.getReaderMode());
        sb.add(Integer.toString(reader.getSecretBits()));
        sb.add(Integer.toString(dpd.getRunTimes()));
        sb.add(Integer.toString(dpd.getWarmupRuns()));
        sb.add(Integer.toString(dpd.gettSecure()));
        sb.add(Integer.toString(dpd.getNumberOfServers()));
        switch (construction) {
            case HASH:
                sb.add(Integer.toString(dpd.getFieldBaseBits()));
                sb.add(Integer.toString(dpd.getGeneratorBits()));
                break;
            case RSA:
                sb.add(Integer.toString(dpd.getFieldBaseBits()));
                sb.add(Integer.toString(dpd.getGeneratorBits()));
                sb.add(Integer.toString(dpd.getRsaBitSize()));
                break;
            case LINEAR:
                sb.add(Integer.toString(dpd.getLinearHatPrimeBitSize()));
                sb.add(Integer.toString(dpd.getLinearPrimeBitSize()));
                break;
            case NONCE:
                break;
        }
        sb.add(new SimpleDateFormat("MMdd-HHmm").format(new Date()));
        sb.add(Long.toString(timer));
        System.out.println(sb.toString());
    }

    private void toggleConstruction() {
        Map<String, Construction> constructionMap = Map.of(
                "1", Construction.HASH,
                "2", Construction.RSA,
                "3", Construction.LINEAR);
        String input;
        do {
            System.out.printf("Client %s: Active: %s \n", clientID, enabledConstructions);
            System.out.printf("Client %s: Press to toggle [1 Hash] [2 RSA] [3 Linear] or [b]ack ", clientID);
            input = scanner.nextLine();
            if ("b".equals(input)) return;
        } while (!constructionMap.containsKey(input));
        Construction construction = constructionMap.get(input);
        if (enabledConstructions.contains(construction)) {
            enabledConstructions.remove(construction);
        } else {
            enabledConstructions.add(construction);
        }
    }

    private void register() {
        ClientStartupData jsonNode = httpAdapter.registerClient();
        this.clientID = jsonNode.getClientID();
        this.substationID = jsonNode.getSubstationID();
        this.fid = jsonNode.getStartFid();
    }

    private void readAndSendShare() {
        BigInteger secret = reader.readValue();
        log.info("=== Starting new share ===");

        if (enabledConstructions.contains(Construction.HASH)) {
            log.info("# FID: {} # Sending with {}", fid, Construction.HASH);

            // Here we perform the ShareSecret function from the Homomorphic Hash Construction.
            long start = System.currentTimeMillis();
            HomomorphicHashData data = homomorphicHash.shareSecret(secret);
            long end = System.currentTimeMillis();
            timer += end - start;
            // We add identifiers to allow for multiple computations.
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);

            // We retrieve all shares going to the servers and send them.
            Map<URI, HomomorphicHashData.ServerData> serverDataMap = data.getServerData();
            serverDataMap.forEach(httpAdapter::sendServerShare);

            // Get nonce data to send to the trusted party to compute R_n.
            httpAdapter.sendNonce(data.getNonceData());

            // Get the tau (proof component) and make it publicly available.
            httpAdapter.sendProofComponent(data.getVerifierData());

            // Prepare for the next computation.
            newFid();
        }

        if (enabledConstructions.contains(Construction.RSA)) {
            log.info("# FID: {} # Sending with {}", fid, Construction.RSA);
            httpAdapter.getRSASecretPrimes(substationID);

            // Here we perform the ShareSecret function from the Threshold Signature Construction.
            long start = System.currentTimeMillis();
            RSAThresholdData data = rsaThreshold.shareSecret(secret);
            long end = System.currentTimeMillis();
            timer += end - start;

            // We add identifiers to allow for multiple computations.
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);

            // We retrieve all shares going to the servers and send them.
            Map<URI, RSAThresholdData.ServerData> shareMap = data.getServerData();
            shareMap.forEach(httpAdapter::sendServerShare);

            // Get nonce data to send to the trusted party to compute R_n.
            httpAdapter.sendNonce(data.getNonceData());

            // Get the tau (proof component) and make it publicly available.
            httpAdapter.sendProofComponent(data.getVerifierData());

            // Prepare for the next computation.
            newFid();
        }

        if (enabledConstructions.contains(Construction.LINEAR)) {
            log.info("# FID: {} # Sending with {}", fid, Construction.LINEAR);

            // Here we perform the ShareSecret function from the Linear Signature Construction.
            long start = System.currentTimeMillis();
            LinearSignatureData data = linearSignature.shareSecret(secret, fid);
            long end = System.currentTimeMillis();
            timer += end - start;
            // We add identifiers to allow for multiple computations.
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);

            // Using the result of the share secret function we compute the partial proof function from Linear Signature Construction
            // The data variable is modified in the function and "replaced" when the partial proof function is completed.
            start = System.currentTimeMillis();
            data = linearSignature.partialProof(data, secret);
            end = System.currentTimeMillis();
            timer += end - start;
            // We retrieve all shares going to the servers and send them.
            Map<URI, LinearSignatureData.ServerData> serverDataMap = data.getServerData();
            serverDataMap.forEach(httpAdapter::sendServerShare);

            // Get nonce data to send to the trusted party to compute R_n.
            httpAdapter.sendNonce(data.getNonceData());

            // Get the proof component (sigma) and make it publicly available.
            httpAdapter.sendProofComponent(data.getVerifierData());

            // Prepare for the next computation.
            newFid();
        }

        log.info("=== Shares sent. Next fid {} ===", fid);

    }

    private void runMany() {
        System.out.println("How many? ");
        int runs = scanner.nextInt();
        scanner.nextLine();
        for (int i = 0; i < runs; i++) {
            readAndSendShare();
        }
    }

    private String listClients() {
        JsonNode clients = httpAdapter.listClients(substationID);
        JsonNode clientsAtSubstation = clients.get(Integer.toString(substationID));
        StringBuilder sb = new StringBuilder("Clients: ");
        clientsAtSubstation.elements().forEachRemaining(node -> sb.append(node.get("clientID")).append(", "));
        return sb.toString();
    }

    private void newFid() {
        fid++;
        httpAdapter.updateFid(substationID, clientID, fid);
    }
}
