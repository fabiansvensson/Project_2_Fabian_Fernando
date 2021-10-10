package Server_A;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

public class ClientHandler implements Runnable {

    public static final int NEW_CONNECTION = 1;
    public static final int END_CONNECTION = 2;

    public static ArrayList<ClientHandler> clientHandlers =  new ArrayList<>();
    public Socket socket;
    public ObjectInputStream ois = null;
    public ObjectOutputStream oos = null;
    public Integer portNumber;
    public boolean done = false;
    public boolean ready = false;
    public boolean token = false;

    public ClientHandler(Socket socket, boolean token) {
        try {
            this.socket = socket;
            this.oos = new ObjectOutputStream(socket.getOutputStream());
            this.ois = new ObjectInputStream(socket.getInputStream());
            portNumber = socket.getPort();
            System.out.println("Adding " + portNumber + " to queue...");
            this.token = token;
            clientHandlers.add(this);
        } catch(IOException e) {
            closeEverything(socket, oos, ois);
        }
    }

    @Override
    public void run() {
        Integer numClients = clientHandlers.size();
        if(socket.isConnected()) {
            try {
                oos.writeObject(numClients);
                oos.flush();
                String msg = (String)ois.readObject();
                if(msg.equals("ack")) {
                    System.out.println(portNumber + " is ready!");
                    ready = true;
                } else throw new IOException("ack was not received");
            } catch(IOException | ClassNotFoundException e) {
                System.out.println(e.getMessage());
                closeEverything(socket, oos, ois);
            }
        }
        while(socket.isConnected()) {
            if(token) {
                try {
                    System.out.println("Waiting for client");
                    Integer clientMessage = (Integer)ois.readObject();
                    if(clientMessage == END_CONNECTION) {
                        System.out.println(portNumber + " is done!");
                        done = true;
                        token = false;
                    }
                } catch (IOException | ClassNotFoundException e) {
                    closeEverything(socket, oos, ois);
                    break;
                }
            }
        }

    }

    public void broadcastMessage(String newClient) {
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if(!Objects.equals(clientHandler.portNumber, portNumber)) {
                    clientHandler.oos.writeObject(newClient);
                    clientHandler.oos.flush();
                }
            } catch(IOException e) {
                closeEverything(socket, oos, ois);
            }
        }
    }

    public void removeClientHandler() {
        clientHandlers.remove(this);
        System.out.println("SERVER: " + portNumber + " has left!");
    }

    public void notifyClient() {
        try {
            token = true;
            oos.writeObject("token");
            oos.flush();
        } catch(IOException e) {
            closeEverything(socket, oos, ois);
        }
    }

    public void closeEverything(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        removeClientHandler();
        try {
            if(ois != null) {
                ois.close();
            }
            if(oos != null) {
                oos.close();
            }
            if(socket != null) {
                socket.close();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
