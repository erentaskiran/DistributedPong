package pong;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private GameState gameState;
    private GamePanel gamePanel;
    private Thread gameLoopThread;
    private volatile boolean running = false;

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

    public void startGameLoop(GameState initialState, GamePanel panel) {
        this.gameState = initialState;
        this.gamePanel = panel;
        running = true;

        gameLoopThread = new Thread(() -> {
            System.out.println("Game loop started on server");

            while (running) {
                try {
                    // Get host player input from GamePanel (left paddle)
                    if (gamePanel != null) {
                        int hostMoveY = gamePanel.getCurrentMoveY();
                        gameState.paddleLeftY += hostMoveY;
                    }

                    // Receive client input (right paddle)
                    PlayerInput playerInput = (PlayerInput) in.readObject();
                    System.out.println("Player received: " + playerInput.toString());

                    if (playerInput != null){
                        gameState.paddleRightY += playerInput.moveY;
                    }

                    // Update game physics
                    gameState.moveBall();
                    gameState.checkBoundaries();

                    // Send updated game state to client
                    out.reset();
                    out.writeObject(gameState);
                    out.flush();
                    System.out.println("game state: " + gameState.paddleRightY);

                    // Update server's screen
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
        stopGameLoop();
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
