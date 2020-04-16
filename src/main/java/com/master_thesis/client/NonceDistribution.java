package com.master_thesis.client;

import ch.qos.logback.classic.Logger;
import com.master_thesis.client.data.Construction;
import com.master_thesis.client.data.NonceDistributionData;
import com.master_thesis.client.data.NonceDistributionData.ServerData;
import com.master_thesis.client.data.Server;
import com.master_thesis.client.util.PublicParameters;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NonceDistribution {

    protected PublicParameters publicParameters;
    private static final Logger log = (Logger) LoggerFactory.getLogger(NonceDistribution.class);
    private final SecureRandom random;

    @Autowired
    public NonceDistribution(PublicParameters publicParameters) {
        this.publicParameters = publicParameters;
        random = new SecureRandom();
    }

    public NonceDistributionData shareSecret(int int_secret) {
        int substationID = publicParameters.getSubstationID();
        BigInteger fieldBase = publicParameters.getFieldBase(substationID);
        BigInteger generator = publicParameters.getGenerator(substationID);
        BigInteger secret = BigInteger.valueOf(int_secret);

        BigInteger nonce = BigInteger.valueOf(random.nextLong()).mod(fieldBase);
        log.info("base: {}, generator: {}, secret: {}, nonce: {}", fieldBase, generator, secret, nonce);
        BigInteger proofComponent = hash(fieldBase, secret.add(nonce), generator);

        Function<Integer, BigInteger> secretPolynomial = generatePolynomial(BigInteger.valueOf(int_secret), fieldBase);
        Function<Integer, BigInteger> noncePolynomial = generatePolynomial(nonce, fieldBase);

        List<Server> servers = publicParameters.getServers();
        Set<Integer> serverIDs = servers.stream().map(Server::getServerID).collect(Collectors.toSet());
        Map<URI, ServerData> shares = new HashMap<>();
        servers.forEach(server -> {
            BigInteger beta = BigInteger.valueOf(beta(server.getServerID(), serverIDs));
            BigInteger secretShare = secretPolynomial.apply(server.getServerID()).multiply(beta);
            BigInteger nonceShare = noncePolynomial.apply(server.getServerID()).multiply(beta);
            shares.put(server.getUri().resolve(Construction.NONCE.getEndpoint()), new ServerData(secretShare, nonceShare));
        });
        return new NonceDistributionData(shares, proofComponent);
    }

    protected Function<Integer, BigInteger> generatePolynomial(BigInteger secret, BigInteger field) {
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
            BigInteger res = secret;
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
