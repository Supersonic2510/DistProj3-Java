package main;

import processes.Process;
import processes.ProcessAChild;
import processes.ProcessBChild;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Thread.sleep;

public class Main {
    public static void main(String[] args) {

        int[] ports = new int[]{8000, 8001, 8002, 8003, 8004};
        LinkedList<Process> processAChildLinkedList = new LinkedList<>();
        for (int i = 0; i < ports.length; i++) {
            processAChildLinkedList.add(new Process(ports.length, i, ports));
        }

        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < processAChildLinkedList.size(); i++) {
            processAChildLinkedList.get(i).start();
        }

        while (true);
    }
}