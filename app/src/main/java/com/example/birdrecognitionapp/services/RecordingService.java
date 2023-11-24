package com.example.birdrecognitionapp.services;



import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.birdrecognitionapp.activities.MainActivity;
import com.example.birdrecognitionapp.api.RetrofitAPI;
import com.example.birdrecognitionapp.dto.SoundPredictionResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.Base64;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordingService extends Service {

    MediaRecorder mediaRecorder;
    long startingTimeMillis=0;
    long elapsedTimeMillis=0;

    String ts;
    File file;
    String filename;
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("SA INTRAT");
        startRecording();

        return START_STICKY;
    }

    private void startRecording() {

            Long timeStampLong = System.currentTimeMillis() / 1000;
            ts = timeStampLong.toString();
            file = new File(Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/");
            file.mkdirs();
            //System.out.println(file.exists());
            //System.out.println(file.getPath());
            filename = file.getPath();
            filename += "/audio" + ts + ".3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setOutputFile(filename);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioChannels(1);
            mediaRecorder.setAudioEncodingBitRate(128);
            mediaRecorder.setAudioSamplingRate(44100);
            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
                startingTimeMillis = System.currentTimeMillis();

            } catch (IOException e) {
                e.printStackTrace();

            }

    }

    @Override
    public void onDestroy() {
        if (mediaRecorder!=null)
        {
            stopRecording();
        }
        System.out.println("S-A DAT DESTROY");
        String mp3FilePath=Environment.getExternalStorageDirectory().getPath() + "/soundrecordings"+"/audio" + ts + ".3gp";
        File mp3File=new File(mp3FilePath);
        Path path = Paths.get(mp3FilePath);
        try {
            byte[] fileContent = Files.readAllBytes(path);
            System.out.println(Base64.getEncoder().encodeToString(fileContent));
            postData(Base64.getEncoder().encodeToString(fileContent));
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
    private void stopRecording()
    {
        mediaRecorder.stop();
        elapsedTimeMillis=(System.currentTimeMillis()-startingTimeMillis);
        mediaRecorder.release();
        mediaRecorder=null;
        Toast.makeText(getApplicationContext(), "Recording saved "+file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

        // maybe add to database
    }

    private void postData(String soundInBase64) {

        // on below line we are creating a retrofit
        // builder and passing our base url
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                // as we are sending data in json format so
                // we have to add Gson converter factory
                .addConverterFactory(GsonConverterFactory.create())
                // at last we are building our retrofit builder.
                .build();
        // below line is to create an instance for our retrofit api class.
        RetrofitAPI retrofitAPI = retrofit.create(RetrofitAPI.class);

        // calling a method to create a post and passing our modal class.
        Call<List<SoundPredictionResponse>> call = retrofitAPI.createPost(soundInBase64);

        // on below line we are executing our method.
        call.enqueue(new Callback<List<SoundPredictionResponse>>() {
            @Override
            public void onResponse(Call<List<SoundPredictionResponse>> call, Response<List<SoundPredictionResponse>> response) {
                Toast.makeText(getApplicationContext(), "Data added to API", Toast.LENGTH_SHORT).show();
                List<?> responseFromAPI = response.body();

                System.out.println(responseFromAPI.toString());
            }

            @Override
            public void onFailure(Call<List<SoundPredictionResponse>> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }


}
