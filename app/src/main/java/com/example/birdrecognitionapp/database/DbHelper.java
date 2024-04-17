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
import com.example.birdrecognitionapp.dto.SoundResponse;
import com.example.birdrecognitionapp.interfaces.OnDatabaseChangedListener;
import com.example.birdrecognitionapp.models.LoadingDialogBar;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.example.birdrecognitionapp.models.User;

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
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PATH = "path";
    public static final String COLUMN_LENGTH = "length";
    public static final String COLUMN_TIME_ADDED = "time_added";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_BLOB_REFERENCE = "blob_reference";
    public static final String COMA_SEP = ",";
    public static final String SQLITE_CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT" + COMA_SEP +
            COLUMN_NAME + " TEXT" + COMA_SEP +
            COLUMN_PATH + " TEXT" + COMA_SEP +
            COLUMN_LENGTH + " INTEGER" + COMA_SEP +
            COLUMN_TIME_ADDED + " INTEGER" + COMA_SEP +
            COLUMN_USER_ID + " TEXT" + COMA_SEP +
            COLUMN_BLOB_REFERENCE + " TEXT" + ")";
    private static OnDatabaseChangedListener onDatabaseChangedListener;
    private boolean firstLogin=true;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    String filePath = Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/";
    User user=new User();
    static Map<String, Long> creationDateOfSounds=new HashMap<>();
    LoadingDialogBar loadingDialogBar;
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQLITE_CREATE_TABLE);
    }

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        this.loadingDialogBar = new LoadingDialogBar(context);
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
            String blobReference=user.getId()+"/"+file.getName();
            long timeAdded;
            System.out.println(file.getName());
            System.out.println(creationDateOfSounds);
            System.out.println(creationDateOfSounds.get(file.getName()));
            if(creationDateOfSounds.get(file.getName())!=null) {
                timeAdded = creationDateOfSounds.get(file.getName())*1000;
            }
            else
                timeAdded=file.lastModified();
            String absolutePath = filePath + file.getName();
            // Now you have the length in milliseconds, proceed to add the recording
            RecordingItem newRecording = new RecordingItem(file.getName(), absolutePath, (int) length, timeAdded,user.getId(),blobReference);
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
                .baseUrl("http://palfirobert.pythonanywhere.com") // Adjust the base URL as necessary
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
        if(firstLogin) {
            getCreationDateOfSounds();
            firstLogin=false;
        }
    }
    public void downloadUserSounds(GetUserSoundsDto userSoundsDto){
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
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("DownloadError", "Server contacted but unable to retrieve content");
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
                    deleteRecording("/storage/emulated/0/soundrecordings/"+file.getName());
                }
            }
        }
    }

    public void deleteSoundFromBlob(DeleteSoundDto soundDto)
    {
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

    private void getCreationDateOfSounds(){
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
                    creationDateOfSounds= response.body();
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


}
