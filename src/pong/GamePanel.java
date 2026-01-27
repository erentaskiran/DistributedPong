package pong;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class GamePanel extends JPanel {
    GameState gameState;
    private int currentMoveY = 0; // Current input state
    private static final int PADDLE_SPEED = 5;

    public GamePanel() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
                    currentMoveY = -PADDLE_SPEED;
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                    currentMoveY = PADDLE_SPEED;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W ||
                    e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
                    currentMoveY = 0;
                }
            }
        });
    }

    public int getCurrentMoveY() {
        return currentMoveY;
    }

    @Override
    public void paintComponent(Graphics g){
        super.paintComponent(g);

        if (gameState == null) {
            gameState = new GameState();
        }

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.WHITE);

        for (int i = 0; i < getHeight(); i += 20) {
            g.fillRect(getWidth() / 2 - 2, i, 4, 10);
        }

        g.fillRect(20, gameState.paddleLeftY, 10, 80);

        g.fillRect(getWidth() - 30, gameState.paddleRightY, 10, 80);

        g.fillOval(gameState.ballX, gameState.ballY, 15, 15);

        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString(String.valueOf(gameState.scoreLeft), getWidth() / 2 - 50, 50);
        g.drawString(String.valueOf(gameState.scoreRight), getWidth() / 2 + 30, 50);
    }
}
