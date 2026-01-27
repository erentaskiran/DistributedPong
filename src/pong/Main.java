package pong;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Ask user if they want to be Host or Client
        String[] options = {"Host", "Client"};
        int choice = JOptionPane.showOptionDialog(
                null,
                "How do you want to connect?",
                "Distributed Pong",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == -1) {
            // User closed the dialog
            System.exit(0);
            return;
        }

        boolean isHost = (choice == 0);

        if (isHost) {
            startAsHost();
        } else {
            startAsClient();
        }
    }

    private static void startAsHost() {
        System.out.println("Starting as Host...");

        GameState gameState = new GameState();
        gameState.ballX = 400;
        gameState.ballY = 250;
        gameState.paddleLeftY = 200;
        gameState.paddleRightY = 200;
        gameState.scoreLeft = 0;
        gameState.scoreRight = 0;

        GamePanel gamePanel = new GamePanel();
        gamePanel.gameState = gameState;

        // Create JFrame window
        JFrame frame = new JFrame("Distributed Pong - Host");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(gamePanel);
        frame.setVisible(true);

        GameServer server = new GameServer();
        server.startGameLoop(gameState, gamePanel);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.close();
        }));
    }

    private static void startAsClient() {
        System.out.println("Starting as Client...");

        GameState gameState = new GameState();
        gameState.prevBallX = 350;
        gameState.prevBallY = 200;
        gameState.ballX = 400;
        gameState.ballY = 250;
        gameState.paddleLeftY = 200;
        gameState.paddleRightY = 200;
        gameState.scoreLeft = 0;
        gameState.scoreRight = 0;

        GamePanel gamePanel = new GamePanel();
        gamePanel.gameState = gameState;

        // Create JFrame window
        JFrame frame = new JFrame("Distributed Pong - Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(gamePanel);
        frame.setVisible(true);

        GameClient client = new GameClient();
        client.startGameLoop(gameState, gamePanel);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close();
        }));
    }
}
