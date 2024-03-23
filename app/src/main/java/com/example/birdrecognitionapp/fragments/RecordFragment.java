package com.example.birdrecognitionapp.fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.database.BirdsDbHelper;
import com.example.birdrecognitionapp.dto.SoundPredictionResponse;
import com.example.birdrecognitionapp.enums.LANGUAGE;
import com.example.birdrecognitionapp.models.LoadingDialogBar;
import com.example.birdrecognitionapp.models.MainActivityRecordFragmentSharedModel;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.example.birdrecognitionapp.services.RecordingService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RecordFragment extends Fragment {

    @BindView(R.id.chronometer)
    Chronometer chronometer;

    @BindView(R.id.recording_status_txt)
    TextView recordingStatusText;

    @BindView(R.id.btn_record)
    FloatingActionButton recordButton;

    @BindView(R.id.button_1)
    Button predictionButtonOne;

    @BindView(R.id.button_2)
    Button predictionButtonTwo;

    @BindView(R.id.button_3)
    Button predictionButtonThree;

    @BindView(R.id.prediction_title)
    TextView predictionTitle;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    LoadingDialogBar loadingDialogBar;

    MainActivityRecordFragmentSharedModel sharedModel;

    BirdsDbHelper dbHelper;

    private static final String PREFS_LANGUAGE = "LanguagePrefs";
    private static final String KEY_SELECTED_LANGUAGE = "selected_language";

    private static final String PREFS_LOCATION = "LocationPrefs";
    private static final String KEY_SELECTED_LOCATION = "selected_option_location";

    private boolean startRecording = true;
    private boolean useLocation;

    private LANGUAGE PREDICTION_LANGUAGE;

    private List<String> prediction_urls;
    List<SoundPredictionResponse> distinctPredictionList;

    long timeWhenPaused = 0;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("RECORDING_STOPPED")) {

            } else {
                // Handle the received data
                List<?> responseList = intent.getParcelableArrayListExtra("predictionList");
                List<SoundPredictionResponse> predictionList = new ArrayList(responseList);
                updateUI(predictionList, PREDICTION_LANGUAGE);
            }

        }
    };
    private BroadcastReceiver recordingStoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("RECORDING_STOPPED".equals(intent.getAction())) {
                // Show dialog here
                loadingDialogBar.showDialog("Predicting");
            }
        }
    };

    private void updateUI(List<SoundPredictionResponse> predictionList, LANGUAGE language) {
        loadingDialogBar.hideDialog();
        // Create a Map with common_name as key and CoiSoundPredictionResponse as value
        Map<String, SoundPredictionResponse> map = new HashMap<>();
        for (SoundPredictionResponse response : predictionList) {
            map.put(response.getCommon_name(), response);
        }

        distinctPredictionList = new ArrayList(map.values());
        prediction_urls=new ArrayList<>();
        Collections.sort(distinctPredictionList, Collections.reverseOrder());

        if (language.equals(LANGUAGE.RO)) {
            distinctPredictionList.parallelStream().forEach(prediction ->
                    prediction.setCommon_name(dbHelper.getCommonNameByLatinName(prediction.getScientific_name())));
            distinctPredictionList.stream().forEach(prediction ->
                    prediction_urls.add(dbHelper.getUrlByLatinName(prediction.getScientific_name())));
            System.out.println(prediction_urls);
        }

        if (distinctPredictionList.size() != 0)
            for (int i = 0; i < distinctPredictionList.size(); i++) {
                switch (i) {
                    case 0:
                        predictionButtonOne.setVisibility(View.VISIBLE);
                        predictionButtonOne.setText(
                                String.valueOf(distinctPredictionList.get(0).getCommon_name()) + " " +
                                        String.format("%.2f", distinctPredictionList.get(0).getConfidence() * 100) + "%"
                        );
                        break;
                    case 1:
                        predictionButtonTwo.setVisibility(View.VISIBLE);
                        predictionButtonTwo.setText(
                                String.valueOf(distinctPredictionList.get(i).getCommon_name()) + " " +
                                        String.format("%.2f", distinctPredictionList.get(i).getConfidence() * 100) + "%"
                        );
                        break;
                    case 2:
                        predictionButtonThree.setVisibility(View.VISIBLE);
                        predictionButtonThree.setText(
                                String.valueOf(distinctPredictionList.get(i).getCommon_name()) + " " +
                                        String.format("%.2f", distinctPredictionList.get(i).getConfidence() * 100) + "%"
                        );
                        break;
                    // Handle additional cases if needed
                }
            }
        else {
            predictionButtonOne.setVisibility(View.VISIBLE);
            if (language.equals(LANGUAGE.EN))
                predictionButtonOne.setText("No match found :(");
            else {
                predictionButtonOne.setText("Nu s-a putut prezice :(");
            }
        }

        vibratePhone();
    }

    private void vibratePhone() {
        // Get the Vibrator service
        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            // Check if the device has a vibrator
            if (vibrator.hasVibrator()) {
                // Vibrate for 500 milliseconds
                // For API 26 or above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    // Deprecated in API 26
                    vibrator.vibrate(300);
                }
            } else {
                Toast.makeText(getContext(), "No vibrator found on device.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.loadingDialogBar = new LoadingDialogBar(getContext());

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.prediction_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.language) {
            // Inflate the dialog's layout
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_language_selection, null);

            // Restore the saved language selection
            String savedLanguage = getSavedLanguage();
            RadioButton radioEnglish = dialogView.findViewById(R.id.radioEnglish);
            RadioButton radioRomanian = dialogView.findViewById(R.id.radioRomanian);

            if (savedLanguage.equals("Romanian")) {
                radioRomanian.setChecked(true);
            } else {
                radioEnglish.setChecked(true);
            }

            // Create the AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(dialogView)
                    .setTitle("Choose your language")
                    .setPositiveButton("Continue", (dialog, which) -> {
                        RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupLanguages);
                        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                        switch (checkedRadioButtonId) {
                            case R.id.radioEnglish:
                                saveSelectedLanguage("English");
                                PREDICTION_LANGUAGE = LANGUAGE.EN;
                                break;
                            case R.id.radioRomanian:
                                saveSelectedLanguage("Romanian");
                                PREDICTION_LANGUAGE = LANGUAGE.RO;
                                break;
                        }
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.show();
            return true;
        } else if (id == R.id.location) {
            useLocation = !useLocation;
            item.setChecked(useLocation);
            saveSelectedOption(useLocation);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem locationItem = menu.findItem(R.id.location);
        if (locationItem != null) {
            // Set the checked state based on the saved preference
            locationItem.setChecked(getSavedOption());
        }
    }

    private void saveSelectedLanguage(String language) {
        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences sharedPref = activity.getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(KEY_SELECTED_LANGUAGE, language);
            editor.apply();
        }
    }

    private void saveSelectedOption(boolean option) {
        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences sharedPref = activity.getSharedPreferences(PREFS_LOCATION, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(KEY_SELECTED_LOCATION, option);
            editor.apply();
        }
    }

    private String getSavedLanguage() {
        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences sharedPref = activity.getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE);
            return sharedPref.getString(KEY_SELECTED_LANGUAGE, "English"); // Default to English
        }
        return "English"; // Default to English if getActivity() is null
    }

    private boolean getSavedOption() {
        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences sharedPref = activity.getSharedPreferences(PREFS_LOCATION, Context.MODE_PRIVATE);
            return sharedPref.getBoolean(KEY_SELECTED_LOCATION, false); // Default to false
        }
        return false; // Default to false if getActivity() is null
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record, container, false);
        ButterKnife.bind(this, recordView);
        resetButtons();

        this.predictionButtonOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchString;
                if (prediction_urls.get(0) != null) {
                    searchString = prediction_urls.get(0); // Use the URL directly if available
                } else {
                    searchString = "https://www.google.com/search?q=" + Uri.encode(distinctPredictionList.get(0).getCommon_name());
                }

                // Create an Intent to open the browser with the Google search URL
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchString));
                startActivity(browserIntent);
            }
        });


        this.predictionButtonTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchString;
                if (prediction_urls.get(1) != null) {
                    searchString = prediction_urls.get(1); // Use the URL directly if available
                } else {
                    searchString = "https://www.google.com/search?q=" + Uri.encode(distinctPredictionList.get(1).getCommon_name());
                }

                // Create an Intent to open the browser with the Google search URL
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchString));
                startActivity(browserIntent);
            }
        });


        this.predictionButtonThree.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchString;
                if (prediction_urls.get(2) != null) {
                    searchString = prediction_urls.get(2); // Use the URL directly if available
                } else {
                    searchString = "https://www.google.com/search?q=" + Uri.encode(distinctPredictionList.get(2).getCommon_name());
                }

                // Create an Intent to open the browser with the Google search URL
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(searchString));
                startActivity(browserIntent);
            }
        });
        return recordView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new BirdsDbHelper(getContext());
        // Register to receive broadcasts from the service
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver,
                new IntentFilter("send-predictions-to-record-fragment"));
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(recordingStoppedReceiver,
                new IntentFilter("RECORDING_STOPPED"));

        // Assuming your activity extends AppCompatActivity
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
            activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        String selectedPredictionLanguage = getSavedLanguage();
        if (selectedPredictionLanguage.equals("English"))
            PREDICTION_LANGUAGE = LANGUAGE.EN;
        else
            PREDICTION_LANGUAGE = LANGUAGE.RO;

        boolean savedCheckboxOption = getSavedOption();

        if (savedCheckboxOption) {
            useLocation = true;
        } else {
            useLocation = false;
        }


    }

    @OnClick(R.id.btn_record)
    public void recordAudio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                onRecord(startRecording);
                startRecording = !startRecording;
            } else { //request for the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }

    }

    private void onRecord(boolean startRecording) {
        Intent intent = new Intent(getContext(), RecordingService.class);
        intent.putExtra("useLocation", useLocation);
        if (startRecording) {
            resetButtons();

            recordButton.setImageResource(R.drawable.ic_media_stop);
            Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
            chronometer.setBase(SystemClock.elapsedRealtime());
            chronometer.start();

            getActivity().startService(intent);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            recordingStatusText.setText("Recording...");

        } else {
            recordButton.setImageResource(R.drawable.ic_mic_white);
            chronometer.stop();
            chronometer.setBase(SystemClock.elapsedRealtime());
            timeWhenPaused = 0;
            recordingStatusText.setText("Tap the button to start recording");
            getActivity().stopService(intent);

        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sharedModel = new ViewModelProvider(requireActivity()).get(MainActivityRecordFragmentSharedModel.class);
        observePredictCommand();
    }

    private void observePredictCommand() {
        sharedModel.getRecordingItemToPredict().observe(getViewLifecycleOwner(), this::predictRecording);
    }

    public void predictRecording(RecordingItem recordingItem) {
        // Show loading dialog
        loadingDialogBar.showDialog("Predicting");
        resetButtons();
        // Read the file and encode it to Base64
        String base64EncodedString = encodeFileToBase64(recordingItem.getPath());

        if (base64EncodedString != null) {

            new RecordingService().postData(base64EncodedString, useLocation);
        }
    }

    private String encodeFileToBase64(String filePath) {
        try {
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void resetButtons() {
        this.predictionButtonOne.setVisibility(View.INVISIBLE);
        this.predictionButtonTwo.setVisibility(View.INVISIBLE);
        this.predictionButtonThree.setVisibility(View.INVISIBLE);
        this.predictionTitle.setVisibility(View.INVISIBLE);
    }

}
