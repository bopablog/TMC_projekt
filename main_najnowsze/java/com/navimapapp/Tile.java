package com.navimapapp;

import android.graphics.Bitmap;

/**
 * Created by Pawel on 2015-05-26.
 */
public class Tile {
    // Made public for simplicity
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