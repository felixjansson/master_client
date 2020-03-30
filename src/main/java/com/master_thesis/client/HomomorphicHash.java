package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component("hash")
public class HomomorphicHash {

    protected PublicParameters publicParameters;
    private static final Logger log = (Logger) LoggerFactory.getLogger(HomomorphicHash.class);
    private final SecureRandom random;

    @Autowired
    public HomomorphicHash(PublicParameters publicParameters) {
        this.publicParameters = publicParameters;
        random = new SecureRandom();
    }

    public ShareInformation shareSecret(int int_secret) {
        int substationID = publicParameters.getSubstationID();
        BigInteger base = publicParameters.getFieldBase(substationID);
        BigInteger generator = publicParameters.getGenerator(substationID);
        BigInteger field = new BigInteger(base.toString());
        BigInteger secret = BigInteger.valueOf(int_secret);

        BigInteger nonce = BigInteger.valueOf(random.nextLong());
        log.info("base: {}, generator: {}, secret: {}, nonce: {}", base, generator, secret, nonce);
        BigInteger proofComponent = hash(base, secret.add(nonce), generator);

        Function<Integer, BigInteger> polynomial = generatePolynomial(int_secret, field);
        List<Server> servers = publicParameters.getServers();
        Set<Integer> serverIDs = servers.stream().map(Server::getServerID).collect(Collectors.toSet());
        HashMap<URI, ServerShare> map = new HashMap<>();
        servers.forEach(server -> {
            BigInteger share = polynomial.apply(server.getServerID());
            share = share.multiply(BigInteger.valueOf(beta(server.getServerID(), serverIDs)));
            map.put(server.getUri(), new ServerShare(share, proofComponent));
        });
        return new ShareInformation(map, nonce, proofComponent);
    }

    protected Function<Integer, BigInteger> generatePolynomial(int secret, BigInteger field) {
        int t = publicParameters.getSecurityThreshold();
        StringBuilder logString = new StringBuilder("Polynomial used: ").append(secret);
        ArrayList<BigInteger> coefficients = new ArrayList<>();
        for (int i = 1; i <= t; i++) {
            BigInteger a;
            do {
                a = new BigInteger(field.bitLength(), random).mod(field);
            } while (a.equals(BigInteger.ZERO) || a.compareTo(field) >= 0);
            logString.append(" + ").append(a).append("x^").append(i);
            coefficients.add(a);
        }
        log.info(logString.toString());

        return (serverID) -> {
            BigInteger serverIDBIG = BigInteger.valueOf(serverID);
            BigInteger res = field.valueOf(secret);
            for (int i = 0; i < coefficients.size(); i++) {
                BigInteger coefficient = coefficients.get(i);
                BigInteger polynomial = serverIDBIG.pow(i + 1);
                res = res.add(coefficient.multiply(polynomial));
            }
            return res;
        };
    }

    public BigInteger hash(BigInteger field, BigInteger input, BigInteger g) {
        return g.modPow(input, field);
    }

    public int beta(int serverID, Set<Integer> serverIDs) {
        return (int) Math.round(serverIDs.stream().mapToDouble(Integer::doubleValue).reduce(1f, (prev, j) -> {
            if (j == serverID) {
                return prev;
            } else {
                return prev * (j / (j - serverID));
            }
        }));
    }

}
