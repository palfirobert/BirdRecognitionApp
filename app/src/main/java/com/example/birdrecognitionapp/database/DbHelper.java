package com.example.birdrecognitionapp.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadataRetriever;
import android.os.Environment;

import com.example.birdrecognitionapp.api.AzureDbAPI;
import com.example.birdrecognitionapp.api.RetrofitAPI;
import com.example.birdrecognitionapp.interfaces.OnDatabaseChangedListener;
import com.example.birdrecognitionapp.models.RecordingItem;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class DbHelper extends SQLiteOpenHelper {
    private Context context;
    public static final String DATABASE_NAME = "saved_recordings.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "saved_recording_table";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_LENGTH = "length";
    public static final String COLUMN_TIME_ADDED = "time_added";
    public static final String COMA_SEP = ",";
    public static final String SQLITE_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + "id INTEGER PRIMARY KEY" +
            " AUTOINCREMENT" + COMA_SEP +
            COLUMN_NAME + " TEXT" + COMA_SEP +
            COLUMN_PATH + " TEXT" + COMA_SEP +
            COLUMN_LENGTH + " INTEGER" + COMA_SEP +
            COLUMN_TIME_ADDED + " INTEGER " + ")";
    private static OnDatabaseChangedListener onDatabaseChangedListener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    String filePath = Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/";
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQLITE_CREATE_TABLE);
    }

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    public boolean addRecording(RecordingItem recordingItem) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_NAME, recordingItem.getName());
            contentValues.put(COLUMN_PATH, recordingItem.getPath());
            contentValues.put(COLUMN_LENGTH, recordingItem.getLength());
            contentValues.put(COLUMN_TIME_ADDED, recordingItem.getTime_added());
            db.insert(TABLE_NAME, null, contentValues);
            if (onDatabaseChangedListener != null)
                onDatabaseChangedListener.onNewDatabaseEntryAdded(recordingItem);
            addSoundToDb(recordingItem);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Method to delete a recording by its path
    public void deleteRecording(String path) {
        SQLiteDatabase db = getWritableDatabase();
        // Assuming COLUMN_PATH is the unique identifier for the recording
        int deletedRows = db.delete(TABLE_NAME, COLUMN_PATH + " = ?", new String[]{path});
        if (deletedRows > 0) {
            File file = new File(path);
            if (file.exists())
                file.delete(); // Delete the file
            if (onDatabaseChangedListener != null) {
                onDatabaseChangedListener.onDatabaseEntryDeleted();
            }
        }
    }

    public ArrayList<RecordingItem> getAllAudios() {
        ArrayList<RecordingItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Updated query to order by timeAdded in descending order
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY time_added ASC";
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                String path = cursor.getString(2);
                int length = (int) cursor.getLong(3);
                long timeAdded = cursor.getLong(4);
                RecordingItem recordingItem = new RecordingItem(name, path, length, timeAdded);
                list.add(recordingItem);
            }
            cursor.close();
            return list;
        } else {
            return null;
        }
    }



    public static void setOnDatabaseChangedListener(OnDatabaseChangedListener listener) {
        onDatabaseChangedListener = listener;
    }

    /**
     * Method that sync the sql database with the internal storage.
     */
    public void synchronizeDatabaseWithStorage() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<RecordingItem> allRecordings = getAllAudios();
                if (allRecordings == null) return;
                // Use your specific directory for sound recordings
                File directory = new File(filePath);
                if (!directory.exists()) {
                    // Directory doesn't exist, no synchronization needed
                    return;
                }

                File[] files = directory.listFiles();
                if (files != null) {
                    ArrayList<String> filePaths = new ArrayList<>();
                    for (File file : files) {
                        String absolutePath = filePath + file.getName();
                        filePaths.add(absolutePath);
                    }

                    SQLiteDatabase db = getWritableDatabase();
                    for (RecordingItem item : allRecordings) {
                        if (!filePaths.contains(item.getPath())) {
                            // File does not exist, delete entry from database
                            deleteRecordingEntryFromDatabase(item.getPath(), db);
                        }
                    }
                }

                for (File file : files) {
                    String absolutePath = filePath + file.getName();
                    // Check if this file's path is not in the list of all recordings' paths
                    boolean existsInDatabase = false;
                    for (RecordingItem item : allRecordings) {
                        if (item.getPath().equals(absolutePath)) {
                            existsInDatabase = true;
                            break;
                        }
                    }

                    if (!existsInDatabase && !file.getName().startsWith(".")) {
                        // This file is not in the database, so add it
                        addRecordingForNewFile(file);
                    }
                }
            }
        });

    }


    private void deleteRecordingEntryFromDatabase(String path, SQLiteDatabase db) {
        db.delete(TABLE_NAME, COLUMN_PATH + " = ?", new String[]{path});
    }

    private void addRecordingForNewFile(File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long length = Long.parseLong(durationStr); // Duration in milliseconds

            long timeAdded = file.lastModified(); // Use file's last modified time as time added
            String absolutePath = filePath + file.getName();
            // Now you have the length in milliseconds, proceed to add the recording
            RecordingItem newRecording = new RecordingItem(file.getName(), absolutePath, (int) length, timeAdded);
            addRecording(newRecording);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
    }

    public void shutdownExecutorService() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    public void addSoundToDb(RecordingItem recordingItem)
    {

        // Build the Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/") // Adjust the base URL as necessary
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create an instance of the RetrofitAPI interface
        AzureDbAPI retrofitAPI = retrofit.create(AzureDbAPI.class);

        // Create a Call object for the API request
        Call<String> call = retrofitAPI.addSound(recordingItem);

        // Enqueue the call
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                // Handle success
                if (response.isSuccessful()) {
                    // Extract the response body
                    String responseBody = response.body();
                    System.out.println("Response from /addsound: " + responseBody);
                } else {
                    // Handle request errors depending on response codes
                    System.out.println("Request error code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Handle failure to execute the call
                t.printStackTrace();
            }
        });

    }

}
