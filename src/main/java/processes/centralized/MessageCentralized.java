package processes.centralized;

import processes.lamport.MessageTypeLamport;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.PriorityQueue;

public class MessageCentralized {
    public MessageTypeCentralized messageTypeCentralized;
    public int id;

    public ArrayDeque<Integer> priorityQueue;

    public MessageCentralized(MessageTypeCentralized messageTypeCentralized, int id, ArrayDeque<Integer> priorityQueue) {
        this.messageTypeCentralized = messageTypeCentralized;
        this.id = id;
        this.priorityQueue = priorityQueue;
    }

    @Override
    public String toString() {
        return messageTypeCentralized.toString() + "-" + id + "-" + priorityQueue.toString();
    }
}
