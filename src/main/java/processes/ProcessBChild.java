package processes;

import org.javatuples.Pair;
import processes.ricardandagrawala.MessageRicarAndAgrawala;
import processes.ricardandagrawala.MessageTypeRicarAndAgrawala;
import processes.socket.ServerHandlerLamport;
import processes.socket.ServerHandlerRicardAndAgrawala;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Semaphore;

public class ProcessBChild  extends Thread{

    PriorityQueue<Pair<Instant, Integer>> queue;
    public int processId;
    int numberProcesses;

    int ownPort;
    int ownCommunicationPort;
    int[] socketPorts;
    boolean[] okArray;

    Socket[] clientSockets;
    ServerSocket serverScokets;

    Instant ownInstant;
    Semaphore semaphore = new Semaphore(1);

    int parentPort;

    Socket parentSocket;

    boolean flag;

    public ProcessBChild(int numberProcesses, int processId, int[] socketPorts, int parentPort, int[] communicationPorts) {
        this.queue = new PriorityQueue<Pair<Instant, Integer>>();
        this.processId = processId;
        this.numberProcesses = numberProcesses;
        this.socketPorts = socketPorts;
        this.parentPort = parentPort;
        this.flag = false;

        clientSockets = new Socket[numberProcesses];
        okArray = new boolean[numberProcesses];

        for (int i = 0; i < numberProcesses; i++) {
            if (i == processId) {
                this.ownPort = socketPorts[i];
                this.ownCommunicationPort = communicationPorts[i];
                try {
                    serverScokets = new ServerSocket(socketPorts[i], 2 * (numberProcesses - 1) + 5);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            parentSocket = new Socket(InetAddress.getLocalHost(), parentPort);
            DataOutputStream dataOutputStream = new DataOutputStream(parentSocket.getOutputStream());

            dataOutputStream.writeInt(processId);
            dataOutputStream.writeInt(ownCommunicationPort);

        }catch (IOException exception){
            throw new RuntimeException();
        }

        Arrays.fill(okArray, false);

        ServerHandlerRicardAndAgrawala serverHandlerRicardAndAgrawala = new ServerHandlerRicardAndAgrawala(this, socketPorts, serverScokets);
        serverHandlerRicardAndAgrawala.start();
    }

    private synchronized void requestCS() {
        ownInstant = Instant.now();
        DataOutputStream dataOutputStream;

        try {
            semaphore.acquire();
            Arrays.fill(okArray, false);
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < socketPorts.length; i++) {
            //Send message per each process to confirm (REQUEST)
            while (true) {
                try {
                    clientSockets[i] = new Socket(InetAddress.getLocalHost() , socketPorts[i]);
                    dataOutputStream = new DataOutputStream(clientSockets[i].getOutputStream());
                    dataOutputStream.writeUTF(MessageTypeRicarAndAgrawala.Request.toString());
                    dataOutputStream.writeInt(processId);
                    dataOutputStream.writeUTF(ownInstant.toString());
                    break;
                } catch (IOException e) {
                    dataOutputStream = null;
                }
            }
        }

        while (!okayCS()) {
        }

        // Close the connection
        for (int i = 0; i < socketPorts.length; i++) {
            try {
                clientSockets[i].close();
            } catch (IOException e) {
                clientSockets[i] = null;
            }
        }
    }

    private synchronized void releaseCS() {
        ownInstant = Instant.MAX;
        DataOutputStream dataOutputStream;
        while (!queue.isEmpty()) {
            Pair<Instant, Integer> instantIntegerPair = queue.poll();
            while (true) {
                try {
                    Socket socket = new Socket(InetAddress.getLocalHost() , (Integer) instantIntegerPair.getValue1());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    dataOutputStream.writeUTF(MessageTypeRicarAndAgrawala.Okay.toString());
                    dataOutputStream.writeInt(processId);
                    dataOutputStream.writeUTF(instantIntegerPair.getValue0().toString());
                    socket.close();
                    break;
                } catch (IOException e) {
                    dataOutputStream = null;
                }
            }
        }
    }

    private boolean okayCS() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < okArray.length; i++) {
            if (!okArray[i]) {
                semaphore.release();
                return false;
            }
        }
        semaphore.release();
        return true;
    }
    public void handleMessage(MessageRicarAndAgrawala message, int port){
        Instant timeStamp = message.instant;
        if (message.messageTypeRicarAndAgrawala == MessageTypeRicarAndAgrawala.Request) {

            try {
                semaphore.acquire();
                if (ownInstant.equals(Instant.MAX) || (timeStamp.compareTo(ownInstant) < 0) ||
                        (((timeStamp.compareTo(ownInstant) == 0) && message.id >= processId))) {
                    //send ok message
                    while (true) {
                        try {
                            Socket socket = new Socket(InetAddress.getLocalHost(), port);
                            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                            //Send back ack
                            dataOutputStream.writeUTF(MessageTypeRicarAndAgrawala.Okay.toString());
                            dataOutputStream.writeInt(processId);
                            dataOutputStream.writeUTF(ownInstant.toString());
                            break;
                        } catch (IOException e) {
                            Socket socket = null;
                        }
                    }
                }else {
                    queue.add(new Pair<Instant, Integer>(timeStamp, port));
                }
                semaphore.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }else if (message.messageTypeRicarAndAgrawala == MessageTypeRicarAndAgrawala.Okay) {
            try {
                semaphore.acquire();
                okArray[message.id] = true;
                semaphore.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    private void notifyHeavyWeight() {
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(parentSocket.getOutputStream());
            dataOutputStream.writeUTF(LightWeightHeavyWeightCommunication.GreenFlag.toString());
            flag = false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean waitHeavyWeight() {
        try {
            DataInputStream dataInputStream = new DataInputStream(parentSocket.getInputStream());
            String result = dataInputStream.readUTF();

            flag = LightWeightHeavyWeightCommunication.valueOf(result) == LightWeightHeavyWeightCommunication.GreenFlag;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }


    @Override
    public void run() {

        while(true){
            while (waitHeavyWeight());
            requestCS();
            for (int i=0; i<10; i++){
                System.out.println("I am the Process B lightweight: "+ processId + " printed times: " + (1 + i));
                try {
                    sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
            releaseCS();

            notifyHeavyWeight();
        }
    }
}
