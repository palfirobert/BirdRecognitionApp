package com.example.birdrecognitionapp.api;

import com.example.birdrecognitionapp.dto.DeleteSoundDto;
import com.example.birdrecognitionapp.dto.GetUserSoundsDto;
import com.example.birdrecognitionapp.dto.LoginReq;
import com.example.birdrecognitionapp.dto.LoginResponse;
import com.example.birdrecognitionapp.dto.SignupReq;
import com.example.birdrecognitionapp.dto.SignupResponse;
import com.example.birdrecognitionapp.dto.SoundResponse;
import com.example.birdrecognitionapp.dto.UserDetailsDto;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.example.birdrecognitionapp.models.UserDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AzureDbAPI {
    @POST("/login")
    Call<LoginResponse> loginUser(@Body LoginReq loginReq);

    @POST("/signup")
    Call<SignupResponse> signupUser(@Body SignupReq signupReq);

    @PUT("/updateuserdetails")
    Call<String> updateUserDetails(@Body UserDetailsDto userDetails);

    @POST("/addsound")
    Call<String>addSound(@Body RecordingItem recordingItem);

    @POST("/downloadusersounds")
    Call<ResponseBody> downloadUserSounds(@Body GetUserSoundsDto user);

    @HTTP(method = "DELETE", path = "deletesound", hasBody = true)
    Call<String> deleteSound(@Body DeleteSoundDto soundDto);

    @GET("getcreationdate")
    Call<Map<String, Long>> getCreationDateOfSounds(@Query("user_id") String userId);
}
