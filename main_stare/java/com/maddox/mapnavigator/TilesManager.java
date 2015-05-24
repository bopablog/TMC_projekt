package com.maddox.mapnavigator;

import android.graphics.Point;
import android.graphics.Rect;

/**
 * Created by Pawel on 2015-05-23.
 */

//Menedzer zarzadzania kafelkami//
public class TilesManager {

    public final static double EarthRadius = 6378137;       //promien ziemi w metrach
    public final static double MinLatitude = -85.05112878;  //Poludnie
    public final static double MaxLatitude = 85.05112878;   //Polnoc
    public final static double MinLongitude = -180;         //Zachod
    public final static double MaxLongitude = 180;          //Wschod

    // this value should be extracted from DB, I used 17 for simplicity
    protected int maxZoom = 17;
    protected int tileSize = 256; // rozmiar w px pojedynczego kafelka

    // wymiary widoku
    protected int viewWidth, viewHeight;

    //ile kafelkow do wypelnienia widoku poziom pion
    protected int tileCountX, tileCountY;

    // Will hold the indices of the visible tiles
    protected Rect visibleRegion;

    // Current location of the tiles manager
    protected PointD location = new PointD(0, 0);

    // Aktualny zoom
    protected int zoom = 0;

    public TilesManager(int tileSize, int viewWidth, int viewHeight)
    {
        this.tileSize = tileSize;

        this.setDimensions(viewWidth, viewHeight);

        // Updates visible region, this function will be explained later
        updateVisibleRegion(location.x, location.y, zoom);
    }

    public static PointD calcRatio(double longitude, double latitude)
    {
        double ratioX = ((longitude + 180.0) / 360.0);

        double sinLatitude = Math.sin(latitude * Math.PI / 180.0);
        double ratioY = (0.5 - Math.log((1 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * Math.PI));

        return new PointD(ratioX, ratioY);
    }

    public int mapSize()
    {
        return (int) Math.pow(2, zoom);
    }

    protected Point calcTileIndices(double longitude, double latitude)
    {
        // Simple calculations
        Point ratio = calcRatio(longitude, latitude);
        int mapSize = mapSize();

        return new Point((int) (ratio.x * mapSize), (int) (ratio.y * mapSize));
    }

    protected void updateVisibleRegion(double longitude, double latitude, int zoom)
    {
        // Update manager state
        location.x = longitude;
        location.y = latitude;
        this.zoom = zoom;

        // Get the index of the tile we are interested in
        Point tileIndex = calcTileIndices(location.x, location.y);

        // We get some of the neighbors from left and some from right
        // Same thing for up and down
        int halfTileCountX = (int) ((float) (tileCountX + 1) / 2f);
        int halfTileCountY = (int) ((float) (tileCountY + 1) / 2f);

        visibleRegion = new Rect(tileIndex.x - halfTileCountX, tileIndex.y - halfTileCountY, tileIndex.x + halfTileCountX, tileIndex.y+ halfTileCountY);
    }

    protected static double clamp(double x, double min, double max)
    {
        return Math.min(Math.max(x, min), max);
    }

    //Ile metrow na 1 px
    public double calcGroundResolution(double latitude)
    {
        latitude = clamp(latitude, MinLatitude, MaxLatitude);
        return Math.cos(latitude * Math.PI / 180.0) * 2.0 * Math.PI * EarthRadius / (double) (tileSize * mapSize());
    }

    //na podstawie dlugosci szerokosci okresla, ktory to  px
    public Point lonLatToPixelXY(double longitude, double latitude)
    {
        // Clamp values
        longitude = clamp(longitude, MinLongitude, MaxLongitude);
        latitude = clamp(latitude, MinLatitude, MaxLatitude);

        PointD ratio = calcRatio(longitude, latitude);
        double x = ratio.x;
        double y = ratio.y;

        long mapSize = mapSize() * tileSize;
        int pixelX = (int) clamp(x * mapSize + 0.5, 0, mapSize - 1);
        int pixelY = (int) clamp(y * mapSize + 0.5, 0, mapSize - 1);

        return new Point(pixelX, pixelY);
    }

    //pixel do dlugosc szerokosc
    public PointD pixelXYToLonLat(int pixelX, int pixelY)
    {
        double mapSize = mapSize() * tileSize;
        double x = (clamp(pixelX, 0, mapSize - 1) / mapSize) - 0.5;
        double y = 0.5 - (clamp(pixelY, 0, mapSize - 1) / mapSize);

        double latitude = 90.0 - 360.0 * Math.atan(Math.exp(-y * 2.0 * Math.PI)) / Math.PI;
        double longitude = 360.0 * x;

        return new PointD(longitude, latitude);
    }

    public void setZoom(int zoom)
    {
        zoom = (int) clamp(zoom, 0, maxZoom);
        updateVisibleRegion(location.x, location.y, zoom);
    }

    public void setLocation(double longitude, double latitude)
    {
        updateVisibleRegion(longitude, latitude, zoom);
    }

    public Rect getVisibleRegion()
    {
        return visibleRegion;
    }

    public int getZoom()
    {
        return zoom;
    }

    public int zoomIn()
    {
        setZoom(zoom + 1);
        return zoom;
    }

    public int zoomOut()
    {
        setZoom(zoom - 1);
        return zoom;
    }

    public int getTileSize()
    {
        return tileSize;
    }

    public int getMaxZoom()
    {
        return maxZoom;
    }

    public void setMaxZoom(int maxZoom)
    {
        this.maxZoom = maxZoom;
    }

    public void setDimensions(int width, int height)
    {
        this.viewWidth = width;
        this.viewHeight = height;

        // Simple math <span class="wp-smiley wp-emoji wp-emoji-smile" title=":)">:)</span>
        tileCountX = (int) ((float) viewWidth / tileSize);
        tileCountY = (int) ((float) viewHeight / tileSize);
    }

}
