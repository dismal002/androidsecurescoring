package com.security.scoringengine;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.google.gson.Gson;
import com.security.scoringengine.models.ScoringConfig;
import com.security.scoringengine.scoring.ScoringEngine;
import com.security.scoringengine.security.SecureConfigStorage;

public class ScoringService extends Service {
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "ScoringEngineChannel";
    private static final long CHECK_INTERVAL = 2 * 60 * 1000; // 2 minutes

    private Handler handler;
    private Runnable scoringRunnable;
    private ScoringEngine scoringEngine;
    private SecureConfigStorage configStorage;
    private ScoringEngine.ScoringResult lastResult;
    private ScoringCallback callback;

    public interface ScoringCallback {
        void onScoreUpdated(ScoringEngine.ScoringResult result);
    }

    public class LocalBinder extends Binder {
        public ScoringService getService() {
            return ScoringService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        
        configStorage = new SecureConfigStorage(this);
        handler = new Handler();
        
        loadConfigAndInitialize();
        
        scoringRunnable = new Runnable() {
            @Override
            public void run() {
                performScoring();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
        
        handler.post(scoringRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallback(ScoringCallback callback) {
        this.callback = callback;
    }

    public void performScoring() {
        if (scoringEngine != null) {
            lastResult = scoringEngine.calculateScore();
            if (callback != null) {
                callback.onScoreUpdated(lastResult);
            }
        }
    }

    public ScoringEngine.ScoringResult getLastResult() {
        return lastResult;
    }
    
    public boolean hasConfiguration() {
        return scoringEngine != null;
    }

    private void loadConfigAndInitialize() {
        try {
            String configJson = configStorage.loadConfig();
            if (configJson != null && !configJson.isEmpty()) {
                Gson gson = new Gson();
                ScoringConfig config = gson.fromJson(configJson, ScoringConfig.class);
                scoringEngine = new ScoringEngine(this, config);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "Scoring Engine Service",
            NotificationManager.IMPORTANCE_LOW);
        
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Security Scoring Engine")
            .setContentText("Monitoring security tasks...")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && scoringRunnable != null) {
            handler.removeCallbacks(scoringRunnable);
        }
    }
}
