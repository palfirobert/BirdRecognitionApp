package com.example.birdrecognitionapp.api;

import com.example.birdrecognitionapp.dto.SecurityCodeDto;
import com.example.birdrecognitionapp.dto.SoundPredictionResponse;

import java.util.HashMap;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface RetrofitAPI {

    @POST("/")
    Call<List<SoundPredictionResponse>> sendDataForPredictionWithoutLocation(@Header("Authorization") String authToken, @Body HashMap<String, Object> parameters);

    @POST("/predictionwithlocation")
    Call<List<SoundPredictionResponse>> sendDataForPredictionWithLocation(@Header("Authorization") String authToken,@Body HashMap<String, Object> parameters);

    @POST("send_code")
    Call<ResponseBody>sendSecurityCode(@Body SecurityCodeDto securityCodeDto);


}
