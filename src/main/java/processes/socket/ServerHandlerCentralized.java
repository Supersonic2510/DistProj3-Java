package processes.socket;

import processes.Process;
import processes.ProcessAChild;
import processes.centralized.MessageCentralized;
import processes.centralized.MessageTypeCentralized;
import processes.lamport.MessageLamport;
import processes.lamport.MessageTypeLamport;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.*;

public class ServerHandlerCentralized extends Thread{
    Process process;
    int[] clientPorts;
    ServerSocket serverScokets;

    public ServerHandlerCentralized(Process process, int[] clientPorts, ServerSocket serverScokets) {
        this.process = process;
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

                    MessageTypeCentralized messageTypeCentralized = MessageTypeCentralized.valueOf(dataInputStream.readUTF());
                    int id = dataInputStream.readInt();
                    String value = dataInputStream.readUTF();
                    List<String> arrayList = new ArrayList<String>(Arrays.asList(value.split(",")));
                    ArrayDeque<Integer> priorityQueue = new ArrayDeque<Integer>();
                    for(String fav : arrayList){
                        try {
                            priorityQueue.add(Integer.parseInt(fav.trim()));
                        }catch (NumberFormatException e) {
                        }
                    }

                    //System.out.println("TYPE: " + messageTypeCentralized + " IN SERVER " + process.processId + " ID REQUEST " + id);

                    // If received some message then use handler
                    synchronized (this) {
                        process.handleMessage(new MessageCentralized(messageTypeCentralized, id, priorityQueue), clientPorts[id]);
                    }

                    localClient.close();

                    //System.out.println("CLOSE");
                } catch (IOException e) {

                }
            }
        }
    }
}
