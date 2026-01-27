package pong;

import java.io.*;
import java.net.Socket;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    public GameClient() {
        try {
            socket = new Socket("localhost", 3000);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connected to server: localhost:3000");
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendInput(PlayerInput input) {
        try {
            out.writeObject(input);
            out.flush();
        } catch (IOException e) {
            System.err.println("Input sending error: " + e.getMessage());
        }
    }

    public GameState receiveGameState() {
        try {
            return (GameState) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("GameState receiving error: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Connection closed.");
        } catch (IOException e) {
            System.err.println("Closing error: " + e.getMessage());
        }
    }
}
