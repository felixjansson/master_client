package com.master_thesis.client.util;

import com.master_thesis.client.differentialprivacy.LaplaceNoise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class NoiseGenerator {

    private static double GAUSSIAN_MEAN = 0.0;
    private LaplaceNoise laplaceNoise;
    private double epsilon = Math.log(2); // Papers say this is a nice epsilon
    private int laplaceL1 = 10;
    private double gaussianVariance;

    @Value("${noise}")
    private String noiseFunction;

    @Autowired
    public NoiseGenerator(LaplaceNoise laplaceNoise) {
        this.laplaceNoise = laplaceNoise;
    }


    public long addNoise(int input) {
        switch (noiseFunction) {
            case "laplace":
                return laplaceNoise.addNoise(input, laplaceL1, epsilon, null);
            case "gaussian":
                return input + generateGaussianIntegerNoise(GAUSSIAN_MEAN, gaussianVariance);
            default:
                throw new RuntimeException("There is no noise generator with name: " + laplaceNoise);
        }
    }

    public void computeGaussianVariance(int varianceLowerBound, int numberOfClients) {
        double delta = 2 / Math.exp(numberOfClients * Math.pow(epsilon, 2) / 64);
//        numberOfClients = Math.round(64 * Math.log(2/delta)/Math.pow(epsilon,2));
//        System.out.println("clients: " + numberOfClients + ", gauss delta: " + delta);
        this.gaussianVariance = (1.5 * varianceLowerBound) / numberOfClients;
    }

    // This function is defined according to "Our data, ..." by C.Dwork 2006.
    public int generateGaussianIntegerNoise(double mean, double variance) {
        Random r = new Random();
        return (int) Math.round(r.nextGaussian() * Math.sqrt(variance) + mean);
    }

    public String getNoiseFunction() {
        return noiseFunction;
    }
}
