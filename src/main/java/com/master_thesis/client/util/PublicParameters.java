package com.master_thesis.client.util;


import com.master_thesis.client.data.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;


@Component
@Qualifier("Dummy")
public class PublicParameters {

    private HttpAdapter httpAdapter;

    @Autowired
    public PublicParameters(HttpAdapter httpAdapter) {
        this.httpAdapter = httpAdapter;
    }

    public List<Server> getServers() {
        return httpAdapter.getServers();
    }

    public int getSubstationID() {
        return 0; // TODO: 27/03/2020 Ask Coordinator
    }


    public BigInteger getGenerator(int substationID) {
        return httpAdapter.getGenerator(substationID);
    }


    public BigInteger getFieldBase(int substationID) {
        return httpAdapter.getFieldBase(getSubstationID());
    }

    public int getSecurityThreshold() {
        return httpAdapter.getTSecurity(getSubstationID());
    }

}
