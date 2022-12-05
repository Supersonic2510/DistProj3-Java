package processes.socket;

import processes.ProcessAChild;
import processes.lamport.MessageLamport;
import processes.lamport.MessageTypeLamport;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.time.Instant;

public class ServerHandlerLamport extends Thread {
    ProcessAChild processAChild;
    int[] clientPorts;
    ServerSocket serverScokets;

    public ServerHandlerLamport(ProcessAChild processAChild, int[] clientPorts, ServerSocket serverScokets) {
        this.processAChild = processAChild;
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

                    MessageTypeLamport messageTypeLamport = MessageTypeLamport.valueOf(dataInputStream.readUTF());
                    int id = dataInputStream.readInt();
                    Instant instant = Instant.parse(dataInputStream.readUTF());

                    //System.out.println("INSTANT IS " + instant + "TYPE: " + messageTypeLamport + " IN SERVER " + processAChild.processId + " ID REQUEST " + id);

                    // If received some message then use handler
                    synchronized (this) {
                        processAChild.handleMessage(new MessageLamport(messageTypeLamport, id, instant), clientPorts[id]);
                    }

                    localClient.close();

                    //System.out.println("CLOSE");
                } catch (IOException e) {

                }
            }
        }
    }
}
