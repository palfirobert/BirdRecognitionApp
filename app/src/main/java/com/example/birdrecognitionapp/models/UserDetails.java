package com.example.birdrecognitionapp.models;

import android.widget.Toast;

import com.example.birdrecognitionapp.activities.MainActivity;
import com.example.birdrecognitionapp.api.AzureDbAPI;
import com.example.birdrecognitionapp.dto.UserDetailsDto;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserDetails implements Serializable {
    private static String user_id;
    private static String language;
    private static Integer use_location;
    private static String token;

    public UserDetails(){}

    public UserDetails(String user_id, String language, Integer use_location) {
        this.user_id = user_id;
        this.language = language;
        this.use_location = use_location;
    }
    public UserDetails(String user_id, String language, Integer use_location,String token) {
        this.user_id = user_id;
        this.language = language;
        this.use_location = use_location;
        this.token=token;
    }


    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        UserDetails.token = token;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getUse_location() {
        return use_location;
    }

    public void setUse_location(Integer use_location) {
        this.use_location = use_location;
    }

    @Override
    public String toString() {
        return "UserDetails{" +
                "user_id='" + user_id + '\'' +
                ", language='" + language + '\'' +
                ", use_location=" + use_location +
                '}';
    }

    public static void updateUserDetails(UserDetails userDetails)
    {
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
        boolean useLocation= userDetails.getUse_location() == 1;

        // Create an instance of the Retrofit AzureDbAPI
        AzureDbAPI azureDbAPI = retrofit.create(AzureDbAPI.class);
        UserDetailsDto userDetailsDto=new UserDetailsDto(userDetails.getUser_id(),userDetails.getLanguage(),useLocation);
        System.out.println(userDetailsDto);
        Call<String> call = azureDbAPI.updateUserDetails("Token " + UserDetails.getToken(),userDetailsDto);
        // Execute the call asynchronously
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.isSuccessful()) {
                    String genericResponse = response.body();
                    if (genericResponse != null) {
                        System.out.println(genericResponse);
                    }
                } else {
                    System.out.println("Update failed. Try again.");
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                System.out.println("server error");
            }
        });

    }
}
