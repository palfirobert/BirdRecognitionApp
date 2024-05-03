package com.example.birdrecognitionapp.fragments;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.birdrecognitionapp.R;
import com.example.birdrecognitionapp.database.BirdsDbHelper;
import com.example.birdrecognitionapp.database.DbHelper;
import com.example.birdrecognitionapp.dto.ObservationSheetDto;
import com.example.birdrecognitionapp.dto.SoundPredictionResponse;
import com.example.birdrecognitionapp.enums.LANGUAGE;
import com.example.birdrecognitionapp.models.LoadingDialogBar;
import com.example.birdrecognitionapp.models.MainActivityRecordFragmentSharedModel;
import com.example.birdrecognitionapp.models.ObservationSheet;
import com.example.birdrecognitionapp.models.RecordingItem;
import com.example.birdrecognitionapp.models.User;
import com.example.birdrecognitionapp.models.UserDetails;
import com.example.birdrecognitionapp.services.RecordingService;
import com.example.birdrecognitionapp.services.SessionManagerService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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
    DbHelper mysqlDbHelper;

    UserDetails userDetails = new UserDetails();

    User user = new User();

    ObservationSheet observationSheet;

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
                if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
                    loadingDialogBar.showDialog("Predicting");
                }
            }
        }
    };

    private void updateUI(List<SoundPredictionResponse> predictionList, LANGUAGE language) {
        if (isAdded() && getActivity() != null && !getActivity().isFinishing()) {
            loadingDialogBar.hideDialog();
            // Create a Map with common_name as key and CoiSoundPredictionResponse as value
            Map<String, SoundPredictionResponse> map = new HashMap<>();
            for (SoundPredictionResponse response : predictionList) {
                map.put(response.getCommon_name(), response);
            }

            distinctPredictionList = new ArrayList(map.values());
            prediction_urls = new ArrayList<>();
            Collections.sort(distinctPredictionList, Collections.reverseOrder());
            if (language.equals(LANGUAGE.RO)) {
                distinctPredictionList.parallelStream().forEach(prediction ->
                        prediction.setCommon_name(dbHelper.getCommonNameByLatinName(prediction.getScientific_name())));
                distinctPredictionList.stream().forEach(prediction ->
                        prediction_urls.add(dbHelper.getUrlByLatinName(prediction.getScientific_name())));
                System.out.println(prediction_urls);
            } else {
                distinctPredictionList.stream().forEach(prediction ->
                        prediction_urls.add(null));
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

    }

    private void vibratePhone() {
        if (isAdded() && getActivity() != null) {
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
                                this.userDetails.setLanguage("English");
                                System.out.println(this.userDetails);
                                UserDetails.updateUserDetails(this.userDetails);
                                saveSelectedLanguage(this.userDetails.getLanguage());
                                PREDICTION_LANGUAGE = LANGUAGE.EN;
                                break;
                            case R.id.radioRomanian:
                                this.userDetails.setLanguage("Romanian");
                                UserDetails.updateUserDetails(this.userDetails);
                                saveSelectedLanguage(userDetails.getLanguage());
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
            Integer useLocInt = (useLocation) ? 1 : 0;
            this.userDetails.setUse_location(useLocInt);
            UserDetails.updateUserDetails(userDetails);
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
        SessionManagerService sessionManager = new SessionManagerService(getContext());
        sessionManager.setLanguage(language);
        if (activity != null) {
            SharedPreferences sharedPref = activity.getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(KEY_SELECTED_LANGUAGE, language);
            editor.apply();
        }
    }

    private void saveSelectedOption(boolean option) {
        Activity activity = getActivity();
        SessionManagerService sessionManager = new SessionManagerService(getContext());
        int optionIntValue = option ? 1 : 0;
        sessionManager.setUseLocation(optionIntValue);
        if (activity != null) {
            SharedPreferences sharedPref = activity.getSharedPreferences(PREFS_LOCATION, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(KEY_SELECTED_LOCATION, option);
            editor.apply();
        }
    }

    private String getSavedLanguage() {
        Activity activity = getActivity();
        System.out.println(userDetails.getLanguage());
        if (activity != null) {
            SharedPreferences sharedPref = activity.getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE);
            return sharedPref.getString(KEY_SELECTED_LANGUAGE, userDetails.getLanguage()); // Default to English
        }
        return userDetails.getLanguage(); // Default to English if getActivity() is null
    }

    private boolean getSavedOption() {
        Activity activity = getActivity();
        Boolean useLocation = this.userDetails.getUse_location() == 1;
        if (activity != null) {
            SharedPreferences sharedPref = activity.getSharedPreferences(PREFS_LOCATION, Context.MODE_PRIVATE);
            return sharedPref.getBoolean(KEY_SELECTED_LOCATION, useLocation); // Default to false
        }
        return useLocation; // Default to false if getActivity() is null
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

        this.predictionButtonOne.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showOptionsPopupMenu(view, predictionButtonOne.getText().toString());
                return true;
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

        this.predictionButtonTwo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showOptionsPopupMenu(view, predictionButtonTwo.getText().toString());
                return true;
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

        this.predictionButtonThree.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showOptionsPopupMenu(view, predictionButtonThree.getText().toString());
                return true;
            }
        });
        return recordView;
    }

    private void showOptionsPopupMenu(View view, String species) {
        PopupMenu popup = new PopupMenu(getContext(), view);
        // Inflate the menu from xml
        popup.inflate(R.menu.observation_sheet_menu);
        // Add click listener for menu items
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_generate_observation_sheet:
                        LayoutInflater inflater = LayoutInflater.from(getContext());
                        View dialogView = inflater.inflate(R.layout.dialog_observation_sheet, null);

                        EditText editUploadDate = dialogView.findViewById(R.id.editUploadDate);
                        // Format the current date and time
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String formattedDateTime = LocalDateTime.now().format(formatter);

                        // Set the formatted date and time to your text field
                        editUploadDate.setText(formattedDateTime);
                        EditText editSpecies = dialogView.findViewById(R.id.editSpecies);
                        editSpecies.setText(species.replaceAll("[0-9%.]", ""));

                        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                        String locationProvider = LocationManager.GPS_PROVIDER;

                        Double lat = 0.0;
                        Double lon = 0.0;
                        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
                            if (lastKnownLocation != null) {
                                lat = lastKnownLocation.getLatitude();
                                lon = lastKnownLocation.getLongitude();
                            }
                        }

                        EditText editLocation = dialogView.findViewById(R.id.editLocation);
                        editLocation.setText(lat + "," + lon);

                        EditText editObserver = dialogView.findViewById(R.id.editObserver);
                        editObserver.setText(user.getName() + " " + user.getSurname());

                        EditText editObservationDate = dialogView.findViewById(R.id.editObservationDate);
                        DateTimeFormatter formatterObservationDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        if (ObservationSheet.getCalledFromSavedRecordingAdapter()) {
                            // Convert timestamp to Instant
                            Instant instant = Instant.ofEpochMilli(Long.valueOf(ObservationSheet.getObservationDate()));

                            // Convert Instant to LocalDateTime using system default time zone
                            LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

                            // Format LocalDateTime
                            String formattedDate = dateTime.format(formatterObservationDate);

                            editObservationDate.setText(formattedDate);

                        } else {
                            LocalDateTime now = LocalDateTime.now();
                            editObservationDate.setText(now.format(formatterObservationDate));
                        }

                        EditText editNumberOfSpecies = dialogView.findViewById(R.id.editNumber);
                        // Create the AlertDialog for observation sheet
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setView(dialogView)
                                .setTitle("Enter Observation Details")
                                .setPositiveButton("Save", null)
                                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                        AlertDialog dialog = builder.create();
                        dialog.setOnShowListener(dialogInterface -> {
                            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            button.setOnClickListener(view1 -> {
                                if (!editNumberOfSpecies.getText().toString().equals("")) {
                                    String initialObservationDate = ObservationSheet.getObservationDate();
                                    Date date1 = new Date(Long.parseLong(ObservationSheet.getObservationDate()));
                                    // Create a SimpleDateFormat instance with your desired format
                                    SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                                    String formattedDate = formatter1.format(date1);
                                    ObservationSheetDto observationSheet = new ObservationSheetDto(formattedDate, editSpecies.getText().toString(),
                                            Integer.parseInt(editNumberOfSpecies.getText().toString()), editObserver.getText().toString(), editUploadDate.getText().toString(),
                                            editLocation.getText().toString(), user.getId(), ObservationSheet.getSoundId());
                                    mysqlDbHelper.addObservation(observationSheet);
                                    try {
                                        SimpleDateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                        // Parse the date string into a Date object
                                        Date date = date_formatter.parse(editUploadDate.getText().toString());
                                        // Convert the Date to a timestamp (milliseconds since January 1, 1970, 00:00:00 GMT)
                                        Long timestamp = date.getTime();

                                        String sound_id = user.getId() + "-" + ObservationSheet.getAudioFileName().replaceAll("[^\\d]", "");
                                        ObservationSheetDto dto = new ObservationSheetDto(initialObservationDate, editSpecies.getText().toString(),
                                                Integer.parseInt(editNumberOfSpecies.getText().toString()), editObserver.getText().toString(), timestamp.toString(),
                                                editLocation.getText().toString(), user.getId(), sound_id);
                                        mysqlDbHelper.insertObservationSheet(dto);
                                        dialog.dismiss();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    Toast.makeText(getContext(), "Insert number of species", Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                        dialog.show();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new BirdsDbHelper(getContext());
        mysqlDbHelper = new DbHelper(getContext());
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

        if (userDetails.getUse_location() != null) {
            clearDirectory(new File(Environment.getExternalStorageDirectory().getPath() + "/soundrecordings/"));
            mysqlDbHelper.fetchAndPopulateUserSounds(user.getId());
            boolean useLocation = userDetails.getUse_location() == 1;
            saveSelectedOption(useLocation);
            saveSelectedLanguage(userDetails.getLanguage());
        } else {
            SessionManagerService sessionManager = new SessionManagerService(getContext());
            userDetails = new UserDetails(sessionManager.getUserId(), sessionManager.getLanguage(), sessionManager.getUseLocation());
            boolean useLocation = userDetails.getUse_location() == 1;
            DbHelper.firstLogin = false;
            saveSelectedOption(useLocation);
            saveSelectedLanguage(userDetails.getLanguage());
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
        // get the location and send it to service method in case of long press "predict" button
        Double lat = 0.0;
        Double lon = 0.0;

        if (base64EncodedString != null) {
            LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            String locationProvider = LocationManager.GPS_PROVIDER;

            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
                if (lastKnownLocation != null) {
                    lat = lastKnownLocation.getLatitude();
                    lon = lastKnownLocation.getLongitude();
                }
            }
            new RecordingService().postData(base64EncodedString, useLocation, Optional.of(lat), Optional.of(lon), Optional.of(recordingItem.getTime_added()), false);
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

    public void clearDirectory(File dir) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            deleteRecording("/storage/emulated/0/soundrecordings/" + file.getName());
                        }
                    }
                }
            }
        });
    }

    public void deleteRecording(String path) {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }


}
