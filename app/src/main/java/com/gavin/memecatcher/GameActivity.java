package com.gavin.memecatcher;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Random;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class GameActivity extends AppCompatActivity {

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler handler = new Handler();

    /**
     * Views
     */
    private GameBoard gameBoard;
    private Button jumpButton;
    private TextView dummyText;

    private Handler frame = new Handler();

    private int score;

    //Velocity includes the speed and the direction of our sprite motion
    private Point sprite1Velocity;
    private Point sprite2Velocity;
    private int sprite1MaxX;
    private int sprite1MaxY;
    private int sprite2MaxX;
    private int sprite2MaxY;
    //Divide the frame by 1000 to calculate how many times per second the screen will update.
    private static final int FRAME_RATE = 20; //50 frames per second

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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);
        gameBoard = (GameBoard) findViewById(R.id.fullscreen_content);
        dummyText = (TextView) findViewById(R.id.dummy_text);
        jumpButton = (Button) findViewById(R.id.jump_button);
        jumpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dummyText.setText("jumped " + score + " times");
                score++;
            }
        });


        //We can't initialize the graphics immediately because the layout manager
        //needs to run first, thus call back in a sec.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initGfx();
            }
        }, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();

        enterFullscreen();
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

    private Point getRandomPoint() {
        Random r = new Random();
        int minX = 0;
        int maxX = gameBoard.getWidth() - gameBoard.getSprite1Width();
        int x = 0;
        int minY = 0;
        int maxY = gameBoard.getHeight() - gameBoard.getSprite1Height();
        int y = 0;
        x = r.nextInt(maxX - minX + 1) + minX;
        y = r.nextInt(maxY - minY + 1) + minY;
        return new Point(x, y);
    }

    synchronized public void initGfx() {
        gameBoard.resetStarField();
        Point p1, p2;
        do {
            p1 = getRandomPoint();
            p2 = getRandomPoint();
        } while (Math.abs(p1.x - p2.x) < gameBoard.getSprite1Width());
        gameBoard.setMemePosition(p1.x, p1.y);
        gameBoard.setDogePosition(p2.x, p2.y);
        //Give the asteroid a random velocity
        sprite1Velocity = getRandomVelocity();
        //Fix the ship velocity at a constant speed for now
        sprite2Velocity = new Point(1, 1);
        //Set our boundaries for the sprites
        sprite1MaxX = gameBoard.getWidth() - gameBoard.getSprite1Width();
        sprite1MaxY = gameBoard.getHeight() - gameBoard.getSprite1Height();
        sprite2MaxX = gameBoard.getWidth() - gameBoard.getSprite2Width();
        sprite2MaxY = gameBoard.getHeight() - gameBoard.getSprite2Height();
        jumpButton.setEnabled(true);
        frame.removeCallbacks(frameUpdate);
        gameBoard.invalidate();
        frame.postDelayed(frameUpdate, FRAME_RATE);
    }

    private Runnable frameUpdate = new Runnable() {

        @Override
        synchronized public void run() {
            //Before we do anything else check for a collision
            if (gameBoard.wasCollisionDetected()) {
                Point collisionPoint = gameBoard.getLastCollision();
                if (collisionPoint.x >= 0) {
                    dummyText.setText("Last Collision XY (" + Integer.toString(collisionPoint.x) + "," + Integer.toString(collisionPoint.y) + ")");
                }
                //turn off the animation until reset gets pressed
                return;
            }
            frame.removeCallbacks(frameUpdate);

            Point sprite1 = new Point(gameBoard.getMemeX(),
                    gameBoard.getMemeY());
            Point sprite2 = new Point(gameBoard.getDogeX(),
                    gameBoard.getDogeY());
            sprite1.x = sprite1.x + sprite1Velocity.x;
            if (sprite1.x > sprite1MaxX || sprite1.x < 5) {
                sprite1Velocity.x *= -1;
            }
            sprite1.y = sprite1.y + sprite1Velocity.y;
            if (sprite1.y > sprite1MaxY || sprite1.y < 5) {
                sprite1Velocity.y *= -1;
            }
            sprite2.x = sprite2.x + sprite2Velocity.x;
            if (sprite2.x > sprite2MaxX || sprite2.x < 5) {
                sprite2Velocity.x *= -1;
            }
            sprite2.y = sprite2.y + sprite2Velocity.y;
            if (sprite2.y > sprite2MaxY || sprite2.y < 5) {
                sprite2Velocity.y *= -1;
            }
            gameBoard.setMemePosition(sprite1.x, sprite1.y);
            gameBoard.setDogePosition(sprite2.x, sprite2.y);
            gameBoard.invalidate();
            frame.postDelayed(frameUpdate, FRAME_RATE);
        }
    };

}
