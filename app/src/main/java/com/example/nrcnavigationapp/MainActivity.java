package com.example.nrcnavigationapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapController;
import org.osmdroid.util.GeoPoint;

public class MainActivity extends AppCompatActivity {

    private MapView map;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid
        Configuration.getInstance().setUserAgentValue("NRCNavigationApp");

        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );

        setContentView(R.layout.activity_main);
        //databaseHelper = new DatabaseHelper(this);
        //databaseHelper.getWritableDatabase();

        map = findViewById(R.id.map);
        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        map.getTileProvider().clearTileCache();

        map.setMultiTouchControls(true);

        org.osmdroid.api.IMapController mapController = map.getController();
        mapController.setZoom(18.0);

        GeoPoint startPoint = new GeoPoint(-14.02, 33.67);

        mapController.setCenter(startPoint);


    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }
}