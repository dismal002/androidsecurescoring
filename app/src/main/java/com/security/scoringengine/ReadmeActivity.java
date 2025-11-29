package com.security.scoringengine;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ReadmeActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readme);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("README");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        try {
            File readmeFile = new File(getFilesDir(), "readme.html");
            if (readmeFile.exists()) {
                // Read the HTML file as bytes to preserve all formatting
                java.io.FileInputStream fis = new java.io.FileInputStream(readmeFile);
                byte[] buffer = new byte[(int) readmeFile.length()];
                fis.read(buffer);
                fis.close();
                
                String htmlContent = new String(buffer, "UTF-8");
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null);
            } else {
                webView.loadData("<html><body><h1>README not found</h1><p>No README file has been loaded.</p></body></html>", 
                    "text/html", "UTF-8");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading README", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
