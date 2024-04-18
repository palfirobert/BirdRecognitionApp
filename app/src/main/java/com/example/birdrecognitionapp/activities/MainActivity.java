package com.example.birdrecognitionapp.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.adapters.SavedRecordingsAdapter;
import com.example.birdrecognitionapp.adapters.ViewPagerAdapter;

import com.example.birdrecognitionapp.models.MainActivityRecordFragmentSharedModel;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.example.birdrecognitionapp.models.User;
import com.example.birdrecognitionapp.services.SessionManagerService;
import com.google.android.material.navigation.NavigationView;
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
public class MainActivity extends AppCompatActivity implements SavedRecordingsAdapter.OnPredictButtonPressListener, NavigationView.OnNavigationItemSelectedListener {


    // constant for storing audio permission
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    TabLayout tabLayout;
    DrawerLayout drawerLayout;
    ViewPager2 viewPager2;
    ViewPagerAdapter myViewPagerAdapter;
    MainActivityRecordFragmentSharedModel sharedModel;
    User user = new User();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            Objects.requireNonNull(getSupportActionBar()).hide();
        } catch (Exception e) {
        }
        if (user.getId() == null) {
            SessionManagerService sessionManager = new SessionManagerService(this);
            user = new User(sessionManager.getUserId(), sessionManager.getName(), sessionManager.getSurname(), sessionManager.getEmail(), sessionManager.getPassword());
        }
        setupMenuDrawerAndToolbar();
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

//        if (!CheckPermissions())
//            RequestPermissions();

    }


//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case REQUEST_AUDIO_PERMISSION_CODE:
//                if (grantResults.length > 0) {
//                    boolean permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
//                    boolean permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED;
//                    boolean permissionToReadStorage = grantResults[2] == PackageManager.PERMISSION_GRANTED;
//                    boolean permissionToLocationFine = grantResults.length > 3 && grantResults[3] == PackageManager.PERMISSION_GRANTED;
//                    boolean permissionToLocationCoarse = grantResults.length > 4 && grantResults[4] == PackageManager.PERMISSION_GRANTED;
//
//                    if (permissionToRecord && permissionToStore && permissionToReadStorage) {
//                        Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_LONG).show();
//                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
//                        Uri uri = Uri.fromParts("package", getPackageName(), null);
//                        intent.setData(uri);
//                        startActivity(intent);
//                    } else {
//                        Toast.makeText(getApplicationContext(), "Permissions Denied", Toast.LENGTH_LONG).show();
//                    }
//                }
//                break;
//        }
//    }
//
//
//    public boolean CheckPermissions() {
//        // this method is used to check permission
//        int resultStorage = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
//        int resultRecord = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
//        int resultLocationFine = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
//        int resultLocationCoarse = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION);
//        return resultStorage == PackageManager.PERMISSION_GRANTED && resultRecord == PackageManager.PERMISSION_GRANTED &&
//                resultLocationFine == PackageManager.PERMISSION_GRANTED && resultLocationCoarse == PackageManager.PERMISSION_GRANTED;
//    }
//
//
//    private void RequestPermissions() {
//        // this method is used to request the
//        // permission for audio recording and storage.
//        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_AUDIO_PERMISSION_CODE);
//    }

    @Override
    public void switchToFirstTab(RecordingItem recordingItem) {

        viewPager2.setCurrentItem(0);
        sharedModel.predictRecording(recordingItem);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_logout:
                SessionManagerService sessionManager = new SessionManagerService(this);
                sessionManager.setLogin(false);
                Toast.makeText(this, "Logout!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                break;
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void setupMenuDrawerAndToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        View headerView = navigationView.getHeaderView(0); // Get the navigation header view
        TextView user_name = headerView.findViewById(R.id.user_name);
        TextView user_email = headerView.findViewById(R.id.user_email);
        user_name.setText(user.getSurname() + " " + user.getName());
        user_email.setText(user.getEmail());
        toggle.syncState();
    }

}