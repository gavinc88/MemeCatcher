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
    private static final int SCREEN_IN_METERS = 5;

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

    //Velocity includes the speed and the direction of our sprite motion
    private Point memeVelocity;
    private Point dogeVelocity;
    private int memeMaxX;
    private int memeMaxY;
    private int dogeMaxY;

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
        int min = 1;
        int max = 5;
        int x = r.nextInt(max - min + 1) + min;
        int y = r.nextInt(max - min + 1) + min;
        return new Point(x, y);
    }

    private Point getRandomPointForMeme() {
        Random r = new Random();
        int minX = 0;
        int maxX = gameBoard.getWidth() - gameBoard.getMemeWidth();
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

        // pick a starting position for meme that doesn't overlap doge
        Point memePosition;
        do {
            memePosition = getRandomPointForMeme();
        } while (Math.abs(memePosition.x - dogePosition.x) < gameBoard.getMemeWidth());

        gameBoard.setDogePosition(dogePosition.x, dogePosition.y);
        gameBoard.setMemePosition(memePosition.x, memePosition.y);

        //Give the meme a random velocity
        memeVelocity = getRandomVelocity();

        //Set our boundaries for the sprites
        memeMaxX = gameBoard.getWidth() - gameBoard.getMemeWidth();
        memeMaxY = gameBoard.getHeight() - gameBoard.getMemeHeight() - gameBoard.getDogeHeight() - button.getHeight() - margin;
        dogeMaxY = button.getTop() - getResources().getDimensionPixelSize(R.dimen.margin) - gameBoard.getDogeHeight();

        button.setEnabled(true);
        frame.removeCallbacks(frameUpdate);
        gameBoard.invalidate();
        frame.postDelayed(frameUpdate, getFrameRate());

        shouldResetGame = false;
        dummyText.setText("Press jump!");
        button.setText("Jump");
        score = 0;
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

            Point memeNewPosition = new Point(gameBoard.getMemeX(), gameBoard.getMemeY());
            Point dogeNewPosition = new Point(gameBoard.getDogeX(), gameBoard.getDogeY());
            memeNewPosition.x = memeNewPosition.x + memeVelocity.x;
            if (memeNewPosition.x > memeMaxX || memeNewPosition.x < 5) {
                memeVelocity.x *= -1;
            }
            memeNewPosition.y = memeNewPosition.y + memeVelocity.y;
            if (memeNewPosition.y > memeMaxY || memeNewPosition.y < 5) {
                memeVelocity.y *= -1;
            }

            updateDogeVelocity();
            dogeNewPosition.y = dogeNewPosition.y + dogeVelocity.y;
            if (dogeNewPosition.y > dogeMaxY) {
                // return doge to original position if it overshoots
                dogeNewPosition.y = dogeMaxY;
            }

            gameBoard.setMemePosition(memeNewPosition.x, memeNewPosition.y);
            gameBoard.setDogePosition(dogeNewPosition.x, dogeNewPosition.y);
            gameBoard.invalidate();
            frame.postDelayed(frameUpdate, getFrameRate());
        }
    };

    private void updateDogeVelocity() {
        if (dogeVelocity == null) {
            dogeVelocity = new Point(0, 0);
        }
        if (!performJump) {
            dogeVelocity.y = 0;
            frameCount = 0;
        } else {
            float currentTimeInSec = frameCount * getFrameTime();
//            Log.i(TAG, "FRAME " + frameCount);
//            Log.i(TAG, "updateDogeVelocity: currentTime = " + currentTimeInSec + "sec");
//            Log.i(TAG, "updateDogeVelocity: frameTime = " + getFrameTime() + "sec");
//
//            Log.i(TAG, "updateDogeVelocity: reachedJumpPeak = " + reachedJumpPeak);
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
                dogeVelocity.y = (int) -(dogeDYInMeters * pixelToMeterRatio);

                // LOGS
//                Log.i(TAG, "updateDogeVelocity: initialVelocityInMeterPerSec = " + initialVelocityInMeterPerSec + "m/sec");
//                Log.i(TAG, "updateDogeVelocity: currentVelocityInMeterPerSec = " + currentVelocityInMeterPerSec + "m/sec");
//                Log.i(TAG, "updateDogeVelocity: dogeDYInMeters = " + dogeDYInMeters + "m");
//                Log.i(TAG, "updateDogeVelocity: dogeVelocityY = " + dogeVelocity.y + "px");

                frameCount++;

                if (currentVelocityInMeterPerSec < 0) {
                    Log.i(TAG, "performJump: doge reached the peak");
                    reachedJumpPeak = true;
                    frameCount = 0;
                }

            } else {
                // doge coming back down
                double initialVelocityInMeterPerSec = 0;
                float acceleration = GRAVITY;

                // current v = initial v + a * t; initial v = 0
                double currentVelocityInMeterPerSec = acceleration * currentTimeInSec;

                // rate = d / t ; d = rate * time
                double dogeDYInMeters = currentVelocityInMeterPerSec * getFrameTime();

                // convert meter back to pixel to get change in y
                dogeVelocity.y = (int) (dogeDYInMeters * pixelToMeterRatio);

//                Log.i(TAG, "updateDogeVelocity: initialVelocityInMeterPerSec = " + initialVelocityInMeterPerSec + "m/sec");
//                Log.i(TAG, "updateDogeVelocity: currentVelocityInMeterPerSec = " + currentVelocityInMeterPerSec + "m/sec");
//                Log.i(TAG, "updateDogeVelocity: dogeDYInMeters = " + dogeDYInMeters + "m");
//                Log.i(TAG, "updateDogeVelocity: dogeVelocityY = " + dogeVelocity.y + "px");

                frameCount++;

                double initialVelocityInMeters = Math.sqrt(2 * GRAVITY * jumpRange / pixelToMeterRatio);
                // time to reach the top = v / g
                double totalTimeInSec = initialVelocityInMeters / GRAVITY;

                if (currentTimeInSec > totalTimeInSec) {
                    Log.i(TAG, "performJump: doge returned to original position");
                    // doge returned to original position
                    dogeVelocity.y = 0;
                    performJump = false;
                    button.setEnabled(true);
                }
            }

//            int topSpace = gameBoard.getDogeY();
//            int currentHeight = dogeMaxY - topSpace;
//            Log.i(TAG, "updateDogeVelocity: currentHeight = " + currentHeight + "px");
        }
    }

}
