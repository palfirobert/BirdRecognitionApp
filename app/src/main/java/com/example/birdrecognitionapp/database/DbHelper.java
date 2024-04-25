package com.example.birdrecognitionapp.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.example.birdrecognitionapp.api.AzureDbAPI;
import com.example.birdrecognitionapp.api.RetrofitAPI;
import com.example.birdrecognitionapp.dto.DeleteSoundDto;
import com.example.birdrecognitionapp.dto.GetUserSoundsDto;
import com.example.birdrecognitionapp.dto.ObservationSheetDto;
import com.example.birdrecognitionapp.dto.SoundResponse;
import com.example.birdrecognitionapp.interfaces.OnDatabaseChangedListener;
import com.example.birdrecognitionapp.models.LoadingDialogBar;
import com.example.birdrecognitionapp.models.ObservationSheet;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.example.birdrecognitionapp.models.User;
import com.example.birdrecognitionapp.utils.DateUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
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
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_LENGTH = "length";
    public static final String COLUMN_TIME_ADDED = "time_added";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_BLOB_REFERENCE = "blob_reference";
    public static final String COMA_SEP = ",";
    public static final String SQLITE_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            "id VARCHAR(255) PRIMARY KEY" + COMA_SEP +
            COLUMN_NAME + " TEXT" + COMA_SEP +
            COLUMN_PATH + " TEXT" + COMA_SEP +
            COLUMN_LENGTH + " INTEGER" + COMA_SEP +
            COLUMN_TIME_ADDED + " INTEGER" + COMA_SEP +
            COLUMN_USER_ID + " TEXT" + COMA_SEP +
            COLUMN_BLOB_REFERENCE + " TEXT" + ")";

    // Constants for the observation_sheet table
    public static final String TABLE_OBSERVATION_SHEET = "observation_sheet";
    public static final String COLUMN_OBSERVATION_DATE = "observation_date";
    public static final String COLUMN_SPECIES = "species";
    public static final String COLUMN_NUMBER = "number";
    public static final String COLUMN_OBSERVER = "observer";
    public static final String COLUMN_UPLOAD_DATE = "upload_date";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_SOUND_ID = "sound_id";
    String SQLITE_CREATE_OBSERVATION_TABLE = "CREATE TABLE " + TABLE_OBSERVATION_SHEET + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT" + COMA_SEP +
            COLUMN_OBSERVATION_DATE + " TIMESTAMP" + COMA_SEP +
            COLUMN_SPECIES + " VARCHAR(255)" + COMA_SEP +
            COLUMN_NUMBER + " INT" + COMA_SEP +
            COLUMN_OBSERVER + " VARCHAR(255)" + COMA_SEP +
            COLUMN_UPLOAD_DATE + " TIMESTAMP" + COMA_SEP +
            COLUMN_LOCATION + " VARCHAR(255)" + COMA_SEP +
            COLUMN_USER_ID + " VARCHAR(255)" + COMA_SEP +
            COLUMN_SOUND_ID + " VARCHAR(255)" + COMA_SEP +
            "UNIQUE(sound_id)" + COMA_SEP +
            "FOREIGN KEY(" + COLUMN_SOUND_ID + ") REFERENCES saved_recording_table(id) ON DELETE SET NULL" +
            ")";

    private static OnDatabaseChangedListener onDatabaseChangedListener;
    public static boolean firstLogin = true;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    String filePath = Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/";
    User user = new User();
    static Map<String, Long> creationDateOfSounds = new HashMap<>();
    LoadingDialogBar loadingDialogBar;
    List<ObservationSheetDto> observationSheets=new ArrayList<>();

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQLITE_CREATE_TABLE);
        sqLiteDatabase.execSQL(SQLITE_CREATE_OBSERVATION_TABLE);
    }

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        this.loadingDialogBar = new LoadingDialogBar(context);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_OBSERVATION_SHEET);
    }

    public boolean addRecording(RecordingItem recordingItem) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(COLUMN_ID, recordingItem.getId());
            contentValues.put(COLUMN_NAME, recordingItem.getName());
            contentValues.put(COLUMN_PATH, recordingItem.getPath());
            contentValues.put(COLUMN_LENGTH, recordingItem.getLength());
            contentValues.put(COLUMN_TIME_ADDED, recordingItem.getTime_added());
            contentValues.put(COLUMN_USER_ID, recordingItem.getUser_id());
            contentValues.put(COLUMN_BLOB_REFERENCE, recordingItem.getBlob_reference());
            db.insert(TABLE_NAME, null, contentValues);
            if (onDatabaseChangedListener != null)
                onDatabaseChangedListener.onNewDatabaseEntryAdded(recordingItem);
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
            if (file.exists()) {
                file.delete();
            }
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

    // Method to add an observation to the database
    public boolean addObservation(ObservationSheet observationItem) {
        SQLiteDatabase db = this.getWritableDatabase();

        // First, delete existing observation sheets with the same sound_id
        deleteObservationsBySoundId(observationItem.getSoundId(), db);

        // Now insert the new observation sheet
        ContentValues values = new ContentValues();
        values.put(COLUMN_OBSERVATION_DATE, observationItem.getObservationDate());
        values.put(COLUMN_SPECIES, observationItem.getSpecies());
        values.put(COLUMN_NUMBER, observationItem.getNumber());
        values.put(COLUMN_OBSERVER, observationItem.getObserver());
        values.put(COLUMN_UPLOAD_DATE, observationItem.getUploadDate());
        values.put(COLUMN_LOCATION, observationItem.getLocation());
        values.put(COLUMN_USER_ID, observationItem.getUserId());
        values.put(COLUMN_SOUND_ID, observationItem.getSoundId());
        long id = db.insert(TABLE_OBSERVATION_SHEET, null, values);
        return id != -1;
    }

    // Method to delete all observations with a specific sound_id
    private void deleteObservationsBySoundId(String soundId, SQLiteDatabase db) {
        db.delete(TABLE_OBSERVATION_SHEET, COLUMN_SOUND_ID + " = ?", new String[]{String.valueOf(soundId)});
    }

    // Method to delete an observation from the database by ID
    public void deleteObservation(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_OBSERVATION_SHEET, "id = ?", new String[]{String.valueOf(id)});
    }

    // Method to fetch all observations from the database
    public ArrayList<ObservationSheet> getAllObservations() {
        ArrayList<ObservationSheet> observations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // Updated query to order by upload_date in descending order (or any other column as needed)
        String query = "SELECT * FROM " + TABLE_OBSERVATION_SHEET;
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String observationDate = cursor.getString(1);
                String species = cursor.getString(2);
                int number = cursor.getInt(3);
                String observer = cursor.getString(4);
                String uploadDate = cursor.getString(5);
                String location = cursor.getString(6);
                String userId = cursor.getString(7);
                ObservationSheet observationItem = new ObservationSheet(observationDate, species, number, observer, uploadDate, location, userId);
                observations.add(observationItem);
            }
            cursor.close();
        }
        return observations;
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
            String blobReference = user.getId() + "/" + file.getName();
            long timeAdded;
            System.out.println(file.getName());
            System.out.println(creationDateOfSounds);
            System.out.println(creationDateOfSounds.get(file.getName()));
            if (creationDateOfSounds.get(file.getName()) != null) {
                timeAdded = creationDateOfSounds.get(file.getName()) * 1000;
            } else
                timeAdded = file.lastModified();
            String absolutePath = filePath + file.getName();
            String sound_id=user.getId()+"-"+file.getName().replaceAll("[^\\d]", "");
            RecordingItem newRecording = new RecordingItem(file.getName(), absolutePath, (int) length, timeAdded, user.getId(), blobReference,sound_id);
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

    public void addSoundToDb(RecordingItem recordingItem) {

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

    public void fetchAndPopulateUserSounds(String userId) {
        if (firstLogin) {
            loadingDialogBar.showDialog("Fetching sounds...");
            getCreationDateOfSounds();
            firstLogin = false;
        }
    }

    public void downloadUserSounds(GetUserSoundsDto userSoundsDto) {
        // Set your desired timeout in seconds
        int timeoutInSeconds = 30;

        // Create an OkHttpClient with the desired timeout
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com") // Adjust the base URL as necessary
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        AzureDbAPI service = retrofit.create(AzureDbAPI.class);
        Call<ResponseBody> call = service.downloadUserSounds(userSoundsDto);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Write the ZIP file to storage, then unzip it
                    try {
                        // Create a temporary file for the ZIP
                        File zipFile = File.createTempFile("sounds", ".zip", context.getExternalFilesDir(null));
                        try (FileOutputStream fos = new FileOutputStream(zipFile)) {
                            fos.write(response.body().bytes());
                        }

                        // Unzip the file to the desired directory
                        unzip(zipFile.getPath(), filePath);

                        // Optionally, delete the ZIP file after extraction
                        zipFile.delete();

                        Toast.makeText(context, "Successfully fetched sounds!", Toast.LENGTH_SHORT).show();
                        System.out.println("++++++++++++++++++++++++++++++++++++");
                        getObservationsOfUser(user.getId());

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("DownloadError", "Server contacted but unable to retrieve content");
                    getObservationsOfUser(user.getId());
                    Toast.makeText(context, "You don't have any sounds..", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });

    }

    public void unzip(String zipFilePath, String destinationDirectory) throws IOException {
        File destDir = new File(destinationDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destinationDirectory + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    // If the entry is a file, extracts it
                    extractFile(zipIn, filePath);
                } else {
                    // If the entry is a directory, make the directory
                    File dir = new File(filePath);
                    dir.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] bytesIn = new byte[4096];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    public void clearDirectory(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteRecording("/storage/emulated/0/soundrecordings/" + file.getName());
                }
            }
        }
    }

    public void deleteSoundFromBlob(DeleteSoundDto soundDto) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com") // Replace with your actual URL
                .addConverterFactory(GsonConverterFactory.create()) // Assuming you're using Gson
                .build();

        AzureDbAPI apiInterface = retrofit.create(AzureDbAPI.class);

        Call<String> call = apiInterface.deleteSound(soundDto);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    // Handle success
                    String responseBody = response.body();
                    System.out.println("Response: " + responseBody);
                } else {
                    // Handle request errors depending on status code
                    System.out.println("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                // Handle failure, e.g., network error, parsing error
                t.printStackTrace();
            }
        });

    }

    private void getCreationDateOfSounds() {
        // Set your desired timeout in seconds
        int timeoutInSeconds = 30;

        // Create an OkHttpClient with the desired timeout
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com") // Replace with your actual base URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        AzureDbAPI service = retrofit.create(AzureDbAPI.class);

        // Assuming you have a GetUserSoundsDto instance named userSoundsDto
        Call<Map<String, Long>> call = service.getCreationDateOfSounds(user.getId());

        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(Call<Map<String, Long>> call, Response<Map<String, Long>> response) {
                if (response.isSuccessful()) {
                    // Here you have your hashmap with sound name as key and timestamp as value
                    creationDateOfSounds = response.body();
                    System.out.println("-----------------------------");
                    downloadUserSounds(new GetUserSoundsDto(user.getId()));

                } else {
                    Log.e("APIError", "Server contacted but unable to retrieve content");
                }
            }

            @Override
            public void onFailure(Call<Map<String, Long>> call, Throwable t) {
                Log.e("APIError", "Network error", t);
            }
        });

    }

    public String getSoundIdByName(String audioName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(
                    TABLE_NAME,                // The table to query
                    new String[]{"id"},        // The columns to return
                    COLUMN_NAME + " = ?",      // The columns for the WHERE clause
                    new String[]{audioName},   // The values for the WHERE clause
                    null,                      // don't group the rows
                    null,                      // don't filter by row groups
                    null                       // The sort order
            );
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("id"));
            }
        } catch (Exception e) {
            Log.e("DbHelper", "Error while trying to get sound ID by name", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null; // Return null if no ID is found or an error occurs
    }

    public void insertObservationSheet(ObservationSheetDto observationSheetDto){
        int timeoutInSeconds = 30;
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/") // Replace with your actual server URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        AzureDbAPI service = retrofit.create(AzureDbAPI.class);
        Call<ResponseBody> call = service.insertObservation(observationSheetDto);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Handle successful response
                    Toast.makeText(context, "Observation inserted successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    // Handle unsuccessful response, like 400 or 500 status codes
                    Log.e("APIError", "Server contacted but failed to insert data");
                    Toast.makeText(context, "Failed to insert observation data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Handle total failure to connect to server
                t.printStackTrace();
                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void getObservationsOfUser(String userId)
    {   deleteAllObservations();
        int timeoutInSeconds = 30;
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/") // Replace with your actual server URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        AzureDbAPI service = retrofit.create(AzureDbAPI.class);
        Call<List<ObservationSheetDto>> call = service.getObservationsByUserId(userId);
        call.enqueue(new Callback<List<ObservationSheetDto>>() {
            @Override
            public void onResponse(Call<List<ObservationSheetDto>> call, Response<List<ObservationSheetDto>> response) {
                if (response.isSuccessful()) {
                    observationSheets = response.body();
                    for(ObservationSheetDto sheet:observationSheets){
                        addObservation(new ObservationSheet(sheet.getObservationDate(),sheet.getSpecies(),sheet.getNumber(),sheet.getObserver(),sheet.getUploadDate(),sheet.getLocation(),sheet.getUserId(),sheet.getSoundId()));
                    }
                    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                    loadingDialogBar.hideDialog();
                } else {
                    // Handle possible errors, such as a 404 or 500
                    System.err.println("Server error: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<ObservationSheetDto>> call, Throwable t) {
                // Handle the case where the call failed, e.g., no internet connection
                t.printStackTrace();
                System.err.println("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Deletes all entries from the observation_sheet table.
     */
    public void deleteAllObservations() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            // Execute SQL statement to delete all rows in the observation_sheet table
            db.execSQL("DELETE FROM " + TABLE_OBSERVATION_SHEET);

            // Optionally, you can reset the SQLITE_SEQUENCE table if you want to reset the autoincrement primary key
            db.execSQL("UPDATE sqlite_sequence SET seq = 0 WHERE name = '" + TABLE_OBSERVATION_SHEET + "'");

        } catch (Exception e) {
            Log.e("DbHelper", "Error while trying to delete all observations", e);
        } finally {
            db.close(); // Close the database connection
        }
    }

}
