package pong;

import java.io.Serializable;

public class PlayerInput implements Serializable {
    int moveY;

    public PlayerInput(int moveY) {
        this.moveY = moveY;
    }
}
