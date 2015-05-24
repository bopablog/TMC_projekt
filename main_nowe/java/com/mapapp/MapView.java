/*
 * MapApp : Simple offline map application, made by Hisham Ghosheh for tutorial purposes only
 * Tutorial on my blog
 * http://ghoshehsoft.wordpress.com/2012/03/09/building-a-map-app-for-android/
 * 
 * Class tutorial:
 * http://ghoshehsoft.wordpress.com/2012/04/06/mapapp5-mapview-and-activity/
 */

package com.mapapp;

import java.util.Collection;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.mapapp.PointD;
import com.mapapp.R;
import com.mapapp.Tile;
import com.mapapp.TilesManager;
import com.mapapp.TilesProvider;

public class MapView extends View
{
	// Needed to pass to View constructor
	protected Context context;

	// MapView dimensions
	protected int viewWidth, viewHeight;

	// Provides us with tiles
	protected TilesProvider tileProvider;

	// Handles calculations
	protected TilesManager tileManager;

	// Different paints
	protected Paint fontPaint;
	protected Paint bitmapPaint = new Paint();
	protected Paint circlePaint = new Paint();

	// The location of the view center in longitude, latitude
	protected PointD seekLocation = new PointD(0, 0);
	// Location of the phone using Gps data
	protected Location gpsLocation = null;
	// If true then seekLocation will always match gpsLocation
	protected boolean autoFollow = false;

	// An image to draw at the phone's position
	protected Bitmap positionMarker;

