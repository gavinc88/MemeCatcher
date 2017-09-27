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

    private Paint paint;
    private List<Point> starField = null;
    private int starAlpha = 80;
    private int starFade = 2;
    private Rect memeBounds = new Rect(0, 0, 0, 0);
    private Rect dogeBounds = new Rect(0, 0, 0, 0);
    @Px private int memeWidth;
    @Px private int memeHeight;
    @Px private int dogeWidth;
    @Px private int dogeHeight;
    private Point memeLocation;
    private Point dogePosition;
    private Matrix matrix = null;
    private Bitmap memeBitmap = null;
    private Bitmap dogeBitmap = null;
    //Collision flag and point
    private boolean collisionDetected = false;
    private Point lastCollision = new Point(-1, -1);

    private int memeRotation = 0;

    private static final int NUM_OF_STARS = 25;

    //Allow our controller to get and set the sprite positions

    // meme setter
    synchronized public void setMemePosition(int x, int y) {
        memeLocation = new Point(x, y);
    }

    // meme getter
    synchronized public int getMemeX() {
        return memeLocation.x;
    }

    synchronized public int getMemeY() {
        return memeLocation.y;
    }

    // doge setter
    synchronized public void setDogePosition(int x, int y) {
        dogePosition = new Point(x, y);
    }

    // doge getter
    synchronized public int getDogeX() {
        return dogePosition.x;
    }

    synchronized public int getDogeY() {
        return dogePosition.y;
    }

    synchronized public void resetStarField() {
        starField = null;
    }

    //expose sprite bounds to controller
    synchronized public int getSprite1Width() {
        return memeBounds.width();
    }

    synchronized public int getSprite1Height() {
        return memeBounds.height();
    }

    synchronized public int getSprite2Width() {
        return dogeBounds.width();
    }

    synchronized public int getSprite2Height() {
        return dogeBounds.height();
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
        memeWidth = getResources().getDimensionPixelSize(R.dimen.meme_width);
        memeHeight = getResources().getDimensionPixelSize(R.dimen.meme_height);
        dogeWidth = getResources().getDimensionPixelSize(R.dimen.doge_width);
        dogeHeight = getResources().getDimensionPixelSize(R.dimen.doge_height);

        //load our bitmaps and set the bounds for the controller
        memeLocation = new Point(-1, -1);
        dogePosition = new Point(-1, -1);
        //Define a matrix so we can rotate the asteroid
        matrix = new Matrix();
        paint = new Paint();

        Bitmap originalMeme = BitmapFactory.decodeResource(getResources(), R.drawable.troll);
        memeBitmap = Bitmap.createScaledBitmap(originalMeme, memeWidth, memeHeight, false);
        Bitmap originalDoge = BitmapFactory.decodeResource(getResources(), R.drawable.doge);
        dogeBitmap = Bitmap.createScaledBitmap(originalDoge, dogeWidth, dogeHeight, false);

        memeBounds = new Rect(0, 0, memeBitmap.getWidth(), memeBitmap.getHeight());
        dogeBounds = new Rect(0, 0, dogeBitmap.getWidth(), dogeBitmap.getHeight());
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
        if (memeLocation.x < 0 && dogePosition.x < 0 && memeLocation.y < 0 && dogePosition.y < 0) return false;
        Rect r1 = new Rect(memeLocation.x, memeLocation.y, memeLocation.x + memeBounds.width(), memeLocation.y + memeBounds.height());
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

        paint.setColor(Color.BLACK);
        paint.setAlpha(255);
        paint.setStrokeWidth(1);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

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

        if (memeLocation.x >= 0) {
            matrix.reset();
            matrix.postTranslate((float) (memeLocation.x), (float) (memeLocation.y));
            matrix.postRotate(memeRotation, (float) (memeLocation.x + memeBounds.width() / 2.0), (float) (memeLocation.y + memeBounds.width() / 2.0));
            canvas.drawBitmap(memeBitmap, matrix, null);
            memeRotation += 5;
            if (memeRotation >= 360) memeRotation = 0;
        }
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
