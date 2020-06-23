package com.master_thesis.client.util;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Random;

@Component
public class NoiseGenerator {

    private double gaussianVariance;

    public BigInteger addNoise(BigInteger input) {
        double GAUSSIAN_MEAN = 0.0;
        return input.add(BigInteger.valueOf(generateGaussianIntegerNoise(GAUSSIAN_MEAN, gaussianVariance)));
    }

    public void computeGaussianVariance(int varianceLowerBound, int numberOfClients) {
//        double delta = 2 / Math.exp(numberOfClients * Math.pow(epsilon, 2) / 64);
//        numberOfClients = Math.round(64 * Math.log(2/delta)/Math.pow(epsilon,2));
        this.gaussianVariance = (1.5 * varianceLowerBound) / numberOfClients;
    }

    // This function is defined according to "Our data, ..." by C.Dwork 2006.
    public int generateGaussianIntegerNoise(double mean, double variance) {
        Random r = new Random();
        return (int) Math.round(r.nextGaussian() * Math.sqrt(variance) + mean);
    }

}
