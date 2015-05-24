/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/03/23/mapapp4-tilesprovider/
 */

package com.mapapp;

import java.util.ArrayList;
import java.util.Hashtable;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;

import com.mapapp.web.DownloadTaskFinishedCallback;
import com.mapapp.web.TileDownloadTask;
import com.mapapp.web.WebTilesProvider;

public class TilesProvider implements DownloadTaskFinishedCallback
{
	WebTilesProvider webProvider;

	// The database that holds the map
	protected SQLiteDatabase tilesDB;

	// Tiles will be stored here, the index\key will be in this format x:y
	protected Hashtable<String, Tile> tiles = new Hashtable<String, Tile>();

	// An object to use with synchronized to lock tiles hashtable
	public Object tilesLock = new Object();

	// A handler from the outside to be informed of new downloaded tiles
	// Used to redraw the map view whenever a new tile arrives
	Handler newTileHandler;

	public TilesProvider(String dbPath, Handler newTileHandler)
	{
		/*
		 *  Create WebTileProvider with max number of thread equal to five
		 *  We also pass this class as a DownloadTaskFinishedCallback
		 *  This way when the web provider downloads a tile we get it
		 *  and insert it into the database and the hashtable
		 */
		webProvider = new WebTilesProvider(5, this);

		// This time we are opening the database as read\write
		tilesDB = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);

		// This handler is to be notified when a new tile is downloaded
		// and available for rendering
		this.newTileHandler = newTileHandler;
	}

	// Updates the tiles in the hashtable
	public void fetchTiles(Rect rect, int zoom)
	{
		// We are using a separate object here for synchronizing
		// Using the hashtable tiles will cause problems when we swap the
		// pointers temp and tiles
		synchronized (tilesLock)
		{
			// Max tile index for x and y
			int maxIndex = (int) Math.pow(2, zoom) - 1;

			// First we create a list containing the index of each tile inside
			// the rectangle rect, we're expecting to find these tiles in memory
			// or
			// in the database
			ArrayList<String> expectedTiles = new ArrayList<String>();
			for (int x = rect.left; x <= rect.right; x++)
			{
				// Ignore tiles with invalid index
				if (x < 0 || x > maxIndex) continue;
				for (int y = rect.top; y <= rect.bottom; y++)
				{
					if (y < 0 || y > maxIndex) continue;
					expectedTiles.add(x + ":" + y);
				}
			}

			// Perpare the query for the database
			String query = "SELECT x,y,image FROM tiles WHERE x >= " + rect.left + " AND x <= " + rect.right + " AND y >= " + rect.top
					+ " AND y <=" + rect.bottom + " AND z == " + (17 - zoom);

			// query should be something like:
			// SELECT x,y,image FROM tiles WHERE x>=0 AND x<=4 AND y>=2 AND
			// y<=6
			// AND
			// z==6

			Cursor cursor;
			cursor = tilesDB.rawQuery(query, null);

			// Now cursor contains a table with these columns
			/*
			 * x(int)	y(int)	image(byte[])
			 */

			// Prepare an empty hash table to fill with the tiles we fetched
			Hashtable<String, Tile> temp = new Hashtable<String, Tile>();

			// Loop through all the rows(tiles) of the table returned by the
			// query
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

					// If This is a new tile, we didn't fetch it in the
					// previous
					// fetchTiles call.
					if (tile == null)
					{
						// Get the binary image data from the third cursor
						// column
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
				while (cursor.moveToNext()); // Move to next tile in the
												// query

				// The hashtable "tiles" is now outdated,
				// so clear it and set it to the new hashtable temp.

				/* 
				 * Swapping here sometimes creates an exception if we use
				 * tiles for synchronizing
				 */
				tiles.clear();
				tiles = temp;
			}

			// Remove the tiles we have from the ones to download
			for (Tile t : tiles.values())
			{
				expectedTiles.remove(t.x + ":" + t.y);
			}

			// Download the tiles we couldn't find
			for (String string : expectedTiles)
			{
				int x = 0, y = 0;
				String[] nums = string.split(":");
				x = Integer.parseInt(nums[0]);
				y = Integer.parseInt(nums[1]);
				webProvider.downloadTile(x, y, zoom);
			}
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
		// Make sure no other thread is using the hashtable before clearing it
		synchronized (tilesLock)
		{
			tiles.clear();
		}

		// Cancel all download operations
		webProvider.cancelDownloads();
	}

	// Called by the WebTilesProvider when a tile was downloaded successfully
	// Also note that it's marked as synchronized to make sure that we only
	// handle one
	// finished task at a time, since the WebTilesProvider will call this method
	// whenever
	// a task is finished
	@Override
	public synchronized void handleDownload(TileDownloadTask task)
	{
		byte[] tile = task.getFile();
		int x = task.getX();
		int y = task.getY();

		// Log.d("TAG", "Downloaded " + x + ":" + y);

		// Insert tile into database as an array of bytes
		insertTileToDB(x, y, 17 - task.getZ(), tile);

		// Creating bitmaps may throw OutOfMemoryError
		try
		{
			Bitmap bm = BitmapFactory.decodeByteArray(tile, 0, tile.length);
			Tile t = new Tile(x, y, bm);

			// Add the new tile to our tiles memory cache
			synchronized (tilesLock)
			{
				tiles.put(x + ":" + y, t);
			}

			// Here we inform who ever interested that we have a new tile
			// ready to be rendered!
			// The handler is in the MapAppActivity and sending it a message
			// will cause it to redraw the MapView
			if (newTileHandler != null) newTileHandler.sendEmptyMessage(0);
		}
		catch (OutOfMemoryError e)
		{
			// At least we got the tile as byte array and saved it in the
			// database
		}
	}

	// Marked as synchronized to prevent to insert operations at the same time
	synchronized void insertTileToDB(int x, int y, int z, byte[] tile)
	{
		ContentValues vals = new ContentValues();
		vals.put("x", x);
		vals.put("y", y);
		vals.put("z", z);
		vals.put("image", tile);
		tilesDB.insert("tiles", null, vals);
	}
}