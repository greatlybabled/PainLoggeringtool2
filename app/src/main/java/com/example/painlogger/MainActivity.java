
package com.example.painlogger;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.fragment.NavHostFragment;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.painlogger.fragments.GeneralFragment;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    WorkScheduler workScheduler;

    private static final String TAG = "PainLoggerApp";
    private static final int CREATE_GENERAL_FILE_REQUEST_CODE = 2;
    private static final int CREATE_DETAILED_FILE_REQUEST_CODE = 3;
    private static final String PREFS_NAME = "PainLoggerPrefs";
    private static final String KEY_GENERAL_LOG_FILE_URI = "general_log_file_uri";
    private static final String KEY_DETAILED_LOG_FILE_URI = "detailed_log_file_uri";
    private static final int NOTIFICATION_PERMISSION_CODE = 102;
    private static final String CHANNEL_ID = "pain_logger_channel";
    private static final String BODY_PARTS_PREFS = "BodyPartsPrefs";
    private static final String KEY_BODY_PARTS = "body_parts_list";

    // Default body parts list (used if no custom list is saved)
    private final String[] DEFAULT_BODY_PARTS = {
            "HD: Head - General", "FRN: Forehead", "TMP-L: Temple (Left)", "TMP-R: Temple (Right)",
            "EYE-L: Eye (Left)", "EYE-R: Eye (Right)", "EAR-L: Ear (Left)", "EAR-R: Ear (Right)",
            "JAW-L: Jaw (Left)", "JAW-R: Jaw (Right)", "NECK: Neck", "SHD-L: Shoulder (Left)",
            "SHD-R: Shoulder (Right)", "ARM-L: Arm (Left)", "ARM-R: Arm (Right)",
            "ELB-L: Elbow (Left)", "ELB-R: Elbow (Right)", "WRS-L: Wrist (Left)",
            "WRS-R: Wrist (Right)", "HND-L: Hand (Left)", "HND-R: Hand (Right)",
            "CHT: Chest", "ABD: Abdomen", "BCK-U: Back (Upper)", "BCK-M: Back (Middle)",
            "BCK-L: Back (Lower)", "HIP-L: Hip (Left)", "HIP-R: Hip (Right)",
            "LEG-L: Leg (Left)", "LEG-R: Leg (Right)", "KNE-L: Knee (Left)",
            "KNE-R: Knee (Right)", "ANK-L: Ankle (Left)", "ANK-R: Ankle (Right)",
            "FT-L: Foot (Left)", "FT-R: Foot (Right)", "OTH: Other", "GEN: General"
    };
    
    private String[] bodyParts;

    private boolean[] selectedBodyPartsState;
    private List<Integer> selectedItemsIndices;
    private List<Integer> processedItemsIndices; // To keep track of processed body parts for back navigation
    private List<DetailedPainEntry> detailedPainEntries;
    private String sessionTimestamp = "";
    private String initialPainLevel = "";
    private String initialTriggers = "";

    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"),
            uri -> {
                if (uri != null) {
                    handleDocumentCreation(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupButtons();
        initializeState();
        handleIntent(getIntent());
        setupNotifications();
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
    }

    private void setupButtons() {
        Button startLoggingButton = findViewById(R.id.startLoggingButton);
        if (startLoggingButton != null) {
            startLoggingButton.setOnClickListener(v -> {
                Log.d(TAG, "Start logging button clicked");
                showLoggingTypeDialog();
            });
        } else {
            Log.e(TAG, "Start logging button not found!");
        }

        Button testWorkerButton = findViewById(R.id.testWorkerButton);
        if (testWorkerButton != null) {
            testWorkerButton.setOnClickListener(v -> enqueueOneTimeTestWorker());
        }
    }
    
    public void showLoggingTypeDialog() {
        Log.d(TAG, "Showing logging type dialog");
        
        // Inflate the main container layout
        LayoutInflater inflater = LayoutInflater.from(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);
        
        // Create detailed logging option
        View detailedOptionView = inflater.inflate(R.layout.dialog_logging_type_option, null);
        TextView detailedTitle = detailedOptionView.findViewById(R.id.optionTitle);
        TextView detailedDesc = detailedOptionView.findViewById(R.id.optionDescription);
        ImageView detailedIcon = detailedOptionView.findViewById(R.id.optionIcon);
        
        detailedTitle.setText("Detailed Logging");
        detailedDesc.setText("Track specific body parts and pain levels for each area");
        detailedIcon.setImageResource(android.R.drawable.ic_menu_more);
        detailedIcon.setColorFilter(getResources().getColor(R.color.purple_500));
        
        // Create general logging option
        View generalOptionView = inflater.inflate(R.layout.dialog_logging_type_option, null);
        TextView generalTitle = generalOptionView.findViewById(R.id.optionTitle);
        TextView generalDesc = generalOptionView.findViewById(R.id.optionDescription);
        ImageView generalIcon = generalOptionView.findViewById(R.id.optionIcon);
        
        generalTitle.setText("General Logging");
        generalDesc.setText("Quick logging of overall pain level and triggers");
        generalIcon.setImageResource(android.R.drawable.ic_menu_edit);
        generalIcon.setColorFilter(getResources().getColor(R.color.teal_700));
        
        // Add margin between options
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = 16;
        generalOptionView.setLayoutParams(params);
        
        // Add options to main layout
        layout.addView(detailedOptionView);
        layout.addView(generalOptionView);
        
        // Set click listeners
        detailedOptionView.setOnClickListener(v -> {
            Log.d(TAG, "Detailed logging option clicked");
            startLoggingSequence(true);
            if (alertDialog != null) alertDialog.dismiss();
        });
        
        generalOptionView.setOnClickListener(v -> {
            Log.d(TAG, "General logging option clicked");
            startLoggingSequence(false);
            if (alertDialog != null) alertDialog.dismiss();
        });
        
        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Choose Logging Type")
                .setView(layout)
                .setNegativeButton("Cancel", null);
        
        alertDialog = builder.create();
        alertDialog.show();
    }
    
    private AlertDialog alertDialog;

    private void initializeState() {
        // Load body parts list from preferences
        loadBodyPartsList();
        
        selectedBodyPartsState = new boolean[bodyParts.length];
        selectedItemsIndices = new ArrayList<>();
        processedItemsIndices = new ArrayList<>();
        detailedPainEntries = new ArrayList<>();
    }
    
    private void loadBodyPartsList() {
        android.content.SharedPreferences prefs = getSharedPreferences(BODY_PARTS_PREFS, MODE_PRIVATE);
        String json = prefs.getString(KEY_BODY_PARTS, null);
        
        if (json != null) {
            try {
                java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<String>>() {}.getType();
                List<String> loadedList = new com.google.gson.Gson().fromJson(json, type);
                bodyParts = loadedList.toArray(new String[0]);
                Log.d(TAG, "Loaded custom body parts list with " + bodyParts.length + " items");
            } catch (Exception e) {
                Log.e(TAG, "Error loading body parts list", e);
                bodyParts = DEFAULT_BODY_PARTS;
            }
        } else {
            Log.d(TAG, "No custom body parts list found, using default list");
            bodyParts = DEFAULT_BODY_PARTS;
        }
    }

    private void setupNotifications() {
        createNotificationChannel();
        requestNotificationPermission();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            // Check for dismiss notification action
            if ("com.example.painlogger.DISMISS_NOTIFICATION".equals(intent.getAction())) {
                int notificationId = intent.getIntExtra("notification_id", -1);
                if (notificationId != -1) {
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                    notificationManager.cancel(notificationId);
                    Log.d(TAG, "Dismissed notification with ID: " + notificationId);
                }
                return;
            }
            
            // Handle navigation based on intent extras
            String openFragment = intent.getStringExtra("open_fragment");
            boolean startLoggingImmediately = intent.getBooleanExtra("start_logging_immediately", false);
            
            if ("detailed".equals(openFragment)) {
                if (startLoggingImmediately) {
                    startLoggingSequence(true); // Start detailed flow
                    Log.d(TAG, "Starting detailed logging sequence immediately");
                } else {
                    openDetailedFlow();
                    Log.d(TAG, "Opening detailed flow fragment");
                }
            } else if ("general".equals(openFragment)) {
                if (startLoggingImmediately) {
                    startLoggingSequence(false); // Start general flow
                    Log.d(TAG, "Starting general logging sequence immediately");
                } else {
                    openGeneralFragment();
                    Log.d(TAG, "Opening general flow fragment");
                }
            } else if (intent.getBooleanExtra("from_notification", false)) {
                // Default to general flow if not specified
                startLoggingSequence(false);
                Log.d(TAG, "Starting general logging sequence from notification");
            }
        }
    }
    private void openDetailedFlow() {
        GeneralFragment fragment = new GeneralFragment();
        Bundle args = new Bundle();
        args.putBoolean("detailed_flow", true);
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openGeneralFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, new GeneralFragment())
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            navigateToSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void navigateToSettings() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            if (navHostFragment.getNavController().getCurrentDestination().getId()
                    != R.id.settingsFragment) {
                navHostFragment.getNavController()
                        .navigate(R.id.action_homeFragment_to_settingsFragment);
            }
        }
    }

    private void enqueueOneTimeTestWorker() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PainLoggingWorker.class)
                .addTag("test_worker")
                .build();
        WorkManager.getInstance(this).enqueue(request);
        Toast.makeText(this, "Test worker enqueued", Toast.LENGTH_SHORT).show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pain Logger Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for pain logging reminders");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE
            );
        }
    }

    public void startLoggingSequence() {
        startLoggingSequence(false);
    }
    
    public void startLoggingSequence(boolean detailedFlow) {
        resetData();
        Log.d(TAG, "Starting logging sequence. Detailed flow: " + detailedFlow);
        showInitialPainDialog(detailedFlow);
    }

    private void resetData() {
        selectedItemsIndices.clear();
        processedItemsIndices.clear();
        detailedPainEntries.clear();
        initialPainLevel = "";
        initialTriggers = "";
        sessionTimestamp = "";
        selectedBodyPartsState = new boolean[bodyParts.length];
    }

    private void showInitialPainDialog() {
        showInitialPainDialog(false);
    }
    
    private void showInitialPainDialog(boolean detailedFlow) {
        EditText input = new EditText(this);
        input.setHint("Enter pain level (0-10)");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        String title = detailedFlow ? "Detailed Pain Assessment" : "Overall Pain Level";
        
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("Next", (dialog, which) -> {
                    initialPainLevel = input.getText().toString().trim();
                    validatePainLevel(detailedFlow);
                })
                .setNegativeButton("Back", (dialog, which) -> {
                    // Go back to logging type selection
                    showLoggingTypeDialog();
                })
                .setCancelable(true)
                .show();
    }

    private void validatePainLevel() {
        validatePainLevel(false);
    }
    
    private void validatePainLevel(boolean detailedFlow) {
        try {
            int level = Integer.parseInt(initialPainLevel);
            if (level < 0 || level > 10) {
                throw new NumberFormatException();
            }
            showInitialTriggersDialog(detailedFlow);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid input! Enter 0-10", Toast.LENGTH_SHORT).show();
            showInitialPainDialog(detailedFlow);
        }
    }

    private void showInitialTriggersDialog() {
        showInitialTriggersDialog(false);
    }
    
    private void showInitialTriggersDialog(boolean detailedFlow) {
        EditText input = new EditText(this);
        input.setHint("Describe triggers (optional)");
        input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        String title = detailedFlow ? "Pain Triggers (Detailed)" : "Triggers";
        String positiveButtonText = detailedFlow ? "Continue to Body Parts" : "Save";

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(positiveButtonText, (dialog, which) -> {
                    initialTriggers = input.getText().toString().trim();
                    if (detailedFlow) {
                        showBodyPartSelector();
                    } else {
                        // For general flow, skip body part selection
                        sessionTimestamp = getCurrentTimestamp();
                        attemptSaveGeneralLog();
                    }
                })
                .setNeutralButton("Skip", (dialog, which) -> {
                    initialTriggers = "";
                    if (detailedFlow) {
                        showBodyPartSelector();
                    } else {
                        // For general flow, skip body part selection
                        sessionTimestamp = getCurrentTimestamp();
                        attemptSaveGeneralLog();
                    }
                })
                .setNegativeButton("Back", (dialog, which) -> {
                    // Go back to pain level input
                    showInitialPainDialog(detailedFlow);
                })
                .setCancelable(true);
                
        builder.show();
    }

    private void showBodyPartSelector() {
        new AlertDialog.Builder(this)
                .setTitle("Select Affected Areas")
                .setMultiChoiceItems(bodyParts, selectedBodyPartsState,
                        (dialog, which, isChecked) ->
                                selectedBodyPartsState[which] = isChecked)
                .setPositiveButton("Continue", (dialog, which) -> processSelectedBodyParts())
                .setNeutralButton("Skip All", (dialog, which) -> {
                    // Skip body part selection and save general log
                    sessionTimestamp = getCurrentTimestamp();
                    attemptSaveGeneralLog();
                })
                .setNegativeButton("Back", (dialog, which) -> {
                    // Go back to triggers input
                    showInitialTriggersDialog(true);
                })
                .setCancelable(true)
                .show();
    }

    private void processSelectedBodyParts() {
        selectedItemsIndices.clear();
        processedItemsIndices.clear();
        for (int i = 0; i < selectedBodyPartsState.length; i++) {
            if (selectedBodyPartsState[i]) {
                selectedItemsIndices.add(i);
            }
        }
        sessionTimestamp = getCurrentTimestamp();
        if (!selectedItemsIndices.isEmpty()) {
            processNextBodyPart();
        } else {
            attemptSaveGeneralLog();
        }
    }

    private void processNextBodyPart() {
        if (selectedItemsIndices.isEmpty()) {
            attemptSaveGeneralLog();
            return;
        }

        int index = selectedItemsIndices.get(0);
        String[] parts = bodyParts[index].split(":");
        String code = parts[0].trim();
        String name = parts[1].trim();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        TextView bodyPartLabel = new TextView(this);
        bodyPartLabel.setText("Body Part: " + name);
        bodyPartLabel.setTextSize(16);
        bodyPartLabel.setPadding(0, 0, 0, 20);

        EditText painInput = new EditText(this);
        painInput.setHint("Pain Level (0-10)");
        painInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText notesInput = new EditText(this);
        notesInput.setHint("Notes (optional)");
        notesInput.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        layout.addView(bodyPartLabel);
        layout.addView(painInput);
        layout.addView(notesInput);

        String buttonText = selectedItemsIndices.size() > 1 ? "Next Body Part" : "Finish";
        
        // Determine if we can go back to a previous body part
        boolean canGoBack = !processedItemsIndices.isEmpty();
        String backButtonText = canGoBack ? "Previous Body Part" : "Back to Selection";

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Pain Details")
                .setView(layout)
                .setPositiveButton(buttonText, (dialogInterface, which) -> {
                    try {
                        String painText = painInput.getText().toString().trim();
                        if (painText.isEmpty()) {
                            painText = "0";
                        }
                        
                        int painLevel = Integer.parseInt(painText);
                        if (painLevel < 0 || painLevel > 10) {
                            throw new NumberFormatException();
                        }
                        
                        // Add current body part to processed list before moving to next
                        processedItemsIndices.add(selectedItemsIndices.get(0));
                        
                        detailedPainEntries.add(new DetailedPainEntry(
                                code,
                                painLevel,
                                notesInput.getText().toString().trim()
                        ));
                        
                        selectedItemsIndices.remove(0);
                        processNextBodyPart();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid pain level! Enter 0-10", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Skip This Body Part", (dialogInterface, which) -> {
                    // Add to processed list even if skipped
                    processedItemsIndices.add(selectedItemsIndices.get(0));
                    selectedItemsIndices.remove(0);
                    processNextBodyPart();
                })
                .setNegativeButton(backButtonText, (dialogInterface, which) -> {
                    if (canGoBack) {
                        // Go back to previous body part
                        goBackToPreviousBodyPart();
                    } else {
                        // Go back to body part selection
                        showBodyPartSelector();
                    }
                })
                .setCancelable(true);
                
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void goBackToPreviousBodyPart() {
        if (processedItemsIndices.isEmpty()) {
            // If no previous body parts, go back to selection
            showBodyPartSelector();
            return;
        }
        
        // Get the last processed body part
        int previousIndex = processedItemsIndices.remove(processedItemsIndices.size() - 1);
        
        // Remove the corresponding entry from detailed entries if it exists
        if (!detailedPainEntries.isEmpty()) {
            detailedPainEntries.remove(detailedPainEntries.size() - 1);
        }
        
        // Add it back to the beginning of the selectedItemsIndices
        selectedItemsIndices.add(0, previousIndex);
        
        // Process it again
        processNextBodyPart();
    }

    private void attemptSaveGeneralLog() {
        String uriString = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_GENERAL_LOG_FILE_URI, null);
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            if (isUriAccessible(uri)) {
                writeGeneralLog(uri);
                return;
            }
        }
        createFile(CREATE_GENERAL_FILE_REQUEST_CODE, "painlog.csv");
    }

    private void attemptSaveDetailedLog() {
        if (detailedPainEntries.isEmpty()) {
            return;
        }

        String uriString = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_DETAILED_LOG_FILE_URI, null);
        if (uriString != null) {
            Uri uri = Uri.parse(uriString);
            if (isUriAccessible(uri)) {
                writeDetailedLog(uri);
                return;
            }
        }
        createFile(CREATE_DETAILED_FILE_REQUEST_CODE, "detailed_painlog.csv");
    }

    private void createFile(int requestCode, String fileName) {
        // Save the request code for later use in handleDocumentCreation
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt("last_request_code", requestCode)
                .apply();
                
        // Use the ActivityResultLauncher instead of startActivityForResult
        createDocumentLauncher.launch(fileName);
    }

    private void handleDocumentCreation(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            android.content.SharedPreferences.Editor editor =
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();

            // Get the last request code from shared preferences
            int lastRequestCode = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getInt("last_request_code", CREATE_GENERAL_FILE_REQUEST_CODE);

            if (lastRequestCode == CREATE_GENERAL_FILE_REQUEST_CODE) {
                editor.putString(KEY_GENERAL_LOG_FILE_URI, uri.toString());
                editor.apply();
                writeGeneralLog(uri);
            } else if (lastRequestCode == CREATE_DETAILED_FILE_REQUEST_CODE) {
                editor.putString(KEY_DETAILED_LOG_FILE_URI, uri.toString());
                editor.apply();
                writeDetailedLog(uri);
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission error!", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeGeneralLog(Uri uri) {
        try {
            StringBuilder content = new StringBuilder();
            if (!appendExistingContent(uri)) {
                content.append("Timestamp,OverallPain,Triggers\n");
            }
            content.append(String.format(Locale.US,
                    "%s,%s,\"%s\"\n",
                    sessionTimestamp,
                    initialPainLevel,
                    initialTriggers.replace("\"", "\"\"")));

            try (java.io.OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output != null) {
                    output.write(content.toString().getBytes());
                    Toast.makeText(this, "General log saved!", Toast.LENGTH_SHORT).show();
                    attemptSaveDetailedLog();
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save general log", Toast.LENGTH_SHORT).show();
        }
    }

    private void writeDetailedLog(Uri uri) {
        try {
            StringBuilder content = new StringBuilder();
            if (!appendExistingContent(uri)) {
                content.append("Timestamp,BodyPart,PainLevel,Notes\n");
            }
            for (DetailedPainEntry entry : detailedPainEntries) {
                content.append(String.format(Locale.US,
                        "%s,%s,%d,\"%s\"\n",
                        sessionTimestamp,
                        entry.getBodyPart(),
                        entry.getIntensity(),
                        entry.getNotes().replace("\"", "\"\"")));
            }

            try (java.io.OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output != null) {
                    output.write(content.toString().getBytes());
                    Toast.makeText(this, "Detailed log saved!", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save detailed log", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean appendExistingContent(Uri uri) {
        try {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(getContentResolver().openInputStream(uri)))) {
                String line;
                StringBuilder content = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }

    private boolean isUriAccessible(Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                inputStream.close();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void onSaveReminder(Reminder reminder) {
        Log.i(TAG, "Reminder saved: " + reminder);
        Toast.makeText(this, "Reminder received in MainActivity", Toast.LENGTH_SHORT).show();
    }

    public void onDeleteReminder(String reminderId) {
        Log.i(TAG, "Delete reminder requested for ID: " + reminderId);
        Toast.makeText(this, "Delete reminder requested for ID: " + reminderId,
                Toast.LENGTH_SHORT).show();
    }
}

