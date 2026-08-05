package com.example.nrcnavigationapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "nrc_navigation.db";
    private static final int DATABASE_VERSION = 1;

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS locations");

        onCreate(db);
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
    //uploading csv file containing coordinates in the database
    public Cursor getAllLocations() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM locations", null);
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
}