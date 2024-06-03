package com.example.birdrecognitionapp.activities;

import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.api.AzureDbAPI;
import com.example.birdrecognitionapp.api.RetrofitAPI;
import com.example.birdrecognitionapp.database.DbHelper;
import com.example.birdrecognitionapp.dto.ChangePasswordDto;
import com.example.birdrecognitionapp.dto.LoginReq;
import com.example.birdrecognitionapp.dto.LoginResponse;
import com.example.birdrecognitionapp.dto.SecurityCodeDto;
import com.example.birdrecognitionapp.models.LoadingDialogBar;
import com.example.birdrecognitionapp.models.User;
import com.example.birdrecognitionapp.models.UserDetails;
import com.example.birdrecognitionapp.services.SessionManagerService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
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

    @BindView(R.id.signup_button)
    Button signupButton;

    @BindView(R.id.forgot_password)
    TextView forgotPassword;

    User user;

    DbHelper dbHelper;

    LoadingDialogBar dialogBar;

    Integer securityCode;
    String email;

    public static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        if (!CheckPermissions())
            RequestPermissions();

        dialogBar = new LoadingDialogBar(this);
        dbHelper = new DbHelper(getApplicationContext());
        SessionManagerService sessionManager = new SessionManagerService(this);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (CheckPermissions()) {
                    dialogBar.showDialog("Logging in...");
                    String email = loginEmail.getText().toString();
                    String password = loginPassword.getText().toString();
                    if (email.equals("") || password.equals("")) {
                        Toast.makeText(LoginActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
                        dialogBar.hideDialog();
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
                                                    loginResponse.getUse_location(),
                                                    loginResponse.getToken()
                                            );
                                            user = new User(loginResponse.getUser_id(), loginResponse.getName(),
                                                    loginResponse.getSurname(), loginResponse.getEmail(), loginResponse.getPassword());

                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            sessionManager.setLogin(true);
                                            sessionManager.setUseLocation(userDetails.getUse_location());
                                            sessionManager.setLanguage(userDetails.getLanguage());
                                            sessionManager.setToken(UserDetails.getToken());
                                            sessionManager.setUserId(userDetails.getUser_id());
                                            sessionManager.setSurname(user.getSurname());
                                            sessionManager.setEmail(user.getEmail());
                                            sessionManager.setPassword(user.getPassword());
                                            sessionManager.setName(user.getName());
                                            dialogBar.hideDialog();
                                            DbHelper.firstLogin = true;
                                            startActivity(intent);
                                        }
                                    }
                                } else {
                                    dialogBar.hideDialog();
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
                } else {
                    Toast.makeText(getApplicationContext(), "You need to allow every permission!", Toast.LENGTH_SHORT).show();
                    RequestPermissions();
                }
            }

        });


        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Enter your email");
        builder.setView(input);

        // Define the email pattern
        final Pattern EMAIL_PATTERN = Pattern.compile(
                "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                        "\\@" +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                        "(" +
                        "\\." +
                        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                        ")+"
        );

        // Set up the buttons
        builder.setPositiveButton("Send", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                email = input.getText().toString();
                Matcher matcher = EMAIL_PATTERN.matcher(email);
                if (matcher.matches()) {
                    dialog.dismiss();
                    sendResetCode(email);
                } else {
                    Toast.makeText(LoginActivity.this, "Please enter a valid email address.", Toast.LENGTH_LONG).show();
                }
            });
        });

        dialog.show();
    }

    private void sendResetCode(String email) {
        Random random = new Random();
        securityCode = 1000 + random.nextInt(9000);
        sendSecurityCodeToEmail(email);

        // Set up the dialog for the code entry
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Code");

        // Set up the code input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter the 4-digit code");
        builder.setView(input);

        builder.setPositiveButton("Verify", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String code = input.getText().toString();
                        if (code.equals(String.valueOf(securityCode))) {
                            dialog.dismiss(); // Close the dialog only if the code is correct
                            showChangePasswordDialog();
                        } else {
                            Toast.makeText(LoginActivity.this, "Incorrect security code", Toast.LENGTH_SHORT).show();
                            // Do not dismiss the dialog, allowing the user to try again
                        }
                    }
                });
            }
        });

        dialog.show();
    }


    private void verifyCode(String inputCode) {
        if (inputCode.equals(String.valueOf(securityCode))) {
            showChangePasswordDialog();
        } else {
            Toast.makeText(LoginActivity.this, "Incorrect security code", Toast.LENGTH_SHORT).show();
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set New Password");

        // Set up the layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Set up the New Password input
        final EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("New Password");
        newPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);

        // Set up the Confirm Password input
        final EditText confirmPasswordInput = new EditText(this);
        confirmPasswordInput.setHint("Confirm Password");
        confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(confirmPasswordInput);

        builder.setView(layout);

        // Set up the buttons
        builder.setPositiveButton("Change password", null);  // Listener set to null to manage manually
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String newPassword = newPasswordInput.getText().toString();
                        String confirmPassword = confirmPasswordInput.getText().toString();

                        if (!newPassword.isEmpty() && newPassword.equals(confirmPassword)) {
                            dialog.dismiss();
                            updatePassword(newPassword);
                        } else if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                            Toast.makeText(LoginActivity.this, "Password fields cannot be empty", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        dialog.show();
    }


    private void updatePassword(String newPassword) {
        int timeoutInSeconds = 30;
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com")  // Adjust the base URL as necessary
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        AzureDbAPI service = retrofit.create(AzureDbAPI.class);
        Call<ResponseBody> call = service.updatePassword(new ChangePasswordDto(email, newPassword));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to update password.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void sendSecurityCodeToEmail(String email) {
        int timeoutInSeconds = 30;
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutInSeconds, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://palfirobert.pythonanywhere.com")  // Adjust the base URL as necessary
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();

        RetrofitAPI service = retrofit.create(RetrofitAPI.class);
        Call<ResponseBody> call = service.sendSecurityCode(new SecurityCodeDto(email, securityCode));

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this, "Security code sent to your email.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Failed to send security code.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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
        int resultStorage = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int resultRecord = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        int resultLocationFine = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        int resultLocationCoarse = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION);
        return resultStorage == PackageManager.PERMISSION_GRANTED && resultRecord == PackageManager.PERMISSION_GRANTED &&
                resultLocationFine == PackageManager.PERMISSION_GRANTED && resultLocationCoarse == PackageManager.PERMISSION_GRANTED;
    }


    private void RequestPermissions() {
        ActivityCompat.requestPermissions(LoginActivity.this, new String[]{RECORD_AUDIO
                , WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_AUDIO_PERMISSION_CODE);
    }
}