package Server_B;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LightweightB {
    public static final int END_CONNECTION = 2;

    private final int N;
    private int myId;
    private int myts;
    private boolean running = true;

    private Socket socket;
    private Socket lightSocket;
    private ObjectOutputStream node_oos = null;
    private ObjectOutputStream server_oos = null;
    private ObjectInputStream server_ois = null;
    private ServerSocket serverSocket;
    private LamportClock c = new LamportClock();
    private LinkedList<Integer> pendingQ = new LinkedList<>();
    private static ReadWriteLock lock = new ReentrantReadWriteLock();
    int numOkay = 0;

    public LightweightB(int numProcesses) {
        N = numProcesses;
        this.myId = 0;
        myts = Integer.MAX_VALUE;
        initCom();
    }

    private void start() {
        while(true) {
            waitHeavyWeight();
            requestCS();
            for(int i = 0; i < 10; i++) {
                System.out.println("I am the process lightweight B" + (myId +1) + " {" + i + "}");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            releaseCS();
            notifyHeavyWeight();
        }
    }

    public synchronized void requestCS() {
        c.tick();
        myts = c.getValue();
        broadcastMsg("request", myts);
        int access = 0;
        while(access < N - 1) {
            access = readNumOkay();
        }

    }

    public synchronized void releaseCS() {
        myts = Integer.MAX_VALUE;
        while(!pendingQ.isEmpty()) {
            int pid = pendingQ.remove();
            sendMsg(pid, "okay", c.getValue());
        }
    }

    private void updateNumOkay() {
        lock.writeLock().lock();
        try {
            numOkay++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private int readNumOkay() {
        lock.readLock().lock();
        try {
            return numOkay;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void handleMessage(Wrapper m) {
        int timeStamp = m.getTimestamp();
        int src = m.getId();
        String tag = m.getMsg();
        c.receiveAction(src, timeStamp);
        if(tag.equals("request")) {
            if((myts == Integer.MAX_VALUE) || timeStamp < myts || (timeStamp == myts && src < myId)) {
                sendMsg(src, "okay", c.getValue());
            } else {
                pendingQ.add(src);
            }
        } else if(tag.equals("okay")) {
            updateNumOkay();
            //if(numOkay == N - 1) System.out.println("yes");;
        }
    }

    private void broadcastMsg(String msg, int timestamp) {
        sendMsg(myId, msg, timestamp);
    }

    private void sendMsg(int src, String msg, int timestamp) {
        try {
            Wrapper w = new Wrapper(myId, src, timestamp, msg);
            node_oos.writeObject(w);
            node_oos.flush();
        } catch(IOException e) {
            closeEverything();
        }

    }

    public void initCom() {
        System.out.println("Initializing Communication");
        try {
            socket = new Socket("localhost", 1234);
            server_oos = new ObjectOutputStream(socket.getOutputStream());
            server_ois = new ObjectInputStream(socket.getInputStream());
            this.myId = (Integer)server_ois.readObject();
            System.out.println("myId: " + this.myId);

            if(myId == 1) {
                serverSocket = new ServerSocket(5001);
                lightSocket = serverSocket.accept();
                node_oos = new ObjectOutputStream(lightSocket.getOutputStream());
                startListening(lightSocket);

            } else {
                lightSocket = new Socket("localhost", 5001);
                node_oos = new ObjectOutputStream(lightSocket.getOutputStream());
                startListening(lightSocket);
            }

            server_oos.writeObject("ack");
            server_oos.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            try {
                System.out.println("Shutting down sockets...");
                if(socket != null) socket.close();
                if(serverSocket != null) serverSocket.close();
                if(server_ois != null) server_ois.close();
                if(server_oos != null) server_oos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void startListening(Socket s) {
        Thread listener = new Thread() {
            public void run() {
                ObjectInputStream node_ois = null;
                try {
                    node_ois = new ObjectInputStream(s.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while(running) {
                    try {
                        Wrapper msg = (Wrapper) node_ois.readObject();
                        handleMessage(msg);
                    } catch (IOException | ClassNotFoundException e) {
                        try {
                            node_ois.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        closeEverything();
                    }
                }
                try {
                    node_ois.close();
                    lightSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        listener.start();
    }

    private void waitHeavyWeight() {
        String str = "";
        try {
            System.out.println("Waiting for heavy to grant access...");
            while(!str.equals("token")) {
                str = (String) server_ois.readObject();
            }
            System.out.println("Access granted");
        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void notifyHeavyWeight() {
        try {
            numOkay = 0;
            server_oos.writeObject((Integer)END_CONNECTION);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeEverything() {
        System.out.println("Error... Shutting down.");
        running = false;
        try {
            if(socket != null) socket.close();
            if(lightSocket != null) lightSocket.close();
            if(server_oos != null) server_oos.close();
            if(server_ois != null) server_ois.close();
            if(serverSocket != null) serverSocket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        LightweightB server =  new LightweightB(2);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.start();
    }

}
