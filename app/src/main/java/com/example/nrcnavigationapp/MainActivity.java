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
import org.osmdroid.views.overlay.Polyline;
import android.graphics.Color;

// importing GPS access libraries
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MapView map;

    private EditText searchLocation;
    private DatabaseHelper databaseHelper;
    private Marker userLocationMarker;

    private TextView gpsInfo;
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
        gpsInfo = findViewById(R.id.gpsInfo);
        searchLocation = findViewById(R.id.searchLocation);
        searchLocation.setOnEditorActionListener((v, actionId, event) -> {

            String searchText = searchLocation.getText().toString().trim();

            if (!searchText.isEmpty()) {
                searchLocation(searchText);
            }

            return true;
        });



        // Initialize the database FIRST
        databaseHelper = new DatabaseHelper(this);
        databaseHelper.getWritableDatabase();

        // Read and combine all three CSV files
        Map<String, DatabaseHelper.LocationRecord> uniqueLocations =
                new LinkedHashMap<>();

        String[] csvFiles = {
                "NRC coordinates(1).csv",
                "Sample data.csv",
                "corridors.csv"
        };

        for (String fileName : csvFiles) {
            List<DatabaseHelper.LocationRecord> fileLocations =
                    readLocationsFromCSV(fileName);

            for (DatabaseHelper.LocationRecord location : fileLocations) {
                String key = location.name.trim().toLowerCase(
                        java.util.Locale.ROOT);

                uniqueLocations.put(key, location);
            }
        }

        List<DatabaseHelper.LocationRecord> allLocations =
                new ArrayList<>(uniqueLocations.values());

        // Refresh the database once
        databaseHelper.syncLocations(allLocations);



        Toast.makeText(
                this,
                "Locations refreshed: " + allLocations.size(),
                Toast.LENGTH_LONG
        ).show();






        map.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.OpenTopo);

        map.setMultiTouchControls(true);

        org.osmdroid.api.IMapController mapController = map.getController();
        mapController.setZoom(18.0);

        GeoPoint startPoint = new GeoPoint(-14.02, 33.67);

        mapController.setCenter(startPoint);
        showLocationsOnMap();
        drawWalkingRoute(
                "Main_gate",
                "Junction-1",
                "Junction-2",
                "TurnPoint",
                "admin-turn-point",
                "TurnPoint-2"
        );


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

    private void importLocationsFromCSV(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(is));

            String line;

            // Skip the header row
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",");

                if (columns.length < 3) {
                    continue;
                }

                try {
                    String name = columns[0]
                            .replace("\"", "").trim();

                    if (name.isEmpty()) {
                        continue;
                    }

                    String latitudeText =
                            columns[1].replace("\"", "").trim();

                    String longitudeText =
                            columns[2].replace("\"", "").trim();

                   // Skipping locations that don't have coordinates yet
                    if (latitudeText.isEmpty() || longitudeText.isEmpty()) {
                        continue;
                    }

                    double latitude = Double.parseDouble(latitudeText);
                    double longitude = Double.parseDouble(longitudeText);

                    // Check whether this location already exists
                    Cursor checkCursor =
                            databaseHelper.getReadableDatabase()
                                    .rawQuery(
                                            "SELECT id FROM locations WHERE name = ? LIMIT 1",
                                            new String[]{name}
                                    );

                    boolean exists = checkCursor.moveToFirst();
                    checkCursor.close();

                    if (!exists) {
                        databaseHelper.insertLocation(
                                name,
                                "Location",
                                latitude,
                                longitude
                        );
                    }

                } catch (NumberFormatException e) {
                    android.util.Log.e(
                            "CSV_IMPORT",
                            "Invalid coordinates in " + fileName
                                    + ": " + line,
                            e
                    );
                }
            }

            reader.close();

        } catch (IOException e) {
            android.util.Log.e(
                    "CSV_IMPORT",
                    "Could not read " + fileName,
                    e
            );

            Toast.makeText(
                    this,
                    "Could not import " + fileName,
                    Toast.LENGTH_LONG
            ).show();
        }


    }

    private List<DatabaseHelper.LocationRecord> readLocationsFromCSV(
            String fileName) {

        List<DatabaseHelper.LocationRecord> locations =
                new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(fileName)))) {

            // Skip the header row
            reader.readLine();

            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",");

                if (columns.length < 3) {
                    continue;
                }

                try {
                    String name = columns[0]
                            .replace("\"", "").trim();

                    double latitude = Double.parseDouble(
                            columns[1].replace("\"", "").trim());

                    double longitude = Double.parseDouble(
                            columns[2].replace("\"", "").trim());

                    if (name.isEmpty()
                            || latitude < -90 || latitude > 90
                            || longitude < -180 || longitude > 180) {
                        continue;
                    }

                    locations.add(
                            new DatabaseHelper.LocationRecord(
                                    name,
                                    "Location",
                                    latitude,
                                    longitude
                            )
                    );

                } catch (NumberFormatException e) {
                    android.util.Log.e(
                            "CSV_IMPORT",
                            "Invalid row in " + fileName + ": " + line,
                            e
                    );
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not read CSV file: " + fileName, e);
        }

        return locations;
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
    // creating the campuss mapping

    private void drawWalkingRoute(String... locationNames) {

        Polyline walkingPath = new Polyline();

        List<GeoPoint> routePoints = new ArrayList<>();

        for (String locationName : locationNames) {

            Cursor cursor = databaseHelper.getReadableDatabase().rawQuery(
                    "SELECT latitude, longitude FROM locations WHERE LOWER(name) = LOWER(?) LIMIT 1",
                    new String[]{locationName}
            );

            if (cursor.moveToFirst()) {

                double latitude = cursor.getDouble(0);
                double longitude = cursor.getDouble(1);

                routePoints.add(new GeoPoint(latitude, longitude));

            } else {

                Toast.makeText(
                        this,
                        "Location not found: " + locationName,
                        Toast.LENGTH_LONG
                ).show();

                cursor.close();
                return;
            }

            cursor.close();
        }

        walkingPath.setPoints(routePoints);
        walkingPath.setColor(Color.BLUE);
        walkingPath.setWidth(8f);

        map.getOverlays().add(walkingPath);

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
                "GPS is available.Getting your location...",
                Toast.LENGTH_LONG
        ).show();
        getCurrentGPSLocation();
    }
    private void getCurrentGPSLocation() {
        // testing
        android.util.Log.d(
                "NRC_GPS",
                "getCurrentGPSLocation() was called"
        );

        LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(
                    this,
                    "Location permission is required.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }
        //TESTTING
        android.util.Log.d(
                "NRC_GPS",
                "Requesting GPS location..."
        );

        locationManager.requestSingleUpdate(

                LocationManager.GPS_PROVIDER,
                new LocationListener() {

                    @Override
                    public void onLocationChanged(Location location) {

                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        float accuracy = location.getAccuracy();

                        android.util.Log.d(
                                "NRC_GPS",
                                "Current location: Latitude = "
                                        + latitude
                                        + ", Longitude = "
                                        + longitude
                                        + ", Accuracy = "
                                        + accuracy
                                        + " meters"
                        );
                        gpsInfo.setText(
                                "GPS Location\n"
                                        + "Latitude: " + latitude + "\n"
                                        + "Longitude: " + longitude + "\n"
                                        + "Accuracy: " + accuracy + " m"
                        );
                        GeoPoint userLocation =
                                new GeoPoint(latitude, longitude);

                       // Creating  user's location marker
                        if (userLocationMarker == null) {

                            userLocationMarker = new Marker(map);

                            userLocationMarker.setTitle("You are here");

                            map.getOverlays().add(userLocationMarker);
                        }

                        // Updating the marker position
                        userLocationMarker.setPosition(userLocation);

                        // Move the map to the user's location
                        map.getController().setZoom(19.0);
                        map.getController().setCenter(userLocation);

                        map.invalidate();

                        Toast.makeText(
                                MainActivity.this,
                                "Your location found.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }

                    @Override
                    public void onStatusChanged(
                            String provider,
                            int status,
                            android.os.Bundle extras) {
                    }
                },
                null
        );
    }

}