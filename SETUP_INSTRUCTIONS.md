# Setup Instructions for Security Scoring Engine

## Prerequisites

1. **Rooted Android 9 Device**
   - Device must be rooted with su binary available
   - SELinux should be in permissive mode or properly configured

2. **Device Policy Manager App**
   - Must have the policy manager app installed at:
     `/data/data/com.deviceconfig.policymanager/`
   - This app should export policy state to `policy_state.json`

3. **Development Environment**
   - Android Studio or Gradle command line tools
   - Android SDK with API 28 (Android 9)

## Build Instructions

### Using Android Studio

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Build > Generate Signed Bundle / APK
4. Choose APK
5. Create or select a keystore
6. Build release APK

### Using Command Line

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing configuration)
./gradlew assembleRelease
```

## Installation Steps

### 1. Prepare Configuration File

Create your scoring configuration JSON file based on `sample_scoring_config.json` and place it somewhere accessible on your device (e.g., `/sdcard/Download/`, `/sdcard/Documents/`, etc.):

```bash
# Copy to device
adb push my_scoring_config.json /sdcard/Download/scoring_config.json
```

**Important**: The configuration file can be stored anywhere on the device. You'll select it using the file picker on first run.

### 2. Install the APK

```bash
# Install the app
adb install app/build/outputs/apk/release/app-release.apk

# Or for debug build
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. First Launch and Configuration

1. Open the app
2. You'll see a welcome screen with "Select Configuration File" button
3. Tap the button and grant storage permissions if prompted
4. Navigate to your configuration file and select it
5. The app will validate, encrypt, and store the configuration
6. When prompted by SuperSU/Magisk, grant root access
7. Select "Remember choice" to persist across reboots

### 4. Verify Operation

1. Check that the score displays correctly
2. Tap "Refresh Score" to manually trigger scoring
3. Verify the scoring report shows expected items

## Root Access Requirements

The app needs root access to read:

- `/data/data/com.deviceconfig.policymanager/policy_state.json`
- `/data/system/users/0/settings_secure.xml`
- `/data/system/users/0/settings_system.xml`
- `/data/system/users/0/settings_global.xml`
- Various file paths for deletion checks
- System app directories

### Granting Root Access via ADB

If using Magisk, you can grant root access via command line:

```bash
adb shell
su
# App will now have root access when it requests it
```

## Troubleshooting

### App Can't Read Policy File

```bash
# Check if file exists
adb shell su -c "ls -la /data/data/com.deviceconfig.policymanager/policy_state.json"

# Check file permissions
adb shell su -c "chmod 644 /data/data/com.deviceconfig.policymanager/policy_state.json"
```

### Settings Files Not Readable

```bash
# Check settings files
adb shell su -c "ls -la /data/system/users/0/settings_*.xml"

# Make readable if needed
adb shell su -c "chmod 644 /data/system/users/0/settings_secure.xml"
adb shell su -c "chmod 644 /data/system/users/0/settings_system.xml"
adb shell su -c "chmod 644 /data/system/users/0/settings_global.xml"
```

### Service Not Starting on Boot

```bash
# Check if boot receiver is enabled
adb shell dumpsys package com.security.scoringengine | grep -A 5 BootReceiver

# Manually start service
adb shell am startservice com.security.scoringengine/.ScoringService
```

### Configuration Not Loading

1. Make sure the JSON file is valid (use a JSON validator)
2. Check that the file has the required `penaltiesandPoints` section
3. Try selecting the file again using "Reset Config" button

```bash
# Check app logs
adb logcat | grep ScoringEngine

# Verify JSON is valid
adb shell cat /sdcard/Download/scoring_config.json | python -m json.tool
```

## Security Considerations

### Configuration Protection

The scoring configuration is protected through:

1. **Encryption**: AES-GCM with 128-bit tag
2. **Hardware-backed keys**: Android Keystore with hardware security module
3. **App-private storage**: Encrypted config stored in app's private directory
4. **No external access**: Config file only accessible by the app

### Preventing Tampering

1. **ProGuard obfuscation**: Release builds are obfuscated
2. **Signature verification**: Only signed APKs can be installed
3. **Root detection**: App requires root but validates environment
4. **Foreground service**: Harder to kill than background service

### Recommended Additional Protections

1. **SELinux policies**: Configure SELinux to protect app files
2. **File system encryption**: Enable full disk encryption
3. **Verified boot**: Use dm-verity for system partition
4. **App pinning**: Pin the scoring app to prevent closure

## Testing

### Test Configuration Loading

```bash
# Create test config
echo '{"UsersAdditions":["TestUser"],"penaltiesandPoints":{"userPoints":5}}' > test_config.json
adb push test_config.json /sdcard/scoring_config.json

# Load in app and verify
```

### Test Scoring Logic

1. Add a test user via Device Policy Manager
2. Refresh score in app
3. Verify points awarded
4. Remove the user
5. Verify penalty applied

### Test Persistence

```bash
# Reboot device
adb reboot

# After boot, check if service is running
adb shell ps | grep scoringengine

# Open app and verify score persists
```

## Maintenance

### Updating Configuration

To update the scoring configuration:

1. Place new configuration file on device
2. Open app and tap "Reset Config"
3. Select the new configuration file
4. Previous scores are reset with new configuration

### Viewing Current Configuration

The encrypted configuration is stored at:
```
/data/data/com.security.scoringengine/files/scoring_config.enc
```

To view it (requires root):
```bash
# This will show encrypted data (not human readable)
adb shell su -c "cat /data/data/com.security.scoringengine/files/scoring_config.enc"
```

Note: The configuration cannot be decrypted outside the app due to hardware-backed encryption keys.

### Viewing Logs

```bash
# View app logs
adb logcat -s ScoringEngine:V

# View all app output
adb logcat | grep com.security.scoringengine
```

### Backup Configuration

The encrypted configuration is stored at:
```
/data/data/com.security.scoringengine/files/scoring_config.enc
```

Note: This file is encrypted with device-specific keys and cannot be transferred to another device.
