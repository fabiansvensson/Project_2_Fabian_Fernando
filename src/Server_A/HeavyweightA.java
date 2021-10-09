package Server_A;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class HeavyweightA {
    private ServerSocket serverSocket;
    private int numConnections;
    private boolean token;
    public static ArrayList<Integer> nodes =  new ArrayList<>();
    public static ArrayList<ClientHandler> clients =  new ArrayList<>();

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

                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);

                Thread thread = new Thread(clientHandler);
                thread.start();

                count++;
            }

            while(!serverSocket.isClosed()) {
                while(!token) listenHeavyweight();
                for(ClientHandler ch : clients) ch.notifyClient();

                boolean done = false;
                while(!done) {
                    done = true;
                    for(ClientHandler ch : clients) {
                        if(!ch.done) done = false;
                    }
                }
                token = false;
                sendTokenToHeavyweight();
            }
        } catch (IOException e) {
            try {
                serverSocket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    public void sendTokenToHeavyweight() {
        System.out.println("Sending token to other heavyweight...");
    }

    public void listenHeavyweight() {
        System.out.println("Listening for other heavyweight...");
    }

    public static void main(String[] args ) throws IOException {
        ServerSocket serverSocket = new ServerSocket(4321);
        HeavyweightA server =  new HeavyweightA(serverSocket, 3);
        server.startHeavyweight();
    }
}
