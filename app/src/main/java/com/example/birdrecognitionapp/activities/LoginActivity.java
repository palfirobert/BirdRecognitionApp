package com.example.birdrecognitionapp.activities;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.api.AzureDbAPI;
import com.example.birdrecognitionapp.database.DbHelper;
import com.example.birdrecognitionapp.dto.LoginReq;
import com.example.birdrecognitionapp.dto.LoginResponse;
import com.example.birdrecognitionapp.models.User;
import com.example.birdrecognitionapp.models.UserDetails;
import com.example.birdrecognitionapp.services.SessionManagerService;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        dbHelper = new DbHelper(getApplicationContext());
        SessionManagerService sessionManager = new SessionManagerService(this);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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

                                        dbHelper.clearDirectory(new File(Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/"));
                                        dbHelper.fetchAndPopulateUserSounds(user.getId());
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
}