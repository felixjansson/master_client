package com.master_thesis.client;

import com.master_thesis.client.data.*;
import com.master_thesis.client.util.NoiseGenerator;
import com.master_thesis.client.util.PublicParameters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigInteger;


@Component("dp")
public class DifferentialPrivacy extends HomomorphicHash {

    private NoiseGenerator noiseGenerator;

    @Autowired
    public DifferentialPrivacy(PublicParameters publicParameters, NoiseGenerator noiseGenerator) {
        super(publicParameters);
        this.noiseGenerator = noiseGenerator;
    }

    /**
     * This is the share secret function from the Homomorphic Hash construction.
     *
     * @param secret the input is the secret that should be shared.
     * @return An object with data that should be sent.
     */
    public DifferentialPrivacyData shareSecret(BigInteger secret, int substationID) {
        return new DifferentialPrivacyData(super.shareSecret(noiseGenerator.addNoise(secret), substationID));
    }



}
