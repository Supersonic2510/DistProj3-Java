package processes;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class CommunicationServerHandler  extends Thread{

    Process process;

    public CommunicationServerHandler(Process process) {
        this.process = process;
    }

    @Override
    public void run() {
        synchronized (this) {
            try {
                process.heavyWeightServerSocket = new ServerSocket(process.ownCommunicationPort, process.childPorts.length + 5);
                for (int i = 0; i < process.childPorts.length; i++) {
                    Socket localClient = process.heavyWeightServerSocket.accept();
                    DataInputStream dataInputStream = new DataInputStream(localClient.getInputStream());

                    int childId = dataInputStream.readInt();
                    int portId = dataInputStream.readInt();

                    if (process.childPorts[childId] == portId) {
                        process.childSockets[childId] = localClient;
                    }
                }
            }catch (IOException exception){
                throw new RuntimeException();
            }
        }
    }
}
