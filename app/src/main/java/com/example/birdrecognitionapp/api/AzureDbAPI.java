package com.example.birdrecognitionapp.api;

import com.example.birdrecognitionapp.dto.LoginReq;
import com.example.birdrecognitionapp.dto.LoginResponse;
import com.example.birdrecognitionapp.dto.SignupReq;
import com.example.birdrecognitionapp.dto.SignupResponse;
import com.example.birdrecognitionapp.dto.UserDetailsDto;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.example.birdrecognitionapp.models.UserDetails;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface AzureDbAPI {
    @POST("/login")
    Call<LoginResponse> loginUser(@Body LoginReq loginReq);

    @POST("/signup")
    Call<SignupResponse> signupUser(@Body SignupReq signupReq);

    @PUT("/updateuserdetails")
    Call<String> updateUserDetails(@Body UserDetailsDto userDetails);

    @POST("/addsound")
    Call<String>addSound(@Body RecordingItem recordingItem);
}
