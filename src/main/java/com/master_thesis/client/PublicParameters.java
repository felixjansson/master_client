package com.master_thesis.client;

import cc.redberry.rings.bigint.BigInteger;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;


public interface PublicParameters {

    List<Server> getServers();

    int getTransformatorID();

    BigInteger getFieldBase();

    int getSecurityThreshold();
}
