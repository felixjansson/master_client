package com.master_thesis.client.differentialprivacy;

import com.master_thesis.client.util.Reader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

class LaplaceNoiseTest {

    private LaplaceNoise laplaceNoise;
    int epsilon_exponent = 4;
    int l1Sensitivity = 1;

    @BeforeEach
    void setup() {
        this.laplaceNoise = new LaplaceNoise();
    }

    @Test
    void addDoubleLaplaceNoise() {
        double secret = 10d;
        int l0Sensitivity = 1;
        double lInfSensitivity = 10;
        double epsilon = (1 << 18);
        double noisySecret = laplaceNoise.addNoise(secret, l0Sensitivity, lInfSensitivity, epsilon, null);
    }

    @Test
    void addLongLaplaceNoise() {
        long secret = 10L;
        int l0Sensitivity = 1;
        long lInfSensitivity = 10L;
        double epsilon = 1 / Math.pow(2d, 4);
        long noisySecret = laplaceNoise.addNoise(secret, l0Sensitivity, epsilon, null);
    }

    @Test
    /**
     * This Test reads the entire user data csv file, maps the dates and compute the sum and noise_sum.
     * Writes the result to another csv file. Note, this is the best test ever!
     */
    void compareNoisySumAndSum() {
        long sum = 0;
        long noisySum = 0;
        int l0Sensitivity = 1;
        double epsilon = 1 / Math.pow(2d, 4);
        Reader reader = new Reader();
        reader.setFilepath("src/main/resources/testdata.csv");
        LinkedList<Integer> consumptions;
        long diff;
        do {
            consumptions = reader.readValuesMappedOnTimeFromCSV();
            assert consumptions != null;
            long internalSum = consumptions.stream().reduce(0, Integer::sum);
            long internalNoisySum = addNoiseAndAggregate(consumptions);
            diff = Math.max(internalSum, internalNoisySum) - Math.min(internalSum, internalNoisySum);
            writeToCSV(internalSum, internalNoisySum, diff);
        } while (consumptions != null);
    }

    private void writeToCSV(long internalSum, long internalNoisySum, long diff) {
        try {
            double percentage = (float)Math.max(internalSum, internalNoisySum) / Math.min(internalSum, internalNoisySum);
            FileWriter csvWriter = new FileWriter("src/main/resources/dp_sum_sample.csv",true);
            csvWriter.write(internalSum + "," + internalNoisySum + "," + diff + "," + epsilon_exponent + "," + l1Sensitivity + "," + percentage + "\n");
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long addNoiseAndAggregate(LinkedList<Integer> consumptions) {
        double epsilon = 1 / Math.pow(2, epsilon_exponent);
        return consumptions.stream().map(x -> laplaceNoise.addNoise(x, l1Sensitivity, epsilon, null)).reduce(0L, Long::sum);
    }

}