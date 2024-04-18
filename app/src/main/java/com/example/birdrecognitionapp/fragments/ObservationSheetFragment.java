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
import com.example.birdrecognitionapp.adapters.ObservationSheetAdapter;
import com.example.birdrecognitionapp.database.DbHelper;
import com.example.birdrecognitionapp.models.ObservationSheet;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ObservationSheetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ObservationSheetFragment extends Fragment {

    @BindView(R.id.recyclerViewObservationSheet)
    RecyclerView recyclerViewObservationSheet;

    private DbHelper dbHelper;
    private ArrayList<ObservationSheet> listObservations;
    private ObservationSheetAdapter observationSheetAdapter;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public ObservationSheetFragment() {
        // Required empty public constructor
    }


    public static ObservationSheetFragment newInstance(String param1, String param2) {
        ObservationSheetFragment fragment = new ObservationSheetFragment();
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
        View view = inflater.inflate(R.layout.fragment_observation_sheet, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new DbHelper(getContext());
        recyclerViewObservationSheet.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerViewObservationSheet.setLayoutManager(linearLayoutManager);
        loadData();
    }
    private void loadData() {
        listObservations = dbHelper.getAllObservations();
        if (listObservations == null || listObservations.isEmpty()) {
            Toast.makeText(getContext(), "No observation data found.", Toast.LENGTH_SHORT).show();
        } else {
            observationSheetAdapter = new ObservationSheetAdapter(getActivity(), listObservations);
            recyclerViewObservationSheet.setAdapter(observationSheetAdapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.shutdownExecutorService();
        }
    }


}