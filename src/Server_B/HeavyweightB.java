package Server_B;

import Server_A.ClientHandlerA;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class HeavyweightB {
    private ServerSocket serverSocket;
    private Socket heavySocket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private int numConnections;
    private boolean token;
    public static ArrayList<ClientHandlerB> clients =  new ArrayList<>();

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
            int count = 0;
            while(!serverSocket.isClosed()) {
                System.out.println("Waiting for Heavyweight A to give token...");
                while (!token) listenHeavyweight();
                System.out.println("Got token from Heavyweight A");
                //Do whatever
                while(count < numConnections) {
                    System.out.println("Waiting for connections...");
                    Socket socket = serverSocket.accept();

                    ClientHandlerB clientHandlerB = new ClientHandlerB(socket, token, count+1);
                    clients.add(clientHandlerB);

                    Thread thread = new Thread(clientHandlerB);
                    thread.start();

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    count++;
                }

                boolean ready = false;
                while(!ready) {
                    ready = true;
                    for(ClientHandlerB ch : clients) {
                        if (!ch.ready) {
                            //System.out.println("Ready is false?");
                            ready = false;
                        }
                    }
                }

                System.out.println("Clients ready");

                for(ClientHandlerB ch : clients) ch.notifyClient();

                boolean done = false;
                while(!done) {
                    done = true;
                    for(ClientHandlerB ch : clients) {
                        if(!ch.done) done = false;
                    }
                }
                System.out.println("Sending token to heavyweight A");
                token = false;
                sendTokenToHeavyweight();

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
        HeavyweightB server =  new HeavyweightB(serverSocket, 2);
        server.startHeavyweight();
    }
}
