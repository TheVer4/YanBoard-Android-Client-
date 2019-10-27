package ru.thever4.yanboard;

import android.os.Build;

/**
 * Created by thever4 on 09.03.19.
 */

public class Key implements Cloneable {

    float startX, endX, startY, endY;

    int midiCode;

    boolean black;

    boolean pressed = false;

    void setBounds(float startX, float endX, float startY, float endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    @Override
    protected Key clone() {
        Key key = new Key();
        key.startX = startX;
        key.endX = endX;
        key.startY = startY;
        key.endY = endY;
        key.midiCode = midiCode;
        key.black = black;

        return key;
    }

    boolean containsPoint(float x, float y) {
        return startX <= x && endX > x && startY <= y && endY > y;
    }

    float getOverlayPivotX() {
        return (endX - startX) / 2f + startX;
    }

    float getOverlayPivotY() {
        if(!black) {
            return (endY - startY) * 0.85f + startY;
        }
        return  (endY - startY) / 2f + startY;
    }

}
