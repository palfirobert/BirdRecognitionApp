package com.example.birdrecognitionapp.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.birdrecognitionapp.fragments.RecordFragment;
import com.example.birdrecognitionapp.fragments.SavedRecordingsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0: return new RecordFragment();
            case 1: return new SavedRecordingsFragment();
            default: return new RecordFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
