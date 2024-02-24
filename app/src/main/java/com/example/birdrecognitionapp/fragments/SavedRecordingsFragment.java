package com.example.birdrecognitionapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.adapters.SavedRecordingsAdapter;
import com.example.birdrecognitionapp.database.DbHelper;
import com.example.birdrecognitionapp.models.RecordingItem;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


public class SavedRecordingsFragment extends Fragment {


    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";


    private String mParam1;
    private String mParam2;

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    DbHelper dbHelper;

    ArrayList<RecordingItem>listAudios;

    SavedRecordingsAdapter savedRecordingsAdapter;

    public SavedRecordingsFragment() {
        // Required empty public constructor
    }


    public static SavedRecordingsFragment newInstance(String param1, String param2) {
        SavedRecordingsFragment fragment = new SavedRecordingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_saved_recordings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        dbHelper=new DbHelper(getContext());
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.setReverseLayout(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        listAudios=dbHelper.getAllAudios();
        if(listAudios==null)
        {
            Toast.makeText(getContext(), "No audio files found.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            savedRecordingsAdapter=new SavedRecordingsAdapter(getActivity(),listAudios,linearLayoutManager);
            recyclerView.setAdapter(savedRecordingsAdapter);
        }

    }
}