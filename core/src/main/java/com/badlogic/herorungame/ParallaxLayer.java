package com.badlogic.herorungame;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class ParallaxLayer {
    private Texture texture;
    private float x1, x2;
    private float baseY;
    private float y;
    private float speedFactor;
    private float scale;
    private float oscillationTime = 0f;
    private boolean oscillate = false;

    // Nuevo: offset manual para vibraciones/efectos
    private float yOffset = 0f;

    public ParallaxLayer(Texture texture, float y, float speedFactor, float scale) {
        this.texture = texture;
        this.x1 = 0;
        this.x2 = texture.getWidth();
        this.baseY = y;
        this.y = y;
        this.speedFactor = speedFactor;
        this.scale = scale;
    }

    public void setOscillate(boolean oscillate) {
        this.oscillate = oscillate;
    }

    // Nuevo: permite aplicar un offset manual
    public void setYOffset(float offset) {
        this.yOffset = offset;
    }

    public void update(float baseSpeed, float delta) {
        float speed = baseSpeed * speedFactor;

        x1 -= speed * delta;
        x2 -= speed * delta;

        if (x1 + texture.getWidth() < 0) {
            x1 = x2 + texture.getWidth();
        }
        if (x2 + texture.getWidth() < 0) {
            x2 = x1 + texture.getWidth();
        }

        if (oscillate) {
            oscillationTime += delta;
            float amplitude = 5f; // altura de oscilación
            float frequency = 2f; // velocidad del vaivén
            y = baseY + MathUtils.sin(oscillationTime * frequency) * amplitude;
        } else {
            y = baseY;
        }

        // Aplicar el offset adicional (para vibración del impacto)
        y += yOffset;
    }

    public void render(SpriteBatch batch) {
        float scaledWidth = texture.getWidth() * scale;
        float scaledHeight = texture.getHeight() * scale;

        batch.draw(texture, x1, y, scaledWidth, scaledHeight);
        batch.draw(texture, x2, y, scaledWidth, scaledHeight);
    }

    public void dispose() {
        texture.dispose();
    }
}
