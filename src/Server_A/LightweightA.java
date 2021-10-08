package Server_A;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class LightweightA {
    private final int myId;
    private final int N;
    private boolean running = true;
    private DirectClock v;
    int[] q;
    private Socket socket;

    public LightweightA(int id, int numProcesses) {
        this.myId = id;
        this.N = numProcesses;
        v = new DirectClock(N, myId);
        q = new int[N];

        for(int j = 0; j < N; j++)
            q[j] = Integer.MAX_VALUE;

        try {
            initCom();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() {
        while(true) {
            waitHeavyWeight();
            requestCS();
            for(int i = 0; i < 10; i++) {
                System.out.println("I am the process lightweightA" + myId);
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


    private void initCom() throws IOException {
        socket = new Socket("localhost", 4321);
    }

    private void waitHeavyWeight() {
        String str = "";
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            while(!str.equals("token")) {
                str = (String) ois.readObject();
            }
            ois.close();
        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void notifyHeavyWeight() {

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
}
