package com.master_thesis.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DummySecretShareTest {

    @Test
    void shareSecret() {
        int secret = 3;
        DummySecretShare dss = new DummySecretShare();
        List<ShareObject> res = dss.shareSecret(secret, Stream.of(URI.create(""), URI.create("")).collect(Collectors.toCollection(ArrayList::new)));
        int val = 0;
        for (ShareObject re : res) {
            val += re.getShare();
        }
        assertEquals(secret, val);
    }
}