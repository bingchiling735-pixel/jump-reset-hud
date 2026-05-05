package com.jumpresethud;

public class HudConfig {
    public int posX = 8;
    public int posY = 8;
    public int offsetX = 0;
    public int offsetY = 0;
    public float scale = 1.0F;
    public boolean enabled = true;

    public int finalX() {
        return posX + offsetX;
    }

    public int finalY() {
        return posY + offsetY;
    }
}
