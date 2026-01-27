package pong;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public GameServer() {
        try {
            serverSocket = new ServerSocket(3000);
            System.out.println("Server started. Port: 3000");
            System.out.println("Waiting for client connection...");

            clientSocket = serverSocket.accept();
            System.out.println("Client connected: " + clientSocket.getInetAddress());

            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());

            System.out.println("Streams created successfully.");
        } catch (IOException e) {
            System.err.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendGameState(GameState state) {
        try {
            out.writeObject(state);
            out.flush();
        } catch (IOException e) {
            System.err.println("GameState sending error: " + e.getMessage());
        }
    }

    public PlayerInput receiveInput() {
        try {
            return (PlayerInput) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("PlayerInput receiving error: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
            System.out.println("Server closed.");
        } catch (IOException e) {
            System.err.println("Closing error: " + e.getMessage());
        }
    }
}
