package pong;

import java.io.Serializable;

public class GameState implements Serializable {
    int prevBallX;
    int prevBallY;
    int ballX;
    int ballY;
    int paddleLeftY;
    int paddleRightY;
    int scoreLeft;
    int scoreRight;

    // Ball velocity
    int ballVelocityX = 5;
    int ballVelocityY = 5;

    // Game constants
    private static final int BALL_SIZE = 15;
    private static final int PADDLE_WIDTH = 10;
    private static final int PADDLE_HEIGHT = 80;
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final int PADDLE_LEFT_X = 20;
    private static final int PADDLE_RIGHT_X = 770;

    public GameState() {
        prevBallX = 0;
        prevBallY = 0;
        ballX = 0;
        ballY = 0;
        paddleLeftY = 0;
        paddleRightY = 0;
        scoreLeft = 0;
        scoreRight = 0;
    }

    public void moveBall(){
        // Store previous position
        prevBallX = ballX;
        prevBallY = ballY;

        // Update ball position
        ballX += ballVelocityX;
        ballY += ballVelocityY;

        // Check collision with top and bottom walls
        if (ballY <= 0) {
            ballY = 0;
            ballVelocityY = -ballVelocityY; // Bounce
        }
        if (ballY >= GAME_HEIGHT - BALL_SIZE) {
            ballY = GAME_HEIGHT - BALL_SIZE;
            ballVelocityY = -ballVelocityY; // Bounce
        }

        // Check collision with left paddle
        if (ballX <= PADDLE_LEFT_X + PADDLE_WIDTH &&
            ballX + BALL_SIZE >= PADDLE_LEFT_X &&
            ballY + BALL_SIZE >= paddleLeftY &&
            ballY <= paddleLeftY + PADDLE_HEIGHT) {
            ballVelocityX = Math.abs(ballVelocityX); // Bounce right
            ballX = PADDLE_LEFT_X + PADDLE_WIDTH; // Prevent sticking
        }

        // Check collision with right paddle
        if (ballX + BALL_SIZE >= PADDLE_RIGHT_X &&
            ballX <= PADDLE_RIGHT_X + PADDLE_WIDTH &&
            ballY + BALL_SIZE >= paddleRightY &&
            ballY <= paddleRightY + PADDLE_HEIGHT) {
            ballVelocityX = -Math.abs(ballVelocityX); // Bounce left
            ballX = PADDLE_RIGHT_X - BALL_SIZE; // Prevent sticking
        }

        // Check if ball went out of bounds (scoring)
        if (ballX < 0) {
            scoreRight++;
            resetBall();
        } else if (ballX > GAME_WIDTH) {
            scoreLeft++;
            resetBall();
        }
    }

    private void resetBall() {
        ballX = GAME_WIDTH / 2;
        ballY = GAME_HEIGHT / 2;
        // Randomize direction slightly
        ballVelocityX = (ballVelocityX > 0 ? -5 : 5);
        ballVelocityY = (Math.random() > 0.5 ? 5 : -5);
    }

    public void checkBoundaries(){
        // Keep left paddle within screen bounds
        if (paddleLeftY < 0) {
            paddleLeftY = 0;
        }
        if (paddleLeftY > GAME_HEIGHT - PADDLE_HEIGHT) {
            paddleLeftY = GAME_HEIGHT - PADDLE_HEIGHT;
        }

        // Keep right paddle within screen bounds
        if (paddleRightY < 0) {
            paddleRightY = 0;
        }
        if (paddleRightY > GAME_HEIGHT - PADDLE_HEIGHT) {
            paddleRightY = GAME_HEIGHT - PADDLE_HEIGHT;
        }
    }


}
