package com.halilibo.bettervideoplayer;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public abstract class OnSwipeTouchListener implements OnTouchListener {

    public enum Direction{
        LEFT, RIGHT, UP, DOWN;
    }

    private final static String TAG = "ClickFrame";

    private final int SWIPE_THRESHOLD = 100;

    // 0: uninitialized
    // 1: horizontal
    // 2: vertical
    private int initialGesture;

    float initialX, initialY, deltaX, deltaY;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {

            case MotionEvent.ACTION_DOWN:
                initialX = event.getX();
                initialY = event.getY();
                initialGesture = 0;

                Log.d(TAG, "Action was DOWN");
                break;

            case MotionEvent.ACTION_MOVE:
                deltaX = event.getX() - initialX;
                deltaY = event.getY() - initialY;

                if(initialGesture == 0 && Math.abs(deltaX) > SWIPE_THRESHOLD){
                    initialGesture = 1;
                    if(deltaX > 0){
                        onBeforeMove(Direction.RIGHT);
                    }
                    else{
                        onBeforeMove(Direction.LEFT);
                    }
                }
                else if(initialGesture == 0 && Math.abs(deltaY) > SWIPE_THRESHOLD){
                    initialGesture = 2;
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
                Log.d(TAG, "Action was MOVE diffX: " + deltaX + " diffY: " + deltaY);

                break;

            case MotionEvent.ACTION_UP:
                Log.d(TAG, "Action was UP");
                deltaX = event.getX() - initialX;
                deltaY = event.getY() - initialY;

                if(initialGesture == 0){
                    onClick();
                    return true;
                }
                onAfterMove();
                return true;

            case MotionEvent.ACTION_CANCEL:
                Log.d(TAG,"Action was CANCEL");
                break;

            case MotionEvent.ACTION_OUTSIDE:
                Log.d(TAG, "Movement occurred outside bounds of current screen element");
                break;
        }

        return true;
    }

    public abstract void onMove(Direction dir, float diff);

    public abstract void onClick();

    public abstract void onAfterMove();

    public abstract void onBeforeMove(Direction dir);
}