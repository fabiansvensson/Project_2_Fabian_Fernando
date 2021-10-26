package Server_A;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientHandlerA implements Runnable {

    public static final int NEW_CONNECTION = 1;
    public static final int END_CONNECTION = 2;

    public static ArrayList<ClientHandlerA> clientHandlerAS =  new ArrayList<>();
    public Socket socket;
    public ObjectInputStream ois = null;
    public ObjectOutputStream oos = null;
    public Integer portNumber;
    public boolean done = false;
    public boolean ready = false;
    public boolean token = false;
    private static ReadWriteLock lock = new ReentrantReadWriteLock();

    public ClientHandlerA(Socket socket, boolean token) {
        try {
            this.socket = socket;
            this.oos = new ObjectOutputStream(socket.getOutputStream());
            this.ois = new ObjectInputStream(socket.getInputStream());
            portNumber = socket.getPort();
            System.out.println("Adding " + portNumber + " to queue...");
            this.token = token;
            clientHandlerAS.add(this);
        } catch(IOException e) {
            closeEverything(socket, oos, ois);
        }
    }

    @Override
    public void run() {
        Integer numClients = clientHandlerAS.size();
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
            boolean tokenVal = readToken();
            if(tokenVal) {
                try {
                    System.out.println("Waiting for client");
                    Integer clientMessage = (Integer)ois.readObject();
                    if(clientMessage == END_CONNECTION) {
                        System.out.println(portNumber + " is done!");
                        done = true;
                        setToken(false);
                    }

                } catch (IOException | ClassNotFoundException e) {
                    closeEverything(socket, oos, ois);
                    break;
                }
            }
        }
        System.out.println("Socket in CLientHandlerA no longer connected...");
    }

    private void setToken(boolean val) {
        lock.writeLock().lock();
        try {
            token = val;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private boolean readToken() {
        lock.readLock().lock();
        try {
            return token;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeClientHandler() {
        clientHandlerAS.remove(this);
        System.out.println("SERVER: " + portNumber + " has left!");
    }

    public void notifyClient() {
        try {
            setToken(true);
            done = false;
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
