package com.example.birdrecognitionapp.services;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManagerService {
    private static final String PREF_NAME = "LoginPreference";
    private static final String IS_LOGIN = "IsLoggedIn";
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
}
