package com.example.birdrecognitionapp.api;

import com.example.birdrecognitionapp.dto.LoginReq;
import com.example.birdrecognitionapp.dto.LoginResponse;
import com.example.birdrecognitionapp.dto.SignupReq;
import com.example.birdrecognitionapp.dto.SignupResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AzureDbAPI {
    @POST("/login")
    Call<LoginResponse> loginUser(@Body LoginReq loginReq);

    @POST("/signup")
    Call<SignupResponse> signupUser(@Body SignupReq signupReq);
}
