package com.master_thesis.client.util;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.*;

@Component
public class Reader {

    private Queue<Integer> queue;
    private int[] staticInput;
    private String filePath;

    private Random random = new SecureRandom();
    private BigInteger value;
    private ReadMode readmode;
    private int bits;

    public Reader() {
        readmode = ReadMode.bits;
        bits = 10;
    }

    private void initiate() {
        try {
            File file = new File(filePath);
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
        switch (readmode) {
            case file:
                return BigInteger.valueOf(readFromFile());
            case value:
                return value;
            case bits:
                BigInteger val;
                do {
                    val = new BigInteger(bits, random);
                } while (val.bitLength() < bits);
                return val;
        }
        throw new RuntimeException("readValue went terribly wrong");
    }

    public void setSecretMode(Scanner input) {
        System.out.println("What should the secret be? [File] [Value] [Bits]");
        switch (input.nextLine().toLowerCase()) {
            case "f":
            case "file":
                readmode = ReadMode.file;
                System.out.print("Enter file path: ");
                filePath = input.nextLine().trim();
                staticInput = null;
                break;
            case "v":
            case "value":
                readmode = ReadMode.value;
                System.out.print("Enter secret value: ");
                value = input.nextBigInteger();
                input.nextLine();
                break;
            case "b":
            case "bits":
                readmode = ReadMode.bits;
                System.out.print("Enter number of bits in secret: ");
                bits = input.nextInt();
                input.nextLine();
                break;

        }
    }

    private enum ReadMode {
        file, value, bits
    }
}
