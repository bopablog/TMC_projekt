/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/03/23/mapapp4-tilesprovider/
 */

package com.mapapp;

import android.graphics.Bitmap;

public class Tile
{
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