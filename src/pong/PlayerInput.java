package pong;

import java.io.Serializable;

public class PlayerInput implements Serializable {
    int moveY;
    boolean pauseRequest = false;
    boolean restartRequest = false;
}
