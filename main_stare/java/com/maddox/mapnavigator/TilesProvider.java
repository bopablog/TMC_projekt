/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 *
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/03/23/mapapp4-tilesprovider/
 */

package com.maddox.mapnavigator;

import java.util.Hashtable;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import com.maddox.mapnavigator.Tile;

public class TilesProvider
{
    // The database that holds the map
    protected SQLiteDatabase tilesDB;

    // Tiles will be stored here, the index\key will be in this format x:y
    protected Hashtable<String, Tile> tiles = new Hashtable<String, Tile>();

    public TilesProvider(String dbPath)
    {
        tilesDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
    }

    // Updates the tiles in the hashtable
    public void fetchTiles(Rect rect, int zoom)
    {
        // Prepare the query
        String query = "SELECT x,y,image FROM tiles WHERE x >= " + rect.left + " AND x <= " + rect.right + " AND y >= " + rect.top
                + " AND y <=" + rect.bottom + " AND z == " + (17 - zoom);

        // query should be something like:
        // SELECT x,y,image FROM tiles WHERE x>=0 AND x<=4 AND y>=2 AND y<=6 AND
        // z==6

        Cursor cursor;
        cursor = tilesDB.rawQuery(query, null);

        // Now cursor contains a table with these columns
		/*
		 * x(int)	y(int)	image(byte[])
		 */

        // Prepare an empty hash table to fill with the tiles we fetched
        Hashtable<String, Tile> temp = new Hashtable<String, Tile>();

        // Loop through all the rows(tiles) of the table returned by the query
        // MUST call moveToFirst
        if (cursor.moveToFirst())
        {
            do
            {
                // Getting the index of this tile
                int x = cursor.getInt(0);
                int y = cursor.getInt(1);

                // Try to get this tile from the hashtable we have
                Tile tile = tiles.get(x + ":" + y);

                // If This is a new tile, we didn't fetch it in the previous
                // fetchTiles call.
                if (tile == null)
                {
                    // Get the binary image data from the third cursor column
                    byte[] img = cursor.getBlob(2);

                    // Create a bitmap (expensive operation)
                    Bitmap tileBitmap = BitmapFactory.decodeByteArray(img, 0, img.length);

                    // Create the new tile
                    tile = new Tile(x, y, tileBitmap);
                }

                // The object "tile" should now be ready for rendering

                // Add the tile to the temp hashtable
                temp.put(x + ":" + y, tile);
            }
            while (cursor.moveToNext()); // Move to next tile in the query
            // result

            // The hashtable "tiles" is now outdated,
            // so clear it and set it to the new hashtable temp.
            tiles.clear();
            tiles = temp;
        }
    }

    // Gets the hashtable where the tiles are stored
    public Hashtable<String, Tile> getTiles()
    {
        return tiles;
    }

    public void close()
    {
        // If fetchTiles is used after closing it will not work, it will throw
        // an exception
        tilesDB.close();
    }

    public void clear()
    {
        tiles.clear();
    }
}