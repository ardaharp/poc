package com.example.flappy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

/**
 * A complete, self-contained implementation of a simple Flappy Bird style game.
 * All game logic, rendering, input handling, and state are encapsulated in this single
 * custom View class.
 */
public class FlappyBirdView extends View {

    // ---------------------------------------------------------------------
    // Constants controlling the overall feel of the game
    // ---------------------------------------------------------------------
    private static final long FRAME_DELAY_MS = 16L;               // ~60 FPS
    private static final float GRAVITY = 0.45f;                   // Downward acceleration per frame
    private static final float FLAP_VELOCITY = -9f;               // Upward velocity applied on tap
    private static final float PIPE_SPEED = 6f;                   // Horizontal speed of pipes
    private static final float PIPE_WIDTH_DP = 60f;               // Width of each pipe
    private static final float PIPE_GAP_DP = 180f;                // Vertical gap between top/bottom pipe
    private static final int NUM_PIPES = 3;                       // Number of pipe pairs recycled
    private static final int NUM_CLOUDS = 4;                      // Decorative cloud count
    private static final float CLOUD_SPEED_FACTOR = 0.25f;        // Parallax speed multiplier for clouds
    private static final float GROUND_SCROLL_FACTOR = 0.6f;       // Parallax speed multiplier for ground tiles

    // Pixel art definitions for decorative elements
    private static final int[][] BIRD_PIXELS = {
            {0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 2, 2, 1, 1, 0, 0, 0},
            {0, 0, 1, 1, 2, 2, 2, 2, 1, 1, 0, 0},
            {0, 1, 1, 2, 2, 7, 7, 2, 2, 1, 3, 0},
            {0, 1, 2, 2, 7, 7, 7, 7, 2, 3, 1, 0},
            {0, 1, 2, 7, 7, 7, 7, 7, 7, 3, 1, 0},
            {0, 1, 2, 7, 7, 7, 7, 7, 7, 3, 1, 0},
            {0, 0, 1, 2, 2, 7, 7, 7, 3, 3, 1, 0},
            {0, 0, 0, 5, 5, 5, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 5, 6, 6, 5, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0}
    };

    private static final int[][] CLOUD_PIXELS = {
            {0, 0, 1, 1, 1, 0, 0},
            {0, 1, 1, 1, 1, 1, 0},
            {1, 1, 1, 2, 2, 1, 1},
            {0, 1, 1, 1, 2, 1, 0},
            {0, 0, 1, 2, 1, 0, 0}
    };

    // ---------------------------------------------------------------------
    // Paint objects for drawing game elements and text
    // ---------------------------------------------------------------------
    private final Paint birdShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint birdHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint birdBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint birdShadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint birdWingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint birdEyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint birdPupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint birdBeakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint pipePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pipeShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pipeHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint backgroundPaint = new Paint();
    private final Paint horizonPaint = new Paint();
    private final Paint skyBandPaint = new Paint();
    private final Paint mountainFarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mountainNearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mountainHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sunGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cloudShadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint groundPaint = new Paint();
    private final Paint groundShadowPaint = new Paint();
    private final Paint groundHighlightPaint = new Paint();

