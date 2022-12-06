package processes;

import processes.lamport.MessageLamport;
import processes.lamport.MessageTypeLamport;
import processes.socket.ServerHandlerLamport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

public class ProcessAChild extends LightweightProcess {

    Instant[] queue;
    public int processId;
    int numberProcesses;

    int ownPort;
    int ownCommunicationPort;
    int[] socketPorts;

    Socket[] clientSockets;
    ServerSocket serverScokets;

    Instant ownInstant;

    int parentPort;

    Socket parentSocket;

    boolean flag;

    Semaphore semaphore = new Semaphore(1);

    public ProcessAChild(int numberProcesses, int processId, int[] socketPorts, int parentPort, int[] communicationPorts) {
        this.queue = new Instant[numberProcesses];
        this.processId = processId;
        this.numberProcesses = numberProcesses;
        this.socketPorts = socketPorts;
        this.parentPort = parentPort;
        this.flag = false;

        clientSockets = new Socket[numberProcesses];

        for (int i = 0; i < numberProcesses; i++) {
            queue[i] = Instant.MIN;

            if (i == processId) {
                this.ownPort = socketPorts[i];
                this.ownCommunicationPort = communicationPorts[i];
                try {
                    serverScokets = new ServerSocket(socketPorts[i], 5 + numberProcesses);
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

        ServerHandlerLamport serverHandlerLamport = new ServerHandlerLamport(this, socketPorts, serverScokets);
        serverHandlerLamport.start();
    }

    public synchronized void requestCS() {
        try {
            semaphore.acquire();
            queue[processId] = Instant.now();
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        DataOutputStream dataOutputStream;

        for (int i = 0; i < socketPorts.length; i++) {
            while (true) {
                //Send message per each process to confirm (REQUEST)
                try {
                    clientSockets[i] = new Socket(InetAddress.getLocalHost() , socketPorts[i]);
                    dataOutputStream = new DataOutputStream(clientSockets[i].getOutputStream());
                    dataOutputStream.writeUTF(MessageTypeLamport.Request.toString());
                    dataOutputStream.writeInt(processId);
                    dataOutputStream.writeUTF(queue[processId].toString());
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

    public synchronized void releaseCS() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        queue[processId] = Instant.MAX;
        semaphore.release();


        for (int i = 0; i < socketPorts.length; i++) {
            //Send message per each process
            try {
                clientSockets[i] = new Socket(InetAddress.getLocalHost() , socketPorts[i]);
                DataOutputStream dataOutputStream = new DataOutputStream(clientSockets[i].getOutputStream());
                dataOutputStream.writeUTF(MessageTypeLamport.Release.toString());
                dataOutputStream.writeInt(processId);
                dataOutputStream.writeUTF(queue[processId].toString());
                clientSockets[i].close();
            } catch (IOException e) {
                clientSockets[i] = null;
            }
        }
    }

    private boolean okayCS() {
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < queue.length; i++) {
            if (isGreater(queue[processId], processId,  queue[i], i)){
                semaphore.release();
                return false;
            }
        }
        semaphore.release();
        return true;
    }

    private boolean isGreater(Instant instant1, int pid1, Instant instant2, int pid2) {
        if (instant2.equals(Instant.MAX)){
            return false;
        }
        return ((instant1.compareTo(instant2) > 0)
                || ((instant1.compareTo(instant2) == 0)
                && pid1 > pid2));
    }


    public void handleMessage(MessageLamport message, int port){
        Instant timeStamp = message.instant;
        if (message.messageTypeLamport == MessageTypeLamport.Request) {

            try {
                semaphore.acquire();
                queue[message.id] = timeStamp;
                semaphore.release();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            while (true) {
                try {
                    Socket socket = new Socket(InetAddress.getLocalHost(), port);
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    //Send back ack
                    dataOutputStream.writeUTF(MessageTypeLamport.Ack.toString());
                    dataOutputStream.writeInt(processId);
                    dataOutputStream.writeUTF(queue[message.id].toString());
                    break;
                } catch (IOException e) {
                    Socket socket = null;
                }
            }
        }else if (message.messageTypeLamport == MessageTypeLamport.Release) {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (message.id != processId) {
                queue[message.id] = Instant.MAX;
            }
            semaphore.release();
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
                System.out.println("I am the Process A lightweight: "+ processId + " printed times: " + (1 + i));
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
