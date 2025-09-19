package com.badlogic.herorungame;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Iterator;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;

    // Hero
    private Texture heroSheet;
    private Animation<TextureRegion> runAnimation;
    private TextureRegion currentFrame;
    private float stateTime;
    private float heroX, heroY;
    private float velocityY = 0;
    private final float gravity = -1.6f;
    private final float jumpStrength = 30f;
    private boolean isJumping = false;
    private final float groundY = 100f;

    // Rock
    private Texture rockTexture;
    private float rockX, rockY;
    private float rockX2, rockY2;
    private float rockSpeed = 400f;
    private boolean rockActive = false;
    private boolean doubleRocks = false;

    // Coins & Hearts
    private Texture coinTexture, heartTexture;
    private ArrayList<GameObject> fallingObjects;
    private float spawnChanceCoin = 0.01f;
    private float spawnChanceHeart = 0.002f;

    // HUD
    private Texture bannerTexture;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    private ShapeRenderer shapeRenderer2;

    // Panels
    private Texture panelStart, panelGameOver;

    // Game state
    private enum GameState { START, PLAYING, GAMEOVER }
    private GameState gameState = GameState.START;

    private float elapsedTime = 0f;
    private float difficultyTimer = 0f;

    // Stats
    private int coinCount = 0;
    private int energy = 100;

    // Invulnerability
    private boolean invulnerable = false;
    private float invulnerableTimer = 0f;

    // Parallax layers
    private ParallaxLayer groundLayer, mountainLayer, skyLayer, cloudLayer;

    // Vibración del piso
    private boolean groundVibrating = false;
    private float vibrationTime = 0f;
    private final float vibrationDuration = 0.2f;
    private final float vibrationAmplitude = 15f;

    // Sounds & music
    private Sound ouchSound, successSound, energySound, gameOverSound, alarmSound;
    private Music splashMusic, gameMusic;
    private boolean alarmPlaying = false;

    // 🔹 Progresión de dificultad visual
    private float targetTintR = 1f, targetTintG = 1f, targetTintB = 1f;
    private float currentTintR = 1f, currentTintG = 1f, currentTintB = 1f;
    private float transitionTime = 0f;
    private final float transitionDuration = 3f;

    @Override
    public void create() {
        batch = new SpriteBatch();

        // Hero setup
        heroSheet = new Texture("Hero-Run.png");
        int frameCols = 8;
        int frameRows = 1;
        int frameWidth = heroSheet.getWidth() / frameCols;
        int frameHeight = heroSheet.getHeight() / frameRows;
        TextureRegion[][] tmp = TextureRegion.split(heroSheet, frameWidth, frameHeight);
        TextureRegion[] frames = new TextureRegion[frameCols];
        for (int i = 0; i < frameCols; i++) frames[i] = tmp[0][i];
        runAnimation = new Animation<>(0.1f, frames);
        stateTime = 0f;
        heroX = 50;
        heroY = groundY;

        // Rock
        rockTexture = new Texture("Roca.png");
        spawnRock();

        // Objects
        coinTexture = new Texture("Coin.png");
        heartTexture = new Texture("Heart.png");
        fallingObjects = new ArrayList<>();

        // HUD
        bannerTexture = new Texture("Banner.png");
        font = new BitmapFont(Gdx.files.internal("HeroRG.fnt"));
        font.setColor(new Color(0.4f, 0.9f, 0.8f, 1f));
        shapeRenderer = new ShapeRenderer();
        shapeRenderer2 = new ShapeRenderer();

        // Panels
        panelStart = new Texture("PanelST.png");
        panelGameOver = new Texture("PanelGO.png");

        // Parallax
        skyLayer = new ParallaxLayer(new Texture("Sky.png"), 100, 0.05f, 1f);
        cloudLayer = new ParallaxLayer(new Texture("Clouds.png"), 100, 0.1f, 1f);
        mountainLayer = new ParallaxLayer(new Texture("mountain.png"), groundY - 35, 0.4f, 1f);
        groundLayer = new ParallaxLayer(new Texture("ground.png"), groundY - 65, 1f, 1f);
        groundLayer.setOscillate(true);

        // Sounds
        ouchSound = Gdx.audio.newSound(Gdx.files.internal("ouch.mp3"));
        successSound = Gdx.audio.newSound(Gdx.files.internal("success.mp3"));
        energySound = Gdx.audio.newSound(Gdx.files.internal("energy.mp3"));
        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("gameover.mp3"));
        alarmSound = Gdx.audio.newSound(Gdx.files.internal("alarm.mp3"));

        // Music
        splashMusic = Gdx.audio.newMusic(Gdx.files.internal("Splash-music-loop.mp3"));
        splashMusic.setLooping(true);
        splashMusic.play();

        gameMusic = Gdx.audio.newMusic(Gdx.files.internal("game-music-loop.mp3"));
        gameMusic.setLooping(true);
    }

    private void spawnRock() {
        rockX = Gdx.graphics.getWidth();
        rockY = groundY;
        rockActive = true;
        if (doubleRocks) {
            rockX2 = rockX + 200;
            rockY2 = groundY;
        }
    }

    private void spawnFallingObject() {
        boolean isCoin = MathUtils.randomBoolean(0.8f);
        Texture texture = isCoin ? coinTexture : heartTexture;
        float y = MathUtils.random(groundY + 150, 380);
        fallingObjects.add(new GameObject(texture, Gdx.graphics.getWidth(), y, isCoin));
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;

        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        switch (gameState) {
            case START: renderStart(delta); break;
            case PLAYING: renderGame(delta); break;
            case GAMEOVER: renderGameOver(); break;
        }
    }

    private void renderStart(float delta) {

        // Background sync

        float currentSpeed = rockSpeed;

        skyLayer.update(currentSpeed, delta);
        cloudLayer.update(currentSpeed, delta);
        mountainLayer.update(currentSpeed, delta);
        groundLayer.update(currentSpeed, delta);

        batch.begin();
        skyLayer.render(batch);
        cloudLayer.render(batch);
        mountainLayer.render(batch);
        groundLayer.render(batch);
        batch.draw(bannerTexture, 0, 0, 800, 100);
        batch.draw(panelStart, 55, 15, 690, 450);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            splashMusic.stop();
            gameMusic.play();
            resetGame();
            gameState = GameState.PLAYING;
        }
    }

    private void renderGame(float delta) {
        // Input
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && !isJumping) {
            velocityY = jumpStrength;
            isJumping = true;
        }

        // Physics
        heroY += velocityY;
        velocityY += gravity;
        if (heroY <= groundY) {
            if (isJumping) {
                groundVibrating = true;
                vibrationTime = 0f;
            }
            heroY = groundY;
            velocityY = 0;
            isJumping = false;
        }

        float currentSpeed = isJumping ? rockSpeed * 2 : rockSpeed;

        // Background sync
        skyLayer.update(currentSpeed, delta);
        cloudLayer.update(currentSpeed, delta);
        mountainLayer.update(currentSpeed, delta);
        groundLayer.update(currentSpeed, delta);

        // Vibración
        if (groundVibrating) {
            vibrationTime += delta;
            if (vibrationTime < vibrationDuration) {
                float offset = (float)Math.sin(vibrationTime * 50) * vibrationAmplitude;
                groundLayer.setYOffset(offset);
            } else {
                groundLayer.setYOffset(0);
                groundVibrating = false;
            }
        }

        // Rocks
        rockX -= currentSpeed * delta;
        if (doubleRocks) rockX2 -= currentSpeed * delta;
        if (rockX + rockTexture.getWidth() < 0) {
            if (MathUtils.randomBoolean(0.02f)) spawnRock();
        }

        // Objetos
        if (MathUtils.randomBoolean(spawnChanceCoin)) spawnFallingObject();
        Iterator<GameObject> iter = fallingObjects.iterator();
        while (iter.hasNext()) {
            GameObject obj = iter.next();
            if (obj.flying) {
                obj.x += obj.vx * delta;
                obj.y += obj.vy * delta;
                obj.vy -= 500 * delta;
                if (obj.x > 820 || obj.y > 500 || obj.y < -50) iter.remove();
                continue;
            }
            obj.x -= currentSpeed * delta;

            int offset = 60;
            Rectangle heroRect = new Rectangle(heroX + offset, heroY,
                currentFrame.getRegionWidth() - 2 * offset, currentFrame.getRegionHeight());
            Rectangle objRect = new Rectangle(obj.x + offset, obj.y,
                obj.texture.getWidth() - 2 * offset, obj.texture.getHeight());

            if (objRect.overlaps(heroRect) && isJumping && !invulnerable) {
                if (obj.isCoin) {
                    coinCount++;
                    successSound.play();
                    obj.flying = true;
                    obj.vx = 1000;
                    obj.vy = 1200;
                } else {
                    energy = Math.min(100, energy + 20);
                    energySound.play();
                    iter.remove();
                }
            }
            if (obj.x + obj.texture.getWidth() < 0) iter.remove();
        }

        // Frame héroe
        currentFrame = isJumping ? runAnimation.getKeyFrame(0) : runAnimation.getKeyFrame(stateTime, true);

        // Collision rock
        int offset = 60;
        Rectangle heroRect = new Rectangle(heroX + offset, heroY,
            currentFrame.getRegionWidth() - 2 * offset, currentFrame.getRegionHeight());
        Rectangle rockRect = new Rectangle(rockX + offset, rockY,
            rockTexture.getWidth() - 2 * offset, rockTexture.getHeight());

        if (!invulnerable && rockRect.overlaps(heroRect)) {
            ouchSound.play();
            energy -= 25;
            if (energy <= 0) {
                gameMusic.stop();
                alarmSound.stop();
                gameOverSound.play();
                gameState = GameState.GAMEOVER;
            } else {
                invulnerable = true;
                invulnerableTimer = 3f;
            }
        }
        if (doubleRocks) {
            Rectangle rockRect2 = new Rectangle(rockX2 + offset, rockY2,
                rockTexture.getWidth() - 2 * offset, rockTexture.getHeight());
            if (!invulnerable && rockRect2.overlaps(heroRect)) {
                ouchSound.play();
                energy -= 25;
                if (energy <= 0) {
                    gameMusic.stop();
                    alarmSound.stop();
                    gameOverSound.play();
                    gameState = GameState.GAMEOVER;
                } else {
                    invulnerable = true;
                    invulnerableTimer = 3f;
                }
            }
        }

        if (invulnerable) {
            invulnerableTimer -= delta;
            if (invulnerableTimer <= 0) invulnerable = false;
        }

        elapsedTime += delta;
        difficultyTimer += delta;

        // Rocas dobles a los 60s
        if (elapsedTime >= 60f) doubleRocks = true;

        // 🔹 Dificultad progresiva
        if (difficultyTimer >= 20f) {
            rockSpeed *= 1.15f;
            spawnChanceCoin *= 0.9f;
            spawnChanceHeart *= 0.9f;
            difficultyTimer = 0f;

            // 🔹 Tint más rojo
            targetTintG *= 0.9f;
            targetTintB *= 0.9f;
            transitionTime = 0f;
        }

        // 🔹 Transición suave del tinte
        if (transitionTime < transitionDuration) {
            transitionTime += delta;
            float alpha = Math.min(1f, transitionTime / transitionDuration);
            currentTintR = 1f;
            currentTintG = MathUtils.lerp(currentTintG, targetTintG, alpha);
            currentTintB = MathUtils.lerp(currentTintB, targetTintB, alpha);
        }

        // 🔹 Alarma energía baja
        if (energy <= 25 && !alarmPlaying) {
            alarmSound.loop();
            alarmPlaying = true;
        } else if (energy > 25 && alarmPlaying) {
            alarmSound.stop();
            alarmPlaying = false;
        }

        // === DRAW ===
        batch.begin();
        batch.setColor(currentTintR, currentTintG, currentTintB, 1f);

        skyLayer.render(batch);
        cloudLayer.render(batch);
        mountainLayer.render(batch);
        groundLayer.render(batch);

        boolean drawHero = true;
        if (invulnerable) {
            float blinkSpeed = 0.1f;
            drawHero = ((int) (invulnerableTimer / blinkSpeed)) % 2 == 0;
        }
        if (drawHero) batch.draw(currentFrame, heroX, heroY);

        if (rockActive) batch.draw(rockTexture, rockX, rockY);
        if (doubleRocks) batch.draw(rockTexture, rockX2, rockY2);

        for (GameObject obj : fallingObjects) batch.draw(obj.texture, obj.x, obj.y);

        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(bannerTexture, 0, 0, 800, 100);

        int seconds = (int) elapsedTime;
        int tenths = (int) ((elapsedTime - seconds) * 10);
        //font.draw(batch, String.format("%d:%d", seconds, tenths), 540, 67);
        font.draw(batch, String.valueOf(coinCount), 740, 70);
        batch.end();

        // Energy bar
        float energyClamped = Math.max(0, Math.min(energy, 100));
        float energiaWidth = 330 * (energyClamped / 100f);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.rect(440, 16, 330, 10);
        shapeRenderer.end();

        shapeRenderer2.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer2.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer2.setColor(new Color(0.4f, 0.9f, 0.8f, 1f));
        shapeRenderer2.rect(439, 14, energiaWidth + 3, 13);
        shapeRenderer2.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Color colorEnergia;
        if (energyClamped >= 51) {
            colorEnergia = Color.GREEN;
        } else if (energyClamped >= 26) {
            colorEnergia = Color.YELLOW;
        } else {
            colorEnergia = Color.RED;
        }
        shapeRenderer.setColor(colorEnergia);
        shapeRenderer.rect(440, 16, energiaWidth, 10);
        shapeRenderer.end();
    }

    private void renderGameOver() {
        batch.begin();
        skyLayer.render(batch);
        cloudLayer.render(batch);
        mountainLayer.render(batch);
        groundLayer.render(batch);
        batch.draw(bannerTexture, 0, 0, 800, 100);
        batch.draw(panelGameOver, 55, 15, 690, 450);
        //font.draw(batch, String.format("YOUR SCORE  %d", coinCount), 300, 230);//DDR
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            gameOverSound.stop();
            splashMusic.play();
            gameState = GameState.START;
        }
    }

    private void resetGame() {
        heroY = groundY;
        velocityY = 0;
        isJumping = false;
        stateTime = 0f;
        elapsedTime = 0f;
        energy = 100;
        coinCount = 0;
        fallingObjects.clear();
        invulnerable = false;
        spawnRock();
        difficultyTimer = 0f;
        rockSpeed = 400f;
        spawnChanceCoin = 0.01f;
        spawnChanceHeart = 0.002f;
        doubleRocks = false;

        // 🔹 Reset tint
        targetTintR = 1f; targetTintG = 1f; targetTintB = 1f;
        currentTintR = 1f; currentTintG = 1f; currentTintB = 1f;
        transitionTime = 0f;
    }

    @Override
    public void dispose() {
        batch.dispose();
        heroSheet.dispose();
        rockTexture.dispose();
        coinTexture.dispose();
        heartTexture.dispose();
        bannerTexture.dispose();
        font.dispose();
        shapeRenderer.dispose();
        shapeRenderer2.dispose();
        skyLayer.dispose();
        cloudLayer.dispose();
        mountainLayer.dispose();
        groundLayer.dispose();
        panelStart.dispose();
        panelGameOver.dispose();
        ouchSound.dispose();
        successSound.dispose();
        energySound.dispose();
        gameOverSound.dispose();
        alarmSound.dispose();
        splashMusic.dispose();
        gameMusic.dispose();
    }

    private static class GameObject {
        Texture texture;
        float x, y;
        boolean isCoin;
        boolean flying = false;
        float vx, vy;

        GameObject(Texture texture, float x, float y, boolean isCoin) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.isCoin = isCoin;
        }
    }
}
