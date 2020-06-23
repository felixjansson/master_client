package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.master_thesis.client.data.*;
import com.master_thesis.client.util.HttpAdapter;
import com.master_thesis.client.util.Reader;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigInteger;
import java.net.URI;
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
    private DifferentialPrivacy differentialPrivacy;
    private HttpAdapter httpAdapter;
    private Scanner scanner;
    private int substationID;
    private Collection<Construction> enabledConstructions = Stream.of(Construction.LINEAR).collect(Collectors.toSet());


    @Autowired
    public SmartMeter(Reader reader, RSAThreshold rsaThreshold, @Qualifier("hash") HomomorphicHash homomorphicHash, LinearSignature linearSignature, @Qualifier("dp") DifferentialPrivacy differentialPrivacy, HttpAdapter httpAdapter) {
        this.reader = reader;
        this.rsaThreshold = rsaThreshold;
        this.homomorphicHash = homomorphicHash;
        this.linearSignature = linearSignature;
        this.differentialPrivacy = differentialPrivacy;
        this.httpAdapter = httpAdapter;

        register();

    }

    public static void main(String[] args) {
        SpringApplication.run(SmartMeter.class, args);
    }

    @Autowired
    public void run() {

        scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {

            System.out.printf("Client %s: [q]uit. [l]ist clients. [r]egister. \n          [d]elete all. [t]oggle construction. [s]end shares. \n          [secret] change which secret to send. [run many] to run many.\n          [1][2][3][4] to run a specific construction\n", clientID);

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
                case "s":
                    readAndSendShare();
                    break;
                case "secret":
                    reader.setSecretMode(scanner);
                    break;
                case "runmany":
                    runMany();
                    break;
                default:
                    if ("1234".contains(input)) {
                        runSingleConstruction(Integer.parseInt(input));
                    } else {
                        log.info("unknown command: [{}]", input);
                    }
            }
        }
    }

    private void runSingleConstruction(int construction) {
        Map<Integer, Construction> constructionMap = Map.of(
                1, Construction.HASH,
                2, Construction.RSA,
                3, Construction.LINEAR,
                4, Construction.DP);
        enabledConstructions.clear();
        enabledConstructions.add(constructionMap.get(construction));
        readAndSendShare();
    }

    private void toggleConstruction() {
        Map<String, Construction> constructionMap = Map.of(
                "1", Construction.HASH,
                "2", Construction.RSA,
                "3", Construction.LINEAR);
        String input;
        do {
            System.out.printf("Client %s: Active: %s \n", clientID, enabledConstructions);
            System.out.printf("Client %s: Press to toggle [1 Hash] [2 RSA] [3 Linear] [4 DP] or [b]ack ", clientID);
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
            HomomorphicHashData data = homomorphicHash.shareSecret(secret, substationID);
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
            RSAThresholdData data = rsaThreshold.shareSecret(secret, substationID);

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
            LinearSignatureData data = linearSignature.shareSecret(secret, fid, substationID);
            // We add identifiers to allow for multiple computations.
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);

            // Using the result of the share secret function we compute the partial proof function from Linear Signature Construction
            // The data variable is modified in the function and "replaced" when the partial proof function is completed.
            data = linearSignature.partialProof(data, secret);
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

        if (enabledConstructions.contains(Construction.DP)) {
            log.info("# FID: {} # Sending with {}", fid, Construction.DP);

            // Here we perform the ShareSecret function from the Homomorphic Hash Construction.
            DifferentialPrivacyData data = differentialPrivacy.shareSecret(secret, substationID);
            // We add identifiers to allow for multiple computations.
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);

            // We retrieve all shares going to the servers and send them.
            Map<URI, DifferentialPrivacyData.ServerData> serverDataMap = data.getServerData();
            serverDataMap.forEach(httpAdapter::sendServerShare);

            // Get nonce data to send to the trusted party to compute R_n.
            httpAdapter.sendNonce(data.getNonceData());

            // Get the tau (proof component) and make it publicly available.
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
