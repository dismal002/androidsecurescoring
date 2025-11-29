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
    private static final int REQUEST_CODE_PICK_CONFIG = 1001;
    private static final int REQUEST_CODE_PICK_README = 1002;
    private static final int REQUEST_CODE_PERMISSIONS = 1003;
    private static final String README_FILENAME = "readme.html";
    
    // Setup screen views
    private LinearLayout setupLayout;
    private TextView setupMessageTextView;
    private Button selectConfigButton;
    private Button selectReadmeButton;
    private TextView readmeStatusText;
    
    // Main screen views
    private ScrollView mainLayout;
    private TextView scoreTextView;
    private TextView reportTextView;
    private Button refreshButton;
    private Button forensicsButton;
    
    private ScoringService scoringService;
    private boolean serviceBound = false;
    private int lastScore = 0;

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
        selectReadmeButton = findViewById(R.id.selectReadmeButton);
        readmeStatusText = findViewById(R.id.readmeStatusText);
        
        mainLayout = findViewById(R.id.mainLayout);
        scoreTextView = findViewById(R.id.scoreTextView);
        reportTextView = findViewById(R.id.reportTextView);
        refreshButton = findViewById(R.id.refreshButton);
        forensicsButton = findViewById(R.id.forensicsButton);
        
        Button readmeButton = findViewById(R.id.readmeButton);
        readmeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ReadmeActivity.class);
                startActivity(intent);
            }
        });
        
        // Setup button listeners
        selectConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndSelectFile(REQUEST_CODE_PICK_CONFIG);
            }
        });
        
        selectReadmeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionsAndSelectFile(REQUEST_CODE_PICK_README);
            }
        });
        
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceBound && scoringService != null) {
                    scoringService.performScoring();
                    Toast.makeText(MainActivity.this, "Score refreshed", Toast.LENGTH_SHORT).show();
                }
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
        
        // Update README button visibility
        Button readmeButton = findViewById(R.id.readmeButton);
        if (hasReadme()) {
            readmeButton.setVisibility(View.VISIBLE);
        } else {
            readmeButton.setVisibility(View.GONE);
        }
    }
    
    private void startScoringService() {
        Intent serviceIntent = new Intent(this, ScoringService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void checkPermissionsAndSelectFile(int requestCode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSIONS);
        } else {
            openFilePicker(requestCode);
        }
    }
    
    private void openFilePicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        String title = requestCode == REQUEST_CODE_PICK_CONFIG ? 
            "Select Scoring Configuration File" : "Select README.html File";
        
        try {
            startActivityForResult(Intent.createChooser(intent, title), requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted. Please select file again.", 
                    Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage permission is required to load files",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (requestCode == REQUEST_CODE_PICK_CONFIG) {
                loadConfigFromUri(uri);
            } else if (requestCode == REQUEST_CODE_PICK_README) {
                loadReadmeFromUri(uri);
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
            
            Toast.makeText(this, "Configuration loaded successfully", 
                Toast.LENGTH_SHORT).show();
            
            // Check if we should proceed to main screen
            if (hasReadme()) {
                // Both files loaded, proceed
                showMainScreen();
                startScoringService();
            } else {
                // Prompt for README
                readmeStatusText.setText("✓ Config loaded. Now select README.html (optional)");
                selectReadmeButton.setEnabled(true);
            }
            
        } catch (Exception e) {
            Toast.makeText(this, "Error loading configuration: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    




    private void updateUI(ScoringEngine.ScoringResult result) {
        int currentScore = result.getCurrentPoints();
        
        // Show toast for score changes
        if (lastScore != 0) {  // Don't show on first load
            if (currentScore > lastScore) {
                Toast.makeText(this, "You have gained points!", Toast.LENGTH_SHORT).show();
            } else if (currentScore < lastScore) {
                Toast.makeText(this, "You have lost points!", Toast.LENGTH_SHORT).show();
            }
        }
        lastScore = currentScore;
        
        scoreTextView.setText(String.format("%d / %d", 
            result.getCurrentPoints(), result.getMaxPoints()));
        
        StringBuilder report = new StringBuilder();
        
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
                report.append("\n━━━ ").append(currentCategory.toUpperCase()).append(" ━━━\n\n");
            }
            
            String sign = item.getPoints() >= 0 ? "+" : "";
            String icon = item.getPoints() >= 0 ? "✓" : "✗";
            report.append(String.format("%s %s\n   %s%d points\n\n", 
                icon, item.getDescription(), sign, item.getPoints()));
        }
        
        if (items.isEmpty()) {
            report.append("No tasks completed yet.\n\nComplete security tasks to earn points.");
        }
        
        reportTextView.setText(report.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void loadReadmeFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            
            // Read the HTML file as bytes to preserve formatting
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            
            // Save README to internal storage (not encrypted, preserves HTML formatting)
            File readmeFile = new File(getFilesDir(), README_FILENAME);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(readmeFile);
            fos.write(buffer);
            fos.close();
            
            Toast.makeText(this, "README loaded successfully", Toast.LENGTH_SHORT).show();
            
            // Proceed to main screen
            showMainScreen();
            startScoringService();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error loading README: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    private boolean hasReadme() {
        File readmeFile = new File(getFilesDir(), README_FILENAME);
        return readmeFile.exists();
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
