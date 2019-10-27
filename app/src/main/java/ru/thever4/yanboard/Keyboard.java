package ru.thever4.yanboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

/**
 * Created by thever4 on 09.03.19.
 */

public class Keyboard {

    private static final int START_MIDI_CODE = 12;
    private static final float BLACK_KEY_HEIGHT_PERCENT = 1.57f;
    private static final float WHITE_KEY_ASPECT_RATIO = 6.12f;
    private static final int OCTAVES = 4;
    private static final int KEYS_IN_OCTAVE = 12;
    private static final int[] BLACK_INDICIES = {1, 3, 6, 8, 10};
    private static final int[] WHITE_INDICIES = {0, 2, 4, 5, 7, 9, 11};
    private static final int NOT_FOUND = -1;

    private int whiteKeyWidth;
    private int blackKeyHeight;
    private int octaveWidth;


    protected int screenLeft;
    protected int screenRight;
    private int touchedKey;

    private Key[] keysArray;

    private Drawable key_black;
    private Drawable key_white;


    public int getTouchedMIDICode() {
        return touchedKey + START_MIDI_CODE;
    }

    public Keyboard(Context context) {

        key_black = context.getResources().getDrawable(R.drawable.key_black);
        key_white = context.getResources().getDrawable(R.drawable.key_white);

    }

    public boolean touchItem(float x, float y) {
        touchedKey = locateTouchedKey(x, y);
        if(touchedKey != -1) {
            keysArray[touchedKey].pressed = true;
            return true;
        }
        return false;
    }

    public boolean releaseTouch() {
        if(touchedKey != -1) {
            keysArray[touchedKey].pressed = false;
            touchedKey = -1;
            return true;
        }
        return  false;
    }

    public void draw(Canvas canvas) {
        final Key[] keys = keysArray;

        int firstVisibleKey = getFirstVisibleKey();
        int lastVisibleKey = getLastVisibleKey();

        for(int i = 0; i < Keyboard.OCTAVES; i++) {
            for(int j = 0; j < Keyboard.WHITE_INDICIES.length; j++) {
                drawSingleKey(canvas, keys[i * Keyboard.KEYS_IN_OCTAVE + Keyboard.WHITE_INDICIES[j]], firstVisibleKey, lastVisibleKey);
            }
            for(int j = 0; j < Keyboard.BLACK_INDICIES.length; j++) {
                drawSingleKey(canvas, keys[i * Keyboard.KEYS_IN_OCTAVE + Keyboard.BLACK_INDICIES[j]], firstVisibleKey, lastVisibleKey);
            }
        }

    }

    private void drawSingleKey(Canvas canvas, Key key, int firstVisibleKey, int lastVisibleKey) {
        if(key.midiCode < firstVisibleKey || key.midiCode > lastVisibleKey) {
            return;
        }
        Drawable drawable = key.black ? key_black : key_white;
        drawable.setState(new int[] {key.pressed ? android.R.attr.state_pressed : -android.R.attr.state_pressed});
        drawable.setBounds((int) key.startX, (int) key.startY, (int) key.endX, (int) key.endY);
        drawable.draw(canvas);
    }

    public void updateBounds(int left, int right) {
        screenLeft = left;
        screenRight = right;
    }

    private int locateVisibleKey(float x, boolean first) {
        int octaveIndex = (int) (x / (float) octaveWidth);

        int resultIndex = 0;
        for(int j = 0; j < BLACK_INDICIES.length; j++) {
            int index = octaveIndex * Keyboard.KEYS_IN_OCTAVE + BLACK_INDICIES[j];
            if(checkKeyAtIndex(x, 40, index)) {
                resultIndex = index;
            }
        }

        int whiteKeyIndex = (int) ((x - octaveIndex * octaveWidth) / (float) whiteKeyWidth);
        int index = octaveIndex * Keyboard.KEYS_IN_OCTAVE + WHITE_INDICIES[whiteKeyIndex];
        if(checkKeyAtIndex(x, 40, index)) {
            if(first) {
                return resultIndex > index ? index : resultIndex;
            }
            else {
                return resultIndex > index ? resultIndex : index;
            }
        }

        return NOT_FOUND;
    }

