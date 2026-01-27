package pong;

import java.io.*;
import java.net.Socket;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private GameState gameState;
    private GamePanel gamePanel;
    private Thread gameLoopThread;
    private volatile boolean running = false;

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

    public void startGameLoop(GameState initialState, GamePanel panel) {
        this.gameState = initialState;
        this.gamePanel = panel;
        running = true;

        gameLoopThread = new Thread(() -> {
            System.out.println("Game loop started on client");

            while (running) {
                try {
                    // Get actual user input from GamePanel
                    PlayerInput playerInput = new PlayerInput();
                    if (gamePanel != null) {
                        playerInput.moveY = gamePanel.getCurrentMoveY();
                    } else {
                        playerInput.moveY = 0;
                    }

                    // Send input to server
                    out.writeObject(playerInput);
                    out.flush();

                    // Receive updated game state from server
                    GameState newGameState = (GameState) in.readObject();
                    if (newGameState != null){
                        gameState = newGameState;
                    }

                    // Update panel with new game state
                    if (gamePanel != null) {
                        gamePanel.gameState = gameState;
                        gamePanel.repaint();
                    }

                    Thread.sleep(16); // ~60 FPS

                } catch (InterruptedException e) {
                    System.out.println("Game loop interrupted");
                    break;
                } catch (Exception e) {
                    System.err.println("Game loop error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("Game loop stopped");
        });

        gameLoopThread.start();
    }

    public void stopGameLoop() {
        running = false;
        if (gameLoopThread != null) {
            try {
                gameLoopThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
