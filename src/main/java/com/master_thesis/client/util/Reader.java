package com.master_thesis.client.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

@Component
public class Reader {

    private Queue<Integer> queue;
    private int[] staticInput;
    private Queue<String[]> records;
    private HashMap<String, LinkedList<Integer>> dateRecords;

    @Value("${input_file_path}")
    private String filepath;

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


    public Integer readValue() {
        if (staticInput == null)
            initiate();

        queue.add(queue.peek());
        return queue.poll();
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

    public Integer readValue(int i) {
        return staticInput[i % staticInput.length];
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

}
