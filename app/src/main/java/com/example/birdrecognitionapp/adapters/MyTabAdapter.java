package com.example.birdrecognitionapp.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;


import com.example.birdrecognitionapp.fragments.RecordFragment;

public class MyTabAdapter extends FragmentPagerAdapter {
    String[] titles={"Record"};
    public MyTabAdapter(@NonNull FragmentManager fm) {
        super(fm);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position)
        {
            case 0:
                return new RecordFragment();
            default:return null;
        }

    }

    @Override
    public int getCount() {
        return titles.length;
    }
    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

//    String[] titles={"Record","Saved Recordings"};
//
//    public MyTabAdapter(@NonNull FragmentManager fm, int behavior) {
//        super(fm, behavior);
//    }
//
//
//    @Override
//    public Fragment getItem(int position) {
//
//        switch (position)
//        {
//            case 0:
//                return new RecordFragment();
//
//        }
//        return null;
//    }
//
//    @Override
//    public int getCount() {
//        return titles.length;
//    }
//
//    @Nullable
//    @Override
//    public CharSequence getPageTitle(int position) {
//        return titles[position];
//    }
}
