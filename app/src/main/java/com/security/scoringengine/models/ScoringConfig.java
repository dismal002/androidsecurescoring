package com.security.scoringengine.models;

import java.util.List;
import java.util.Map;

public class ScoringConfig {
    public List<String> UsersAdditions;
    public List<String> AuthorizedUsers;
    public List<String> UnauthorizedUsers;
    public DeviceRestrictions deviceRestrictions;
    public UserRestrictions userRestrictions;
    public PasswordPolicies passwordPolicies;
    public Map<String, Integer> SettingsSecure;
    public Map<String, Integer> SettingsSystem;
    public Map<String, Integer> SettingsGlobal;
    public AdditionalRestrictions additionalRestrictions;
    public SystemUpdatePolicy systemUpdatePolicy;
    public List<String> fileDeletions;
    public List<String> appDeletions;
    public Map<String, String> appUpdates;
    public List<String> appInstalls;
    public Map<String, List<String>> forensicsQuestions;
    public PenaltiesAndPoints penaltiesandPoints;

    public static class DeviceRestrictions {
        public Boolean screenCaptureDisabled;
        public Boolean networkLoggingEnabled;
    }

    public static class UserRestrictions {
        public Boolean noConfigWifi;
        public Boolean disallowDebugging;
        public Boolean noPrinting;
    }

    public static class PasswordPolicies {
        public List<String> passwordQualityName;
        public Long passwordExpirationTimeout;
    }

    public static class AdditionalRestrictions {
        public Boolean disallowFactoryReset;
    }

    public static class SystemUpdatePolicy {
        public String policyTypeName;
    }

    public static class PenaltiesAndPoints {
        public int updatePoints;
        public int appInstallPoints;
        public int appDeletionsPoints;
        public int fileDeletionPoints;
        public int settingsPoints;
        public int policyPoints;
        public int userPoints;
        public int userPenalty;
        public int appPenalty;
        public int forensicsPoints;
    }
}
