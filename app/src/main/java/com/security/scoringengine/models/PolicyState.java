package com.security.scoringengine.models;

import java.util.List;

public class PolicyState {
    public DevicePolicies devicePolicies;
    public SystemUpdatePolicy systemUpdatePolicy;
    public PasswordPolicies passwordPolicies;
    public AdditionalRestrictions additionalRestrictions;
    public UserRestrictions userRestrictions;
    public List<UserProfile> userProfiles;

    public static class DevicePolicies {
        public boolean screenCaptureDisabled;
        public boolean networkLoggingEnabled;
    }

    public static class SystemUpdatePolicy {
        public String policyTypeName;
    }

    public static class PasswordPolicies {
        public long passwordExpirationTimeout;
        public String passwordQualityName;
    }

    public static class AdditionalRestrictions {
        public boolean disallowFactoryReset;
    }

    public static class UserRestrictions {
        public boolean noConfigWifi;
        public boolean disallowDebugging;
        public boolean noPrinting;
    }

    public static class UserProfile {
        public int userId;
        public String userName;
        public boolean isOwner;
    }
}
