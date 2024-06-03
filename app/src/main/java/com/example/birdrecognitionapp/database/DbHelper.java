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
import com.example.birdrecognitionapp.models.UserDetails;
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

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
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


    public boolean addObservation(ObservationSheetDto observationItem) {
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
    public void deleteObservation(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_OBSERVATION_SHEET, COLUMN_SOUND_ID + " = ?", new String[]{String.valueOf(id)});
    }
    /**
     * Deletes an observation from the observation sheet table based on user ID, location, and upload date.
     *
     * @param userId The user ID associated with the observation.
     * @param location The location of the observation.
     * @param uploadDate The upload date of the observation.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean deleteObservationByUserLocationDate(String userId, String location, String uploadDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            int deletedRows = db.delete(TABLE_OBSERVATION_SHEET,
                    COLUMN_USER_ID + " = ? AND " + COLUMN_LOCATION + " = ? AND " + COLUMN_UPLOAD_DATE + " = ?",
                    new String[]{userId, location, uploadDate});
            if (deletedRows > 0) {
                if (onDatabaseChangedListener != null) {
                    onDatabaseChangedListener.onDatabaseEntryDeleted();
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e("DbHelper", "Error while trying to delete observation", e);
            return false;
        } finally {
            db.close();
        }
    }


    // Method to fetch all observations from the database
    public ArrayList<ObservationSheetDto> getAllObservations() {
        ArrayList<ObservationSheetDto> observations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_OBSERVATION_SHEET+ " ORDER BY observation_date DESC";
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
                String sound_id=cursor.getString(8);
                ObservationSheetDto observationItem = new ObservationSheetDto(observationDate, species, number, observer, uploadDate, location, userId,sound_id);
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
                    boolean existsInDatabase = false;
                    for (RecordingItem item : allRecordings) {
                        if (item.getPath().equals(absolutePath)) {
                            existsInDatabase = true;
                            break;
                        }
                    }

                    if (!existsInDatabase && !file.getName().startsWith(".")) {
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
        // Set your desired timeout in seconds
        int timeoutInSeconds = 30;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Token " + UserDetails.getToken())
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        // Build the Retrofit instance
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com") // Adjust the base URL as necessary
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        // Create an instance of the RetrofitAPI interface
        AzureDbAPI retrofitAPI = retrofit.create(AzureDbAPI.class);

        // Create a Call object for the API request
        Call<String> call = retrofitAPI.addSound("Token " + UserDetails.getToken(),recordingItem);

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

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Token " + UserDetails.getToken())
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
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
        Call<ResponseBody> call = service.downloadUserSounds("Token " + UserDetails.getToken(),userSoundsDto);

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
                    System.out.println((new File(Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/").getPath()));
                    getObservationsOfUser(user.getId());
                    Toast.makeText(context, "You don't have any sounds..", Toast.LENGTH_SHORT).show();
                }
                loadingDialogBar.hideDialog();
                loadingDialogBar.showDialog("Fetching observations...");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
            }
        });

    }

    public void deleteObservationSheetFromDb(ObservationSheetDto observationSheetDto){
        // Set your desired timeout in seconds
        int timeoutInSeconds = 30;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Token " + UserDetails.getToken())
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com") // Update this with your actual URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
        AzureDbAPI service = retrofit.create(AzureDbAPI.class);


        Call<Void> call = service.deleteObservationSheet("Token " + UserDetails.getToken(),observationSheetDto);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(context, "Observation deleted successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Failed to delete observation.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(context, "Network error.", Toast.LENGTH_SHORT).show();
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
        ExecutorService executorService=Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            deleteRecording("/storage/emulated/0/soundrecordings/" + file.getName());
                        }
                    }
                }
            }
        });
    }

    public void deleteSoundFromBlob(DeleteSoundDto soundDto) {
        // Set your desired timeout in seconds
        int timeoutInSeconds = 30;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Token " + UserDetails.getToken())
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        AzureDbAPI apiInterface = retrofit.create(AzureDbAPI.class);

        Call<String> call = apiInterface.deleteSound("Token " + UserDetails.getToken(),soundDto);
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

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Token " + UserDetails.getToken())
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
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
        Call<Map<String, Long>> call = service.getCreationDateOfSounds("Token " + UserDetails.getToken(),user.getId());

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
        // Set your desired timeout in seconds
        int timeoutInSeconds = 30;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Token " + UserDetails.getToken())
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com") // Replace with your actual server URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        AzureDbAPI service = retrofit.create(AzureDbAPI.class);
        Call<ResponseBody> call = service.insertObservation("Token " + UserDetails.getToken(),observationSheetDto);

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
        // Set your desired timeout in seconds
        int timeoutInSeconds = 30;

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
                        Request originalRequest = chain.request();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Token " + UserDetails.getToken())
                                .build();
                        return chain.proceed(newRequest);
                    }
                })
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com") // Replace with your actual server URL
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        AzureDbAPI service = retrofit.create(AzureDbAPI.class);
        Call<List<ObservationSheetDto>> call = service.getObservationsByUserId("Token " + UserDetails.getToken(),userId);
        call.enqueue(new Callback<List<ObservationSheetDto>>() {
            @Override
            public void onResponse(Call<List<ObservationSheetDto>> call, Response<List<ObservationSheetDto>> response) {
                if (response.isSuccessful()) {
                    observationSheets = response.body();
                    for(ObservationSheetDto sheet:observationSheets){
                        System.out.println(sheet.getUploadDate());
                        addObservation(new ObservationSheetDto(sheet.getObservationDate(),sheet.getSpecies(),sheet.getNumber(),sheet.getObserver(),sheet.getUploadDate(),sheet.getLocation(),sheet.getUserId(),sheet.getSoundId()));
                    }
                    System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
                } else {
                    // Handle possible errors, such as a 404 or 500
                    System.err.println("Server error: " + response.message());
                }
                loadingDialogBar.hideDialog();
            }

            @Override
            public void onFailure(Call<List<ObservationSheetDto>> call, Throwable t) {
                // Handle the case where the call failed, e.g., no internet connection
                t.printStackTrace();
                System.err.println("Network error: " + t.getMessage());
                loadingDialogBar.hideDialog();
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

    /**
     * Sets the sound_id to null for all observation sheets with the specified sound_id.
     *
     * @param soundId The sound ID of the observation sheets to modify.
     * @return true if the update was successful, false otherwise.
     */
    public boolean nullifySoundIdInObservations(String soundId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.putNull(COLUMN_SOUND_ID);  // Set sound_id to null

        try {
            int rowsUpdated = db.update(TABLE_OBSERVATION_SHEET, values, COLUMN_SOUND_ID + " = ?", new String[]{soundId});
            if (rowsUpdated > 0) {
                return true;  // Return true if one or more rows were updated
            }
            return false;  // Return false if no rows were updated
        } catch (Exception e) {
            Log.e("DbHelper", "Error while trying to update observation sheets", e);
            return false;
        } finally {
            db.close();
        }
    }


}
