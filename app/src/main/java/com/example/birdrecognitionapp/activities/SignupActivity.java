package com.example.birdrecognitionapp.activities;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.api.AzureDbAPI;
import com.example.birdrecognitionapp.dto.SignupReq;
import com.example.birdrecognitionapp.dto.SignupResponse;
import com.example.birdrecognitionapp.models.LoadingDialogBar;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SignupActivity extends AppCompatActivity {

    @BindView(R.id.signup_email)
    EditText signupEmail;

    @BindView(R.id.signup_password)
    EditText signupPassword;

    @BindView(R.id.signup_confirm)
    EditText signupConfirmPassword;

    @BindView(R.id.signup_name)
    EditText signupName;

    @BindView(R.id.signup_surname)
    EditText signupSurname;

    @BindView(R.id.signup_button)
    Button signupButton;

    @BindView(R.id.loginRedirectText)
    TextView loginRedirectText;

    LoadingDialogBar loadingDialogBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);
        loadingDialogBar=new LoadingDialogBar(this);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingDialogBar.showDialog("Signing up...");
                String email = signupEmail.getText().toString();
                String password = signupPassword.getText().toString();
                String confirmPassword = signupConfirmPassword.getText().toString();
                String name = signupName.getText().toString();
                String surname = signupSurname.getText().toString();

                // Email validation pattern
                String emailPattern = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
                Pattern pattern = Pattern.compile(emailPattern);
                Matcher matcher = pattern.matcher(email);

                // Check for empty fields and password match
                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty() || surname.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
                } else if (!password.equals(confirmPassword)) {
                    Toast.makeText(SignupActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                } else if (!matcher.matches()) {
                    Toast.makeText(SignupActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                } else {
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

                    // Prepare the signup request
                    SignupReq signupReq = new SignupReq(name, surname, email, password);

                    // Create an instance of the Retrofit AzureDbAPI
                    AzureDbAPI azureDbAPI = retrofit.create(AzureDbAPI.class);
                    Call<SignupResponse> call = azureDbAPI.signupUser(signupReq);

                    // Execute the call asynchronously
                    call.enqueue(new Callback<SignupResponse>() {
                        @Override
                        public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {
                            if (response.isSuccessful()) {
                                SignupResponse signupResponse = response.body();
                                if (signupResponse != null) {
                                    if (signupResponse.getError() != null) {
                                        Toast.makeText(SignupActivity.this, signupResponse.getError(), Toast.LENGTH_SHORT).show();
                                    } else if (signupResponse.getMessage() != null) {
                                        Toast.makeText(SignupActivity.this, "Signup Successfully!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                        startActivity(intent);
                                    }
                                }
                            } else {
                                System.out.println(response);
                                Toast.makeText(SignupActivity.this, "Account with this email already in use. Try again.", Toast.LENGTH_SHORT).show();
                            }
                            loadingDialogBar.hideDialog();
                        }

                        @Override
                        public void onFailure(Call<SignupResponse> call, Throwable t) {
                            Toast.makeText(SignupActivity.this, "Server error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                            loadingDialogBar.hideDialog();
                        }
                    });

                }
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }


}