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

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.oos = new ObjectOutputStream(socket.getOutputStream());
            this.ois = new ObjectInputStream(socket.getInputStream());
            portNumber = socket.getPort();
            System.out.println("Adding " + portNumber + " to queue...");
            clientHandlers.add(this);
            broadcastMessage(portNumber);
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
                    for(ClientHandler ch : clientHandlers) {
                        Integer port = ch.portNumber;
                        oos.writeObject((Integer)port);
                        oos.flush();
                        msg = (String)ois.readObject();
                        if(!msg.equals("ack")) throw new IOException("ack was not received");
                    }
                } else throw new IOException("ack was not received");
            } catch(IOException | ClassNotFoundException e) {
                System.out.println(e.getMessage());
                closeEverything(socket, oos, ois);
            }
        }
        while(socket.isConnected()) {
            try {
                System.out.println("Waiting for client");
                Integer clientMessage = (Integer)ois.readObject();
                if(clientMessage == END_CONNECTION) done = true;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                closeEverything(socket, oos, ois);
                break;
            }
        }
    }

    public void broadcastMessage(Integer newClient) {
        System.out.println(newClient);
        for (ClientHandler clientHandler : clientHandlers) {
            try {
                if(!Objects.equals(clientHandler.portNumber, portNumber)) {
                    System.out.println(clientHandler.portNumber + " vs " + portNumber);
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
        System.out.println("SERVER: " + portNumber + " has left the chat!");
    }

    public void notifyClient() {
        try {
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