	// touch position values kept for panning\dragging
	protected PointD lastTouchPos = new PointD(-1, -1);

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);

		if (!this.isInEditMode())
		{
			// Update the tiles manager
			this.viewWidth = getWidth();
			this.viewHeight = getHeight();

			tileManager.setDimensions(viewWidth, viewHeight);

			refresh();
		}
	}
	
	
	public MapView(Context context, AttributeSet attrs)
	{		
		super(context, attrs);
		
		
		// We don't know view dimensions yet, super.onMeasure will be called
		viewWidth = viewHeight = -1;

		if (!isInEditMode())
		{
			int zoomLevel = 0;

			TypedArray arr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MapView, 0, 0);
			try
			{
				zoomLevel = arr.getInt(R.styleable.MapView_zoomLevel, 0);
				Drawable d = arr.getDrawable(R.styleable.MapView_marker);
				if (d != null) positionMarker = ((BitmapDrawable) d).getBitmap();
			}
			finally
			{
				arr.recycle();
			}

			tileManager = new TilesManager(256, viewWidth, viewHeight);
			tileManager.setZoom(zoomLevel);
		}

		initPaints();
	}

	public MapView(Context context, int viewWidth, int viewHeight, TilesProvider tilesProvider, Bitmap positionMarker)
	{
		super(context);
		this.context = context;

		// Tiles provider is passed not created.
		// The idea is to hide the actual tiles source from the view
		// This way the view doesn't care whether the source is a database or
		// the Internet
		this.tileProvider = tilesProvider;

		// These values will be used later
		this.viewWidth = viewWidth;
		this.viewHeight = viewHeight;

		// Get the marker image
		this.positionMarker = positionMarker;

		// Creating a TilesManager assuming that the tile size is 256*256.
		// You might want to pass tile size as a parameter or even calculate it
		// somehow
		tileManager = new TilesManager(256, viewWidth, viewHeight);

		// Initializes paints
		initPaints();

		// Fetching tiles from the tilesProvider
		fetchTiles();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		// Case1 : width and height are provided in the constructor
		if (viewWidth != -1 && viewHeight != -1)
		{
			// Setting width,height that was passed in the constructor as the
			// view's dimensions
			setMeasuredDimension(viewWidth, viewHeight);
		}
		// Case2: view was created using XML
		else
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	void initPaints()
	{
		// Font paint is used to draw text
		fontPaint = new Paint();
		fontPaint.setColor(Color.DKGRAY);
		fontPaint.setShadowLayer(1, 1, 1, Color.BLACK);
		fontPaint.setTextSize(20);

		// Used to draw a semi-transparent circle at the phone's gps location
		circlePaint.setARGB(70, 170, 170, 80);
		circlePaint.setAntiAlias(true);
	}

	void fetchTiles()
	{
		// Update tilesManager to have the center of the view as its location
		tileManager.setLocation(seekLocation.x, seekLocation.y);

		// Get the visible tiles indices as a Rect
		Rect visibleRegion = tileManager.getVisibleRegion();

		if (tileProvider == null) return;

		// Tell tiles provider what tiles we need and which zoom level.
		// The tiles will be stored inside the tilesProvider.
		// We can get those tiles later when drawing the view
		tileProvider.fetchTiles(visibleRegion, tileManager.getZoom());
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		if (this.isInEditMode())
		{
			canvas.drawARGB(255, 255, 255, 255);

			String str = "MapView: Design Mode";

			// Text centering, Modified version of :
			// http://stackoverflow.com/a/11121873
			int xPos = (int) ((canvas.getWidth() / 2f) - fontPaint.measureText(str) / 2f);
			int yPos = (int) ((canvas.getHeight() / 2) - ((fontPaint.descent() + fontPaint.ascent()) / 2));

			canvas.drawText(str, xPos, yPos, fontPaint);
			return;
		}

		if (tileProvider == null)
		{
			canvas.drawARGB(255, 250, 111, 103);

			String str = "TilesProvider not set";

			// Text centering, Modified version of :
			// http://stackoverflow.com/a/11121873
			int xPos = (int) ((canvas.getWidth() / 2f) - fontPaint.measureText(str) / 2f);
			int yPos = (int) ((canvas.getHeight() / 2) - ((fontPaint.descent() + fontPaint.ascent()) / 2));

			canvas.drawText(str, xPos, yPos, fontPaint);

			return;
		}

		// Clear the view to grey
		canvas.drawARGB(255, 100, 100, 100);

		/*
		 * To draw the map we need to find the position of the pixel representing the center of the view.
		 * We need the position to be relative to the full world map, lets call this pixel position "pix"
		 * pix.x will range from 0 to (2^zoom)*tileSize-1, same for pix.y		
		 * To draw anything on the map we subtract pix from the original position
		 * It's just like dragging the map so that the pixel representing the gps location gets into the center of the view 		 
		*/

		// In a square world map,
		// we need to know pix location as two values from 0.0 to 1.0
		PointD pixRatio = TilesManager.calcRatio(seekLocation.x, seekLocation.y);

		// Full world map width in pixels
		int mapWidth = tileManager.mapSize() * 256;
		Point pix = new Point((int) (pixRatio.x * mapWidth), (int) (pixRatio.y * mapWidth));

		/*
		 * Subtracting pix from each tile position will result in pix being drawn at the top left corner of the view 
		 * To drag it to the center we add (viewWidth/2, viewHeight/2) to the final result
		 * pos.x = pos.x - pix.x + viewWidth/2f
		 * pos.x = pox.x - (pix.x - viewWidth/2f)
		 * ---> offset.x =  (pix.x - viewWidth/2f)
		 * same for offset.y
		 */

		Point offset = new Point((int) (pix.x - viewWidth / 2f), (int) (pix.y - viewHeight / 2f));
		// offset is now ready to use

		// Drawing tiles in a separate function to make the code more readable
		drawTiles(canvas, offset);

		// Draw the marker that pinpoints the user's location
		drawMarker(canvas, offset);
	}

	void drawTiles(Canvas canvas, Point offset)
	{
		/* 
		 * We use the same object in the TilesProvider when drawing
		 * This is necessary to make sure no one changes the available tiles
		 * while the MapView is being rendered
		 */
		synchronized (tileProvider.tilesLock)
		{
			// Get tiles from the Hashtable inside tilesProvider
			Collection<Tile> tilesList = tileProvider.getTiles().values();

			// x,y are the calculated offset

			// Go trough all the available tiles
			for (Tile tile : tilesList)
			{
				// We act as if we're drawing a map of the whole world at a
				// specific
				// zoom level
				// The top left corner of the map occupies the pixel (0,0) of
				// the
				// view
				int tileSize = tileManager.getTileSize();
				long tileX = tile.x * tileSize;
				long tileY = tile.y * tileSize;

				// Subtract offset from the previous calculations
				long finalX = tileX - offset.x;
				long finalY = tileY - offset.y;

				// Draw the bitmap of the tiles using a simple paint
				canvas.drawBitmap(tile.img, finalX, finalY, bitmapPaint);
			}
		}
	}

	void drawMarker(Canvas canvas, Point offset)
	{
		// Proceed only if a gps fix is available
		if (gpsLocation != null)
		{
			// Get marker position in pixels as if we're going to draw it on a
			// world map where the top left corner of the map occupies the (0,0)
			// pixel of the view
			Point markerPos = tileManager.lonLatToPixelXY(gpsLocation.getLongitude(), gpsLocation.getLatitude());

			// Add offset to the marker position
			int markerX = markerPos.x - offset.x;
			int markerY = markerPos.y - offset.y;

			// If marker bitmap exists
			if (positionMarker != null)
			{
				// Draw the marker and make sure you draw the center of the
				// marker
				// at the marker location
				canvas.drawBitmap(positionMarker, markerX - positionMarker.getWidth() / 2, markerY - positionMarker.getHeight() / 2,
						bitmapPaint);
			}
			else
			{
				// Draw an ugly circle if no bitmap is set :P
				canvas.drawCircle(markerX, markerY, 10, bitmapPaint);
			}

			// Around the marker we will draw a circle representing the accuracy
			// of the gps fix
			// We first calculate its radius

			// Calculate how many meters one pixel represents
			float ground = (float) tileManager.calcGroundResolution(gpsLocation.getLatitude());

			// Location.getAccuracy() returns the accuracy in meters.
			float rad = gpsLocation.getAccuracy() / ground;

			canvas.drawCircle(markerX, markerY, rad, circlePaint);

			// Just drawing location info
			int pen = 1;
			canvas.drawText("lon:" + gpsLocation.getLongitude(), 0, 20 * pen++, fontPaint);
			canvas.drawText("lat:" + gpsLocation.getLatitude(), 0, 20 * pen++, fontPaint);
			canvas.drawText("alt:" + gpsLocation.getAltitude(), 0, 20 * pen++, fontPaint);
			canvas.drawText("Zoom:" + tileManager.getZoom(), 0, 20 * pen++, fontPaint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		int action = event.getAction();

		if (action == MotionEvent.ACTION_DOWN)
		{
			// Keep touch position for later use (dragging)
			lastTouchPos.x = (int) event.getX();
			lastTouchPos.y = (int) event.getY();

			return true;
		}
		else if (action == MotionEvent.ACTION_MOVE)
		{
			autoFollow = false;

			PointD current = new PointD(event.getX(), event.getY());

			// Find how many pixels the users finger moved in both x and y
			PointD diff = new PointD(current.x - lastTouchPos.x, current.y - lastTouchPos.y);

			// In a full wolrd map, get the position of the center of the view
			// in pixels
			Point pixels1 = tileManager.lonLatToPixelXY(seekLocation.x, seekLocation.y);

			// Subtract diff from that position
			Point pixels2 = new Point(pixels1.x - (int) diff.x, pixels1.y - (int) diff.y);

			// Reconvert the final result to longitude, latitude point
			PointD newSeek = tileManager.pixelXYToLonLat((int) pixels2.x, (int) pixels2.y);

			// Finally move the center of the view to the new location
			seekLocation = newSeek;

			// Refresh the view
			fetchTiles();
			invalidate(); // Causes the view to redraw itself

			// Prepare for the next drag event
			lastTouchPos.x = current.x;
			lastTouchPos.y = current.y;

			return true;
		}

		return super.onTouchEvent(event);
	}

	// Fetch the tiles then draw, don't call to often
	public void refresh()
	{
		fetchTiles();
		invalidate();
	}

	// Like refresh but called from a non UI thread
	public void postRefresh()
	{
		fetchTiles();
		postInvalidate();
	}

	// Simply sets seek location to gpsLocation (if exists)
	public void followMarker()
	{
		if (gpsLocation != null)
		{
			seekLocation.x = gpsLocation.getLongitude();
			seekLocation.y = gpsLocation.getLatitude();
			autoFollow = true;

			fetchTiles();
			invalidate();
		}
	}

	public void zoomIn()
	{
		tileManager.zoomIn();
		onMapZoomChanged();
	}

	public void zoomOut()
	{
		tileManager.zoomOut();
		onMapZoomChanged();
	}

	protected void onMapZoomChanged()
	{
		if (tileProvider != null) tileProvider.clear();

		fetchTiles();
		invalidate();
	}

	// Returns the gps coordinates of the user
	public Location getGpsLocation()
	{
		return gpsLocation;
	}

	// Returns the gps coordinates of our view center
	public PointD getSeekLocation()
	{
		return seekLocation;
	}

	// Centers the given gps coordinates in our view
	public void setSeekLocation(double longitude, double latitude)
	{
		seekLocation.x = longitude;
		seekLocation.y = latitude;
	}

	// Sets the marker position
	public void setGpsLocation(Location location)
	{
		setGpsLocation(location.getLongitude(), location.getLatitude(), location.getAltitude(), location.getAccuracy());
	}

	// Sets the marker position
	public void setGpsLocation(double longitude, double latitude, double altitude, float accuracy)
	{
		if (gpsLocation == null) gpsLocation = new Location("");
		gpsLocation.setLongitude(longitude);
		gpsLocation.setLatitude(latitude);
		gpsLocation.setAltitude(altitude);
		gpsLocation.setAccuracy(accuracy);

		if (autoFollow) followMarker();

	}

	public int getZoom()
	{
		return tileManager.getZoom();
	}

	public void setZoom(int zoom)
	{
		tileManager.setZoom(zoom);
		onMapZoomChanged();
	}

	public void setTilesProvider(TilesProvider tilesProvider)
	{
		this.tileProvider = tilesProvider;
	}
}