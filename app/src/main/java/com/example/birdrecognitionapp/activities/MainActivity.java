package com.example.birdrecognitionapp.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.adapters.SavedRecordingsAdapter;
import com.example.birdrecognitionapp.adapters.ViewPagerAdapter;
import com.example.birdrecognitionapp.fragments.RecordFragment;
import com.example.birdrecognitionapp.models.MainActivityRecordFragmentSharedModel;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.google.android.material.tabs.TabLayout;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;

import java.util.Objects;

/**
 * So this is the first checkpoint of the app
 * I made the logic to record sound in a WAV format, and to comunicate to a python deployed server
 * that is making predictions
 * The server is deployed on python anywhere and its endpoint is configured in the record service
 * If you want to take from this checkpoint the main topic is the sound recording service that works
 */
public class MainActivity extends AppCompatActivity implements SavedRecordingsAdapter.OnPredictButtonPressListener {


    // constant for storing audio permission
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    TabLayout tabLayout;
    ViewPager2 viewPager2;
    ViewPagerAdapter myViewPagerAdapter;
    MainActivityRecordFragmentSharedModel sharedModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // make the action bar disappear
        try {
            Objects.requireNonNull(getSupportActionBar()).hide();
        } catch (Exception e) {
        }

        tabLayout = findViewById(R.id.tab_layout);
        viewPager2 = findViewById(R.id.view_pager);
        myViewPagerAdapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(myViewPagerAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager2.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                tabLayout.getTabAt(position).select();
            }
        });

        sharedModel = new ViewModelProvider(this).get(MainActivityRecordFragmentSharedModel.class);

        if (!CheckPermissions())
            RequestPermissions();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToReadStorage = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToLocationFine = grantResults.length > 3 && grantResults[3] == PackageManager.PERMISSION_GRANTED;
                    boolean permissionToLocationCoarse = grantResults.length > 4 && grantResults[4] == PackageManager.PERMISSION_GRANTED;

                    if (permissionToRecord && permissionToStore && permissionToReadStorage && permissionToLocationFine && permissionToLocationCoarse) {
                        Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_LONG).show();
                        // Additional logic to handle permission granted case
                    } else {
                        Toast.makeText(getApplicationContext(), "Permissions Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }


    public boolean CheckPermissions() {
        // this method is used to check permission
        int resultStorage = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int resultRecord = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        int resultLocationFine = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        int resultLocationCoarse = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return resultStorage == PackageManager.PERMISSION_GRANTED && resultRecord == PackageManager.PERMISSION_GRANTED &&
                resultLocationFine == PackageManager.PERMISSION_GRANTED && resultLocationCoarse == PackageManager.PERMISSION_GRANTED;
    }


    private void RequestPermissions() {
        // this method is used to request the
        // permission for audio recording and storage.
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_AUDIO_PERMISSION_CODE);
    }

    @Override
    public void switchToFirstTab(RecordingItem recordingItem) {

        viewPager2.setCurrentItem(0);
        sharedModel.predictRecording(recordingItem);
    }
}