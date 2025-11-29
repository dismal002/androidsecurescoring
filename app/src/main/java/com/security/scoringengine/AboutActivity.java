package com.security.scoringengine;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AboutActivity extends AppCompatActivity {
    protected LinearLayout preferenceContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("About");
        }
        
        preferenceContainer = findViewById(R.id.preference_container);
        buildPreferences();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    protected void buildPreferences() {
        // App Version
        addPreference("Application Version", getAppVersion(), null);
        
        // Contributors Header
        addHeaderPreference("Contributors");
        
        // Add GitHub links
        addClickablePreference("Dismal", "Original Creator", v -> openUrl("https://github.com/dismal002"));
        addClickablePreference("InsaaneUnicorn", "Tester", v -> openUrl("https://github.com/insaaneunicorn"));
        
        // Project Repository
        addHeaderPreference("Project");
        addClickablePreference("GitHub Repository", "View source", v -> openUrl("https://github.com/dismal002/androidsecurescoring"));
    }
    
    private void addPreference(String title, String summary, View.OnClickListener listener) {
        View view = LayoutInflater.from(this).inflate(R.layout.preference, preferenceContainer, false);
        TextView titleView = view.findViewById(android.R.id.title);
        TextView summaryView = view.findViewById(android.R.id.summary);
        
        titleView.setText(title);
        if (summary != null && !summary.isEmpty()) {
            summaryView.setText(summary);
            summaryView.setVisibility(View.VISIBLE);
        }
        
        if (listener != null) {
            view.setOnClickListener(listener);
        } else {
            view.setClickable(false);
        }
        
        preferenceContainer.addView(view);
    }
    
    private void addClickablePreference(String title, String summary, View.OnClickListener listener) {
        addPreference(title, summary, listener);
    }
    
    private void addHeaderPreference(String title) {
        View view = LayoutInflater.from(this).inflate(R.layout.preference, preferenceContainer, false);
        TextView titleView = view.findViewById(android.R.id.title);
        
        titleView.setText(title);
        titleView.setTextAppearance(android.R.style.TextAppearance_Material_Subhead);
        view.setClickable(false);
        view.setPadding(view.getPaddingLeft(), 32, view.getPaddingRight(), 8);
        
        preferenceContainer.addView(view);
    }
    
    private String getAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName + " (" + pInfo.versionCode + ")";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }
}
