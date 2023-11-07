package com.example.birdrecognitionapp.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitAPI {

    @POST("/")
    Call<String> createPost(@Body String soundInBase64);
}
