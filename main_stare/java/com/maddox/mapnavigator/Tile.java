package com.maddox.mapnavigator;

import android.graphics.Bitmap;

/**
 * Created by Pawel on 2015-05-23.
 */
public class Tile {

    public int x;
    public int y;
    public Bitmap img;

    public Tile(int x, int y, Bitmap img)
    {
        this.x = x;
        this.y = y;
        this.img = img;
    }

}
