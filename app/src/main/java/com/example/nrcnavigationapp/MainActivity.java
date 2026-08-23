package com.example.nrcnavigationapp;

import android.os.Bundle;
import android.widget.Toast;
import android.widget.EditText;


import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapController;
import org.osmdroid.util.GeoPoint;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.database.Cursor;
import org.osmdroid.views.overlay.Marker;

// importing GPS access libraries
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



public class MainActivity extends AppCompatActivity {

    private MapView map;

    private EditText searchLocation;
    private DatabaseHelper databaseHelper;
    // requesting for GPS permission
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Initializing OSMDroid
        Configuration.getInstance().setDebugMode(true);
        // Using a completely fresh cache directory to bypass any cached "Blocked" tiles
        Configuration.getInstance().setOsmdroidTileCache(new java.io.File(getCacheDir(), "osmdroid_tiles_fresh"));
        
        Configuration.getInstance().load(
                getApplicationContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        );
        
        // Very specific User-Agent as required by OSM policy
        String userAgent = "NRCNavigationApp/1.1 (Unique-ID: " + System.currentTimeMillis() + ")";
        Configuration.getInstance().setUserAgentValue("NRCNavigationApp/1.1");

        setContentView(R.layout.activity_main);
        map = findViewById(R.id.map);
        searchLocation = findViewById(R.id.searchLocation);
        searchLocation.setOnEditorActionListener((v, actionId, event) -> {

            String searchText = searchLocation.getText().toString().trim();

            if (!searchText.isEmpty()) {
                searchLocation(searchText);
            }

            return true;
        });

        databaseHelper = new DatabaseHelper(this);
        databaseHelper.getWritableDatabase();
        if (!databaseHelper.hasLocations()) {
            importLocationsFromCSV();
        }
        showLocationsOnMap();

        Toast.makeText(
                this,
                "Locations imported: " + databaseHelper.getAllLocations().getCount(),
                Toast.LENGTH_LONG
        ).show();


        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.OpenTopo);

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
    private void importLocationsFromCSV() {

        try {

            InputStream is = getAssets().open("NRC coordinates(1).csv");

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(is));

            String line;

            // Skip the header row
            reader.readLine();

            while ((line = reader.readLine()) != null) {

                String[] columns = line.split(",");

                if (columns.length >= 3) {

                    String name = columns[0].trim();

                    double latitude = Double.parseDouble(
                            columns[1].replace("\"", "").trim()
                    );

                    double longitude = Double.parseDouble(
                            columns[2].replace("\"", "").trim()
                    );

                    databaseHelper.insertLocation(
                            name,
                            "Location",
                            latitude,
                            longitude
                    );
                }
            }

            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showLocationsOnMap() {

        Cursor cursor = databaseHelper.getAllLocations();

        while (cursor.moveToNext()) {

            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow("name")
            );

            double latitude = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("latitude")
            );

            double longitude = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("longitude")
            );
            android.util.Log.d(
                    "NRC_COORDINATES",
                    name + " | Latitude: " + latitude + " | Longitude: " + longitude
            );

            GeoPoint point = new GeoPoint(latitude, longitude);

            Marker marker = new Marker(map);

            marker.setPosition(point);
            marker.setTitle(name);

            map.getOverlays().add(marker);
        }

        cursor.close();

        map.invalidate();
    }
    private void searchLocation(String searchText) {

        Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                "SELECT * FROM locations WHERE name LIKE ?",
                new String[]{"%" + searchText + "%"}
        );

        if (cursor.moveToFirst()) {

            double latitude = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("latitude")
            );

            double longitude = cursor.getDouble(
                    cursor.getColumnIndexOrThrow("longitude")
            );

            String name = cursor.getString(
                    cursor.getColumnIndexOrThrow("name")
            );
            checkUserLocation();

            GeoPoint point = new GeoPoint(latitude, longitude);

            map.getController().setZoom(19.0);
            map.getController().setCenter(point);

            Toast.makeText(
                    this,
                    "Found: " + name,
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    this,
                    "Location not found",
                    Toast.LENGTH_SHORT
            ).show();
        }

        cursor.close();
    }
    // checking the user location
    private void checkUserLocation() {

        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        boolean gpsEnabled = locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
        );

        if (!gpsEnabled) {

            Toast.makeText(
                    this,
                    "GPS is not available. Indoor positioning will be used.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );

            return;
        }

        Toast.makeText(
                this,
                "GPS is available. Outdoor positioning will be used.",
                Toast.LENGTH_LONG
        ).show();
    }
}