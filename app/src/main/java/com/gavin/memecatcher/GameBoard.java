package com.gavin.memecatcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.Px;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameBoard extends View {

    private static final int NUM_OF_STARS = 25;

    private Paint paint;
    private List<Point> starField = null;
    private int starAlpha = 80;
    private int starFade = 2;

    // Player Character
    private Rect dogeBounds = new Rect(0, 0, 0, 0);
    private Point dogePosition;
    private Bitmap dogeBitmap = null;

    // Target Character
    private Rect memeBounds = new Rect(0, 0, 0, 0);
    private Point memePosition;
    private Bitmap memeBitmap = null;

    // Obstacle Characters
    private Rect obstacle1Bounds = new Rect(0, 0, 0, 0);
    private Point obstacle1Position;
    private Bitmap obstacle1Bitmap = null;

    private Rect obstacle2Bounds = new Rect(0, 0, 0, 0);
    private Point obstacle2Position;
    private Bitmap obstacle2Bitmap = null;

    // Used for Rotations
    private Matrix matrix = null;
    private int memeRotation = 0;
    private int obstacle1Rotation = 0;
    private int obstacle2Rotation = 180;

    //Collision flag and point
    private boolean collisionDetected = false;
    private Point lastCollision = new Point(-1, -1);

    //Allow our controller to get and set the sprite positions

    // region Player Character Getters & Setters

    synchronized public void setPlayerPosition(int x, int y) {
        dogePosition = new Point(x, y);
    }

    synchronized public int getPlayerX() {
        return dogePosition.x;
    }

    synchronized public int getPlayerY() {
        return dogePosition.y;
    }

    synchronized public int getDogeWidth() {
        return dogeBounds.width();
    }

    synchronized public int getDogeHeight() {
        return dogeBounds.height();
    }

    // endregion

    // region Target Character Getters & Setters

    synchronized public void setTargetPosition(int x, int y) {
        memePosition = new Point(x, y);
    }

    synchronized public int getTargetX() {
        return memePosition.x;
    }

    synchronized public int getTargetY() {
        return memePosition.y;
    }

    synchronized public int getTargetWidth() {
        return memeBounds.width();
    }

    synchronized public int getTargetHeight() {
        return memeBounds.height();
    }

    // endregion

    // region Obstacle 1 Character Getters & Setters

    synchronized public void setObstacle1Position(int x, int y) {
        obstacle1Position = new Point(x, y);
    }

    synchronized public int getObstacle1X() {
        return obstacle1Position.x;
    }

    synchronized public int getObstacle1Y() {
        return obstacle1Position.y;
    }

    synchronized public int getObstacle1Width() {
        return obstacle1Bounds.width();
    }

    synchronized public int getObstacle1Height() {
        return obstacle1Bounds.height();
    }

    // endregion

    // region Obstacle 2 Character Getters & Setters

    synchronized public void setObstacle2Position(int x, int y) {
        obstacle2Position = new Point(x, y);
    }

    synchronized public int getObstacle2X() {
        return obstacle2Position.x;
    }

    synchronized public int getObstacle2Y() {
        return obstacle2Position.y;
    }

    synchronized public int getObstacle2Width() {
        return obstacle2Bounds.width();
    }

    synchronized public int getObstacle2Height() {
        return obstacle2Bounds.height();
    }

    // endregion

    synchronized public void resetStarField() {
        starField = null;
    }

    //return the point of the last collision
    synchronized public Point getLastCollision() {
        return lastCollision;
    }

    //return the collision flag
    synchronized public boolean wasCollisionDetected() {
        return collisionDetected;
    }

    public GameBoard(Context context, AttributeSet aSet) {
        super(context, aSet);
        paint = new Paint();

        // define initial pixel size for icons
        @Px int dogeWidth = getResources().getDimensionPixelSize(R.dimen.doge_width);
        @Px int dogeHeight = getResources().getDimensionPixelSize(R.dimen.doge_height);
        @Px int memeWidth = getResources().getDimensionPixelSize(R.dimen.meme_width);
        @Px int memeHeight = getResources().getDimensionPixelSize(R.dimen.meme_height);

        //Define a matrix so we can rotate the asteroid
        matrix = new Matrix();

        // 1) initialize position
        // 2) load bitmap
        // 3) scale bitmap
        // 4) set the bounds for the controller
        dogePosition = new Point(-1, -1);
        Bitmap originalDoge = BitmapFactory.decodeResource(getResources(), R.drawable.doge);
        dogeBitmap = Bitmap.createScaledBitmap(originalDoge, dogeWidth, dogeHeight, false);
        dogeBounds = new Rect(0, 0, dogeBitmap.getWidth(), dogeBitmap.getHeight());

        memePosition = new Point(-1, -1);
        Bitmap originalMeme = BitmapFactory.decodeResource(getResources(), R.drawable.heavy_breathing_cat);
        memeBitmap = Bitmap.createScaledBitmap(originalMeme, memeWidth, memeHeight, false);
        memeBounds = new Rect(0, 0, memeBitmap.getWidth(), memeBitmap.getHeight());

        obstacle1Position = new Point(-1, -1);
        Bitmap originalObstacle1 = BitmapFactory.decodeResource(getResources(), R.drawable.troll);
        obstacle1Bitmap = Bitmap.createScaledBitmap(originalObstacle1, memeWidth, memeHeight, false);
        obstacle1Bounds = new Rect(0, 0, memeBitmap.getWidth(), memeBitmap.getHeight());

        obstacle2Position = new Point(-1, -1);
        Bitmap originalObstacle2 = BitmapFactory.decodeResource(getResources(), R.drawable.trollololo);
        obstacle2Bitmap = Bitmap.createScaledBitmap(originalObstacle2, memeWidth, memeHeight, false);
        obstacle2Bounds = new Rect(0, 0, memeBitmap.getWidth(), memeBitmap.getHeight());
    }

    synchronized private void initializeStars(int maxX, int maxY) {
        starField = new ArrayList<>();
        for (int i = 0; i < NUM_OF_STARS; i++) {
            Random r = new Random();
            int x = r.nextInt(maxX - 5 + 1) + 5;
            int y = r.nextInt(maxY - 5 + 1) + 5;
            starField.add(new Point(x, y));
        }
        collisionDetected = false;
    }

    private boolean checkForCollision() {
        if (memePosition.x < 0 && dogePosition.x < 0 && memePosition.y < 0 && dogePosition.y < 0) return false;
        Rect r1 = new Rect(memePosition.x, memePosition.y, memePosition.x + memeBounds.width(), memePosition.y + memeBounds.height());
        Rect r2 = new Rect(dogePosition.x, dogePosition.y, dogePosition.x + dogeBounds.width(), dogePosition.y + dogeBounds.height());
        Rect r3 = new Rect(r1);
        if (r1.intersect(r2)) {
            for (int i = r1.left; i < r1.right; i++) {
                for (int j = r1.top; j < r1.bottom; j++) {
                    if (memeBitmap.getPixel(i - r3.left, j - r3.top) != Color.TRANSPARENT) {
                        if (dogeBitmap.getPixel(i - r2.left, j - r2.top) != Color.TRANSPARENT) {
                            lastCollision = new Point(dogePosition.x + i - r2.left, dogePosition.y + j - r2.top);
                            return true;
                        }
                    }
                }
            }
        }
        lastCollision = new Point(-1, -1);
        return false;
    }

    @Override
    synchronized public void onDraw(Canvas canvas) {
        // Paint the background
        paint.setColor(Color.BLACK);
        paint.setAlpha(255);
        paint.setStrokeWidth(1);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        // Paint the stars
        if (starField == null) {
            initializeStars(canvas.getWidth(), canvas.getHeight());
        }
        paint.setColor(Color.CYAN);
        paint.setAlpha(starAlpha += starFade);
        if (starAlpha >= 252 || starAlpha <= 80) starFade = starFade * -1;
        paint.setStrokeWidth(5);
        for (int i = 0; i < NUM_OF_STARS; i++) {
            canvas.drawPoint(starField.get(i).x, starField.get(i).y, paint);
        }

        // Paint the obstacles
        if (obstacle1Position.x >= 0) {
            matrix.reset();
            matrix.postTranslate((float) (obstacle1Position.x), (float) (obstacle1Position.y));
            matrix.postRotate(obstacle1Rotation, (float) (obstacle1Position.x + obstacle1Bounds.width() / 2.0), (float) (obstacle1Position.y + obstacle1Bounds.width() / 2.0));
            canvas.drawBitmap(obstacle1Bitmap, matrix, null);
            obstacle1Rotation += 5;
            if (obstacle1Rotation >= 360) obstacle1Rotation = 0;
        }
        if (obstacle2Position.x >= 0) {
            matrix.reset();
            matrix.postTranslate((float) (obstacle2Position.x), (float) (obstacle2Position.y));
            matrix.postRotate(obstacle2Rotation, (float) (obstacle2Position.x + obstacle2Bounds.width() / 2.0), (float) (obstacle2Position.y + obstacle2Bounds.width() / 2.0));
            canvas.drawBitmap(obstacle2Bitmap, matrix, null);
            obstacle2Rotation += 5;
            if (obstacle2Rotation >= 360) obstacle2Rotation = 0;
        }

        // Paint the target character
        if (memePosition.x >= 0) {
            matrix.reset();
            matrix.postTranslate((float) (memePosition.x), (float) (memePosition.y));
            matrix.postRotate(memeRotation, (float) (memePosition.x + memeBounds.width() / 2.0), (float) (memePosition.y + memeBounds.width() / 2.0));
            canvas.drawBitmap(memeBitmap, matrix, null);
            memeRotation += 1;
            if (memeRotation >= 360) memeRotation = 0;
        }

        // Paint the player character
        if (dogePosition.x >= 0) {
            canvas.drawBitmap(dogeBitmap, dogePosition.x, dogePosition.y, null);
        }

        //The last order of business is to check for a collision
        collisionDetected = checkForCollision();
        if (collisionDetected) {
            //if there is one lets draw a red X
            paint.setColor(Color.RED);
            paint.setAlpha(255);
            paint.setStrokeWidth(5);
            canvas.drawLine(lastCollision.x - 5, lastCollision.y - 5, lastCollision.x + 5, lastCollision.y + 5, paint);
            canvas.drawLine(lastCollision.x + 5, lastCollision.y - 5, lastCollision.x - 5, lastCollision.y + 5, paint);
        }
    }
}
