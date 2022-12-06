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

        int[] ports = new int[]{8000, 8001};
        int[] communicationPorts = new int[]{8007,8008};
        int[][] processChildPorts = new int[][]{{8002, 8003, 8004}, {8005, 8006}};
        int[][] communicationChildPorts = new int[][]{{8009, 8010, 8011}, {8012, 8013}};
        LinkedList<ProcessAChild> processAChildLinkedList = new LinkedList<>();
        LinkedList<ProcessBChild> processBChildLinkedList = new LinkedList<>();
        LinkedList<Process> processLinkedList = new LinkedList<>();
        for (int i = 0; i < ports.length; i++) {
            processLinkedList.add(new Process(ports.length, i, ports, communicationChildPorts[i], communicationPorts));
        }

        for (int i = 0; i < processChildPorts[0].length; i++) {
            processAChildLinkedList.add(new ProcessAChild(processChildPorts[0].length, i, processChildPorts[0], communicationPorts[0], communicationChildPorts[0]));
        }

        for (int i = 0; i < processChildPorts[1].length; i++) {
            processBChildLinkedList.add(new ProcessBChild(processChildPorts[1].length, i, processChildPorts[1], communicationPorts[1], communicationChildPorts[1]));
        }

        try {
            sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < processLinkedList.size(); i++) {
            processLinkedList.get(i).start();
        }

        for (int i = 0; i < processChildPorts[0].length; i++) {
            processAChildLinkedList.get(i).start();
        }

        for (int i = 0; i < processChildPorts[1].length; i++) {
            processBChildLinkedList.get(i).start();
        }

        while (true);
    }
}