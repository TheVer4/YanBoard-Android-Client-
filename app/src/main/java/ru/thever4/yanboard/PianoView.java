package ru.thever4.yanboard;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by thever4 on 09.03.19.
 */

public class PianoView extends View {

    private boolean measurementChanged = false;
    private int canvasWidth = 0;
    private OnKeyTouchListener onTouchListener;
    private int instrumentWidth;
    private float scaleX = 1.0f;
    private int xOffset = 0;

    private GestureDetector gestureDetector;

    private Keyboard keyboard;

    public PianoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();

        keyboard = new Keyboard(context);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if(measurementChanged) {
            measurementChanged = false;
            keyboard.initializeInstrument(getMeasuredHeight(), getContext());

            float oldInstrumentWidth = instrumentWidth;
            instrumentWidth = keyboard.getWidth();

            float ratio = (float) instrumentWidth / oldInstrumentWidth;
            xOffset = (int) (xOffset * ratio);
        }

        int localXOffset = getOffsetInsideOfBounds();

        canvas.save();
        canvas.scale(scaleX, 1.0f);
        canvas.translate(-localXOffset, 0);

        keyboard.updateBounds(localXOffset, canvasWidth + localXOffset);
        keyboard.draw(canvas);

        canvas.restore();
        //super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int width, int height) {
        canvasWidth = MeasureSpec.getSize(width);
        measurementChanged = true;

        super.onMeasure(width, height);
    }

    private GestureDetector.OnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            fireTouchListeners(keyboard.getTouchedMIDICode());

            resetTouchFeedback();
            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            fireLongTouchListeners(keyboard.getTouchedMIDICode());

            resetTouchFeedback();
            super.onLongPress(e);
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if(keyboard.touchItem(e.getX() / scaleX + xOffset, e.getY())) {
                invalidate();
            }

            return true;
        }

    };

    private void fireTouchListeners(int code) {
        if(onTouchListener != null) {
            onTouchListener.onTouch(code);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if(action == MotionEvent.ACTION_CANCEL) {
            resetTouchFeedback();
            xOffset = getOffsetInsideOfBounds();
            ViewCompat.postInvalidateOnAnimation(this);
        }
        if(action == MotionEvent.ACTION_UP) {
            Log.d("TouchEVENT", "Отскок клавиши");
        }
        if(action == MotionEvent.ACTION_DOWN) {
            Log.d("TouchEVENT", "Отжим клавиши");
        }
        return super.onTouchEvent(event) || gestureDetector.onTouchEvent(event);
    }

    private void fireLongTouchListeners(int code) {
        if(onTouchListener != null) {
            onTouchListener.onLongTouch(code);
        }
    }

    public void resetTouchFeedback() {
        if(keyboard.releaseTouch()) {
            invalidate();
        }
    }

    public void setOnKeyTouchListener(OnKeyTouchListener listener) {
        this.onTouchListener = listener;
    }

    public interface OnKeyTouchListener {

        void onTouch(int midiCode);

        void onLongTouch(int midiCode);

    }

    private void init() {
        gestureDetector = new GestureDetector(getContext(), gestureListener);
    }

    private int getOffsetInsideOfBounds() {
        int localXOffset = xOffset;
        if(localXOffset < 0) {
            localXOffset = 0;
        }
        if(localXOffset > instrumentWidth - getMeasuredWidth()) {
            localXOffset = instrumentWidth - getMeasuredWidth();
        }
        return localXOffset;
    }
}