    private int getFirstVisibleKey() {
        int locateVisibleKey = locateVisibleKey(screenLeft, true);
        if(locateVisibleKey == NOT_FOUND) {
            locateVisibleKey = 0;
        }
        return locateVisibleKey + Keyboard.START_MIDI_CODE;
    }

    private int getLastVisibleKey() {
        int locateVisibleKey = locateVisibleKey(screenRight, false);
        if(locateVisibleKey == NOT_FOUND) {
            locateVisibleKey = keysArray.length - 1;
        }
        return locateVisibleKey + Keyboard.START_MIDI_CODE;
    }

    public int getWidth() {
        return octaveWidth * Keyboard.OCTAVES;
    }

    public void initializeInstrument(float measuredHeight, Context context) {
        whiteKeyWidth = Math.round(measuredHeight / WHITE_KEY_ASPECT_RATIO);
        octaveWidth = whiteKeyWidth * 7;

        int blackHalfWidth = octaveWidth / 20;
        blackKeyHeight = Math.round(measuredHeight / BLACK_KEY_HEIGHT_PERCENT);

        keysArray = new Key[KEYS_IN_OCTAVE * OCTAVES];
        int whiteIndex = 0;
        int blackIndex = 0;

        for(int i = 0; i < KEYS_IN_OCTAVE; i++) {

            Key key = new Key();
            if(isWhite(i)) {
                key.black = false;
                key.setBounds(whiteKeyWidth * whiteIndex, whiteKeyWidth * whiteIndex + whiteKeyWidth, 0, measuredHeight);
                whiteIndex++;
            }
            else {
                key.black = true;
                int indexDisplacement = i == 1 || i == 3 ? 1 : 2;
                key.setBounds(whiteKeyWidth * (blackIndex + indexDisplacement) - blackHalfWidth, whiteKeyWidth * (blackIndex + indexDisplacement) + blackHalfWidth, 0, blackKeyHeight);
                blackIndex++;
            }
            key.midiCode = START_MIDI_CODE + i;
            keysArray[i] = key;
        }

        for(int i = KEYS_IN_OCTAVE; i < KEYS_IN_OCTAVE * OCTAVES; i++) {
            Key firstOctaveKey = keysArray[i % KEYS_IN_OCTAVE];

            Key key = firstOctaveKey.clone();
            key.startX += (i / KEYS_IN_OCTAVE) * octaveWidth;
            key.endX += (i / KEYS_IN_OCTAVE) * octaveWidth;
            key.midiCode = START_MIDI_CODE + i;
            keysArray[i] = key;
        }
    }

    private static boolean isWhite(int i) {
        return i == 0 || i == 2 || i == 4 || i == 5 || i == 7 || i == 9 || i == 11;
    }

    private int locateTouchedKey(float x, float y) {
        int octaveIndex = (int) (x/ (float) octaveWidth);

        if(y <= blackKeyHeight) {
            for(int j = 0; j < BLACK_INDICIES.length; j++) {
                int index = octaveIndex * Keyboard.KEYS_IN_OCTAVE + BLACK_INDICIES[j];
                if(checkKeyAtIndex(x, y, index)) {
                    return  index;
                }
            }
        }

        int whiteKeyIndex = (int) ((x - octaveIndex * octaveWidth) / (float) whiteKeyWidth);
        int index = octaveIndex * Keyboard.KEYS_IN_OCTAVE + WHITE_INDICIES[whiteKeyIndex];
        if(checkKeyAtIndex(x, y, index)) {
            return  index;
        }

        return NOT_FOUND;
    }

    private boolean checkKeyAtIndex(float x, float y, int index) {
        if (index < 0 || index >= keysArray.length) {
            return false;
        }
        return keysArray[index].containsPoint(x, y);
    }

}
