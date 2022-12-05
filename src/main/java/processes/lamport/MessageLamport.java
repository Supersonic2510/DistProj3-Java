package processes.lamport;

import processes.ProcessAChild;

import java.time.Instant;
import java.util.UUID;


public class MessageLamport {
    public MessageTypeLamport messageTypeLamport;
    public int id;
    public Instant instant;

    public MessageLamport(MessageTypeLamport messageTypeLamport, int id, Instant instant) {
        this.messageTypeLamport = messageTypeLamport;
        this.id = id;
        this.instant = instant;
    }

    @Override
    public String toString() {
        return messageTypeLamport.toString() + "-" + id + "-" +instant.toString();
    }
}
