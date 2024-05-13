package com.example.birdrecognitionapp.api;

import com.example.birdrecognitionapp.dto.ChangePasswordDto;
import com.example.birdrecognitionapp.dto.DeleteSoundDto;
import com.example.birdrecognitionapp.dto.GetUserSoundsDto;
import com.example.birdrecognitionapp.dto.LoginReq;
import com.example.birdrecognitionapp.dto.LoginResponse;
import com.example.birdrecognitionapp.dto.ObservationSheetDto;
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
import retrofit2.http.Header;
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
    Call<String> updateUserDetails(@Header("Authorization") String authToken,@Body UserDetailsDto userDetails);

    @POST("/addsound")
    Call<String>addSound(@Header("Authorization") String authToken,@Body RecordingItem recordingItem);

    @POST("/downloadusersounds")
    Call<ResponseBody> downloadUserSounds(@Header("Authorization") String authToken,@Body GetUserSoundsDto user);

    @HTTP(method = "DELETE", path = "deletesound", hasBody = true)
    Call<String> deleteSound(@Header("Authorization") String authToken,@Body DeleteSoundDto soundDto);

    @GET("getcreationdate")
    Call<Map<String, Long>> getCreationDateOfSounds(@Header("Authorization") String authToken,@Query("user_id") String userId);

    @POST("addobservationsheet")
    Call<ResponseBody> insertObservation(@Header("Authorization") String authToken,@Body ObservationSheetDto observationSheetDto);

    @GET("observations/{user_id}/")
    Call<List<ObservationSheetDto>> getObservationsByUserId(@Header("Authorization") String authToken,@Path("user_id") String userId);

    @HTTP(method = "DELETE", path = "deleteobservationsheet", hasBody = true)
    Call<Void> deleteObservationSheet(@Header("Authorization") String authToken,@Body ObservationSheetDto observationSheetDto);

    @POST("update_password")
    Call<ResponseBody> updatePassword(@Body ChangePasswordDto changePasswordDto);

}
