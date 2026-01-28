package pong;

import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GameState implements Serializable {
    // Transient ReadWriteLock for thread-safe variables
    private transient ReadWriteLock lock = new ReentrantReadWriteLock();

    // Atomic variables - thread-safe
    private AtomicInteger atomicBallX = new AtomicInteger(0);
    private AtomicInteger atomicBallY = new AtomicInteger(0);
    private AtomicInteger atomicPaddleLeftY = new AtomicInteger(0);
    private AtomicInteger atomicPaddleRightY = new AtomicInteger(0);
    private AtomicInteger atomicScoreLeft = new AtomicInteger(0);
    private AtomicInteger atomicScoreRight = new AtomicInteger(0);
    private AtomicBoolean atomicIsPaused = new AtomicBoolean(false);

    // Primitive variables for serialization (legacy API compatibility)
    int prevBallX;
    int prevBallY;
    int ballX;
    int ballY;
    int paddleLeftY;
    int paddleRightY;
    int scoreLeft;
    int scoreRight;

    // Ball velocity - volatile for thread visibility
    volatile int ballVelocityX = 5;
    volatile int ballVelocityY = 5;

    // Pause state
    boolean isPaused = false;

    // Game constants
    private static final int BALL_SIZE = 15;
    private static final int PADDLE_WIDTH = 10;
    private static final int PADDLE_HEIGHT = 80;
    private static final int GAME_WIDTH = 800;
    private static final int GAME_HEIGHT = 600;
    private static final int PADDLE_LEFT_X = 20;
    private static final int PADDLE_RIGHT_X = 770;

    public GameState() {
        // Initialize lock if null (for deserialization)
        if (lock == null) {
            lock = new ReentrantReadWriteLock();
        }

        prevBallX = 0;
        prevBallY = 0;
        ballX = 0;
        ballY = 0;
        paddleLeftY = 0;
        paddleRightY = 0;
        scoreLeft = 0;
        scoreRight = 0;

        // Initialize atomic values
        atomicBallX.set(0);
        atomicBallY.set(0);
        atomicPaddleLeftY.set(0);
        atomicPaddleRightY.set(0);
        atomicScoreLeft.set(0);
        atomicScoreRight.set(0);
        atomicIsPaused.set(false);
    }

    // Thread-safe getters using ReadLock
    public int getBallX() {
        lock.readLock().lock();
        try {
            return atomicBallX.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getBallY() {
        lock.readLock().lock();
        try {
            return atomicBallY.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getPaddleLeftY() {
        lock.readLock().lock();
        try {
            return atomicPaddleLeftY.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getPaddleRightY() {
        lock.readLock().lock();
        try {
            return atomicPaddleRightY.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Thread-safe setters using WriteLock
    public void setBallX(int x) {
        lock.writeLock().lock();
        try {
            atomicBallX.set(x);
            this.ballX = x;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setBallY(int y) {
        lock.writeLock().lock();
        try {
            atomicBallY.set(y);
            this.ballY = y;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setPaddleLeftY(int y) {
        lock.writeLock().lock();
        try {
            atomicPaddleLeftY.set(y);
            this.paddleLeftY = y;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setPaddleRightY(int y) {
        lock.writeLock().lock();
        try {
            atomicPaddleRightY.set(y);
            this.paddleRightY = y;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Synchronized method for updating paddle position
    public synchronized void updatePaddleLeft(int deltaY) {
        lock.writeLock().lock();
        try {
            int newY = atomicPaddleLeftY.get() + deltaY;
            atomicPaddleLeftY.set(newY);
            this.paddleLeftY = newY;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public synchronized void updatePaddleRight(int deltaY) {
        lock.writeLock().lock();
        try {
            int newY = atomicPaddleRightY.get() + deltaY;
            atomicPaddleRightY.set(newY);
            this.paddleRightY = newY;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public synchronized void moveBall(){
        lock.writeLock().lock();
        try {
            // Store previous position
            prevBallX = atomicBallX.get();
            prevBallY = atomicBallY.get();

            int currentBallX = atomicBallX.get();
            int currentBallY = atomicBallY.get();

            // Update ball position
            currentBallX += ballVelocityX;
            currentBallY += ballVelocityY;

            // Check collision with top and bottom walls
            if (currentBallY <= 0) {
                currentBallY = 0;
                ballVelocityY = -ballVelocityY; // Bounce
            }
            if (currentBallY >= GAME_HEIGHT - BALL_SIZE) {
                currentBallY = GAME_HEIGHT - BALL_SIZE;
                ballVelocityY = -ballVelocityY; // Bounce
            }

            int currentPaddleLeftY = atomicPaddleLeftY.get();
            int currentPaddleRightY = atomicPaddleRightY.get();

            // Check collision with left paddle
            if (currentBallX <= PADDLE_LEFT_X + PADDLE_WIDTH &&
                currentBallX + BALL_SIZE >= PADDLE_LEFT_X &&
                currentBallY + BALL_SIZE >= currentPaddleLeftY &&
                currentBallY <= currentPaddleLeftY + PADDLE_HEIGHT) {
                ballVelocityX = Math.abs(ballVelocityX); // Bounce right
                currentBallX = PADDLE_LEFT_X + PADDLE_WIDTH; // Prevent sticking
            }

            // Check collision with right paddle
            if (currentBallX + BALL_SIZE >= PADDLE_RIGHT_X &&
                currentBallX <= PADDLE_RIGHT_X + PADDLE_WIDTH &&
                currentBallY + BALL_SIZE >= currentPaddleRightY &&
                currentBallY <= currentPaddleRightY + PADDLE_HEIGHT) {
                ballVelocityX = -Math.abs(ballVelocityX); // Bounce left
                currentBallX = PADDLE_RIGHT_X - BALL_SIZE; // Prevent sticking
            }

            // Check if ball went out of bounds (scoring)
            if (currentBallX < 0) {
                atomicScoreRight.incrementAndGet();
                scoreRight = atomicScoreRight.get();
                resetBallInternal();
                return;
            } else if (currentBallX > GAME_WIDTH) {
                atomicScoreLeft.incrementAndGet();
                scoreLeft = atomicScoreLeft.get();
                resetBallInternal();
                return;
            }

            // Update atomic values
            atomicBallX.set(currentBallX);
            atomicBallY.set(currentBallY);
            this.ballX = currentBallX;
            this.ballY = currentBallY;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void resetBallInternal() {
        // Must be called within write lock
        int newBallX = GAME_WIDTH / 2;
        int newBallY = GAME_HEIGHT / 2;
        atomicBallX.set(newBallX);
        atomicBallY.set(newBallY);
        this.ballX = newBallX;
        this.ballY = newBallY;

        // Randomize direction slightly
        ballVelocityX = (ballVelocityX > 0 ? -5 : 5);
        ballVelocityY = (Math.random() > 0.5 ? 5 : -5);
    }

    public synchronized void checkBoundaries(){
        lock.writeLock().lock();
        try {
            // Keep left paddle within screen bounds
            int leftY = atomicPaddleLeftY.get();
            if (leftY < 0) {
                leftY = 0;
                atomicPaddleLeftY.set(leftY);
                this.paddleLeftY = leftY;
            }
            if (leftY > GAME_HEIGHT - PADDLE_HEIGHT) {
                leftY = GAME_HEIGHT - PADDLE_HEIGHT;
                atomicPaddleLeftY.set(leftY);
                this.paddleLeftY = leftY;
            }

            // Keep right paddle within screen bounds
            int rightY = atomicPaddleRightY.get();
            if (rightY < 0) {
                rightY = 0;
                atomicPaddleRightY.set(rightY);
                this.paddleRightY = rightY;
            }
            if (rightY > GAME_HEIGHT - PADDLE_HEIGHT) {
                rightY = GAME_HEIGHT - PADDLE_HEIGHT;
                atomicPaddleRightY.set(rightY);
                this.paddleRightY = rightY;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public synchronized void resetGame() {
        lock.writeLock().lock();
        try {
            int centerX = GAME_WIDTH / 2;
            int centerY = GAME_HEIGHT / 2;
            int paddleCenterY = centerY - PADDLE_HEIGHT / 2;

            atomicBallX.set(centerX);
            atomicBallY.set(centerY);
            atomicPaddleLeftY.set(paddleCenterY);
            atomicPaddleRightY.set(paddleCenterY);
            atomicScoreLeft.set(0);
            atomicScoreRight.set(0);
            atomicIsPaused.set(false);

            // Update primitive values
            ballX = centerX;
            ballY = centerY;
            paddleLeftY = paddleCenterY;
            paddleRightY = paddleCenterY;
            scoreLeft = 0;
            scoreRight = 0;
            ballVelocityX = 5;
            ballVelocityY = 5;
            isPaused = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public synchronized void togglePause() {
        boolean newPauseState = atomicIsPaused.get();
        newPauseState = !newPauseState;
        atomicIsPaused.set(newPauseState);
        isPaused = newPauseState;
    }

    // Synchronize state before serialization
    public synchronized void syncStateForSerialization() {
        lock.readLock().lock();
        try {
            this.ballX = atomicBallX.get();
            this.ballY = atomicBallY.get();
            this.paddleLeftY = atomicPaddleLeftY.get();
            this.paddleRightY = atomicPaddleRightY.get();
            this.scoreLeft = atomicScoreLeft.get();
            this.scoreRight = atomicScoreRight.get();
            this.isPaused = atomicIsPaused.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Restore atomic values after deserialization
    private Object readResolve() {
        lock = new ReentrantReadWriteLock();
        atomicBallX = new AtomicInteger(ballX);
        atomicBallY = new AtomicInteger(ballY);
        atomicPaddleLeftY = new AtomicInteger(paddleLeftY);
        atomicPaddleRightY = new AtomicInteger(paddleRightY);
        atomicScoreLeft = new AtomicInteger(scoreLeft);
        atomicScoreRight = new AtomicInteger(scoreRight);
        atomicIsPaused = new AtomicBoolean(isPaused);
        return this;
    }
}
