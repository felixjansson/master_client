package com.master_thesis.client.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

@Component
public class Reader {

    private Queue<Integer> queue;
    private int[] staticInput;

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


    public Integer readValue(int i) {
        return staticInput[i%staticInput.length];
    }
}
