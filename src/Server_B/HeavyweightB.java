package Server_B;

import Server_A.HeavyweightA;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HeavyweightB {
    private ServerSocket serverSocket;
    private Socket heavySocket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private int numConnections;
    private boolean token;

    public HeavyweightB(ServerSocket serverSocket, int numConnections) {
        this.serverSocket = serverSocket;
        this.numConnections = numConnections;
        this.token = false;
    }

    public void startHeavyweight() {
        try {
            System.out.println("Connecting to Heavyweight A");
            heavySocket = new Socket("localhost", 4321);
            oos = new ObjectOutputStream(heavySocket.getOutputStream());
            ois = new ObjectInputStream(heavySocket.getInputStream());
            while(!serverSocket.isClosed()) {
                while (!token) listenHeavyweight();
                System.out.println("Got token from Heavyweight A");
                //Do whatever

                //When done: sendTokenToHeavyweight
            }
            closeEverything();
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void sendTokenToHeavyweight() {
        try {
            oos.writeObject(1);
        } catch (IOException e) {
            closeEverything();
        }
    }

    public void listenHeavyweight() {
        try {
            token = ((Integer)ois.readObject() == 1);
        } catch (IOException | ClassNotFoundException e) {
            closeEverything();
        }
    }

    public void closeEverything() {
        System.out.println("Heavyweight Server B shutting down...");
        try {
            if(ois != null) {
                ois.close();
            }
            if(oos != null) {
                oos.close();
            }
            if(serverSocket != null) {
                serverSocket.close();
            }
            if(heavySocket != null) {
                heavySocket.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args ) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        HeavyweightB server =  new HeavyweightB(serverSocket, 3);
        server.startHeavyweight();
    }
}
