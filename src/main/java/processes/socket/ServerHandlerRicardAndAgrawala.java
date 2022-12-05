package processes.socket;

import processes.ProcessAChild;
import processes.ProcessBChild;
import processes.lamport.MessageLamport;
import processes.lamport.MessageTypeLamport;
import processes.ricardandagrawala.MessageRicarAndAgrawala;
import processes.ricardandagrawala.MessageTypeRicarAndAgrawala;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;

public class ServerHandlerRicardAndAgrawala extends Thread {
    ProcessBChild processBChild;
    int[] clientPorts;
    ServerSocket serverScokets;

    public ServerHandlerRicardAndAgrawala(ProcessBChild processBChild, int[] clientPorts, ServerSocket serverScokets) {
        this.processBChild = processBChild;
        this.clientPorts = clientPorts;
        this.serverScokets = serverScokets;
    }

    @Override
    public void run() {
        while (true) {
            for (int i = 0; i < clientPorts.length; i++) {
                try {
                    // Gets a connection
                    Socket localClient = serverScokets.accept();
                    DataInputStream dataInputStream = new DataInputStream(localClient.getInputStream());

                    MessageTypeRicarAndAgrawala messageTypeRicarAndAgrawala = MessageTypeRicarAndAgrawala.valueOf(dataInputStream.readUTF());
                    int id = dataInputStream.readInt();
                    Instant instant = Instant.parse(dataInputStream.readUTF());

                    //System.out.println("INSTANT IS " + instant + "TYPE: " + messageTypeRicarAndAgrawala + " IN SERVER " + processBChild.processId + " ID REQUEST " + id);

                    // If received some message then use handler
                    synchronized (this) {
                        processBChild.handleMessage(new MessageRicarAndAgrawala(messageTypeRicarAndAgrawala, id, instant), clientPorts[id]);
                    }

                    localClient.close();

                    //System.out.println("CLOSE");
                } catch (IOException e) {

                }
            }
        }
    }
}
