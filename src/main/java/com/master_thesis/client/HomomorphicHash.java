package com.master_thesis.client;

import cc.redberry.rings.Ring;
import cc.redberry.rings.Rings;
import cc.redberry.rings.bigint.BigInteger;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


@Component("hash")
public class HomomorphicHash implements ClientSecretSharing {

    private PublicParameters publicParameters;
    private static final Logger log = (Logger) LoggerFactory.getLogger(HomomorphicHash.class);

    @Autowired
    public HomomorphicHash(PublicParameters publicParameters) {
        this.publicParameters = publicParameters;
    }

    @Override
    public Map<URI, SecretShare> shareSecret(int int_secret) {
        BigInteger base = publicParameters.getFieldBase(0);
        BigInteger generator = publicParameters.getGenerator(0);
        Ring<BigInteger> field = Rings.Zp(base);
        BigInteger secret = BigInteger.valueOf(int_secret);

        BigInteger nonce = field.randomElement();
        BigInteger proofComponent = hash(base, secret.add(nonce), generator);//.mod(base));

        Function<Integer, BigInteger> polynomial = generatePolynomial(int_secret, field);
        List<Server> servers = publicParameters.getServers();
        HashMap<URI, SecretShare> map = new HashMap<>();
        servers.forEach(server -> {
            BigInteger share = polynomial.apply(server.getServerID());
            map.put(server.getUri(), new SecretShare(share, proofComponent, nonce));
        });

        return map;
    }

    private Function<Integer, BigInteger> generatePolynomial(int secret, Ring<BigInteger> field){
        int t = publicParameters.getSecurityThreshold();
        StringBuilder logString = new StringBuilder("Polynomial used: ").append(secret);
        ArrayList<BigInteger> coefficients = new ArrayList<>();
        for (int i = 1; i <= t; i++) {
            BigInteger a = field.getZero();
            while (a.equals(field.getZero())) {
                a = field.randomElement();
            }
            logString.append(" + ").append(a).append("x^").append(i);
            coefficients.add(a);
        }
        log.info(logString.toString());

        return (serverID) -> {
            BigInteger serverIDBIG = BigInteger.valueOf(serverID);
            BigInteger res = field.valueOf(secret);
            for (int i = 0; i < coefficients.size(); i++) {
                BigInteger coefficient = coefficients.get(i);
                BigInteger polynomial = serverIDBIG.pow(i+1);
                res = field.add(res, field.multiply(coefficient, polynomial));
            }
            return res;
        };
    }

    public BigInteger hash(BigInteger field, BigInteger input, BigInteger g) {
        return g.modPow(input, field);


    }
}
