package com.master_thesis.client;

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

    public Reader() {
        try {
            File file = new File("src/Demo.txt");    //creates a new file instance
            FileReader fr = new FileReader(file);   //reads the file
            BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
            queue = new LinkedList<>();    //constructs a string buffer with no characters
            String line;
            while ((line = br.readLine()) != null) {
                queue.add(Integer.parseInt(line));      //appends line to string buffer
            }
            fr.close();    //closes the stream and release the resources
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert queue != null;
        staticInput = queue.stream().mapToInt(Integer::intValue).toArray();

    }

    public Integer readValue() {
        queue.add(queue.peek());
        return queue.poll();
    }

    public Integer readValue(int i) {
        return staticInput[i%staticInput.length];
    }
}
