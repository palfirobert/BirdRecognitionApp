package com.example.birdrecognitionapp.services;


import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.birdrecognitionapp.api.RetrofitAPI;
import com.example.birdrecognitionapp.database.DbHelper;
import com.example.birdrecognitionapp.dto.SoundPredictionResponse;
import com.example.birdrecognitionapp.models.ObservationSheet;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.example.birdrecognitionapp.models.User;
import com.example.birdrecognitionapp.models.UserDetails;
import com.example.birdrecognitionapp.utils.CompressionUtils;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordingService extends Service {
    long startingTimeMillis = 0;
    long elapsedTimeMillis = 0;
    private static final int MAX_RETRIES = 3; // Max number of retries
    private int retryCount = 0; // Current retry count
    String ts;
    File file;
    DbHelper dbHelper;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;
    String filePath;
    String fileName;
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    private boolean useLocation;
    private User user=new User();
    private double lon, lat;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DbHelper(getApplicationContext());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fetchLocation();
        useLocation = intent.getBooleanExtra("useLocation", false); // Default to false if not found
        startRecording();
        return START_STICKY;
    }

    private void startRecording() {

        // Check if the permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Set the timestamp
            ts = Long.toString(System.currentTimeMillis() / 1000);
            file = new File(Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/");
            file.mkdirs();
            // Initialize the AudioRecord if permission is granted
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            audioRecord.startRecording();
            isRecording = true;
            recordingThread = new Thread(new Runnable() {
                public void run() {
                    writeAudioDataToFile();
                }
            }, "AudioRecorder Thread");
            recordingThread.start();
            startingTimeMillis = System.currentTimeMillis();
        } else {
            Toast.makeText(this, "Recording permission not granted", Toast.LENGTH_SHORT).show();
        }
    }


    private void writeAudioDataToFile() {
        this.filePath = Environment.getExternalStorageDirectory().getPath() + "/soundrecordings" + "/audio" + ts + ".wav";
        this.fileName = "audio" + ts + ".wav";
        FileOutputStream os = null;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = SAMPLE_RATE;
        int channels = 2;
        long byteRate = 16 * SAMPLE_RATE * channels / 8;

        byte data[] = new byte[bufferSize];
        try {
            os = new FileOutputStream(filePath);
            while (isRecording) {
                int read = audioRecord.read(data, 0, bufferSize);
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    os.write(data, 0, read);
                }
            }
            os.close();

            totalAudioLen = new File(filePath).length() - 44; // Subtract WAV header size
            totalDataLen = totalAudioLen + 36;

            addWavHeader(filePath, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.GPS_PROVIDER;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            if (lastKnownLocation != null) {
                lat = lastKnownLocation.getLatitude();
                lon = lastKnownLocation.getLongitude();
            }
        }
    }

    private void addWavHeader(String filePath, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];

        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
        randomAccessFile.seek(0);

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        randomAccessFile.write(header, 0, 44);
        randomAccessFile.close();
    }

    @Override
    public void onDestroy() {
        if (isRecording) {
            stopRecording();
        }

        String mp3FilePath = Environment.getExternalStorageDirectory().getPath() + "/soundrecordings" + "/audio" + ts + ".wav";
        Path path = Paths.get(mp3FilePath);
        try {
            byte[] fileContent = Files.readAllBytes(path);

            // flag the recording activity that the recording stopped and to show la loading dialog
            Intent intent = new Intent("RECORDING_STOPPED");
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            if(useLocation) {
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                String locationProvider = LocationManager.GPS_PROVIDER;

                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
                    if (lastKnownLocation != null) {
                        lat = lastKnownLocation.getLatitude();
                        lon = lastKnownLocation.getLongitude();
                    }
                }
                postData(Base64.getEncoder().encodeToString(fileContent), useLocation, Optional.of(lat), Optional.of(lon),Optional.empty(),true);
            }else{
                postData(Base64.getEncoder().encodeToString(fileContent), useLocation, Optional.empty(), Optional.empty(),Optional.empty(),true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void stopRecording() {
        if (audioRecord != null) {
            elapsedTimeMillis = (System.currentTimeMillis() - startingTimeMillis);
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            recordingThread = null;
            String sound_id=user.getId()+"-"+fileName.replaceAll("[^\\d]", "");
            System.out.println(fileName);
            ObservationSheet.setAudioFileName(fileName);
            RecordingItem recordingItem = new RecordingItem(fileName, filePath, elapsedTimeMillis, System.currentTimeMillis(),user.getId(),"sounds/"+user.getId()+"/"+this.fileName,sound_id);
            ObservationSheet.setObservationDate(String.valueOf(recordingItem.getTime_added()));
            ObservationSheet.setSoundId(sound_id);
            dbHelper.addRecording(recordingItem);
            dbHelper.addSoundToDb(recordingItem);
        }

        // Toast.makeText(getApplicationContext(), "Recording saved " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
    }


    public void postData(String soundInBase64, boolean useLocation, Optional<Double> latitude, Optional<Double> longitude,Optional<Long>timestamp,Boolean newRecording) {
        if(timestamp.isPresent()) {
            ts = timestamp.get().toString();
            this.fileName = "audio" + ts + ".wav";
        }

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

        if (!useLocation) {// Create a Retrofit instance with the custom OkHttpClient
            HashMap<String, Object> parameters = new HashMap<>();
            parameters.put("sound_data", CompressionUtils.compressString(soundInBase64));
            parameters.put("user_id", user.getId());
            parameters.put("audio_name", this.fileName);
            parameters.put("is_new_recording",newRecording);

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://palfirobert.pythonanywhere.com") // sau http://10.0.2.2:8000/  sau palfirobert.pythonanywhere.com
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)  // Set the custom OkHttpClient
                    .build();

            // Create a RetrofitAPI instance
            RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);

            // Create a Call object for the API request
            Call<List<SoundPredictionResponse>> call = retrofitAPI.sendDataForPredictionWithoutLocation("Token " + UserDetails.getToken(), parameters);

            // Enqueue the API call
            call.enqueue(new Callback<List<SoundPredictionResponse>>() {
                @Override
                public void onResponse(Call<List<SoundPredictionResponse>> call, Response<List<SoundPredictionResponse>> response) {
                    // Handle the successful response
                    retryCount = 0; // Reset retry count
                    List<SoundPredictionResponse> responseFromAPI = response.body();
                    //assert responseFromAPI != null;
                    Collections.sort(responseFromAPI);
                    for (SoundPredictionResponse sound : responseFromAPI) {
                        System.out.println(sound.toString());
                    }
                    // Broadcast the list to the fragment
                    sendBroadcast(responseFromAPI);
                }

                @Override
                public void onFailure(Call<List<SoundPredictionResponse>> call, Throwable t) {
                    if (retryCount < MAX_RETRIES) {
                        retryCount++;
                        System.out.println("Retrying... Attempt: " + retryCount);
                        postData(soundInBase64, useLocation, Optional.empty(), Optional.empty(),Optional.empty(),newRecording); // Retry the call
                    } else {
                        // Handle the failure after exceeding retry count
                        t.printStackTrace();
                        retryCount = 0; // Reset retry count
                    }
                }
            });
        } else {
            if (latitude.isPresent() && longitude.isPresent()) {
                this.lat=latitude.get();
                this.lon=longitude.get();
                HashMap<String, Object> parameters = new HashMap<>();
                parameters.put("user_id", user.getId());
                parameters.put("audio_name", this.fileName);
                parameters.put("sound_data", CompressionUtils.compressString(soundInBase64));
                parameters.put("lon", lon);
                parameters.put("lat", lat);
                parameters.put("is_new_recording",newRecording);

                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl("http://palfirobert.pythonanywhere.com") // sau http://10.0.2.2:8000/  sau palfirobert.pythonanywhere.com
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(okHttpClient)  // Set the custom OkHttpClient
                        .build();

                // Create a RetrofitAPI instance
                RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);

                // Create a Call object for the API request
                Call<List<SoundPredictionResponse>> call = retrofitAPI.sendDataForPredictionWithLocation("Token " + UserDetails.getToken(),parameters);

                // Enqueue the API call
                call.enqueue(new Callback<List<SoundPredictionResponse>>() {
                    @Override
                    public void onResponse(Call<List<SoundPredictionResponse>> call, Response<List<SoundPredictionResponse>> response) {
                        // Handle the successful response
                        retryCount = 0; // Reset retry count
                        List<SoundPredictionResponse> responseFromAPI = response.body();
                        //assert responseFromAPI != null;
                        Collections.sort(responseFromAPI);
                        for (SoundPredictionResponse sound : responseFromAPI) {
                            System.out.println(sound.toString());
                        }
                        // Broadcast the list to the fragment
                        sendBroadcast(responseFromAPI);
                    }

                    @Override
                    public void onFailure(Call<List<SoundPredictionResponse>> call, Throwable t) {
                        if (retryCount < MAX_RETRIES) {
                            retryCount++;
                            System.out.println("Retrying... Attempt: " + retryCount);
                            postData(soundInBase64, useLocation, latitude, longitude,Optional.empty(),newRecording); // Retry the call
                        } else {
                            // Handle the failure after exceeding retry count
                            t.printStackTrace();
                            retryCount = 0; // Reset retry count
                        }
                    }
                });
            }

        }
    }


    private void sendBroadcast(List<SoundPredictionResponse> responseList) {
        Intent intent = new Intent("send-predictions-to-record-fragment");
        ArrayList<Parcelable> parcelableList = new ArrayList(responseList);
        intent.putParcelableArrayListExtra("predictionList", parcelableList);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}
