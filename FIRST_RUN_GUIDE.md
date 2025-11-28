# First Run Guide

## What Happens on First Launch

When you install and launch the Security Scoring Engine for the first time, you'll see a clean setup screen with no pre-configured settings.

### Setup Screen

The setup screen displays:
- App title: "Security Scoring Engine"
- Welcome message explaining the setup process
- "Select Configuration File" button
- Note about root access requirement

### Step-by-Step First Run

1. **Launch the App**
   - Open the Security Scoring Engine app
   - You'll see the welcome/setup screen

2. **Grant Storage Permission**
   - Tap "Select Configuration File"
   - Android will prompt for storage permission
   - Tap "Allow" to grant access

3. **Select Your Configuration**
   - A file picker will open
   - Navigate to where you stored your JSON config file
   - Tap on your configuration file to select it

4. **Configuration Processing**
   - The app validates the JSON format
   - If valid, it encrypts the configuration using Android Keystore
   - The encrypted config is saved to app-private storage
   - You'll see a success message

5. **Grant Root Access**
   - SuperSU or Magisk will prompt for root access
   - Tap "Grant" to allow root access
   - Check "Remember choice" for persistent access

6. **Main Screen Appears**
   - The app switches to the main scoring screen
   - Background service starts monitoring
   - Initial score calculation begins

## What Gets Stored

After first run, the following is stored:

- **Encrypted Configuration**: `/data/data/com.security.scoringengine/files/scoring_config.enc`
  - AES-GCM encrypted with hardware-backed key
  - Cannot be decrypted outside the app
  - Cannot be transferred to another device

- **Android Keystore Entry**: `ScoringEngineKey`
  - Hardware-backed encryption key
  - Tied to this specific device
  - Cannot be extracted

## No Hardcoded Configuration

The app contains **zero** hardcoded configuration:
- No default scoring rules
- No embedded JSON files
- No fallback configurations
- Everything comes from your selected file

## Subsequent Launches

After the first run:
- App opens directly to the main scoring screen
- Configuration is automatically loaded from encrypted storage
- Background service starts immediately
- No need to select the file again

## Resetting Configuration

If you need to load a different configuration:

1. Tap "Reset Config" button on main screen
2. Confirm the reset
3. App returns to setup screen
4. Select a new configuration file
5. Process repeats as in first run

## Security Notes

### Why This Approach?

1. **No Embedded Secrets**: Configuration never compiled into the app
2. **User Control**: You choose what gets monitored
3. **Secure Storage**: Hardware-backed encryption protects the config
4. **Tamper Resistant**: Encrypted config can't be modified without the app

### What If Someone Gets the APK?

Even if someone:
- Decompiles the APK
- Extracts all resources
- Analyzes the code

They will find:
- No scoring configuration
- No answer keys
- No hardcoded values
- Only the framework for loading and processing configs

### What If Someone Gets Root Access?

Even with root access, an attacker:
- Cannot decrypt the config without the app
- Cannot extract the encryption key from Android Keystore
- Cannot modify the encrypted config (it would fail validation)
- Would need to compromise the app itself to access the config

## Troubleshooting First Run

### "Invalid configuration file format" Error

Your JSON file is malformed or missing required fields:
- Validate JSON syntax using a JSON validator
- Ensure `penaltiesandPoints` section exists
- Check that all required fields are present

### "Storage permission is required" Message

You denied storage permission:
- Go to Settings > Apps > Security Scoring Engine > Permissions
- Enable Storage permission
- Return to app and try again

### File Picker Doesn't Open

No file manager app installed:
- Install a file manager (e.g., Files by Google)
- Try selecting the file again

### Configuration Loads But No Scores

Root access not granted:
- Check SuperSU/Magisk logs
- Grant root access to the app
- Restart the app

### Service Not Starting

Configuration loaded but service won't start:
- Check logcat: `adb logcat | grep ScoringEngine`
- Verify root access is granted
- Check that policy files exist on device

## Example Configuration File

Here's a minimal valid configuration:

```json
{
  "UsersAdditions": [],
  "AuthorizedUsers": [],
  "UnauthorizedUsers": [],
  "deviceRestrictions": {},
  "userRestrictions": {},
  "passwordPolicies": {},
  "SettingsSecure": {},
  "SettingsSystem": {},
  "SettingsGlobal": {},
  "additionalRestrictions": {},
  "systemUpdatePolicy": {},
  "fileDeletions": [],
  "appDeletions": [],
  "appUpdates": {},
  "appInstalls": [],
  "forensicsQuestions": {},
  "penaltiesandPoints": {
    "updatePoints": 2,
    "appInstallPoints": 2,
    "appDeletionsPoints": 2,
    "fileDeletionPoints": 4,
    "settingsPoints": 3,
    "policyPoints": 3,
    "userPoints": 3,
    "userPenalty": 3,
    "appPenalty": 3,
    "forensicsPoints": 5
  }
}
```

The only required section is `penaltiesandPoints`. All other sections can be empty but must be present.
