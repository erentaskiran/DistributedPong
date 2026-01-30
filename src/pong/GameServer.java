package pong;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class GameServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private GameState gameState;
    private GamePanel gamePanel;

    // ExecutorService for parallel programming
    private ExecutorService executorService;
    private ScheduledExecutorService gameLoopExecutor;
    private ScheduledFuture<?> gameLoopFuture;

    // Thread-safe variables
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    // ReentrantLock for synchronization
    private final ReentrantLock gameStateLock = new ReentrantLock();
    private final ReentrantLock networkLock = new ReentrantLock();

    // Concurrent queue for player inputs
    private final BlockingQueue<PlayerInput> inputQueue = new LinkedBlockingQueue<>();

    public GameServer() {
        // Create thread pool for parallel operations
        executorService = Executors.newFixedThreadPool(4);
        gameLoopExecutor = Executors.newScheduledThreadPool(2);

        try {
            serverSocket = new ServerSocket(3000);
            System.out.println("Server started. Port: 3000");
            System.out.println("Waiting for client connection...");

            // Open connection in separate thread
            CompletableFuture.runAsync(() -> {
                try {
                    clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    out = new ObjectOutputStream(clientSocket.getOutputStream());
                    out.flush();
                    in = new ObjectInputStream(clientSocket.getInputStream());

                    System.out.println("Streams created successfully.");
                } catch (IOException e) {
                    System.err.println("Connection error: " + e.getMessage());
                    e.printStackTrace();
                }
            }, executorService).join(); // Wait for connection to complete

        } catch (IOException e) {
            System.err.println("Server startup error: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void togglePause() {
        gameStateLock.lock();
        try {
            boolean newPauseState = !paused.get();
            paused.set(newPauseState);
            if (gameState != null) {
                gameState.togglePause();
            }
        } finally {
            gameStateLock.unlock();
        }
    }

    public void restartGame() {
        gameStateLock.lock();
        try {
            if (gameState != null) {
                gameState.resetGame();
                paused.set(false);
            }
            if (gamePanel != null) {
                gamePanel.setPaused(false);
                gamePanel.repaint();
            }
        } finally {
            gameStateLock.unlock();
        }
    }

    public void startGameLoop(GameState initialState, GamePanel panel) {
        this.gameState = initialState;
        this.gamePanel = panel;
        running.set(true);

        // Set up callbacks for pause and restart
        gamePanel.setOnPauseToggle(this::togglePause);

        gamePanel.setOnRestart(this::restartGame);

        // Client input processor thread - Processes inputs from BlockingQueue
        CompletableFuture.runAsync(() -> {
            System.out.println("Input processor started");
            while (running.get()) {
                try {
                    PlayerInput playerInput = (PlayerInput) in.readObject();
                    if (!inputQueue.offer(playerInput, 100, TimeUnit.MILLISECONDS)) {
                        System.err.println("WARNING: Player input dropped - queue full or timeout");
                    }
                } catch (Exception e) {
                    if (running.get()) {
                        System.err.println("Input reading error: " + e.getMessage());
                    }
                }

            }
        }, executorService);

        // Game loop with fixed FPS using ScheduledExecutorService
        gameLoopFuture = gameLoopExecutor.scheduleAtFixedRate(() -> {
            try {
                gameStateLock.lock();
                try {
                    // Get host player input from GamePanel (left paddle)
                    if (gamePanel != null && !paused.get()) {
                        int hostMoveY = gamePanel.getCurrentMoveY();
                        gameState.updatePaddleLeft(hostMoveY);
                    }

                    // Process client input from queue (right paddle)
                    PlayerInput playerInput = inputQueue.poll();
                    if (playerInput != null) {
                        System.out.println("Player input processed: moveY=" + playerInput.moveY);

                        // Handle client pause request
                        if (playerInput.pauseRequest) {
                            togglePause();
                            if (gamePanel != null) {
                                gamePanel.setPaused(paused.get());
                            }
                        }

                        // Handle client restart request
                        if (playerInput.restartRequest) {
                            restartGame();
                        }

                        if (!paused.get()) {
                            gameState.updatePaddleRight(playerInput.moveY);
                        }
                    }

                    // Update game physics only if not paused - in parallel
                    if (!paused.get()) {
                        // Parallel operations with CompletableFuture
                        CompletableFuture<Void> ballMovement = CompletableFuture.runAsync(() -> {
                            gameState.moveBall();
                        }, executorService);

                        CompletableFuture<Void> boundaryCheck = CompletableFuture.runAsync(() -> {
                            gameState.checkBoundaries();
                        }, executorService);

                        // Wait for both operations to complete
                        CompletableFuture.allOf(ballMovement, boundaryCheck).join();
                    }

                    // Sync pause state
                    gameState.isPaused = paused.get();
                    gameState.syncStateForSerialization();

                } finally {
                    gameStateLock.unlock();
                }

                // Network send - using separate lock
                networkLock.lock();
                try {
                    out.reset();
                    out.writeObject(gameState);
                    out.flush();
                } finally {
                    networkLock.unlock();
                }

                // UI update - run on EDT
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

        System.out.println("Game loop started with ScheduledExecutorService");
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
            if (clientSocket != null) clientSocket.close();
            if (serverSocket != null) serverSocket.close();
            System.out.println("Server closed.");
        } catch (IOException e) {
            System.err.println("Closing error: " + e.getMessage());
        }
    }
}
