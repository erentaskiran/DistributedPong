package pong;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        GameState gameState = new GameState();
        gameState.ballX = 400;
        gameState.ballY = 250;
        gameState.paddleLeftY = 200;
        gameState.paddleRightY = 200;
        gameState.scoreLeft = 3;
        gameState.scoreRight = 5;

        GamePanel gamePanel = new GamePanel();
        gamePanel.gameState = gameState;

        // Create JFrame window
        JFrame frame = new JFrame("Distributed Pong");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(gamePanel);
        frame.setVisible(true);
    }
}
