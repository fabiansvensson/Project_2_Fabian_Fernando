package Server_A;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class LightweightA {
    public static final int NEW_CONNECTION = 1;
    public static final int END_CONNECTION = 2;

    private int myId;
    private final int N;
    private boolean running = true;
    private DirectClock v;
    int[] q;
    private Socket socket;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private ServerSocket serverSocket;
    public static ArrayList<Integer> nodes =  new ArrayList<>();
    public static ArrayList<Socket> lightSockets =  new ArrayList<>();
    public static ArrayList<ObjectOutputStream> outs =  new ArrayList<>();

    public LightweightA(int id, int numProcesses) {
        this.N = numProcesses;
        v = new DirectClock(N);
        q = new int[N];

        for(int j = 0; j < N; j++)
            q[j] = Integer.MAX_VALUE;

        initCom();
    }

    private void start() {
        Thread listener = null;
        while(true) {
            try {
                waitHeavyWeight();
                for(Socket s : lightSockets) {
                    listener = startListening(s);
                    listener.start();
                }
                requestCS();
                for(int i = 0; i < 10; i++) {
                    System.out.println("I am the process lightweight A" + (myId +1) + " {" + i + "}");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                releaseCS();
                notifyHeavyWeight();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestCS() {
        v.tick();
        q[myId] = v.getValue(myId);
        System.out.println("Sending request: " + q[myId]);
        broadcastMsg("request", q[myId]);
        boolean getAccess = false;
        while(!getAccess) {
            getAccess = okayCS();
            System.out.println("getAccess: " + getAccess);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

    public void releaseCS() {
        q[myId] = Integer.MAX_VALUE;
        broadcastMsg("release", q[myId]);
    }
    boolean okayCS() {
        for (int j = 0; j < N; j++) {
            if(j == myId) continue;
            System.out.println("q[myId]: " + q[myId] + ", q[j]: " + q[j] + ", v.getValue(j): " + v.getValue(j) + ", myId: " + myId + ", j: " + j);
            if(isGreater(q[myId], myId, q[j], j)) {
                //System.out.println("q[j] " + q[myId] + myId + " > " + q[j] + j + "? --> true");
                return false;
            }
            if(isGreater(q[myId], myId, v.getValue(j), j)) {
                //System.out.println("v.getValue(j) " + q[myId] + myId + " > " + q[j] + j + "? --> true");
                return false;
            }
        }
        return true;
    }
    boolean isGreater(int entry1, int pid1, int entry2, int pid2) {
        if(entry2 == Integer.MAX_VALUE) return false;
        return ((entry1 > entry2) || ((entry1 == entry2) && (pid1 > pid2)));
    }

    public void handleMsg(Wrapper m) {
        int timeStamp= m.getTimestamp();
        int src = m.getId();
        String tag = m.getMsg();
        v.receieveAction(src, timeStamp);
        if(tag.equals("request")) {
            q[src] = timeStamp;
            System.out.println("Read request from " + src);
            try {
                sendMsg(src, "ack", v.getValue(myId));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if(tag.equals("release")) {
            System.out.println("Read release from " + src);
            q[src] = Integer.MAX_VALUE;
        }
        //notify();
    }


    private void initCom() {
        System.out.println("Initializing Communication");
        try {
            socket = new Socket("localhost", 4321);
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            Integer localPort = socket.getLocalPort();
            Integer numNodes = (Integer)ois.readObject();
            System.out.println("Read number of nodes: " + numNodes);
            myId = numNodes - 1;
            v.setId(myId);

            System.out.println("{port, id} = {" + localPort + ", " + (4000+myId) + "}");

            serverSocket = new ServerSocket(4000 + myId);
            for(int i = 0; i < myId; i++) {
                System.out.println("Connecting to " + (4000+i));
                Socket s = new Socket("localhost", 4000+i);
                lightSockets.add(s);
                outs.add(new ObjectOutputStream(s.getOutputStream()));
            }

            for(int j = (myId+1); j<N; j++) {
                Socket s = serverSocket.accept();
                System.out.println("Connecting from " + (4000+j));
                lightSockets.add(s);
                outs.add(new ObjectOutputStream(s.getOutputStream()));
            }

            oos.writeObject("ack");
            oos.flush();

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
            System.out.println("Waiting for heavy to grant access...");
            while(!str.equals("token")) {
                str = (String) ois.readObject();
                if(!str.equals("token")) nodes.add(Integer.valueOf(str));
            }
            v = new DirectClock(3);
            v.setId(myId);
        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void notifyHeavyWeight() {
        try {
            oos.writeObject((Integer)END_CONNECTION);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Thread startListening(Socket s) throws IOException {
        Thread connectionListener = null;
        connectionListener = new Thread() {
            public void run() {
                System.out.println("Starting to listen from " + s.getPort());
                ObjectInputStream node_ois = null;
                try {
                    node_ois = new ObjectInputStream(s.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while(running) {
                    try {
                        Wrapper msg = (Wrapper) node_ois.readObject();
                        handleMsg(msg);
                    } catch (IOException | ClassNotFoundException e) {
                        if(node_ois != null) {
                            try {
                                node_ois.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                        closeEverything();
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
        for(int i = 0; i < lightSockets.size(); i++) {
            if(src == lightSockets.get(i).getPort()) {
                ObjectOutputStream node_oos = outs.get(i);
                Wrapper w = new Wrapper(myId, src, timestamp, msg);
                node_oos.writeObject(w);
                node_oos.flush();
            }
        }
    }

    private void broadcastMsg(String msg, int timestamp) {
        try {
            for(ObjectOutputStream out : outs) {
                Wrapper w = new Wrapper(myId, timestamp, msg);
                out.writeObject(w);
                out.flush();
            }
        } catch(IOException e) {
            closeEverything();
        }
    }


    public void closeEverything() {
        try {
            for(ObjectOutputStream out : outs) {
                if(out != null) out.close();
            }
            for(Socket s : lightSockets) {
                if(s != null) s.close();
            }

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        LightweightA server =  new LightweightA(1,3);
        server.start();
    }

}
