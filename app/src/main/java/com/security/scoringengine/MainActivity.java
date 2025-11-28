package com.security.scoringengine;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.security.scoringengine.models.ScoreItem;
import com.security.scoringengine.scoring.ScoringEngine;
import com.security.scoringengine.security.SecureConfigStorage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_FILE = 1001;
    private static final int REQUEST_CODE_PERMISSIONS = 1002;
    
    // Setup screen views
    private LinearLayout setupLayout;
    private TextView setupMessageTextView;
    private Button selectConfigButton;
    
    // Main screen views
    private LinearLayout mainLayout;
    private TextView scoreTextView;
    private TextView reportTextView;
    private Button refreshButton;
    private Button resetConfigButton;
    private Button forensicsButton;
    private ScrollView scrollView;
    
    private ScoringService scoringService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ScoringService.LocalBinder binder = (ScoringService.LocalBinder) service;
            scoringService = binder.getService();
            serviceBound = true;
            
            scoringService.setCallback(new ScoringService.ScoringCallback() {
                @Override
                public void onScoreUpdated(ScoringEngine.ScoringResult result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUI(result);
                        }
                    });
                }
            });
            
            ScoringEngine.ScoringResult lastResult = scoringService.getLastResult();
            if (lastResult != null) {
                updateUI(lastResult);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            scoringService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        setupLayout = findViewById(R.id.setupLayout);
        setupMessageTextView = findViewById(R.id.setupMessageTextView);
        selectConfigButton = findViewById(R.id.selectConfigButton);
        
        mainLayout = findViewById(R.id.mainLayout);
        scoreTextView = findViewById(R.id.scoreTextView);
        reportTextView = findViewById(R.id.reportTextView);
        refreshButton = findViewById(R.id.refreshButton);
        resetConfigButton = findViewById(R.id.resetConfigButton);
        forensicsButton = findViewById(R.id.forensicsButton);
        scrollView = findViewById(R.id.scrollView);
        
        // Setup button listeners
        selectConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndSelectFile();
            }
        });
        
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceBound && scoringService != null) {
                    scoringService.performScoring();
                    Toast.makeText(MainActivity.this, "Scoring refreshed", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        resetConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetConfiguration();
            }
        });
        
        forensicsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ForensicsActivity.class);
                startActivity(intent);
            }
        });
        
        // Check if configuration exists
        checkConfigurationStatus();
    }
    
    private void checkConfigurationStatus() {
        SecureConfigStorage storage = new SecureConfigStorage(this);
        try {
            String config = storage.loadConfig();
            if (config == null || config.isEmpty()) {
                showSetupScreen();
            } else {
                showMainScreen();
                startScoringService();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showSetupScreen();
        }
    }
    
    private void showSetupScreen() {
        setupLayout.setVisibility(View.VISIBLE);
        mainLayout.setVisibility(View.GONE);
    }
    
    private void showMainScreen() {
        setupLayout.setVisibility(View.GONE);
        mainLayout.setVisibility(View.VISIBLE);
    }
    
    private void startScoringService() {
        Intent serviceIntent = new Intent(this, ScoringService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void checkPermissionsAndSelectFile() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSIONS);
        } else {
            openFilePicker();
        }
    }
    
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select Scoring Configuration File"),
                    REQUEST_CODE_PICK_FILE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFilePicker();
            } else {
                Toast.makeText(this, "Storage permission is required to load configuration",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                loadConfigFromUri(uri);
            }
        }
    }
    
    private void loadConfigFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            inputStream.close();
            
            String configJson = sb.toString();
            
            // Validate JSON by trying to parse it
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.security.scoringengine.models.ScoringConfig config = 
                gson.fromJson(configJson, com.security.scoringengine.models.ScoringConfig.class);
            
            if (config == null || config.penaltiesandPoints == null) {
                Toast.makeText(this, "Invalid configuration file format", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Save encrypted configuration
            SecureConfigStorage storage = new SecureConfigStorage(this);
            storage.saveConfig(configJson);
            
            Toast.makeText(this, "Configuration loaded and encrypted successfully", 
                Toast.LENGTH_SHORT).show();
            
            // Switch to main screen and start service
            showMainScreen();
            startScoringService();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error loading configuration: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    private void resetConfiguration() {
        try {
            // Stop service
            if (serviceBound) {
                unbindService(serviceConnection);
                serviceBound = false;
            }
            Intent serviceIntent = new Intent(this, ScoringService.class);
            stopService(serviceIntent);
            
            // Delete encrypted config
            File configFile = new File(getFilesDir(), "scoring_config.enc");
            if (configFile.exists()) {
                configFile.delete();
            }
            
            Toast.makeText(this, "Configuration reset. Please select a new configuration file.",
                    Toast.LENGTH_SHORT).show();
            
            showSetupScreen();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error resetting configuration: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }



    private void updateUI(ScoringEngine.ScoringResult result) {
        scoreTextView.setText(String.format("Score: %d / %d", 
            result.getCurrentPoints(), result.getMaxPoints()));
        
        StringBuilder report = new StringBuilder();
        report.append("=== SCORING REPORT ===\n\n");
        
        List<ScoreItem> items = result.getScoreItems();
        Collections.sort(items, new Comparator<ScoreItem>() {
            @Override
            public int compare(ScoreItem o1, ScoreItem o2) {
                return o1.getCategory().compareTo(o2.getCategory());
            }
        });
        
        String currentCategory = "";
        for (ScoreItem item : items) {
            if (!item.getCategory().equals(currentCategory)) {
                currentCategory = item.getCategory();
                report.append("\n--- ").append(currentCategory.toUpperCase()).append(" ---\n");
            }
            
            String sign = item.getPoints() >= 0 ? "+" : "";
            report.append(String.format("%s - %s%d Points\n", 
                item.getDescription(), sign, item.getPoints()));
        }
        
        if (items.isEmpty()) {
            report.append("No tasks completed yet.\n");
        }
        
        report.append("\n======================\n");
        report.append(String.format("Total: %d / %d Points", 
            result.getCurrentPoints(), result.getMaxPoints()));
        
        reportTextView.setText(report.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }
}
