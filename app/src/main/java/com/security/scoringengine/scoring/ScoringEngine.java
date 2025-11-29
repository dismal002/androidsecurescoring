package com.security.scoringengine.scoring;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.google.gson.Gson;
import com.security.scoringengine.models.PolicyState;
import com.security.scoringengine.models.ScoreItem;
import com.security.scoringengine.models.ScoringConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScoringEngine {
    private static final String PREFS_NAME = "ForensicsPrefs";
    private static final String PREFS_ANSWERED = "answered_questions";
    
    private Context context;
    private ScoringConfig config;
    private Map<String, ScoreItem> currentScores;
    private Set<String> previousUsers;
    
    public ScoringEngine(Context context, ScoringConfig config) {
        this.context = context;
        this.config = config;
        this.currentScores = new HashMap<>();
        this.previousUsers = new HashSet<>();
    }

    public ScoringResult calculateScore() {
        Map<String, ScoreItem> newScores = new HashMap<>();
        int totalPoints = 0;
        int maxPoints = calculateMaxPoints();
        
        android.util.Log.d("ScoringEngine", "Starting score calculation. Max points: " + maxPoints);
        
        try {
            PolicyState policyState = loadPolicyState();
            android.util.Log.d("ScoringEngine", "Policy state loaded successfully");
            
            // Check users
            totalPoints += checkUsers(policyState, newScores);
            
            // Check policies
            totalPoints += checkDeviceRestrictions(policyState, newScores);
            totalPoints += checkUserRestrictions(policyState, newScores);
            totalPoints += checkPasswordPolicies(policyState, newScores);
            totalPoints += checkAdditionalRestrictions(policyState, newScores);
            totalPoints += checkSystemUpdatePolicy(policyState, newScores);
            
            // Check settings
            totalPoints += checkSettingsSecure(newScores);
            totalPoints += checkSettingsSystem(newScores);
            totalPoints += checkSettingsGlobal(newScores);
            
            // Check files
            totalPoints += checkFileDeletions(newScores);
            
            // Check apps
            totalPoints += checkAppDeletions(newScores);
            totalPoints += checkAppInstalls(newScores);
            totalPoints += checkAppUpdates(newScores);
            
            // Check forensics questions
            totalPoints += checkForensicsQuestions(newScores);
            
            android.util.Log.d("ScoringEngine", "Score calculation complete. Total: " + totalPoints + "/" + maxPoints);
            android.util.Log.d("ScoringEngine", "Score items: " + newScores.size());
            
        } catch (Exception e) {
            android.util.Log.e("ScoringEngine", "Error calculating score", e);
            e.printStackTrace();
        }
        
        currentScores = newScores;
        return new ScoringResult(totalPoints, maxPoints, new ArrayList<>(newScores.values()));
    }

    private int checkUsers(PolicyState policyState, Map<String, ScoreItem> scores) {
        int points = 0;
        Set<String> currentUsers = new HashSet<>();
        
        if (policyState.userProfiles != null) {
            for (PolicyState.UserProfile profile : policyState.userProfiles) {
                currentUsers.add(profile.userName);
            }
        }
        
        // Check UsersAdditions
        if (config.UsersAdditions != null) {
            for (String user : config.UsersAdditions) {
                String key = "user_add_" + user;
                if (currentUsers.contains(user)) {
                    if (!scores.containsKey(key)) {
                        scores.put(key, new ScoreItem(
                            "User '" + user + "' has been added",
                            config.penaltiesandPoints.userPoints,
                            "users"));
                        points += config.penaltiesandPoints.userPoints;
                    }
                } else if (previousUsers.contains(user)) {
                    // User was removed - penalty
                    scores.put(key + "_penalty", new ScoreItem(
                        "User '" + user + "' was removed (penalty)",
                        -config.penaltiesandPoints.userPenalty,
                        "users"));
                    points -= config.penaltiesandPoints.userPenalty;
                }
            }
        }
        
        // Check AuthorizedUsers
        if (config.AuthorizedUsers != null) {
            for (String user : config.AuthorizedUsers) {
                if (!currentUsers.contains(user)) {
                    String key = "auth_user_removed_" + user;
                    scores.put(key, new ScoreItem(
                        "Authorized user '" + user + "' was removed (penalty)",
                        -config.penaltiesandPoints.userPenalty,
                        "users"));
                    points -= config.penaltiesandPoints.userPenalty;
                }
            }
        }
        
        // Check UnauthorizedUsers
        if (config.UnauthorizedUsers != null) {
            for (String user : config.UnauthorizedUsers) {
                String key = "unauth_user_" + user;
                if (!currentUsers.contains(user)) {
                    scores.put(key, new ScoreItem(
                        "Unauthorized user '" + user + "' has been removed",
                        config.penaltiesandPoints.userPoints,
                        "users"));
                    points += config.penaltiesandPoints.userPoints;
                }
            }
        }
        
        previousUsers = currentUsers;
        return points;
    }

    private int checkDeviceRestrictions(PolicyState policyState, Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.deviceRestrictions != null && policyState.devicePolicies != null) {
            if (config.deviceRestrictions.screenCaptureDisabled != null &&
                config.deviceRestrictions.screenCaptureDisabled == policyState.devicePolicies.screenCaptureDisabled) {
                scores.put("device_screen_capture", new ScoreItem(
                    "Screen capture disabled policy set correctly",
                    config.penaltiesandPoints.policyPoints,
                    "policy"));
                points += config.penaltiesandPoints.policyPoints;
            }
            
            if (config.deviceRestrictions.networkLoggingEnabled != null &&
                config.deviceRestrictions.networkLoggingEnabled == policyState.devicePolicies.networkLoggingEnabled) {
                scores.put("device_network_logging", new ScoreItem(
                    "Network logging policy set correctly",
                    config.penaltiesandPoints.policyPoints,
                    "policy"));
                points += config.penaltiesandPoints.policyPoints;
            }
        }
        return points;
    }

    private int checkUserRestrictions(PolicyState policyState, Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.userRestrictions != null && policyState.userRestrictions != null) {
            if (config.userRestrictions.noConfigWifi != null &&
                config.userRestrictions.noConfigWifi == policyState.userRestrictions.noConfigWifi) {
                scores.put("user_no_config_wifi", new ScoreItem(
                    "WiFi configuration restriction set correctly",
                    config.penaltiesandPoints.policyPoints,
                    "policy"));
                points += config.penaltiesandPoints.policyPoints;
            }
            
            if (config.userRestrictions.disallowDebugging != null &&
                config.userRestrictions.disallowDebugging == policyState.userRestrictions.disallowDebugging) {
                scores.put("user_disallow_debugging", new ScoreItem(
                    "Debugging restriction set correctly",
                    config.penaltiesandPoints.policyPoints,
                    "policy"));
                points += config.penaltiesandPoints.policyPoints;
            }
            
            if (config.userRestrictions.noPrinting != null &&
                config.userRestrictions.noPrinting == policyState.userRestrictions.noPrinting) {
                scores.put("user_no_printing", new ScoreItem(
                    "Printing restriction set correctly",
                    config.penaltiesandPoints.policyPoints,
                    "policy"));
                points += config.penaltiesandPoints.policyPoints;
            }
        }
        return points;
    }

    private int checkPasswordPolicies(PolicyState policyState, Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.passwordPolicies != null && policyState.passwordPolicies != null) {
            if (config.passwordPolicies.passwordQualityName != null &&
                config.passwordPolicies.passwordQualityName.contains(policyState.passwordPolicies.passwordQualityName)) {
                scores.put("password_quality", new ScoreItem(
                    "Password quality set correctly",
                    config.penaltiesandPoints.policyPoints,
                    "policy"));
                points += config.penaltiesandPoints.policyPoints;
            }
            
            if (config.passwordPolicies.passwordExpirationTimeout != null &&
                config.passwordPolicies.passwordExpirationTimeout.equals(policyState.passwordPolicies.passwordExpirationTimeout)) {
                scores.put("password_expiration", new ScoreItem(
                    "Password expiration timeout set correctly",
                    config.penaltiesandPoints.policyPoints,
                    "policy"));
                points += config.penaltiesandPoints.policyPoints;
            }
        }
        return points;
    }

    private int checkAdditionalRestrictions(PolicyState policyState, Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.additionalRestrictions != null && policyState.additionalRestrictions != null) {
            if (config.additionalRestrictions.disallowFactoryReset != null &&
                config.additionalRestrictions.disallowFactoryReset == policyState.additionalRestrictions.disallowFactoryReset) {
                scores.put("additional_factory_reset", new ScoreItem(
                    "Factory reset restriction set correctly",
                    config.penaltiesandPoints.policyPoints,
                    "policy"));
                points += config.penaltiesandPoints.policyPoints;
            }
        }
        return points;
    }

    private int checkSystemUpdatePolicy(PolicyState policyState, Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.systemUpdatePolicy != null && policyState.systemUpdatePolicy != null) {
            if (config.systemUpdatePolicy.policyTypeName != null &&
                config.systemUpdatePolicy.policyTypeName.equals(policyState.systemUpdatePolicy.policyTypeName)) {
                scores.put("system_update_policy", new ScoreItem(
                    "System update policy set correctly",
                    config.penaltiesandPoints.updatePoints,
                    "policy"));
                points += config.penaltiesandPoints.updatePoints;
            }
        }
        return points;
    }

    private int checkSettingsSecure(Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.SettingsSecure != null) {
            Map<String, String> settings = readSettingsXml("/data/system/users/0/settings_secure.xml");
            for (Map.Entry<String, Integer> entry : config.SettingsSecure.entrySet()) {
                String value = settings.get(entry.getKey());
                if (value != null && value.equals(String.valueOf(entry.getValue()))) {
                    scores.put("settings_secure_" + entry.getKey(), new ScoreItem(
                        "Secure setting '" + entry.getKey() + "' set correctly",
                        config.penaltiesandPoints.settingsPoints,
                        "settings"));
                    points += config.penaltiesandPoints.settingsPoints;
                }
            }
        }
        return points;
    }

    private int checkSettingsSystem(Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.SettingsSystem != null) {
            Map<String, String> settings = readSettingsXml("/data/system/users/0/settings_system.xml");
            for (Map.Entry<String, Integer> entry : config.SettingsSystem.entrySet()) {
                String value = settings.get(entry.getKey());
                if (value != null && value.equals(String.valueOf(entry.getValue()))) {
                    scores.put("settings_system_" + entry.getKey(), new ScoreItem(
                        "System setting '" + entry.getKey() + "' set correctly",
                        config.penaltiesandPoints.settingsPoints,
                        "settings"));
                    points += config.penaltiesandPoints.settingsPoints;
                }
            }
        }
        return points;
    }

    private int checkSettingsGlobal(Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.SettingsGlobal != null) {
            Map<String, String> settings = readSettingsXml("/data/system/users/0/settings_global.xml");
            for (Map.Entry<String, Integer> entry : config.SettingsGlobal.entrySet()) {
                String value = settings.get(entry.getKey());
                if (value != null && value.equals(String.valueOf(entry.getValue()))) {
                    scores.put("settings_global_" + entry.getKey(), new ScoreItem(
                        "Global setting '" + entry.getKey() + "' set correctly",
                        config.penaltiesandPoints.settingsPoints,
                        "settings"));
                    points += config.penaltiesandPoints.settingsPoints;
                }
            }
        }
        return points;
    }

    private int checkFileDeletions(Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.fileDeletions != null) {
            for (String filePath : config.fileDeletions) {
                boolean exists = checkFileExistsWithRoot(filePath);
                if (!exists) {
                    String fileName = new File(filePath).getName();
                    scores.put("file_" + filePath, new ScoreItem(
                        fileName + " has been deleted",
                        config.penaltiesandPoints.fileDeletionPoints,
                        "files"));
                    points += config.penaltiesandPoints.fileDeletionPoints;
                }
            }
        }
        return points;
    }
    
    private boolean checkFileExistsWithRoot(String filePath) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "test -e " + filePath + " && echo exists"});
            BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            int exitCode = process.waitFor();
            reader.close();
            return exitCode == 0 && "exists".equals(result);
        } catch (Exception e) {
            android.util.Log.w("ScoringEngine", "Error checking file existence: " + filePath, e);
            // Fallback to regular file check
            return new File(filePath).exists();
        }
    }

    private int checkAppDeletions(Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.appDeletions != null) {
            PackageManager pm = context.getPackageManager();
            for (String packageName : config.appDeletions) {
                if (!isAppInstalled(pm, packageName)) {
                    scores.put("app_del_" + packageName, new ScoreItem(
                        packageName + " has been deleted",
                        config.penaltiesandPoints.appDeletionsPoints,
                        "apps"));
                    points += config.penaltiesandPoints.appDeletionsPoints;
                }
            }
        }
        return points;
    }

    private int checkAppInstalls(Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.appInstalls != null) {
            PackageManager pm = context.getPackageManager();
            for (String packageName : config.appInstalls) {
                if (isAppInstalled(pm, packageName)) {
                    scores.put("app_inst_" + packageName, new ScoreItem(
                        packageName + " has been installed",
                        config.penaltiesandPoints.appInstallPoints,
                        "apps"));
                    points += config.penaltiesandPoints.appInstallPoints;
                }
            }
        }
        return points;
    }

    private int checkAppUpdates(Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.appUpdates != null) {
            PackageManager pm = context.getPackageManager();
            for (Map.Entry<String, String> entry : config.appUpdates.entrySet()) {
                String packageName = entry.getKey();
                String requiredVersion = entry.getValue();
                
                try {
                    PackageInfo info = pm.getPackageInfo(packageName, 0);
                    if (compareVersions(info.versionName, requiredVersion) > 0) {
                        scores.put("app_upd_" + packageName, new ScoreItem(
                            packageName + " has been updated",
                            config.penaltiesandPoints.updatePoints,
                            "apps"));
                        points += config.penaltiesandPoints.updatePoints;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // App not installed
                }
            }
        }
        return points;
    }

    private int checkForensicsQuestions(Map<String, ScoreItem> scores) {
        int points = 0;
        if (config.forensicsQuestions != null && !config.forensicsQuestions.isEmpty() && 
            config.penaltiesandPoints != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String answeredJson = prefs.getString(PREFS_ANSWERED, "{}");
            
            Gson gson = new Gson();
            Map<String, Boolean> answeredMap = gson.fromJson(answeredJson, Map.class);
            
            if (answeredMap != null) {
                for (Map.Entry<String, Boolean> entry : answeredMap.entrySet()) {
                    String questionId = entry.getKey();
                    Boolean isAnswered = entry.getValue();
                    
                    if (isAnswered != null && isAnswered && config.forensicsQuestions.containsKey(questionId)) {
                        scores.put("forensics_" + questionId, new ScoreItem(
                            "Forensics question '" + questionId + "' answered correctly",
                            config.penaltiesandPoints.forensicsPoints,
                            "forensics"));
                        points += config.penaltiesandPoints.forensicsPoints;
                    }
                }
            }
        }
        return points;
    }

    private PolicyState loadPolicyState() throws Exception {
        String filePath = "/data/data/com.deviceconfig.policymanager/policy_state.json";
        
        android.util.Log.d("ScoringEngine", "Reading policy file with root: " + filePath);
        
        try {
            // Use su to read the file with root privileges
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat " + filePath});
            
            BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new java.io.InputStreamReader(process.getErrorStream()));
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            // Check for errors
            StringBuilder errors = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errors.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            reader.close();
            errorReader.close();
            
            if (exitCode != 0) {
                android.util.Log.e("ScoringEngine", "Failed to read policy file. Exit code: " + exitCode);
                android.util.Log.e("ScoringEngine", "Error output: " + errors.toString());
                throw new Exception("Failed to read policy file with root. Exit code: " + exitCode);
            }
            
            String jsonContent = sb.toString();
            if (jsonContent.isEmpty()) {
                android.util.Log.e("ScoringEngine", "Policy file is empty or could not be read");
                throw new Exception("Policy file is empty");
            }
            
            android.util.Log.d("ScoringEngine", "Policy file read successfully. Length: " + jsonContent.length());
            
            Gson gson = new Gson();
            PolicyState state = gson.fromJson(jsonContent, PolicyState.class);
            
            android.util.Log.d("ScoringEngine", "Policy state parsed. Users: " + 
                (state.userProfiles != null ? state.userProfiles.size() : 0));
            
            return state;
            
        } catch (Exception e) {
            android.util.Log.e("ScoringEngine", "Error reading policy file", e);
            throw new Exception("Failed to read policy file: " + e.getMessage(), e);
        }
    }

    private Map<String, String> readSettingsXml(String path) {
        Map<String, String> settings = new HashMap<>();
        try {
            android.util.Log.d("ScoringEngine", "Reading settings file with root: " + path);
            
            // Use su to read the file with root privileges
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat " + path});
            
            BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<setting")) {
                    String name = extractAttribute(line, "name");
                    String value = extractAttribute(line, "value");
                    if (name != null && value != null) {
                        settings.put(name, value);
                    }
                }
            }
            
            int exitCode = process.waitFor();
            reader.close();
            
            if (exitCode != 0) {
                android.util.Log.w("ScoringEngine", "Failed to read settings file: " + path + " (exit code: " + exitCode + ")");
            } else {
                android.util.Log.d("ScoringEngine", "Settings file read. Found " + settings.size() + " settings");
            }
            
        } catch (Exception e) {
            android.util.Log.w("ScoringEngine", "Error reading settings file: " + path, e);
        }
        return settings;
    }

    private String extractAttribute(String line, String attribute) {
        int start = line.indexOf(attribute + "=\"");
        if (start == -1) return null;
        start += attribute.length() + 2;
        int end = line.indexOf("\"", start);
        if (end == -1) return null;
        return line.substring(start, end);
    }

    private boolean isAppInstalled(PackageManager pm, String packageName) {
        try {
            pm.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) {
                return p1 - p2;
            }
        }
        return 0;
    }

    private int calculateMaxPoints() {
        int max = 0;
        
        if (config.penaltiesandPoints == null) {
            android.util.Log.e("ScoringEngine", "penaltiesandPoints is null in config!");
            return 0;
        }
        
        android.util.Log.d("ScoringEngine", "Calculating max points...");
        
        if (config.UsersAdditions != null) {
            max += config.UsersAdditions.size() * config.penaltiesandPoints.userPoints;
        }
        if (config.UnauthorizedUsers != null) {
            max += config.UnauthorizedUsers.size() * config.penaltiesandPoints.userPoints;
        }
        
        // Device restrictions (2 policies)
        if (config.deviceRestrictions != null) {
            if (config.deviceRestrictions.screenCaptureDisabled != null) max += config.penaltiesandPoints.policyPoints;
            if (config.deviceRestrictions.networkLoggingEnabled != null) max += config.penaltiesandPoints.policyPoints;
        }
        
        // User restrictions (3 policies)
        if (config.userRestrictions != null) {
            if (config.userRestrictions.noConfigWifi != null) max += config.penaltiesandPoints.policyPoints;
            if (config.userRestrictions.disallowDebugging != null) max += config.penaltiesandPoints.policyPoints;
            if (config.userRestrictions.noPrinting != null) max += config.penaltiesandPoints.policyPoints;
        }
        
        // Password policies (2 policies)
        if (config.passwordPolicies != null) {
            if (config.passwordPolicies.passwordQualityName != null) max += config.penaltiesandPoints.policyPoints;
            if (config.passwordPolicies.passwordExpirationTimeout != null) max += config.penaltiesandPoints.policyPoints;
        }
        
        // Additional restrictions
        if (config.additionalRestrictions != null && config.additionalRestrictions.disallowFactoryReset != null) {
            max += config.penaltiesandPoints.policyPoints;
        }
        
        // System update policy
        if (config.systemUpdatePolicy != null) {
            max += config.penaltiesandPoints.updatePoints;
        }
        
        // Settings
        if (config.SettingsSecure != null) {
            max += config.SettingsSecure.size() * config.penaltiesandPoints.settingsPoints;
        }
        if (config.SettingsSystem != null) {
            max += config.SettingsSystem.size() * config.penaltiesandPoints.settingsPoints;
        }
        if (config.SettingsGlobal != null) {
            max += config.SettingsGlobal.size() * config.penaltiesandPoints.settingsPoints;
        }
        
        // Files
        if (config.fileDeletions != null) {
            max += config.fileDeletions.size() * config.penaltiesandPoints.fileDeletionPoints;
        }
        
        // Apps
        if (config.appDeletions != null) {
            max += config.appDeletions.size() * config.penaltiesandPoints.appDeletionsPoints;
        }
        if (config.appInstalls != null) {
            max += config.appInstalls.size() * config.penaltiesandPoints.appInstallPoints;
        }
        if (config.appUpdates != null) {
            max += config.appUpdates.size() * config.penaltiesandPoints.updatePoints;
        }
        
        // Forensics questions
        if (config.forensicsQuestions != null) {
            max += config.forensicsQuestions.size() * config.penaltiesandPoints.forensicsPoints;
        }
        
        return max;
    }

    public static class ScoringResult {
        private int currentPoints;
        private int maxPoints;
        private List<ScoreItem> scoreItems;

        public ScoringResult(int currentPoints, int maxPoints, List<ScoreItem> scoreItems) {
            this.currentPoints = currentPoints;
            this.maxPoints = maxPoints;
            this.scoreItems = scoreItems;
        }

        public int getCurrentPoints() {
            return currentPoints;
        }

        public int getMaxPoints() {
            return maxPoints;
        }

        public List<ScoreItem> getScoreItems() {
            return scoreItems;
        }
    }
}
