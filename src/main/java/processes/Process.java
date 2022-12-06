package processes;

import processes.centralized.MessageCentralized;
import processes.centralized.MessageTypeCentralized;
import processes.lamport.MessageTypeLamport;
import processes.socket.ServerHandlerCentralized;
import processes.socket.ServerHandlerLamport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.Semaphore;

public class Process extends Thread{

    boolean haveToken;
    int leader = 0;
    ArrayDeque<Integer> pendingQueue;
    public int processId;
    int numberProcesses;

    int ownPort;
    int ownCommunicationPort;
    int[] socketPorts;
    int[] childPorts;

    Socket[] childSockets;

    Boolean[] childFlags;

    Socket clientSocket;
    ServerSocket serverScokets;

    ServerSocket heavyWeightServerSocket;

    Semaphore semaphore = new Semaphore(1);

    CommunicationServerHandler communicationServerHandler;

    public Process(int numberProcesses, int processId, int[] socketPorts, int[] childPorts, int[] communicationPorts) {
        this.pendingQueue = new ArrayDeque<Integer>();
        this.processId = processId;
        this.numberProcesses = numberProcesses;
        this.socketPorts = socketPorts;
        this.childPorts = childPorts;
        this.childSockets = new Socket[childPorts.length];
        this.childFlags = new Boolean[childPorts.length];

        Arrays.fill(childFlags, true);

        haveToken = processId == leader;

        for (int i = 0; i < numberProcesses; i++) {
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

        communicationServerHandler = new CommunicationServerHandler(this);
        communicationServerHandler.start();


        ServerHandlerCentralized serverHandlerCentralized = new ServerHandlerCentralized(this, socketPorts, serverScokets);
        serverHandlerCentralized.start();
    }

    public synchronized void requestCS() {

        // If I'm the leader but not have token wait to receive new leader or get token
        while (leaderHasToken());

        DataOutputStream dataOutputStream;

        while (true) {
            try {
                clientSocket = new Socket(InetAddress.getLocalHost() , socketPorts[leader]);
                dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                dataOutputStream.writeUTF(MessageTypeCentralized.Request.toString());
                dataOutputStream.writeInt(processId);
                String pq = pendingQueue.toString();
                String replace = pq.replace("[","");
                String replace1 = replace.replace("]","");
                dataOutputStream.writeUTF(replace1);
                break;
            } catch (IOException e) {
                dataOutputStream = null;
            }
        }

        //SEND REQUEST
        while (!tokenValid());

        try {
            clientSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean leaderHasToken() {
        boolean valuetoReturn;
        try {
            semaphore.acquire();
            valuetoReturn = haveToken != (leader == processId);
            semaphore.release();

        } catch (InterruptedException e) {
            valuetoReturn = true;
        }
        return valuetoReturn;
    }

    public boolean tokenValid() {
        boolean token;
        try {
            semaphore.acquire();
            token = haveToken;
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return token;
    }

    public synchronized void releaseCS() {
        try {
            semaphore.acquire();
            DataOutputStream dataOutputStream;

            while (true) {
                try {
                    clientSocket = new Socket(InetAddress.getLocalHost() , socketPorts[leader]);
                    dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                    dataOutputStream.writeUTF(MessageTypeCentralized.Release.toString());
                    dataOutputStream.writeInt(processId);
                    String pq = pendingQueue.toString();
                    String replace = pq.replace("[","");
                    String replace1 = replace.replace("]","");
                    dataOutputStream.writeUTF(replace1);
                    break;
                } catch (IOException e) {
                    dataOutputStream = null;
                }
            }

            haveToken = false;

            try {
                clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleMessage(MessageCentralized messageCentralized, int port) {
        try {
            semaphore.acquire();
            if (messageCentralized.messageTypeCentralized == MessageTypeCentralized.Request) {
                if (haveToken && messageCentralized.id != processId) {
                    pendingQueue.add(messageCentralized.id);
                }
            }else if (messageCentralized.messageTypeCentralized == MessageTypeCentralized.Release) {
                if (messageCentralized.id == processId) {
                    if (!pendingQueue.isEmpty()) {
                        int pid = pendingQueue.poll();
                        while (true) {
                            try {
                                Socket socket = new Socket(InetAddress.getLocalHost(), socketPorts[pid]);
                                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                                //Send back update
                                dataOutputStream.writeUTF(MessageTypeCentralized.SetToken.toString());
                                dataOutputStream.writeInt(pid);
                                String pq = pendingQueue.toString();
                                String replace = pq.replace("[","");
                                String replace1 = replace.replace("]","");
                                dataOutputStream.writeUTF(replace1);
                                //System.out.println("SENDING QUEUE (ID " + processId + "): " + pendingQueue);
                                pendingQueue.clear();
                                break;
                            } catch (IOException e) {
                                Socket socket = null;
                            }
                        }
                    }
                }
            }else if (messageCentralized.messageTypeCentralized == MessageTypeCentralized.SetToken) {
                haveToken = true;
                pendingQueue = new ArrayDeque<Integer>(messageCentralized.priorityQueue);
                //System.out.println("QUEUE RECEIVED (ID " + processId + "): " + pendingQueue);

                // Send update leader to all processes
                for (int i = 0; i < socketPorts.length; i++) {
                    while (true) {
                        try {
                            Socket socket = new Socket(InetAddress.getLocalHost(), socketPorts[i]);
                            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                            //Send back update
                            dataOutputStream.writeUTF(MessageTypeCentralized.UpdateLeader.toString());
                            dataOutputStream.writeInt(processId);
                            String pq = pendingQueue.toString();
                            String replace = pq.replace("[","");
                            String replace1 = replace.replace("]","");
                            dataOutputStream.writeUTF(replace1);
                            break;
                        } catch (IOException e) {
                            Socket socket = null;
                        }
                    }
                }

            }else if (messageCentralized.messageTypeCentralized == MessageTypeCentralized.UpdateLeader) {
                leader = messageCentralized.id;
            }
            semaphore.release();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void notifyLightWeights() {
        for (int i = 0; i < childSockets.length; i++) {
            if (childSockets[i] != null) {
                try {
                    DataOutputStream dataOutputStream = new DataOutputStream(childSockets[i].getOutputStream());
                    dataOutputStream.writeUTF(LightWeightHeavyWeightCommunication.GreenFlag.toString());
                    childFlags[i] = false;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public boolean waitLightWeights() {
        for (int i = 0; i < childSockets.length; i++) {
            if (childSockets[i] != null) {
                try {
                    DataInputStream dataInputStream = new DataInputStream(childSockets[i].getInputStream());
                    String result = dataInputStream.readUTF();

                    childFlags[i] = LightWeightHeavyWeightCommunication.valueOf(result) == LightWeightHeavyWeightCommunication.GreenFlag;

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return Arrays.stream(childFlags).allMatch(Boolean::valueOf);
    }

    @Override
    public void run() {

        while(true){
            requestCS();

            notifyLightWeights();

            while (!waitLightWeights());

            releaseCS();
        }

        /*for (int i = 0; i < childSockets.length; i++) {
            if (childSockets[i] != null) {
                try {
                    childSockets[i].close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }*/
    }
}
