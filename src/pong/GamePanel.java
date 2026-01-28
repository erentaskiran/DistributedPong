package pong;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GamePanel extends JPanel {
    volatile GameState gameState;

    // Thread-safe değişkenler
    private final AtomicInteger currentMoveY = new AtomicInteger(0);
    private static final int PADDLE_SPEED = 5;

    // Pause menu components
    private final AtomicBoolean showPauseMenu = new AtomicBoolean(false);
    private Rectangle pauseButtonRect = new Rectangle(10, 10, 40, 40);
    private Rectangle resumeButtonRect;
    private Rectangle scoresButtonRect;
    private Rectangle restartButtonRect;
    private Rectangle backButtonRect;
    private final AtomicBoolean showScoresPanel = new AtomicBoolean(false);

    // Callback for restart action
    private Runnable onRestart;
    private Runnable onPauseToggle;

    // Flag to request pause from client (sent to server)
    private final AtomicBoolean pauseRequested = new AtomicBoolean(false);
    private final AtomicBoolean restartRequested = new AtomicBoolean(false);

    // Lock for thread-safe rendering
    private final ReentrantReadWriteLock renderLock = new ReentrantReadWriteLock();

    public GamePanel() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    togglePauseMenu();
                } else if (!showPauseMenu.get()) {
                    if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
                        currentMoveY.set(-PADDLE_SPEED);
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                        currentMoveY.set(PADDLE_SPEED);
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W ||
                    e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                    currentMoveY.set(0);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });
    }

    public synchronized void setOnRestart(Runnable onRestart) {
        this.onRestart = onRestart;
    }

    public synchronized void setOnPauseToggle(Runnable onPauseToggle) {
        this.onPauseToggle = onPauseToggle;
    }

    private synchronized void togglePauseMenu() {
        showPauseMenu.set(!showPauseMenu.get());
        showScoresPanel.set(false);
        if (onPauseToggle != null) {
            onPauseToggle.run();
        }
        repaint();
    }

    private synchronized void handleMouseClick(int x, int y) {
        // Check pause button click
        if (pauseButtonRect.contains(x, y)) {
            togglePauseMenu();
            return;
        }

        if (showPauseMenu.get()) {
            if (showScoresPanel.get()) {
                // Back button in scores panel
                if (backButtonRect != null && backButtonRect.contains(x, y)) {
                    showScoresPanel.set(false);
                    repaint();
                }
            } else {
                // Resume button
                if (resumeButtonRect != null && resumeButtonRect.contains(x, y)) {
                    togglePauseMenu();
                }
                // Scores button
                else if (scoresButtonRect != null && scoresButtonRect.contains(x, y)) {
                    showScoresPanel.set(true);
                    repaint();
                }
                // Restart button
                else if (restartButtonRect != null && restartButtonRect.contains(x, y)) {
                    showPauseMenu.set(false);
                    showScoresPanel.set(false);
                    if (onRestart != null) {
                        onRestart.run();
                    }
                    repaint();
                }
            }
        }
    }

    public int getCurrentMoveY() {
        if (showPauseMenu.get()) return 0; // No movement when paused
        return currentMoveY.get();
    }

    public boolean isPaused() {
        return showPauseMenu.get();
    }

    public synchronized void setPaused(boolean paused) {
        // Only reset scores panel if we're unpausing
        if (!paused) {
            this.showScoresPanel.set(false);
        }
        this.showPauseMenu.set(paused);
        repaint();
    }

    // For client: request pause toggle (will be sent to server)
    public boolean isPauseRequested() {
        return pauseRequested.getAndSet(false); // Atomic get-and-reset
    }

    // Request a pause toggle (used by client)
    public synchronized void requestPauseToggle() {
        pauseRequested.set(true);
    }

    // For client: request restart (will be sent to server)
    public boolean isRestartRequested() {
        return restartRequested.getAndSet(false); // Atomic get-and-reset
    }

    // Request a restart (used by client)
    public synchronized void requestRestart() {
        restartRequested.set(true);
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        renderLock.readLock().lock();
        try {
            if (gameState == null) {
                gameState = new GameState();
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw game background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.WHITE);

            // Draw center line
            for (int i = 0; i < getHeight(); i += 20) {
                g.fillRect(getWidth() / 2 - 2, i, 4, 10);
            }

            // Draw paddles
            g.fillRect(20, gameState.paddleLeftY, 10, 80);
            g.fillRect(getWidth() - 30, gameState.paddleRightY, 10, 80);

            // Draw ball
            g.fillOval(gameState.ballX, gameState.ballY, 15, 15);

            // Draw scores
            g.setFont(new Font("Arial", Font.BOLD, 30));
            g.drawString(String.valueOf(gameState.scoreLeft), getWidth() / 2 - 50, 50);
            g.drawString(String.valueOf(gameState.scoreRight), getWidth() / 2 + 30, 50);

            // Draw pause button (stop sign style)
            drawPauseButton(g2d);

            // Draw pause menu overlay if paused
            if (showPauseMenu.get()) {
                drawPauseMenu(g2d);
            }
        } finally {
            renderLock.readLock().unlock();
        }
    }

    private void drawPauseButton(Graphics2D g2d) {
        // Draw stop sign style button
        g2d.setColor(new Color(200, 50, 50));
        int[] xPoints = {15, 35, 45, 45, 35, 15, 5, 5};
        int[] yPoints = {5, 5, 15, 35, 45, 45, 35, 15};
        g2d.fillPolygon(xPoints, yPoints, 8);

        // Draw pause bars
        g2d.setColor(Color.WHITE);
        g2d.fillRect(18, 15, 6, 20);
        g2d.fillRect(28, 15, 6, 20);
    }

    private void drawPauseMenu(Graphics2D g2d) {
        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (showScoresPanel.get()) {
            drawScoresPanel(g2d);
        } else {
            drawMainPauseMenu(g2d);
        }
    }

    private void drawMainPauseMenu(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Draw "PAUSED" title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        String pausedText = "PAUSED";
        int textWidth = fm.stringWidth(pausedText);
        g2d.drawString(pausedText, centerX - textWidth / 2, centerY - 100);

        // Button dimensions
        int buttonWidth = 200;
        int buttonHeight = 50;
        int buttonSpacing = 20;

        // Resume button
        resumeButtonRect = new Rectangle(centerX - buttonWidth / 2, centerY - 30, buttonWidth, buttonHeight);
        drawButton(g2d, resumeButtonRect, "RESUME", new Color(50, 150, 50));

        // Scores button
        scoresButtonRect = new Rectangle(centerX - buttonWidth / 2, centerY - 30 + buttonHeight + buttonSpacing, buttonWidth, buttonHeight);
        drawButton(g2d, scoresButtonRect, "SCORES", new Color(50, 100, 200));

        // Restart button
        restartButtonRect = new Rectangle(centerX - buttonWidth / 2, centerY - 30 + 2 * (buttonHeight + buttonSpacing), buttonWidth, buttonHeight);
        drawButton(g2d, restartButtonRect, "RESTART", new Color(200, 100, 50));
    }

    private void drawScoresPanel(Graphics2D g2d) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        // Draw "SCORES" title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g2d.getFontMetrics();
        String title = "SCORES";
        int textWidth = fm.stringWidth(title);
        g2d.drawString(title, centerX - textWidth / 2, centerY - 80);

        // Draw scores
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        fm = g2d.getFontMetrics();

        // Left player score
        g2d.setColor(new Color(100, 200, 255));
        String leftLabel = "Player 1 (Left)";
        String leftScore = String.valueOf(gameState.scoreLeft);
        g2d.drawString(leftLabel, centerX - fm.stringWidth(leftLabel) / 2, centerY - 20);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        fm = g2d.getFontMetrics();
        g2d.drawString(leftScore, centerX - fm.stringWidth(leftScore) / 2, centerY + 25);

        // Right player score
        g2d.setFont(new Font("Arial", Font.BOLD, 36));
        fm = g2d.getFontMetrics();
        g2d.setColor(new Color(255, 150, 100));
        String rightLabel = "Player 2 (Right)";
        String rightScore = String.valueOf(gameState.scoreRight);
        g2d.drawString(rightLabel, centerX - fm.stringWidth(rightLabel) / 2, centerY + 60);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        fm = g2d.getFontMetrics();
        g2d.drawString(rightScore, centerX - fm.stringWidth(rightScore) / 2, centerY + 105);

        // Back button
        backButtonRect = new Rectangle(centerX - 60, centerY + 130, 120, 40);
        drawButton(g2d, backButtonRect, "BACK", new Color(100, 100, 100));
    }

    private void drawButton(Graphics2D g2d, Rectangle rect, String text, Color color) {
        // Button background
        g2d.setColor(color);
        g2d.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 15, 15);

        // Button border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 15, 15);

        // Button text
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = rect.x + (rect.width - fm.stringWidth(text)) / 2;
        int textY = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(text, textX, textY);
    }
}
