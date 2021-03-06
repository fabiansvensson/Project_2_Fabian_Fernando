package Server_A;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class HeavyweightA {
    private ServerSocket serverSocket;
    private Socket heavySocket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private int numConnections;
    private boolean token;
    public static ArrayList<Integer> nodes =  new ArrayList<>();
    public static ArrayList<ClientHandlerA> clients =  new ArrayList<>();

    public HeavyweightA(ServerSocket serverSocket, int numConnections) {
        this.serverSocket = serverSocket;
        this.numConnections = numConnections;
        this.token = true;
    }

    public void startHeavyweight() {
        try {
            int count = 0;
            while(count < numConnections) {
                System.out.println("Waiting for connections...");
                Socket socket = serverSocket.accept();

                ClientHandlerA clientHandlerA = new ClientHandlerA(socket, token);
                clients.add(clientHandlerA);

                Thread thread = new Thread(clientHandlerA);
                thread.start();

                count++;
            }

            boolean ready = false;
            while(!ready) {
                ready = true;
                for(ClientHandlerA ch : clients) {
                    if (!ch.ready) {
                        //System.out.println("Ready is false?");
                        ready = false;
                    }
                }
            }

            System.out.println("Waiting for other heavy to connect...");
            heavySocket = serverSocket.accept();
            System.out.println("Heavyweight servers connected!");
            oos = new ObjectOutputStream(heavySocket.getOutputStream());
            ois = new ObjectInputStream(heavySocket.getInputStream());

            while(!serverSocket.isClosed()) {
                System.out.println("Waiting for Heavyweight B to give token...");
                while(!token) listenHeavyweight();
                System.out.println("Received token from Heavyweight B");
                for(ClientHandlerA ch : clients) ch.notifyClient();

                boolean done = false;
                while(!done) {
                    done = true;
                    for(ClientHandlerA ch : clients) {
                        if(!ch.done) done = false;
                    }
                }
                System.out.println("Sending token to heavyweight B");
                token = false;
                sendTokenToHeavyweight();
            }
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
        System.out.println("Server shutting down...");
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
        ServerSocket serverSocket = new ServerSocket(4321);
        HeavyweightA server =  new HeavyweightA(serverSocket, 3);
        server.startHeavyweight();
    }
}
