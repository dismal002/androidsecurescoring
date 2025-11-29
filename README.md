# Security Scoring Engine for Android 9

A secure scoring engine application for rooted Android 9 devices that monitors and scores cybersecurity task completion.

## Features

- **First-Run Setup**: Clean setup experience with file picker for configuration
- **Secure Configuration Storage**: Uses Android Keystore for hardware-backed encryption
- **No Hardcoded Config**: All configuration loaded from user-selected file
- **Persistent Monitoring**: Background service checks every 2 minutes
- **Boot Persistence**: Automatically starts on device boot
- **Real-time Scoring**: Dynamic scoring with penalties for reversions
- **Comprehensive Checks**:
  - User management (additions, authorized, unauthorized)
  - Device policies and restrictions
  - Password policies
  - System settings (Secure, System, and Global)
  - File deletions
  - App installations, deletions, and updates
  - Forensics questions with answer validation

## Security Features

1. **Encrypted Configuration**: Scoring config is encrypted using AES-GCM with Android Keystore
2. **No Plain Text Storage**: Configuration never stored in plain text on device
3. **Hardware-Backed Keys**: Uses hardware security module when available
4. **Obfuscation**: ProGuard rules included for release builds
5. **Restricted Permissions**: App files only readable by the app itself
6. **No Hardcoded Secrets**: No configuration embedded in the app

## Installation

1. Build the APK:
   ```bash
   ./gradlew assembleRelease
   ```

2. Install on rooted device:
   ```bash
   adb install app/build/outputs/apk/release/app-release.apk
   ```

3. Grant root permissions when prompted

4. On first launch, tap "Select Configuration File" and choose your JSON config

5. Configuration is automatically encrypted and saved for future runs

## First Run Experience

When you launch the app for the first time:

1. You'll see a welcome screen
2. Tap "Select Configuration File"
3. Navigate to your scoring configuration JSON file
4. The app validates, encrypts, and stores the configuration
5. Main scoring screen appears and monitoring begins

## Configuration Format

Create a JSON file with your scoring criteria. See `sample_scoring_config.json` for the complete format.

## Usage

- **First Run**: Select your configuration file using the file picker
- **Automatic Scoring**: Runs every 2 minutes in the background
- **Manual Refresh**: Tap "Refresh Score" button
- **Reset Config**: Tap "Reset Config" to clear current config and load a new one

## Requirements

- Android 9 (API 28)
- Root access
- Device Policy Manager app at `/data/data/com.deviceconfig.policymanager/`

## Architecture

- **MainActivity**: UI for displaying scores and reports
- **ScoringService**: Background service for continuous monitoring
- **ScoringEngine**: Core logic for calculating scores
- **SecureConfigStorage**: Encrypted storage using Android Keystore
- **BootReceiver**: Ensures service starts on boot

## Permissions

- `READ_EXTERNAL_STORAGE`: Read config file
- `WRITE_EXTERNAL_STORAGE`: (Android 9 requirement)
- `RECEIVE_BOOT_COMPLETED`: Start on boot
- `FOREGROUND_SERVICE`: Run persistent background service

## Notes

- Root access required to read policy files and system settings
- Service runs as foreground service to prevent termination
- Configuration persists across reboots
- Scoring state maintained in memory for penalty tracking
