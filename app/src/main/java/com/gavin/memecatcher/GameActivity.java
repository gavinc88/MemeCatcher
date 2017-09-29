package com.gavin.memecatcher;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Px;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class GameActivity extends AppCompatActivity implements View.OnTouchListener {

    private static final String TAG = "GameActivity";

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    private static final long MAX_JUMP_PRESS_TIME = 1000;
    private static final float GRAVITY = 9.8f;
    private static final int SCREEN_IN_METERS = 2; //increase to slow down jump; decrease to speed up jump

    private final Handler handler = new Handler();

    /**
     * Views
     */
    private GameBoard gameBoard;
    private Button button;
    private TextView dummyText;


    private Handler frame = new Handler();

    private int frameCount;
    private int score;
    private int jumpPower;
    @Px private int jumpRange;
    @Px private int totalJumpRange;
    @Px private int margin;
    private int pixelToMeterRatio;
    private long jumpStartTime;
    private boolean performJump;
    private boolean shouldResetGame;
    private boolean reachedJumpPeak;
    private boolean hasPressedDown;

    // Velocity includes the speed and the direction of our sprite motion
    private Point playerVelocity;
    private Point targetVelocity;
    private Point obstacle1Velocity;
    private Point obstacle2Velocity;
    private int playerMaxY; // player's starting position
    private int memeMaxX;
    private int memeMaxY;

    //Divide the frame by 1000 to calculate how many times per second the screen will update.
    private static final int FRAMES_PER_SEC = 50;
    private static final int FRAME_RATE = 20; //50 frames per second
    // 4 frames per sec = 1000 / 4 = 250
    private static final int SLOW_FRAMES_PER_SEC = 10; //4 frames per second
    private static final int SLOW_FRAME_RATE = 1000 / SLOW_FRAMES_PER_SEC; // 250 frame rate

    private boolean useSlowFrameRate = false;

    private int getFramesPerSec() {
        return useSlowFrameRate ? SLOW_FRAMES_PER_SEC : FRAMES_PER_SEC;
    }

    private int getFrameRate() {
        return useSlowFrameRate ? SLOW_FRAME_RATE : FRAME_RATE;
    }

    private float getFrameTime() {
        return getFrameRate() / 1000f;
    }

    private final Runnable hideSystemUIRunnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            gameBoard.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

            calculateConstants();
        }
    };

    private void calculateConstants() {
        margin = getResources().getDimensionPixelSize(R.dimen.margin);
        totalJumpRange = gameBoard.getHeight() - button.getHeight() - gameBoard.getDogeHeight() - margin;
        pixelToMeterRatio = totalJumpRange / SCREEN_IN_METERS; // assume 5 meter for screen height
        Log.i(TAG, "calculateConstants: gameBoard.getHeight(): " + gameBoard.getHeight());
        Log.i(TAG, "calculateConstants: button.getHeight(): " + button.getHeight());
        Log.i(TAG, "calculateConstants: gameBoard.getDogeHeight(): " + gameBoard.getDogeHeight());
        Log.i(TAG, "calculateConstants: margin: " + getResources().getDimensionPixelSize(R.dimen.margin));
        Log.i(TAG, "calculateConstants: totalJumpRange: " + totalJumpRange);
        Log.i(TAG, "calculateConstants: pixelToMeterRatio: " + pixelToMeterRatio);

        int totalJumpRangeInMeters = totalJumpRange / pixelToMeterRatio;
        Log.i(TAG, "calculateConstants: height = " + totalJumpRangeInMeters + "m");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);
        gameBoard = (GameBoard) findViewById(R.id.fullscreen_content);
        dummyText = (TextView) findViewById(R.id.dummy_text);
        button = (Button) findViewById(R.id.button);

        shouldResetGame = true;
        button.setText("Start");
        button.setEnabled(true);
        button.setOnTouchListener(this);
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                hasPressedDown = true;
                jumpStartTime = System.currentTimeMillis();;
                Log.i(TAG, "onTouch: ACTION_DOWN");
                Log.i(TAG, "jumpStartTime: " + jumpStartTime);
                break;
            }
            case MotionEvent.ACTION_UP: {
                Log.i(TAG, "onTouch: ACTION_UP");
                if (shouldResetGame) {
                    resetGame();
                    break;
                }
                if (!hasPressedDown) {
                    break;
                }
                hasPressedDown = false;
                long jumpReleaseTime = System.currentTimeMillis();;
                long jumpTimeDiff = jumpReleaseTime - jumpStartTime;
                Log.i(TAG, "jumpReleaseTime: " + jumpReleaseTime);
                Log.i(TAG, "jumpTimeDiff: " + jumpTimeDiff);
                if (jumpTimeDiff > MAX_JUMP_PRESS_TIME) {
                    // pressed for too long
                    dummyText.setText("JUMPING POWER OVER 9000!!!");
                    button.setText("Reset");
                    performJump = true;
                    reachedJumpPeak = false;
                    shouldResetGame = true;
                    frameCount = 0;
                    jumpRange = totalJumpRange * 2;
                } else {
                    float powerRatio = (float) jumpTimeDiff / MAX_JUMP_PRESS_TIME;
                    jumpRange = (int) (powerRatio * totalJumpRange);
                    Log.i(TAG, "onTouch: jumpRange = " + jumpRange);
                    jumpPower = (int) (powerRatio * 1000);
                    dummyText.setText("Jump power: " + jumpPower);
                    frameCount = 0;
                    performJump = true;
                    reachedJumpPeak = false;
                    button.setEnabled(false);
                }
                Log.i(TAG, "onTouch: performJump = " + performJump);
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        enterFullscreen();
    }

    @Override
    protected void onPause() {
        super.onPause();

        frame.removeCallbacks(frameUpdate);
    }

    private void enterFullscreen() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        handler.postDelayed(hideSystemUIRunnable, UI_ANIMATION_DELAY);
    }

    private Point getRandomVelocity() {
        Random r = new Random();
        int min = getResources().getDimensionPixelSize(R.dimen.min_velocity);
        int max = getResources().getDimensionPixelSize(R.dimen.max_velocity);
        int x = r.nextInt(max - min + 1) + min;
        int y = r.nextInt(max - min + 1) + min;
        return new Point(x, y);
    }

    private Point getRandomPoint() {
        Random r = new Random();
        int minX = 0;
        int maxX = gameBoard.getWidth() - gameBoard.getTargetWidth();
        int minY = 0;
        int maxY = button.getTop() - gameBoard.getDogeHeight() - getResources().getDimensionPixelSize(R.dimen.margin) * 2;
        int x = r.nextInt(maxX - minX + 1) + minX;
        int y = r.nextInt(maxY - minY + 1) + minY;
        return new Point(x, y);
    }

    private Point getDogeStartingPoint() {
        int x = (gameBoard.getWidth() / 2) - (gameBoard.getDogeWidth() / 2); // center
        int y = button.getTop() - getResources().getDimensionPixelSize(R.dimen.margin)
                - gameBoard.getDogeHeight();
        return new Point(x, y);
    }

    synchronized public void resetGame() {
        Log.i(TAG, "resetGame: ");
        gameBoard.resetStarField();
        Point dogePosition = getDogeStartingPoint();

        // Pick starting positions for target and obstacle memes that don't overlap doge
        Point memePosition;
        do {
            memePosition = getRandomPoint();
        } while (Math.abs(memePosition.x - dogePosition.x) < gameBoard.getDogeWidth()
                && Math.abs(memePosition.y - dogePosition.y) < gameBoard.getDogeHeight());

        Point obstacle1Position;
        Point obstacle2Position;
        do {
            obstacle1Position = getRandomPoint();
            obstacle2Position = getRandomPoint();
        } while (Math.abs(obstacle1Position.y - dogePosition.y) < gameBoard.getDogeHeight()
                && Math.abs(obstacle2Position.y - dogePosition.y) < gameBoard.getDogeHeight());

        gameBoard.setPlayerPosition(dogePosition.x, dogePosition.y);
        gameBoard.setTargetPosition(memePosition.x, memePosition.y);
        gameBoard.setObstacle1Position(obstacle1Position.x, obstacle1Position.y);
        gameBoard.setObstacle2Position(obstacle2Position.x, obstacle2Position.y);

        // Give the target meme a random velocity
        targetVelocity = getRandomVelocity();
        obstacle1Velocity = new Point(10, 20);
        obstacle2Velocity = new Point(15, -10);

        // Set our boundaries for the sprites
        playerMaxY = button.getTop() - getResources().getDimensionPixelSize(R.dimen.margin) - gameBoard.getDogeHeight();
        memeMaxX = gameBoard.getWidth() - gameBoard.getTargetWidth();
        memeMaxY = gameBoard.getHeight() - gameBoard.getTargetHeight();

        // Reset game conditions
        shouldResetGame = false;
        button.setEnabled(true);
        button.setText("Jump");
        dummyText.setText("Press jump!");
        score = 0;

        // Start game
        frame.removeCallbacks(frameUpdate);
        gameBoard.invalidate();
        frame.postDelayed(frameUpdate, getFrameRate());
    }

    private Runnable frameUpdate = new Runnable() {

        @Override
        synchronized public void run() {
            //Before we do anything else check for a collision
            if (gameBoard.wasCollisionDetected()) {
                Point collisionPoint = gameBoard.getLastCollision();
                if (collisionPoint.x >= 0) {
                    dummyText.setText("Last Collision XY (" + Integer.toString(collisionPoint.x) + "," + Integer.toString(collisionPoint.y) + ")");
                    score++;
                }
                //turn off the animation until reset gets pressed
                //return;
            }
            frame.removeCallbacks(frameUpdate);

            Point playerNewPosition = new Point(gameBoard.getPlayerX(), gameBoard.getPlayerY());
            Point targetNewPosition = new Point(gameBoard.getTargetX(), gameBoard.getTargetY());
            Point obstacle1NewPosition = new Point(gameBoard.getObstacle1X(), gameBoard.getObstacle1Y());
            Point obstacle2NewPosition = new Point(gameBoard.getObstacle2X(), gameBoard.getObstacle2Y());

            updatePlayerVelocity();
            playerNewPosition.y = playerNewPosition.y + playerVelocity.y;
            if (playerNewPosition.y > playerMaxY) {
                // return doge to original position if it overshoots
                playerNewPosition.y = playerMaxY;
                performJump = false;
                button.setEnabled(true);
            }

            targetNewPosition.x = targetNewPosition.x + targetVelocity.x;
            if (targetNewPosition.x > memeMaxX || targetNewPosition.x < 5) {
                targetVelocity.x *= -1;
            }
            targetNewPosition.y = targetNewPosition.y + targetVelocity.y;
            if (targetNewPosition.y > memeMaxY || targetNewPosition.y < 5) {
                targetVelocity.y *= -1;
            }

            obstacle1NewPosition.x = obstacle1NewPosition.x + obstacle1Velocity.x;
            if (obstacle1NewPosition.x > memeMaxX || obstacle1NewPosition.x < 5) {
                obstacle1Velocity.x *= -1;
            }
            obstacle1NewPosition.y = obstacle1NewPosition.y + obstacle1Velocity.y;
            if (obstacle1NewPosition.y > memeMaxY || obstacle1NewPosition.y < 5) {
                obstacle1Velocity.y *= -1;
            }

            obstacle2NewPosition.x = obstacle2NewPosition.x + obstacle2Velocity.x;
            if (obstacle2NewPosition.x > memeMaxX || obstacle2NewPosition.x < 5) {
                obstacle2Velocity.x *= -1;
            }
            obstacle2NewPosition.y = obstacle2NewPosition.y + obstacle2Velocity.y;
            if (obstacle2NewPosition.y > memeMaxY || obstacle2NewPosition.y < 5) {
                obstacle2Velocity.y *= -1;
            }

            gameBoard.setPlayerPosition(playerNewPosition.x, playerNewPosition.y);
            gameBoard.setTargetPosition(targetNewPosition.x, targetNewPosition.y);
            gameBoard.setObstacle1Position(obstacle1NewPosition.x, obstacle1NewPosition.y);
            gameBoard.setObstacle2Position(obstacle2NewPosition.x, obstacle2NewPosition.y);
            gameBoard.invalidate();
            frame.postDelayed(frameUpdate, getFrameRate());
        }
    };

    private void updatePlayerVelocity() {
        if (playerVelocity == null) {
            playerVelocity = new Point(0, 0);
        }
        if (!performJump) {
            playerVelocity.y = 0;
            frameCount = 0;
        } else {
            float currentTimeInSec = frameCount * getFrameTime();
            if (!reachedJumpPeak) {
                // doge going up

                // initial v = sqrt(2 * g * h)
                double initialVelocityInMeterPerSec = Math.sqrt(2 * GRAVITY * jumpRange / pixelToMeterRatio);
                double acceleration = -GRAVITY;

                // current v = initial v + a * t
                double currentVelocityInMeterPerSec = initialVelocityInMeterPerSec + acceleration * currentTimeInSec;

                // rate = d / t ; d = rate * time
                double dogeDYInMeters = currentVelocityInMeterPerSec * getFrameTime();

                // convert meter back to pixel to get change in y
                playerVelocity.y = (int) -(dogeDYInMeters * pixelToMeterRatio);

                frameCount++;

                if (currentVelocityInMeterPerSec < 0) {
                    Log.i(TAG, "performJump: doge reached the peak");
                    reachedJumpPeak = true;
                    frameCount = 0;
                }

            } else {
                // doge coming back down

                // current v = initial v + a * t; initial v = 0
                double currentVelocityInMeterPerSec = GRAVITY * currentTimeInSec;

                // rate = d / t ; d = rate * time
                double dogeDYInMeters = currentVelocityInMeterPerSec * getFrameTime();

                // convert meter back to pixel to get change in y
                playerVelocity.y = (int) (dogeDYInMeters * pixelToMeterRatio);

                frameCount++;
            }
        }
    }

}
