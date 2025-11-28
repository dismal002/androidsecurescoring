package com.security.scoringengine;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.security.scoringengine.models.ScoringConfig;
import com.security.scoringengine.security.SecureConfigStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForensicsActivity extends AppCompatActivity {
    private static final long COOLDOWN_DURATION = 2 * 60 * 1000; // 2 minutes
    private static final String PREFS_NAME = "ForensicsPrefs";
    private static final String PREFS_ANSWERED = "answered_questions";
    private static final String PREFS_COOLDOWNS = "cooldowns";

    private LinearLayout questionsContainer;
    private TextView noQuestionsTextView;
    private ScrollView scrollView;

    private ScoringConfig config;
    private Map<String, Long> cooldownMap;
    private Map<String, Boolean> answeredMap;
    private Handler handler;

    private ScoringService scoringService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ScoringService.LocalBinder binder = (ScoringService.LocalBinder) service;
            scoringService = binder.getService();
            serviceBound = true;
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
        setContentView(R.layout.activity_forensics);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Forensics Questions");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        questionsContainer = findViewById(R.id.questionsContainer);
        noQuestionsTextView = findViewById(R.id.noQuestionsTextView);
        scrollView = findViewById(R.id.scrollView);

        handler = new Handler();
        cooldownMap = new HashMap<>();
        answeredMap = new HashMap<>();

        loadConfiguration();
        loadPersistedData();
        buildQuestionsUI();

        Intent serviceIntent = new Intent(this, ScoringService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        startCooldownUpdater();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadConfiguration() {
        try {
            SecureConfigStorage storage = new SecureConfigStorage(this);
            String configJson = storage.loadConfig();
            if (configJson != null && !configJson.isEmpty()) {
                Gson gson = new Gson();
                config = gson.fromJson(configJson, ScoringConfig.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading configuration", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadPersistedData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load answered questions
        String answeredJson = prefs.getString(PREFS_ANSWERED, "{}");
        Gson gson = new Gson();
        Map<String, Boolean> loadedAnswered = gson.fromJson(answeredJson, Map.class);
        if (loadedAnswered != null) {
            answeredMap.putAll(loadedAnswered);
        }

        // Load cooldowns
        String cooldownsJson = prefs.getString(PREFS_COOLDOWNS, "{}");
        Map<String, Double> loadedCooldowns = gson.fromJson(cooldownsJson, Map.class);
        if (loadedCooldowns != null) {
            for (Map.Entry<String, Double> entry : loadedCooldowns.entrySet()) {
                cooldownMap.put(entry.getKey(), entry.getValue().longValue());
            }
        }
    }

    private void savePersistedData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        editor.putString(PREFS_ANSWERED, gson.toJson(answeredMap));
        editor.putString(PREFS_COOLDOWNS, gson.toJson(cooldownMap));
        editor.apply();
    }

    private void buildQuestionsUI() {
        questionsContainer.removeAllViews();

        if (config == null || config.forensicsQuestions == null || config.forensicsQuestions.isEmpty()) {
            noQuestionsTextView.setVisibility(View.VISIBLE);
            scrollView.setVisibility(View.GONE);
            return;
        }

        noQuestionsTextView.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);

        List<String> sortedKeys = new ArrayList<>(config.forensicsQuestions.keySet());
        java.util.Collections.sort(sortedKeys);

        for (String questionId : sortedKeys) {
            List<String> questionData = config.forensicsQuestions.get(questionId);
            if (questionData == null || questionData.size() < 2) continue;

            String questionText = questionData.get(0);
            String answer = questionData.get(1);

            View questionView = createQuestionView(questionId, questionText, answer);
            questionsContainer.addView(questionView);
        }
    }

    private View createQuestionView(String questionId, String questionText, String answer) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(16, 16, 16, 16);
        container.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, 16);
        container.setLayoutParams(containerParams);

        // Question ID
        TextView idTextView = new TextView(this);
        idTextView.setText(questionId);
        idTextView.setTextSize(12);
        idTextView.setTextColor(0xFF666666);
        idTextView.setPadding(0, 0, 0, 8);
        container.addView(idTextView);

        // Question text
        TextView questionTextView = new TextView(this);
        questionTextView.setText(questionText);
        questionTextView.setTextSize(16);
        questionTextView.setTextColor(0xFF000000);
        questionTextView.setPadding(0, 0, 0, 16);
        container.addView(questionTextView);

        // Check if already answered
        boolean isAnswered = answeredMap.containsKey(questionId) && answeredMap.get(questionId);

        if (isAnswered) {
            TextView answeredTextView = new TextView(this);
            answeredTextView.setText("✓ Answered Correctly");
            answeredTextView.setTextSize(14);
            answeredTextView.setTextColor(0xFF4CAF50);
            answeredTextView.setTextStyle(android.graphics.Typeface.BOLD);
            answeredTextView.setPadding(0, 8, 0, 8);
            container.addView(answeredTextView);
        } else {
            // Answer input
            EditText answerInput = new EditText(this);
            answerInput.setHint("Enter your answer");
            answerInput.setSingleLine(true);
            answerInput.setPadding(16, 16, 16, 16);
            container.addView(answerInput);

            // Submit button and cooldown text
            LinearLayout buttonContainer = new LinearLayout(this);
            buttonContainer.setOrientation(LinearLayout.HORIZONTAL);
            buttonContainer.setPadding(0, 8, 0, 0);

            Button submitButton = new Button(this);
            submitButton.setText("Submit Answer");
            submitButton.setId(View.generateViewId());

            TextView cooldownTextView = new TextView(this);
            cooldownTextView.setTextSize(12);
            cooldownTextView.setTextColor(0xFFFF5722);
            cooldownTextView.setPadding(16, 0, 0, 0);
            cooldownTextView.setVisibility(View.GONE);

            buttonContainer.addView(submitButton);
            buttonContainer.addView(cooldownTextView);
            container.addView(buttonContainer);

            // Update button state
            updateButtonState(questionId, submitButton, cooldownTextView);

            submitButton.setOnClickListener(v -> {
                String userAnswer = answerInput.getText().toString().trim();
                if (userAnswer.isEmpty()) {
                    Toast.makeText(this, "Please enter an answer", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isOnCooldown(questionId)) {
                    long remaining = getRemainingCooldown(questionId);
                    Toast.makeText(this, "Please wait " + formatTime(remaining) + " before trying again",
                        Toast.LENGTH_SHORT).show();
                    return;
                }

                checkAnswer(questionId, userAnswer, answer, answerInput, submitButton, cooldownTextView, container);
            });
        }

        return container;
    }

    private void checkAnswer(String questionId, String userAnswer, String correctAnswer,
                            EditText answerInput, Button submitButton, TextView cooldownTextView,
                            LinearLayout container) {
        boolean isCorrect = userAnswer.equalsIgnoreCase(correctAnswer.trim());

        if (isCorrect) {
            answeredMap.put(questionId, true);
            savePersistedData();

            Toast.makeText(this, "Correct! Points awarded.", Toast.LENGTH_LONG).show();

            // Trigger scoring update
            if (serviceBound && scoringService != null) {
                scoringService.performScoring();
            }

            // Rebuild this question's view
            questionsContainer.removeView(container);
            View newView = createQuestionView(questionId, 
                config.forensicsQuestions.get(questionId).get(0), correctAnswer);
            questionsContainer.addView(newView, getQuestionIndex(questionId));

        } else {
            cooldownMap.put(questionId, System.currentTimeMillis() + COOLDOWN_DURATION);
            savePersistedData();

            Toast.makeText(this, "Incorrect answer. Try again in 2 minutes.", Toast.LENGTH_LONG).show();

            answerInput.setText("");
            updateButtonState(questionId, submitButton, cooldownTextView);
        }
    }

    private int getQuestionIndex(String questionId) {
        List<String> sortedKeys = new ArrayList<>(config.forensicsQuestions.keySet());
        java.util.Collections.sort(sortedKeys);
        return sortedKeys.indexOf(questionId);
    }

    private boolean isOnCooldown(String questionId) {
        if (!cooldownMap.containsKey(questionId)) return false;
        long cooldownEnd = cooldownMap.get(questionId);
        return System.currentTimeMillis() < cooldownEnd;
    }

    private long getRemainingCooldown(String questionId) {
        if (!cooldownMap.containsKey(questionId)) return 0;
        long cooldownEnd = cooldownMap.get(questionId);
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    private void updateButtonState(String questionId, Button button, TextView cooldownTextView) {
        if (isOnCooldown(questionId)) {
            button.setEnabled(false);
            cooldownTextView.setVisibility(View.VISIBLE);
            long remaining = getRemainingCooldown(questionId);
            cooldownTextView.setText("Cooldown: " + formatTime(remaining));
        } else {
            button.setEnabled(true);
            cooldownTextView.setVisibility(View.GONE);
            // Remove expired cooldown
            cooldownMap.remove(questionId);
            savePersistedData();
        }
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void startCooldownUpdater() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateAllCooldowns();
                handler.postDelayed(this, 1000); // Update every second
            }
        }, 1000);
    }

    private void updateAllCooldowns() {
        for (int i = 0; i < questionsContainer.getChildCount(); i++) {
            View child = questionsContainer.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout container = (LinearLayout) child;
                if (container.getChildCount() > 0) {
                    TextView idTextView = (TextView) container.getChildAt(0);
                    String questionId = idTextView.getText().toString();

                    // Find button and cooldown text
                    for (int j = 0; j < container.getChildCount(); j++) {
                        View view = container.getChildAt(j);
                        if (view instanceof LinearLayout) {
                            LinearLayout buttonContainer = (LinearLayout) view;
                            if (buttonContainer.getChildCount() >= 2) {
                                Button button = (Button) buttonContainer.getChildAt(0);
                                TextView cooldownText = (TextView) buttonContainer.getChildAt(1);
                                updateButtonState(questionId, button, cooldownText);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    public Map<String, Boolean> getAnsweredQuestions() {
        return new HashMap<>(answeredMap);
    }
}
