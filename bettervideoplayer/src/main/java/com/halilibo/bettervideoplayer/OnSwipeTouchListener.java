package com.halilibo.bettervideoplayer;

import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * @author: Halil Ozercan
 */
abstract class OnSwipeTouchListener implements OnTouchListener {

    private final boolean doubleTapEnabled;
    private final Handler mHandler;
    private Runnable futureClickRunnable = new Runnable() {
        @Override
        public void run() {
            onClick();
        }
    };

    enum Direction{
        LEFT, RIGHT, UP, DOWN;
    }
    public OnSwipeTouchListener(boolean doubleTapEnabled){
        this.doubleTapEnabled = doubleTapEnabled;
        this.mHandler = new Handler();
    }

    private final static String TAG = "ClickFrame";
    private final static int SWIPE_THRESHOLD = 100;
    private static final long DOUBLE_TAP_THRESHOLD = 150;

    // 0: uninitialized
    // 1: horizontal
    // 2: vertical
    private int initialGesture;

    protected float initialX;
    protected float initialY;
    private float decidedX;
    private float decidedY;
    private long lastClick = 0;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {

            case MotionEvent.ACTION_DOWN:
                initialX = event.getX();
                initialY = event.getY();
                initialGesture = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaX;
                float deltaY;
                if(initialGesture == 0) {
                    deltaX = event.getX() - initialX;
                    deltaY = event.getY() - initialY;
                }
                else{
                    deltaX = event.getX() - decidedX;
                    deltaY = event.getY() - decidedY;
                }

                if(initialGesture == 0 && Math.abs(deltaX) > SWIPE_THRESHOLD){
                    initialGesture = 1;
                    decidedX = event.getX();
                    decidedY = event.getY();
                    if(deltaX > 0){
                        onBeforeMove(Direction.RIGHT);
                    }
                    else{
                        onBeforeMove(Direction.LEFT);
                    }
                }
                else if(initialGesture == 0 && Math.abs(deltaY) > SWIPE_THRESHOLD){
                    initialGesture = 2;
                    decidedX = event.getX();
                    decidedY = event.getY();
                    if(deltaY > 0){
                        onBeforeMove(Direction.DOWN);
                    }
                    else{
                        onBeforeMove(Direction.UP);
                    }
                }

                if(initialGesture == 1){
                    if(deltaX > 0){
                        onMove(Direction.RIGHT, deltaX);
                    }
                    else{
                        onMove(Direction.LEFT, -deltaX);
                    }
                }
                else if(initialGesture == 2){
                    if(deltaY > 0){
                        onMove(Direction.DOWN, deltaY);
                    }
                    else{
                        onMove(Direction.UP, -deltaY);
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
                if(initialGesture == 0){ // Finger did not move enough to trigger a swipe
                    if(doubleTapEnabled &&
                        System.currentTimeMillis() - lastClick <= DOUBLE_TAP_THRESHOLD &&
                        lastClick != 0) {
                        mHandler.removeCallbacks(futureClickRunnable);
                        onDoubleTap(event);
                        return true;
                    }
                    else {
                        lastClick = System.currentTimeMillis();
                        if(doubleTapEnabled)
                            mHandler.postDelayed(futureClickRunnable, DOUBLE_TAP_THRESHOLD);
                        else
                            mHandler.post(futureClickRunnable);
                        return true;
                    }
                }

                onAfterMove();
                initialGesture = 0;
                return true;

            case MotionEvent.ACTION_CANCEL:
                break;

            case MotionEvent.ACTION_OUTSIDE:
                break;
        }

        return true;
    }

    public abstract void onMove(Direction dir, float diff);

    public abstract void onClick();

    public abstract void onDoubleTap(MotionEvent event);

    public abstract void onAfterMove();

    public abstract void onBeforeMove(Direction dir);
}