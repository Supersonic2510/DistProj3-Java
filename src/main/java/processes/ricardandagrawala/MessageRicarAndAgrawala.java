package processes.ricardandagrawala;

import processes.lamport.MessageTypeLamport;

import java.time.Instant;

public class MessageRicarAndAgrawala {
    public MessageTypeRicarAndAgrawala messageTypeRicarAndAgrawala;
    public int id;
    public Instant instant;

    public MessageRicarAndAgrawala(MessageTypeRicarAndAgrawala messageTypeRicarAndAgrawala, int id, Instant instant) {
        this.messageTypeRicarAndAgrawala = messageTypeRicarAndAgrawala;
        this.id = id;
        this.instant = instant;
    }

    @Override
    public String toString() {
        return messageTypeRicarAndAgrawala.toString() + "-" + id + "-" +instant.toString();
    }
}
