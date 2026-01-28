package pong;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private GameState gameState;
    private GamePanel gamePanel;

    private ExecutorService executorService;
    private ScheduledExecutorService gameLoopExecutor;
    private ScheduledFuture<?> gameLoopFuture;

    private final AtomicBoolean running = new AtomicBoolean(false);

    // ReentrantLock for synchronization
    private final ReentrantLock gameStateLock = new ReentrantLock();
    private final ReentrantLock networkLock = new ReentrantLock();

    // Concurrent queue for game state updates
    private final BlockingQueue<GameState> stateQueue = new LinkedBlockingQueue<>();

    public GameClient() {
        // Create thread pool for parallel operations
        executorService = Executors.newFixedThreadPool(3);
        gameLoopExecutor = Executors.newScheduledThreadPool(2);

        try {
            // Open connection asynchronously
            CompletableFuture.runAsync(() -> {
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
            }, executorService).join(); // Wait for connection to complete
        } catch (Exception e) {
            System.err.println("Client initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startGameLoop(GameState initialState, GamePanel panel) {
        this.gameState = initialState;
        this.gamePanel = panel;
        running.set(true);

        // Set up callback for client pause toggle - will send request to server
        gamePanel.setOnPauseToggle(() -> {
            gamePanel.requestPauseToggle();
        });

        // Set up callback for client restart - will send request to server
        gamePanel.setOnRestart(() -> {
            gamePanel.requestRestart();
        });

        // Network receiver thread - Receives GameStates in separate thread
        CompletableFuture.runAsync(() -> {
            System.out.println("Network receiver started");
            while (running.get()) {
                try {
                    GameState newGameState = (GameState) in.readObject();
                    if (newGameState != null) {
                        stateQueue.offer(newGameState, 100, TimeUnit.MILLISECONDS);
                    }
                } catch (Exception e) {
                    if (running.get()) {
                        System.err.println("Network receive error: " + e.getMessage());
                    }
                }
            }
        }, executorService);

        // Game loop with fixed FPS using ScheduledExecutorService
        gameLoopFuture = gameLoopExecutor.scheduleAtFixedRate(() -> {
            try {
                // Get actual user input from GamePanel
                PlayerInput playerInput = new PlayerInput();
                if (gamePanel != null) {
                    playerInput.moveY = gamePanel.getCurrentMoveY();
                    // Check if client requested pause
                    playerInput.pauseRequest = gamePanel.isPauseRequested();
                    // Check if client requested restart
                    playerInput.restartRequest = gamePanel.isRestartRequested();
                } else {
                    playerInput.moveY = 0;
                }

                // Send input to server - using separate lock
                networkLock.lock();
                try {
                    out.writeObject(playerInput);
                    out.flush();
                } finally {
                    networkLock.unlock();
                }

                // Process received game state from queue
                GameState newGameState = stateQueue.poll();
                if (newGameState != null) {
                    gameStateLock.lock();
                    try {
                        gameState = newGameState;

                        // Sync pause state from server to client UI
                        if (gamePanel != null) {
                            gamePanel.setPaused(gameState.isPaused);
                        }
                    } finally {
                        gameStateLock.unlock();
                    }
                }

                // Update panel with new game state - run on EDT
                if (gamePanel != null) {
                    javax.swing.SwingUtilities.invokeLater(() -> {
                        gamePanel.gameState = gameState;
                        gamePanel.repaint();
                    });
                }

            } catch (Exception e) {
                System.err.println("Game loop error: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 16, TimeUnit.MILLISECONDS); // ~60 FPS

        System.out.println("Game loop started on client with ScheduledExecutorService");
    }

    public void stopGameLoop() {
        running.set(false);
        if (gameLoopFuture != null) {
            gameLoopFuture.cancel(true);
        }
    }

    public void close() {
        stopGameLoop();

        // Shutdown executor services gracefully
        if (gameLoopExecutor != null) {
            gameLoopExecutor.shutdown();
            try {
                if (!gameLoopExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    gameLoopExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                gameLoopExecutor.shutdownNow();
            }
        }

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }

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
