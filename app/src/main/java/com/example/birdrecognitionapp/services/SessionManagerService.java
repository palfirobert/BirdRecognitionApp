package com.example.birdrecognitionapp.services;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManagerService {
    private static final String PREF_NAME = "LoginPreference";
    private static final String IS_LOGIN = "IsLoggedIn";
    private static final String USER_ID = "UserId";
    private static final String LANGUAGE = "UserLanguage";
    private static final String USE_LOCATION = "UserUseLocation";
    private static final String NAME = "UserName";
    private static final String SURNAME = "UserSurname";
    private static final String EMAIL = "UserEmail";
    private static final String PASSWORD = "UserPassword";
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    Context context;


    public SessionManagerService(Context context) {
        this.context = context;
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void setLogin(boolean isLoggedIn) {
        editor.putBoolean(IS_LOGIN, isLoggedIn);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(IS_LOGIN, false);
    }
    public void setUserId(String userId) {
        editor.putString(USER_ID, userId);
        editor.commit();
    }

    public String getUserId() {
        return prefs.getString(USER_ID, null);
    }

    public void setLanguage(String language) {
        editor.putString(LANGUAGE, language);
        editor.commit();
    }

    public String getLanguage() {
        return prefs.getString(LANGUAGE, "English");
    }

    public void setUseLocation(Integer useLocation) {
        editor.putInt(USE_LOCATION, useLocation);
        editor.commit();
    }

    public Integer getUseLocation() {
        return prefs.getInt(USE_LOCATION, 1);
    }
    public void setName(String name) {
        editor.putString(NAME, name);
        editor.commit();
    }

    public String getName() {
        return prefs.getString(NAME, "");
    }

    public void setSurname(String surname) {
        editor.putString(SURNAME, surname);
        editor.commit();
    }

    public String getSurname() {
        return prefs.getString(SURNAME, "");
    }

    public void setEmail(String email) {
        editor.putString(EMAIL, email);
        editor.commit();
    }

    public String getEmail() {
        return prefs.getString(EMAIL, "");
    }

    public void setPassword(String password) {
        editor.putString(PASSWORD, password);
        editor.commit();
    }

    public String getPassword() {
        return prefs.getString(PASSWORD, "");
    }
}
