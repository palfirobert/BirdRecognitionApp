package com.example.birdrecognitionapp.api;

import com.example.birdrecognitionapp.dto.SoundPredictionResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RetrofitAPI {

    @POST("/")
    Call<List<SoundPredictionResponse>> createPost(@Body String soundInBase64);
}
