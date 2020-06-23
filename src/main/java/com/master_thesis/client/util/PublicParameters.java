package com.master_thesis.client.util;


import com.master_thesis.client.data.LinearSignatureData;
import com.master_thesis.client.data.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;


@Component
public class PublicParameters {

    private HttpAdapter httpAdapter;

    @Autowired
    public PublicParameters(HttpAdapter httpAdapter) {
        this.httpAdapter = httpAdapter;
    }

    public List<Server> getServers() {
        return httpAdapter.getServers();
    }

    public BigInteger getGenerator(int substationID) {
        return httpAdapter.getGenerator(substationID);
    }


    public BigInteger getFieldBase(int substationID) {
        return httpAdapter.getFieldBase(substationID);
    }

    public int getSecurityThreshold(int substationID) {
        return httpAdapter.getTSecurity(substationID);
    }

    public LinearSignatureData.PublicData getLinearPublicData(int substationID, int fid) {
        return httpAdapter.getLinearPublicData(substationID, fid);

    }

    public BigInteger[] getRsaN(int substationID) {
        return httpAdapter.getRSASecretPrimes(substationID);
    }
}
