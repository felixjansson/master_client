package com.master_thesis.client.differentialprivacy;

import com.master_thesis.client.util.Reader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class LaplaceNoiseTest {

    private LaplaceNoise laplaceNoise;
    int epsilon_exponent = 4;
    int l1Sensitivity = 1;
    DefaultPublicData dpd;

    @BeforeEach
    void setup() {
        this.laplaceNoise = new LaplaceNoise();
        dpd = new DefaultPublicData();
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
        Reader reader = new Reader(dpd);
        reader.setFilePath("src/main/resources/testdata.csv");
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

    @Test
    void testL0Linf() {
        double epsilon = Math.log(3);

        Reader reader = new Reader(dpd);
        System.out.println("day,hour,sum,sm1,sm2,sm3,sm4,noise sum,diff");

        List<String> times = new LinkedList<>();
        for (int i = 0; i < 10; i++) {
            times.add("2020-01-09 0" + i);
            times.add("2020-01-09 1" + i);
            if (i < 4) {
                times.add("2020-01-09 2" + i);
            }
        }
        times.sort(null);

        List<Integer> consumptions;

        Map<String, List<Long>> l1s = new HashMap<>();


        for (String time : times) {
            List<Integer> day0Consumptions = reader.readValuesMappedOnTimeFromCSV(time);
            Iterator<Integer> iterDay0 = day0Consumptions.iterator();
            StringJoiner sj = new StringJoiner(",");
            sj.add("0");
            sj.add(time.substring(11, 13));
            sj.add(Long.toString(day0Consumptions.stream().reduce(0, Integer::sum)));
            for (int i = 0; i < day0Consumptions.size(); i++) {
                l1s.put(time, new ArrayList<>());
                int x = iterDay0.next();
                sj.add(Integer.toString(x));
                l1s.get(time).add((long) (x / day0Consumptions.size()));
            }
            sj.add("N/A");
            sj.add("N/A");
            System.out.println(sj.toString());
        }


        for (int days = 1; days <= 100; days++) {
            for (String time : times) {
                StringJoiner sj = new StringJoiner(",");
                sj.add(Integer.toString(days));
                sj.add(time.substring(11, 13));
                consumptions = reader.readValuesMappedOnTimeFromCSV(time);
                assert consumptions != null;
                long sum = consumptions.stream().reduce(0, Integer::sum);
//                long sumThenNoise = laplaceNoise.addNoise(correct, l1Sensitivity, epsilon, null);
                long noiseThenSum = 0;
                List<Long> l1sofTime = l1s.get(time);

                sj.add(Long.toString(sum));
                List<Long> missingRecord = new LinkedList<>();
                for (int i = 0; i < consumptions.size(); i++) {
                    long cons = consumptions.get(i);
                    long newL1 = (l1sofTime.get(i) + cons / consumptions.size());
                    l1sofTime.add(i, newL1);
                    missingRecord.add(sum - cons);
                    noiseThenSum += laplaceNoise.addNoise(cons, Math.max(Math.round(newL1 / (float) days), 1), epsilon, null);
                    sj.add(Long.toString(l1sofTime.get(i) / days));
                }

                sj.add(Long.toString(noiseThenSum));
                sj.add(Long.toString(Math.abs(noiseThenSum - sum)));
                missingRecord.forEach(x -> sj.add(Long.toString(x)));
                System.out.println(sj.toString());
            }
        }

    }
}