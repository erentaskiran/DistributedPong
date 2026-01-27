package pong;

import java.io.Serializable;

public class GameState implements Serializable {
    int ballX;
    int ballY;
    int paddleLeftY;
    int paddleRightY;
    int scoreLeft;
    int scoreRight;

    public GameState() {
        ballX = 0;
        ballY = 0;
        paddleLeftY = 0;
        paddleRightY = 0;
        scoreLeft = 0;
        scoreRight = 0;
    }
}
