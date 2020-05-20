package com.master_thesis.client.util;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

@Component
public class Reader {

    private static final Logger log = (Logger) LoggerFactory.getLogger(Reader.class);

    private Queue<Integer> queue;
    private int[] staticInput;
    private Queue<String[]> records;
    private HashMap<String, LinkedList<Integer>> dateRecords;

    @Value("${input_file_path}")
    private String filepath;

    @Value("${read_mode}")
    private String readMode;

    @Value("${secret_bits}")
    private int secret_bits;

    private Random random = new SecureRandom();

    private void initiate() {
        try {
            File file = new File(filepath);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            queue = new LinkedList<>();
            String line;
            while ((line = br.readLine()) != null) {
                queue.add(Integer.parseInt(line));
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert queue != null;
        staticInput = queue.stream().mapToInt(Integer::intValue).toArray();
    }

    private Integer readFromFile() {
        if (staticInput == null)
            initiate();

        queue.add(queue.peek());
        return queue.poll();
    }

    public BigInteger readValue() {
        switch (readMode) {
            case "file":
                return BigInteger.valueOf(readFromFile());
            case "random":
                return BigInteger.valueOf(random.nextInt(1000));
            case "bits":
                BigInteger val;
                do {
                    val = new BigInteger(secret_bits, random);
                } while (val.bitLength() < secret_bits);
                return val;
            default:
                log.error("No valid reader mode selected. Using random!");
                return BigInteger.valueOf(random.nextInt(1000));
        }
    }

    public Integer readValue(int i) {
        return staticInput[i % staticInput.length];
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String getReaderMode() {
        return readMode;
    }

    public int getSecretBits() {
        return secret_bits;
    }

    public Integer readValueFromCSV() {
        if (records == null) {
            initiateCSV();
            records.remove(); // Remove headers
        }
        String[] record = records.poll();
        assert record != null;
        return (int) Double.parseDouble(record[5]);
    }

    @Nullable
    public LinkedList<Integer> readValuesMappedOnTimeFromCSV() {
        if (dateRecords == null) {
            initiateDateRecordsFromCSV();
        }
        if (!dateRecords.isEmpty()) {
            String anyKey = dateRecords.keySet().iterator().next();
            return dateRecords.remove(anyKey);
        } else {
            return null;
        }
    }

    private void initiateCSV() {
        try {
            File file = new File(filepath);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            records = new LinkedList<>();
            String headers = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] recordValues = line.split(",");
                records.add(recordValues);
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initiateDateRecordsFromCSV() {
        try {
            File file = new File(filepath);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            dateRecords = new HashMap<>();
            String headers = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] recordValues = line.split(",");
                String date = recordValues[3];
                dateRecords.putIfAbsent(date, new LinkedList<>());
                int consumption = (int) Double.parseDouble(recordValues[5]);
                dateRecords.get(date).add(consumption);
            }
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
