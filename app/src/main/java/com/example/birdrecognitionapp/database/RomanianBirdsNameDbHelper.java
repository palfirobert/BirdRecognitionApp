package com.example.birdrecognitionapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;

public class RomanianBirdsNameDbHelper extends SQLiteOpenHelper {
    private Context context;
    private static final String TAG = "RomanianBirdsDbHelper";
    private static final String DATABASE_NAME = "romanian_bird_names.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "romanian_birds";
    private static final String COLUMN_LATIN_NAME = "latin_name";
    private static final String COLUMN_COMMON_NAME = "common_name";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_LATIN_NAME + " TEXT PRIMARY KEY," +
                    COLUMN_COMMON_NAME + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public RomanianBirdsNameDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
        populateDatabaseFromJson(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL(SQL_DELETE_ENTRIES);
    }

    public void populateDatabaseFromJson(SQLiteDatabase db) {
        // Use the passed SQLiteDatabase object instead of getting a new one
        try {
            // Open your JSON file from the assets folder
            InputStream is = context.getAssets().open("birds_names.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            // Convert the buffer into a string
            String json = new String(buffer, "UTF-8");

            // Parse the string into a JSON array
            JSONArray jsonArray = new JSONArray(json);

            // Begin the database transaction
            db.beginTransaction();

            try {
                // Iterate over the JSON array
                for (int i = 0; i < jsonArray.length(); i++) {
                    // Get the JSON object for the current array element
                    JSONObject jsonObject = jsonArray.getJSONObject(i);

                    // Extract the Latin and common names
                    String latinName = jsonObject.getString("latin_name");
                    String commonName = jsonObject.getString("common_name");

                    // Prepare the content values
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_LATIN_NAME, latinName);
                    values.put(COLUMN_COMMON_NAME, commonName);

                    // Insert the values into the table
                    db.insert(TABLE_NAME, null, values);
                }

                // Mark the transaction as successful
                db.setTransactionSuccessful();
            } finally {
                // End the transaction
                db.endTransaction();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getCommonNameByLatinName(String latinName) {
        SQLiteDatabase db = this.getReadableDatabase();
        String commonName = null;

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {COLUMN_COMMON_NAME};

        // Filter results WHERE "latin_name" = 'latinName'
        String selection = COLUMN_LATIN_NAME + " = ?";
        String[] selectionArgs = {latinName};

        Cursor cursor = db.query(
                TABLE_NAME,   // The table to query
                projection,   // The array of columns to return (pass null to get all)
                selection,    // The columns for the WHERE clause
                selectionArgs,// The values for the WHERE clause
                null,         // Don't group the rows
                null,         // Don't filter by row groups
                null          // The sort order
        );

        if (cursor.moveToFirst()) {
            commonName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMON_NAME));
        }
        cursor.close();

        return commonName;
    }

}
