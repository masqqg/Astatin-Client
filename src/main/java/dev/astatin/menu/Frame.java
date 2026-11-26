package dev.astatin.menu;

public record Frame(float x, float y, float w, float h) {
    public float right() {
        return x + w;
    }

    public float bottom() {
        return y + h;
    }

    public float centerX() {
        return x + w * 0.5f;
    }

    public float centerY() {
        return y + h * 0.5f;
    }

    public boolean contains(float px, float py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public boolean contains(double px, double py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public Frame inset(float value) {
        return new Frame(x + value, y + value, w - value * 2f, h - value * 2f);
    }

    public Frame offset(float dx, float dy) {
        return new Frame(x + dx, y + dy, w, h);
    }
}
