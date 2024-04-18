package com.example.birdrecognitionapp.activities;

import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.api.AzureDbAPI;
import com.example.birdrecognitionapp.database.DbHelper;
import com.example.birdrecognitionapp.dto.LoginReq;
import com.example.birdrecognitionapp.dto.LoginResponse;
import com.example.birdrecognitionapp.models.LoadingDialogBar;
import com.example.birdrecognitionapp.models.User;
import com.example.birdrecognitionapp.models.UserDetails;
import com.example.birdrecognitionapp.services.SessionManagerService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {
    @BindView(R.id.login_email)
    EditText loginEmail;

    @BindView(R.id.login_password)
    EditText loginPassword;

    @BindView(R.id.login_button)
    Button loginButton;

    @BindView(R.id.signupRedirectText)
    TextView signupRedirectText;

    User user;

    DbHelper dbHelper;

    LoadingDialogBar dialogBar;

    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        if (!CheckPermissions())
            RequestPermissions();

        dialogBar=new LoadingDialogBar(this);
        dbHelper = new DbHelper(getApplicationContext());
        SessionManagerService sessionManager = new SessionManagerService(this);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                dialogBar.showDialog("Logging in...");
                String email = loginEmail.getText().toString();
                String password = loginPassword.getText().toString();
                if (email.equals("") || password.equals(""))
                    Toast.makeText(LoginActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
                else {

                    OkHttpClient okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS) // Set the connection timeout
                            .readTimeout(30, TimeUnit.SECONDS) // Set the read timeout
                            .writeTimeout(30, TimeUnit.SECONDS) // Set the write timeout
                            .build();

                    // Use the custom OkHttpClient in your Retrofit builder
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl("http://palfirobert.pythonanywhere.com") // Your base URL
                            .addConverterFactory(GsonConverterFactory.create())
                            .client(okHttpClient) // Set the custom client here
                            .build();
                    // Prepare the login request
                    LoginReq loginReq = new LoginReq(email, password);

                    // Get the Retrofit instance and prepare the call
                    AzureDbAPI azureDbAPI = retrofit.create(AzureDbAPI.class);
                    Call<LoginResponse> call = azureDbAPI.loginUser(loginReq);

                    // Execute the call asynchronously
                    call.enqueue(new Callback<LoginResponse>() {
                        @Override
                        public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                            if (response.isSuccessful()) {
                                LoginResponse loginResponse = response.body();
                                if (loginResponse != null) {
                                    if (loginResponse.getError() != null) {
                                        Toast.makeText(LoginActivity.this, loginResponse.getError(), Toast.LENGTH_SHORT).show();
                                    } else if (loginResponse.getMessage() != null) {
                                        Toast.makeText(LoginActivity.this, "Login Successfully!", Toast.LENGTH_SHORT).show();
                                        // Populate the UserDetails object
                                        UserDetails userDetails = new UserDetails(
                                                loginResponse.getUser_id(),
                                                loginResponse.getLanguage(),
                                                loginResponse.getUse_location()
                                        );
                                        user=new User(loginResponse.getUser_id(),loginResponse.getName(),loginResponse.getSurname(),loginResponse.getEmail(),loginResponse.getPassword());

                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        System.out.println(userDetails.getUse_location());
                                        System.out.println(userDetails.getLanguage());
                                        sessionManager.setLogin(true);
                                        sessionManager.setUseLocation(userDetails.getUse_location());
                                        sessionManager.setLanguage(userDetails.getLanguage());
                                        sessionManager.setUserId(userDetails.getUser_id());
                                        sessionManager.setSurname(user.getSurname());
                                        sessionManager.setEmail(user.getEmail());
                                        sessionManager.setPassword(user.getPassword());
                                        sessionManager.setName(user.getName());
                                        dialogBar.hideDialog();
                                        DbHelper.firstLogin=true;
                                        startActivity(intent);
                                    }
                                }
                            } else {
                                Toast.makeText(LoginActivity.this, "Invalid credentials.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<LoginResponse> call, Throwable t) {
                            Toast.makeText(LoginActivity.this, "Server error.", Toast.LENGTH_SHORT).show();
                            System.out.println(t.getMessage());
                        }
                    });

                }
            }
        });


        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
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

                    if (permissionToRecord && permissionToStore && permissionToReadStorage) {
                        Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
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
        ActivityCompat.requestPermissions(LoginActivity.this, new String[]{RECORD_AUDIO, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_AUDIO_PERMISSION_CODE);
    }
}