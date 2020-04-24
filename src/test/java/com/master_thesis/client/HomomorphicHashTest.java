package com.master_thesis.client;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.master_thesis.client.data.Server;
import com.master_thesis.client.util.PublicParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HomomorphicHashTest {

    PublicParameters pp;
    HomomorphicHash hh;
    private static final Logger log = (Logger) LoggerFactory.getLogger(HomomorphicHash.class);

    @BeforeEach
    void setUp() {
        log.setLevel(Level.OFF);
        BigInteger generator = BigInteger.valueOf(191);
        BigInteger prime = BigInteger.ONE.shiftLeft(107).subtract(BigInteger.ONE);
        int numberOfServers = 10;
        int tSecurity = 20;
        List<Server> serverList = new LinkedList<>();

        for (int i = 0; i < numberOfServers; i++) {
            Server tmpServer = new Server();
            tmpServer.setUri(URI.create("localhost:200" + i));
            tmpServer.setServerID(i);
            serverList.add(tmpServer);
        }

        pp = Mockito.mock(PublicParameters.class);
        Mockito.when(pp.getSubstationID()).thenReturn(0);
        Mockito.when(pp.getGenerator(Mockito.anyInt())).thenReturn(generator);
        Mockito.when(pp.getFieldBase(Mockito.anyInt())).thenReturn(prime);
        Mockito.when(pp.getServers()).thenReturn(serverList);
        Mockito.when(pp.getSecurityThreshold()).thenReturn(tSecurity);

        hh = new HomomorphicHash(pp);
    }

    @org.junit.jupiter.api.Test
    void shareSecret() {
        hh.shareSecret(5);
    }
}