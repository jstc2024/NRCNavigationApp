package com.example.nrcnavigationapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static class LocationRecord {
        public String name;
        public String category;
        public double latitude;
        public double longitude;

        public LocationRecord(
                String name,
                String category,
                double latitude,
                double longitude) {
            this.name = name;
            this.category = category;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }


    private static final String DATABASE_NAME = "nrc_navigation.db";
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_LOCATIONS_TABLE =
                "CREATE TABLE locations (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT NOT NULL," +
                        "category TEXT," +
                        "latitude REAL," +
                        "longitude REAL" +
                        ");";

        db.execSQL(CREATE_LOCATIONS_TABLE);

        String CREATE_NAVIGATION_EDGES_TABLE =
                "CREATE TABLE navigation_edges (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "from_location_id INTEGER NOT NULL," +
                        "to_location_id INTEGER NOT NULL," +
                        "distance REAL NOT NULL," +
                        "FOREIGN KEY(from_location_id) REFERENCES locations(id)," +
                        "FOREIGN KEY(to_location_id) REFERENCES locations(id)" +
                        ");";

        db.execSQL(CREATE_NAVIGATION_EDGES_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 2) {

            String CREATE_NAVIGATION_EDGES_TABLE =
                    "CREATE TABLE IF NOT EXISTS navigation_edges (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "from_location_id INTEGER NOT NULL," +
                            "to_location_id INTEGER NOT NULL," +
                            "distance REAL NOT NULL," +
                            "FOREIGN KEY(from_location_id) REFERENCES locations(id)," +
                            "FOREIGN KEY(to_location_id) REFERENCES locations(id)" +
                            ");";

            db.execSQL(CREATE_NAVIGATION_EDGES_TABLE);
        }
    }
    //updating the database using a method
    public long insertLocation(String name,
                               String category,
                               double latitude,
                               double longitude) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("category", category);
        values.put("latitude", latitude);
        values.put("longitude", longitude);

        return db.insert("locations", null, values);
    }
    // saving connections
    public long insertNavigationEdge(
            int fromLocationId,
            int toLocationId,
            double distance) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("from_location_id", fromLocationId);
        values.put("to_location_id", toLocationId);
        values.put("distance", distance);

        return db.insert("navigation_edges", null, values);
    }
    // creating a bidirectinal route
    public long insertBidirectionalEdge(
            int firstLocationId,
            int secondLocationId,
            double distance) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("from_location_id", firstLocationId);
        values.put("to_location_id", secondLocationId);
        values.put("distance", distance);

        return db.insert("navigation_edges", null, values);
    }

    //uploading csv file containing coordinates in the database
    public Cursor getAllLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM locations", null);
    }
    // getting edges of connected points
    public Cursor getAllNavigationEdges() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM navigation_edges",
                null
        );
    }

    public void clearLocations() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("locations", null, null);
    }

    // checking that the location is unique
    public boolean hasLocations() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM locations",
                null
        );

        cursor.moveToFirst();

        int count = cursor.getInt(0);

        cursor.close();

        return count > 0;
    }

    public void syncLocations(
            java.util.List<LocationRecord> locations) {

        SQLiteDatabase db = this.getWritableDatabase();

        db.beginTransaction();

        try {
            // Remove navigation connections before removing locations
            db.delete("navigation_edges", null, null);

            // Remove old locations
            db.delete("locations", null, null);

            // Insert the latest locations from the CSV files
            for (LocationRecord location : locations) {
                ContentValues values = new ContentValues();

                values.put("name", location.name);
                values.put("category", location.category);
                values.put("latitude", location.latitude);
                values.put("longitude", location.longitude);

                db.insertOrThrow("locations", null, values);
            }

            // Commit only if all operations succeed
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
        }
    }

}