package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;
import java.util.Map;
import java.util.Scanner;

@SpringBootApplication
public class SmartMeter {

    private static final Logger log = (Logger) LoggerFactory.getLogger(SmartMeter.class);
    private Reader reader;
    private int fid;
    private int clientID;
    private RSAThreshold clientSecretSharing;
    private HttpAdapter httpAdapter;
    private Scanner scanner;
    private int substationID;


    @Autowired
    public SmartMeter(Reader reader, RSAThreshold clientSecretSharing, HttpAdapter httpAdapter) {
        this.reader = reader;
        this.clientSecretSharing = clientSecretSharing;
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
        this.substationID = jsonNode.get("substationID").asInt();
        this.fid = jsonNode.get("fid").asInt();
    }

    private void readAndSendShare() {
        int val = reader.readValue();
        log.info("=== Starting new share fid:{} ===", fid);
        ShareInformation shareInfo = clientSecretSharing.shareSecret(val);
        shareInfo.setFid(fid).setClientID(clientID).setSubstationID(substationID);

        Map<URI, ServerShare> shareMap = shareInfo.getServerShares();
        shareMap.forEach(httpAdapter::sendServerShare);

        httpAdapter.sendNonce(shareInfo);

        newFid();
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
        int coordinatorFid = httpAdapter.updateFid(substationID, clientID, fid);
        if (fid != coordinatorFid) {
            log.info("Fid was not updated as expected. Client expected fid to be {} but server is at {}. Updating fid in client!", fid, coordinatorFid);
            fid = coordinatorFid;
        }
    }
}
