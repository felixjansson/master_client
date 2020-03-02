package com.master_thesis.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class SmartMeter {

    public static void main(String[] args) {
        SpringApplication.run(SmartMeter.class, args);
    }

    private Reader reader;
    private ClientSecretSharing clientSecretSharing;
    private int clientID;
    private int transformatorID;
    private HttpAdapter httpAdapter;
    private Scanner scanner;

    @Autowired
    public SmartMeter(Reader reader, @Qualifier("hash") ClientSecretSharing clientSecretSharing, HttpAdapter httpAdapter) {
        this.reader = reader;
        this.clientSecretSharing = clientSecretSharing;
        this.httpAdapter = httpAdapter;
        register();
        scanner = new Scanner(System.in);
    }

    @Autowired
    public void run() throws InterruptedException {
        boolean running = true;
        while (running) {

            System.out.printf("Client %s: [q]uit. [l]ist clients. [r]egister. [d]elete all. [any] to send shares. ", clientID);
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
                default:
                    readAndSendShare();
                    break;
            }
        }
    }

    private void register() {
        JsonNode jsonNode = httpAdapter.registerClient();
        this.clientID = jsonNode.get("clientID").asInt();
        this.transformatorID = jsonNode.get("transformatorID").asInt();
    }

    private void readAndSendShare() {
        int val = reader.readValue();
        Map<URI, SecretShare> shareMap = clientSecretSharing.shareSecret(val);
        shareMap.values().forEach(secretShare -> secretShare.setClientID(clientID).setTransformatorID(transformatorID));
        shareMap.forEach(httpAdapter::sendShare);
    }

    private String listClients() {
        JsonNode clients = httpAdapter.listClients(transformatorID);
        JsonNode clientsAtTransformator = clients.get(Integer.toString(transformatorID));
        StringBuilder sb = new StringBuilder("Clients: ");
        clientsAtTransformator.elements().forEachRemaining(node -> {
            sb.append(node.get("clientID")).append(", ");
        });
        return sb.toString();

    }
}