    private final Paint hudPanelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gameOverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ---------------------------------------------------------------------
    // Game loop machinery: Handler + Runnable for a continuous update cycle
    // ---------------------------------------------------------------------
    private final Handler handler = new Handler();
    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            boolean shouldContinue = updateGame();
            invalidate();
            if (shouldContinue) {
                handler.postDelayed(this, FRAME_DELAY_MS);
            }
        }
    };

    // ---------------------------------------------------------------------
    // Bird state: position and velocity
    // ---------------------------------------------------------------------
    private float birdX;
    private float birdY;
    private float birdRadius;
    private float birdVelocity;

    // ---------------------------------------------------------------------
    // Pipe state: each pipe pair shares x and opening properties
    // ---------------------------------------------------------------------
    private final Pipe[] pipes = new Pipe[NUM_PIPES];
    private float pipeWidth;
    private float pipeGap;
    private float pipeSpacing;
    private float pipeSpeed;

    private final float[] cloudX = new float[NUM_CLOUDS];
    private final float[] cloudY = new float[NUM_CLOUDS];
    private final float[] cloudScale = new float[NUM_CLOUDS];

    private final Paint[] birdPalette = new Paint[8];
    private final Paint[] cloudPalette = new Paint[3];

    private float groundHeight;
    private float groundTileWidth;
    private float groundOffset;
    private float cloudBaseSize;

    // ---------------------------------------------------------------------
    // Global game state
    // ---------------------------------------------------------------------
    private boolean isGameOver = false;
    private boolean isInitialized = false;
    private int score = 0;

    private final Random random = new Random();

    public FlappyBirdView(Context context) {
        super(context);
        init();
    }

    public FlappyBirdView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FlappyBirdView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    // ---------------------------------------------------------------------
    // Initialization helpers: configure paints and game parameters
    // ---------------------------------------------------------------------
    private void init() {
        birdShadowPaint.setColor(Color.argb(80, 0, 0, 0));
        birdHighlightPaint.setColor(Color.rgb(255, 242, 173));
        birdBodyPaint.setColor(Color.rgb(255, 219, 94));
        birdShadePaint.setColor(Color.rgb(245, 176, 48));
        birdWingPaint.setColor(Color.rgb(217, 132, 36));
        birdEyePaint.setColor(Color.WHITE);
        birdPupilPaint.setColor(Color.BLACK);
        birdBeakPaint.setColor(Color.rgb(255, 137, 20));

        birdPalette[1] = birdHighlightPaint;
        birdPalette[2] = birdBodyPaint;
        birdPalette[3] = birdShadePaint;
        birdPalette[4] = birdBeakPaint;
        birdPalette[5] = birdEyePaint;
        birdPalette[6] = birdPupilPaint;
        birdPalette[7] = birdWingPaint;

        pipePaint.setColor(Color.rgb(60, 170, 73));
        pipeShadowPaint.setColor(Color.rgb(43, 120, 57));
        pipeHighlightPaint.setColor(Color.rgb(166, 239, 172));

        backgroundPaint.setColor(Color.rgb(125, 192, 255));
        horizonPaint.setColor(Color.rgb(163, 215, 255));
        skyBandPaint.setColor(Color.rgb(143, 205, 255));
        mountainFarPaint.setColor(Color.rgb(96, 160, 214));
        mountainNearPaint.setColor(Color.rgb(77, 140, 190));
        mountainHighlightPaint.setColor(Color.rgb(160, 200, 230));
        sunPaint.setColor(Color.rgb(255, 240, 170));
        sunGlowPaint.setColor(Color.argb(120, 255, 250, 200));
        cloudLightPaint.setColor(Color.rgb(255, 255, 255));
        cloudShadePaint.setColor(Color.rgb(214, 234, 248));

        cloudPalette[1] = cloudLightPaint;
        cloudPalette[2] = cloudShadePaint;

        groundPaint.setColor(Color.rgb(224, 180, 92));
        groundShadowPaint.setColor(Color.rgb(188, 140, 64));
        groundHighlightPaint.setColor(Color.rgb(244, 204, 120));

        hudPanelPaint.setColor(Color.argb(150, 0, 0, 0));

        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextAlign(Paint.Align.CENTER);
        scorePaint.setTextSize(spToPx(24));
        scorePaint.setFakeBoldText(true);
        scorePaint.setShadowLayer(dpToPx(2f), 0f, dpToPx(1f), Color.argb(160, 0, 0, 0));

        gameOverPaint.setColor(Color.WHITE);
        gameOverPaint.setTextAlign(Paint.Align.CENTER);
        gameOverPaint.setTextSize(spToPx(32));
        gameOverPaint.setFakeBoldText(true);
        gameOverPaint.setShadowLayer(dpToPx(3f), 0f, dpToPx(1.5f), Color.argb(180, 0, 0, 0));

        pipeWidth = dpToPx(PIPE_WIDTH_DP);
        pipeGap = dpToPx(PIPE_GAP_DP);
        pipeSpeed = PIPE_SPEED * getResources().getDisplayMetrics().density;
        pipeSpacing = pipeWidth * 3f;

        groundTileWidth = dpToPx(24f);
        cloudBaseSize = dpToPx(12f);

        for (int i = 0; i < pipes.length; i++) {
            pipes[i] = new Pipe();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        resetGame();
        isInitialized = true;
        startLoop();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(gameLoop);
    }

    // ---------------------------------------------------------------------
    // Game loop control
    // ---------------------------------------------------------------------
    private void startLoop() {
        handler.removeCallbacks(gameLoop);
        handler.post(gameLoop);
    }

    // ---------------------------------------------------------------------
    // Game state reset: called on first layout and after game over
    // ---------------------------------------------------------------------
    private void resetGame() {
        birdRadius = Math.min(getWidth(), getHeight()) * 0.03f;
        birdX = getWidth() * 0.35f;
        birdY = getHeight() * 0.5f;
        birdVelocity = 0f;

        pipeSpacing = Math.max(pipeWidth * 2f, getWidth() / (float) NUM_PIPES);

        groundHeight = getHeight() * 0.18f;
        groundOffset = 0f;

        for (int i = 0; i < NUM_CLOUDS; i++) {
            cloudScale[i] = 0.6f + random.nextFloat() * 0.7f;
            float spacing = getWidth() / (float) (NUM_CLOUDS + 1);
            cloudX[i] = spacing * (i + 1) + random.nextFloat() * spacing * 0.5f;
            cloudY[i] = getHeight() * 0.08f + random.nextFloat() * getHeight() * 0.22f;
        }

        float startX = getWidth();
        for (int i = 0; i < pipes.length; i++) {
            Pipe pipe = pipes[i];
            pipe.x = startX + i * pipeSpacing;
            pipe.resetOpening();
            pipe.scored = false;
        }

        score = 0;
        isGameOver = false;
    }

    // ---------------------------------------------------------------------
    // Main update routine: physics, pipe recycling, scoring, collision
    // ---------------------------------------------------------------------
    private boolean updateGame() {
        if (!isInitialized) {
            return true;
        }

        if (isGameOver) {
            return false;
        }

        // ----- Physics: apply gravity and update bird position
        birdVelocity += GRAVITY;
        birdY += birdVelocity;

        // ----- Move pipes left and recycle when off-screen
        for (Pipe pipe : pipes) {
            pipe.x -= pipeSpeed;
            if (pipe.x + pipeWidth < 0) {
                float farthest = getFarthestPipeX();
                pipe.x = Math.max(getWidth(), farthest) + pipeSpacing;
                pipe.resetOpening();
                pipe.scored = false;
            }
        }

        if (groundTileWidth > 0f) {
            groundOffset += pipeSpeed * GROUND_SCROLL_FACTOR;
            if (groundOffset > groundTileWidth) {
                groundOffset -= groundTileWidth;
            }
        }

        for (int i = 0; i < NUM_CLOUDS; i++) {
            float speed = pipeSpeed * CLOUD_SPEED_FACTOR * cloudScale[i];
            cloudX[i] -= speed;
            float cloudWidth = CLOUD_PIXELS[0].length * cloudBaseSize * cloudScale[i];
            if (cloudX[i] + cloudWidth < 0) {
                cloudScale[i] = 0.6f + random.nextFloat() * 0.7f;
                float farthest = Math.max(getWidth(), getFarthestCloudX());
                cloudX[i] = farthest + cloudBaseSize * 4f;
                cloudY[i] = getHeight() * 0.08f + random.nextFloat() * getHeight() * 0.22f;
            }
        }

        // ----- Scoring: bird passes the center line of a pipe pair
        for (Pipe pipe : pipes) {
            float pipeCenter = pipe.x + pipeWidth / 2f;
            if (!pipe.scored && birdX > pipeCenter) {
                pipe.scored = true;
                score++;
            }
        }

        // ----- Collision detection with pipes and screen bounds
        RectF birdRect = new RectF(
                birdX - birdRadius,
                birdY - birdRadius,
                birdX + birdRadius,
                birdY + birdRadius
        );

        // Screen bounds collision
        if (birdRect.top <= 0 || birdRect.bottom >= getHeight()) {
            triggerGameOver();
            return false;
        }

        for (Pipe pipe : pipes) {
            RectF topPipe = pipe.getTopRect();
            RectF bottomPipe = pipe.getBottomRect();
            if (RectF.intersects(birdRect, topPipe) || RectF.intersects(birdRect, bottomPipe)) {
                triggerGameOver();
                return false;
            }
        }
        return true;
    }

    private void triggerGameOver() {
        isGameOver = true;
    }

    // ---------------------------------------------------------------------
    // Rendering: draw background, pipes, bird, score, and game over message
    // ---------------------------------------------------------------------
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawSky(canvas);
        drawSun(canvas);
        drawMountains(canvas);
        drawClouds(canvas);
        drawPipes(canvas);
        drawGround(canvas);
        drawBird(canvas);
        drawHud(canvas);
    }

    private void drawSky(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        canvas.drawRect(0, 0, width, height, backgroundPaint);

        float bandHeight = height * 0.045f;
        for (int i = 0; i < 6; i++) {
            float top = i * bandHeight * 1.3f;
            canvas.drawRect(0, top, width, top + bandHeight, skyBandPaint);
        }

        float horizonY = height * 0.65f;
        canvas.drawRect(0, horizonY, width, height, horizonPaint);
    }

    private void drawSun(Canvas canvas) {
        float radius = Math.min(getWidth(), getHeight()) * 0.085f;
        float cx = getWidth() * 0.18f;
        float cy = getHeight() * 0.2f;
        canvas.drawCircle(cx, cy, radius * 1.4f, sunGlowPaint);
        canvas.drawCircle(cx, cy, radius, sunPaint);
    }

    private void drawMountains(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float horizonY = height * 0.72f;

        drawSteppedMountain(canvas, width * 0.15f, horizonY, width * 0.5f, height * 0.42f, mountainFarPaint, 12);
        drawSteppedMountain(canvas, width * 0.55f, horizonY, width * 0.45f, height * 0.38f, mountainFarPaint, 10);

        drawSteppedMountain(canvas, width * 0.05f, horizonY + height * 0.05f, width * 0.6f, height * 0.48f, mountainNearPaint, 14);
        drawSteppedMountain(canvas, width * 0.52f, horizonY + height * 0.06f, width * 0.5f, height * 0.45f, mountainNearPaint, 13);
    }

    private void drawClouds(Canvas canvas) {
        for (int i = 0; i < NUM_CLOUDS; i++) {
            float scale = cloudScale[i];
            float pixelSize = cloudBaseSize * scale;
            float width = CLOUD_PIXELS[0].length * pixelSize;
            float height = CLOUD_PIXELS.length * pixelSize;
            float left = cloudX[i] - width / 2f;
            float top = cloudY[i];
            drawPixelArt(canvas, left, top, pixelSize, CLOUD_PIXELS, cloudPalette);
        }
    }

    private void drawPipes(Canvas canvas) {
        for (Pipe pipe : pipes) {
            drawPipeSegment(canvas, pipe.getTopRect(), true);
            drawPipeSegment(canvas, pipe.getBottomRect(), false);
        }
    }

    private void drawGround(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        if (groundHeight <= 0f) {
            groundHeight = height * 0.18f;
        }
        float tileWidth = groundTileWidth;
        if (tileWidth <= 0f) {
            tileWidth = dpToPx(24f);
        }

        float top = height - groundHeight;
        float soilHeight = tileWidth * 0.5f;
        canvas.drawRect(0, top, width, height, groundShadowPaint);
        canvas.drawRect(0, top - soilHeight, width, top, groundPaint);

        for (float x = -tileWidth + groundOffset; x < width + tileWidth; x += tileWidth) {
            float grassHeight = tileWidth * 0.5f;
            float left = x;
            float right = x + tileWidth;
            float grassTop = top - grassHeight;
            canvas.drawRect(left, grassTop, right, top, groundHighlightPaint);
            canvas.drawRect(left, grassTop + grassHeight * 0.6f, right, top, groundPaint);

            float ridgeHeight = grassHeight * 0.35f;
            canvas.drawRect(left, grassTop + ridgeHeight, right, grassTop + ridgeHeight + grassHeight * 0.18f, groundShadowPaint);

            float tuftWidth = tileWidth * 0.18f;
            float tuftHeight = grassHeight * 0.3f;
            canvas.drawRect(left + tileWidth * 0.2f, grassTop - tuftHeight, left + tileWidth * 0.2f + tuftWidth, grassTop, groundHighlightPaint);
            canvas.drawRect(right - tileWidth * 0.4f, grassTop - tuftHeight * 0.6f, right - tileWidth * 0.22f, grassTop + tuftHeight * 0.1f, groundPaint);
        }
    }

    private void drawBird(Canvas canvas) {
        float scale = 1.2f;
        float pixelSize = (birdRadius * 2f * scale) / BIRD_PIXELS[0].length;
        float artWidth = BIRD_PIXELS[0].length * pixelSize;
        float artHeight = BIRD_PIXELS.length * pixelSize;
        float left = birdX - artWidth / 2f;
        float top = birdY - artHeight / 2f;

        RectF shadow = new RectF(
                birdX - artWidth * 0.45f,
                birdY + artHeight * 0.35f,
                birdX + artWidth * 0.45f,
                birdY + artHeight * 0.55f
        );
        canvas.drawOval(shadow, birdShadowPaint);

        drawPixelArt(canvas, left, top, pixelSize, BIRD_PIXELS, birdPalette);
    }

    private void drawHud(Canvas canvas) {
        String scoreText = String.valueOf(score);
        float panelWidth = Math.max(dpToPx(80f), scorePaint.measureText(scoreText) + dpToPx(24f));
        float panelHeight = scorePaint.getTextSize() * 1.5f;
        float panelTop = getHeight() * 0.08f;
        RectF scoreRect = new RectF(
                getWidth() / 2f - panelWidth / 2f,
                panelTop,
                getWidth() / 2f + panelWidth / 2f,
                panelTop + panelHeight
        );
        canvas.drawRect(scoreRect, hudPanelPaint);
        Paint.FontMetrics fm = scorePaint.getFontMetrics();
        float scoreBaseline = scoreRect.centerY() - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(scoreText, getWidth() / 2f, scoreBaseline, scorePaint);

        if (isGameOver) {
            String message = "Game Over";
            String finalScore = "Score: " + score;
            float textPadding = dpToPx(20f);
            float messageWidth = Math.max(gameOverPaint.measureText(message), scorePaint.measureText(finalScore));
            float panelHalfWidth = (messageWidth + textPadding) / 2f;
            float panelHalfHeight = gameOverPaint.getTextSize() * 1.6f;
            RectF gameOverRect = new RectF(
                    getWidth() / 2f - panelHalfWidth,
                    getHeight() / 2f - panelHalfHeight,
                    getWidth() / 2f + panelHalfWidth,
                    getHeight() / 2f + panelHalfHeight
            );
            canvas.drawRect(gameOverRect, hudPanelPaint);

            Paint.FontMetrics goMetrics = gameOverPaint.getFontMetrics();
            float gameOverBaseline = gameOverRect.centerY() - gameOverPaint.getTextSize() * 0.25f - (goMetrics.ascent + goMetrics.descent) / 2f;
            canvas.drawText(message, getWidth() / 2f, gameOverBaseline, gameOverPaint);

            Paint.FontMetrics finalMetrics = scorePaint.getFontMetrics();
            float finalBaseline = gameOverRect.centerY() + scorePaint.getTextSize() * 0.65f - (finalMetrics.ascent + finalMetrics.descent) / 2f;
            canvas.drawText(finalScore, getWidth() / 2f, finalBaseline, scorePaint);
        }
    }

    private void drawPipeSegment(Canvas canvas, RectF rect, boolean isTop) {
        if (rect.width() <= 0f || rect.height() <= 0f) {
            return;
        }

        canvas.drawRect(rect, pipePaint);

        float accentInset = rect.width() * 0.12f;
        float highlightWidth = rect.width() * 0.18f;
        float shadowWidth = rect.width() * 0.2f;
        RectF highlightRect = new RectF(
                rect.left + accentInset,
                rect.top + accentInset,
                rect.left + accentInset + highlightWidth,
                rect.bottom - accentInset
        );
        RectF shadowRect = new RectF(
                rect.right - accentInset - shadowWidth,
                rect.top + accentInset,
                rect.right - accentInset,
                rect.bottom - accentInset
        );
        if (highlightRect.bottom > highlightRect.top) {
            canvas.drawRect(highlightRect, pipeHighlightPaint);
        }
        if (shadowRect.bottom > shadowRect.top) {
            canvas.drawRect(shadowRect, pipeShadowPaint);
        }

        float stripeHeight = pipeWidth * 0.12f;
        for (float y = rect.top + stripeHeight; y < rect.bottom - stripeHeight; y += stripeHeight * 2f) {
            float stripeBottom = Math.min(rect.bottom - accentInset, y + stripeHeight * 0.4f);
            canvas.drawRect(rect.left, y, rect.right, stripeBottom, pipeShadowPaint);
        }

        float lipHeight = Math.min(pipeWidth * 0.35f, rect.height());
        RectF lipRect;
        if (isTop) {
            lipRect = new RectF(
                    rect.left - rect.width() * 0.18f,
                    rect.bottom - lipHeight,
                    rect.right + rect.width() * 0.18f,
                    rect.bottom
            );
        } else {
            lipRect = new RectF(
                    rect.left - rect.width() * 0.18f,
                    rect.top,
                    rect.right + rect.width() * 0.18f,
                    rect.top + lipHeight
            );
        }
        canvas.drawRect(lipRect, pipePaint);

        RectF lipHighlight = new RectF(
                lipRect.left + accentInset,
                lipRect.top,
                lipRect.left + accentInset + highlightWidth,
                lipRect.bottom
        );
        RectF lipShadow = new RectF(
                lipRect.right - accentInset - shadowWidth,
                lipRect.top,
                lipRect.right - accentInset,
                lipRect.bottom
        );
        if (lipHighlight.bottom > lipHighlight.top) {
            canvas.drawRect(lipHighlight, pipeHighlightPaint);
        }
        if (lipShadow.bottom > lipShadow.top) {
            canvas.drawRect(lipShadow, pipeShadowPaint);
        }
    }

    private void drawSteppedMountain(Canvas canvas, float startX, float baseY, float width, float height, Paint paint, int steps) {
        float stepHeight = height / Math.max(steps, 1);
        float center = startX + width / 2f;

        for (int i = 0; i < steps; i++) {
            float progress = (steps - i) / (float) steps;
            float halfWidth = width * progress / 2f;
            float stepBottom = baseY - i * stepHeight;
            float stepTop = stepBottom - stepHeight;
            canvas.drawRect(center - halfWidth, stepTop, center + halfWidth, stepBottom, paint);

            float highlightWidth = Math.max(width * 0.02f, stepHeight * 0.6f);
            canvas.drawRect(center - halfWidth, stepTop, center - halfWidth + highlightWidth, stepBottom, mountainHighlightPaint);
            float crestHeight = stepHeight * 0.25f;
            canvas.drawRect(center - halfWidth, stepTop, center + halfWidth, stepTop + crestHeight, mountainHighlightPaint);
        }
    }

    private void drawPixelArt(Canvas canvas, float left, float top, float pixelSize, int[][] pattern, Paint[] palette) {
        for (int row = 0; row < pattern.length; row++) {
            int[] line = pattern[row];
            for (int col = 0; col < line.length; col++) {
                int code = line[col];
                if (code <= 0 || code >= palette.length) {
                    continue;
                }
                Paint paint = palette[code];
                if (paint == null) {
                    continue;
                }
                float l = left + col * pixelSize;
                float t = top + row * pixelSize;
                canvas.drawRect(l, t, l + pixelSize, t + pixelSize, paint);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Input handling: tap to flap or restart the game when over
    // ---------------------------------------------------------------------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (isGameOver) {
                resetGame();
                startLoop();
            } else {
                birdVelocity = FLAP_VELOCITY;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    // ---------------------------------------------------------------------
    // Utility helpers
    // ---------------------------------------------------------------------
    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float spToPx(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    private float getFarthestPipeX() {
        float farthest = 0f;
        for (Pipe pipe : pipes) {
            if (pipe.x > farthest) {
                farthest = pipe.x;
            }
        }
        return farthest;
    }

    private float getFarthestCloudX() {
        float farthest = 0f;
        for (int i = 0; i < NUM_CLOUDS; i++) {
            float width = CLOUD_PIXELS[0].length * cloudBaseSize * cloudScale[i];
            float right = cloudX[i] + width;
            if (right > farthest) {
                farthest = right;
            }
        }
        return farthest;
    }

    // ---------------------------------------------------------------------
    // Representation of a pipe pair (top and bottom rectangles)
    // ---------------------------------------------------------------------
    private class Pipe {
        float x;
        float openingTop;
        boolean scored;

        void resetOpening() {
            float minOpening = pipeGap * 0.5f;
            float maxOpening = getHeight() - pipeGap - minOpening;
            if (maxOpening <= minOpening) {
                openingTop = getHeight() / 2f - pipeGap / 2f;
            } else {
                openingTop = minOpening + random.nextFloat() * (maxOpening - minOpening);
            }
        }

        RectF getTopRect() {
            return new RectF(x, 0, x + pipeWidth, openingTop);
        }

        RectF getBottomRect() {
            return new RectF(x, openingTop + pipeGap, x + pipeWidth, getHeight());
        }
    }
}
