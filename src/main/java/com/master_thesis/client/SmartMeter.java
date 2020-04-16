package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.master_thesis.client.data.*;
import com.master_thesis.client.util.HttpAdapter;
import com.master_thesis.client.util.Reader;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class SmartMeter {

    private static final Logger log = (Logger) LoggerFactory.getLogger(SmartMeter.class);
    private Reader reader;
    private int fid;
    private int clientID;
    private RSAThreshold RSAThresholdConstruction;
    private HomomorphicHash homomorphicHashConstruction;
    private LinearSignature linearSignatureConstruction;
    private NonceDistribution nonceDistribution;
    private HttpAdapter httpAdapter;
    private Scanner scanner;
    private int substationID;
    private Collection<Construction> enabledConstructions = Stream.of(Construction.NONCE, Construction.LINEAR, Construction.HASH, Construction.RSA).collect(Collectors.toSet());


    @Autowired
    public SmartMeter(Reader reader, RSAThreshold RSAThresholdConstruction, HomomorphicHash homomorphicHashConstruction, LinearSignature linearSignatureConstruction, NonceDistribution nonceDistribution, HttpAdapter httpAdapter) {
        this.reader = reader;
        this.RSAThresholdConstruction = RSAThresholdConstruction;
        this.homomorphicHashConstruction = homomorphicHashConstruction;
        this.linearSignatureConstruction = linearSignatureConstruction;
        this.nonceDistribution = nonceDistribution;
        this.httpAdapter = httpAdapter;
        register();
        scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        SpringApplication.run(SmartMeter.class, args);
    }

    @Autowired
    public void run() throws InterruptedException {
        boolean running = true;
        while (running) {

            System.out.printf("Client %s: [q]uit. [l]ist clients. [r]egister. [d]elete all. [t]oggle construction. [any] to send shares. ", clientID);
            while (!scanner.hasNext())
                Thread.sleep(1000);

            String input = scanner.next();
            switch (input.toLowerCase()) {
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
                default:
                    readAndSendShare();
                    break;
            }
        }
    }

    private void toggleConstruction() {
        Map<String, Construction> constructionMap = Map.of(
                "1", Construction.HASH,
                "2", Construction.RSA,
                "3", Construction.LINEAR,
                "4", Construction.NONCE);
        String input;
        do {
            System.out.printf("Client %s: Active: %s \n", clientID, enabledConstructions);
            System.out.printf("Client %s: Press to toggle [1 Hash] [2 RSA] [3 Linear] [4 Nonce] or [b]ack ", clientID);
            input = scanner.next();
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
        JsonNode jsonNode = httpAdapter.registerClient();
        this.clientID = jsonNode.get("clientID").asInt();
        this.substationID = jsonNode.get("substationID").asInt();
        this.fid = jsonNode.get("startFid").asInt();
    }

    private void readAndSendShare() {
        int secret = reader.readValue();
        log.info("=== Starting new share ===");

        if (enabledConstructions.contains(Construction.HASH)) {
            log.info("# FID: {} # Sending with {}", fid, Construction.HASH);
            HomomorphicHashData data = homomorphicHashConstruction.shareSecret(secret);
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);

            Map<URI, HomomorphicHashData.ServerData> serverDataMap = data.getServerData();
            serverDataMap.forEach(httpAdapter::sendServerShare);

            httpAdapter.sendNonce(data.getNonceData());
            httpAdapter.sendProofComponent(data.getVerifierData());

            newFid();
        }

        if (enabledConstructions.contains(Construction.RSA)) {
            log.info("# FID: {} # Sending with {}", fid, Construction.RSA);

            RSAThresholdData data = RSAThresholdConstruction.shareSecret(secret);
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);

            Map<URI, RSAThresholdData.ServerData> shareMap = data.getServerData();
            shareMap.forEach(httpAdapter::sendServerShare);

            httpAdapter.sendNonce(data.getNonceData());
            httpAdapter.sendProofComponent(data.getVerifierData());

            newFid();
        }

        if (enabledConstructions.contains(Construction.LINEAR)) {
            log.info("# FID: {} # Sending with {}", fid, Construction.LINEAR);
            LinearSignatureData data = linearSignatureConstruction.shareSecret(secret);
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);
            data = linearSignatureConstruction.partialProof(data, secret);

            Map<URI, LinearSignatureData.ServerData> serverDataMap = data.getServerData();
            serverDataMap.forEach(httpAdapter::sendServerShare);

            httpAdapter.sendNonce(data.getNonceData());
            httpAdapter.sendProofComponent(data.getVerifierData());

            newFid();
        }

        if (enabledConstructions.contains(Construction.NONCE)) {
            log.info("# FID: {} # Sending with {}", fid, Construction.NONCE);
            NonceDistributionData data = nonceDistribution.shareSecret(secret);
            data.setFid(fid).setClientID(clientID).setSubstationID(substationID);

            Map<URI, NonceDistributionData.ServerData> serverDataMap = data.getServerData();
            serverDataMap.forEach(httpAdapter::sendServerShare);

            httpAdapter.sendProofComponent(data.getVerifierData());

            newFid();

        }

        log.info("=== Shares sent. Next fid {} ===", fid);

    }

    private String listClients() {
        JsonNode clients = httpAdapter.listClients(substationID);
        JsonNode clientsAtSubstation = clients.get(Integer.toString(substationID));
        StringBuilder sb = new StringBuilder("Clients: ");
        clientsAtSubstation.elements().forEachRemaining(node -> {
            sb.append(node.get("clientID")).append(", ");
        });
        return sb.toString();
    }

    private void newFid() {
        fid++;
        httpAdapter.updateFid(substationID, clientID, fid);
    }
}
