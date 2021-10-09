package Server_A;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class LightweightA {
    public static final int NEW_CONNECTION = 1;
    public static final int END_CONNECTION = 2;

    private final int myId;
    private final int N;
    private boolean running = true;
    private DirectClock v;
    int[] q;
    private Socket socket;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private ServerSocket serverSocket;
    public static ArrayList<Integer> nodes =  new ArrayList<>();

    public LightweightA(int id, int numProcesses) {
        this.myId = id;
        this.N = numProcesses;
        v = new DirectClock(N, myId);
        q = new int[N];

        for(int j = 0; j < N; j++)
            q[j] = Integer.MAX_VALUE;

        initCom();
    }

    private void start() {
        while(true) {
            waitHeavyWeight();
            //requestCS();
            for(int i = 0; i < 10; i++) {
                System.out.println("I am the process lightweightA" + myId);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            //releaseCS();
            notifyHeavyWeight();
        }
    }

    private void requestCS() {
        v.tick();
        q[myId] = v.getValue(myId);
        broadcastMsg("request", q[myId]);
        while(!okayCS());
    }

    public void releaseCS() {
        q[myId] = Integer.MAX_VALUE;
        broadcastMsg("release", q[myId]);
    }
    boolean okayCS() {
        for (int j = 0; j < N; j++) {
            if(isGreater(q[myId], myId, q[j], j))
                return false;
            if(isGreater(q[myId], myId, v.getValue(j), j));
                return false;
        }
        return true;
    }
    boolean isGreater(int entry1, int pid1, int entry2, int pid2) {
        if(entry2 == Integer.MAX_VALUE) return false;
        return ((entry1 > entry2) || ((entry1 == entry2) && (pid1 > pid2)));
    }

    public void handleMsg(Wrapper m) {
        int timeStamp= m.getId();
        int src = m.getId();
        String tag = m.getMsg();
        v.receieveAction(src, timeStamp);
        if(tag.equals("request")) {
            q[src] = timeStamp;
            try {
                sendMsg(src, "ack", v.getValue(myId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if(tag.equals("release"))
            q[src] = Integer.MAX_VALUE;
        notify();
    }


    private void initCom() {
        System.out.println("Initializing Communication");
        try {
            socket = new Socket("localhost", 4321);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            System.out.println("My port: " + socket.getLocalPort());
            serverSocket = new ServerSocket(socket.getLocalPort());
            Integer numNodes = (Integer)ois.readObject();
            oos.writeObject("ack");
            oos.flush();
            System.out.println("Read number of nodes: " + numNodes);
            while(numNodes > 0 ) {
                Integer node = (Integer)ois.readObject();
                oos.writeObject("ack");
                oos.flush();
                System.out.println("Adding Node " + node);
                nodes.add(node);
                numNodes--;
            }
        } catch (IOException | ClassNotFoundException e) {
            try {
                System.out.println("Shutting down sockets...");
                if(socket != null) socket.close();
                if(serverSocket != null) serverSocket.close();
                if(ois != null) ois.close();
                if(oos != null) oos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void waitHeavyWeight() {
        String str = "";
        try {
            while(!str.equals("token")) {
                str = (String) ois.readObject();
                System.out.println("Read " + str + " while waiting for heavy weight");
            }
        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void notifyHeavyWeight() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject((Integer)END_CONNECTION);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Thread startListening() throws IOException {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Thread connectionListener = null;
        connectionListener = new Thread() {
            public void run() {
                while(running) {
                    try {
                        Wrapper msg = (Wrapper) ois.readObject();
                        handleMsg(msg);
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    ois.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        return connectionListener;
    }

    private void sendMsg(int src, String msg, int timestamp) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        Wrapper w = new Wrapper(myId, src, timestamp, msg);
        oos.writeObject(w);
        oos.flush();
        oos.close();
    }

    private void broadcastMsg(String msg, int timestamp) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            Wrapper w = new Wrapper(myId, timestamp, msg);
            oos.writeObject(w);
            oos.flush();
            oos.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        LightweightA server =  new LightweightA(1,3);
        System.out.println("Starting Server!");
        server.start();
    }

}
