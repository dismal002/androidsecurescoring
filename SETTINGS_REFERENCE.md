# Android Settings Reference

This document explains the three settings tables that can be scored by the Security Scoring Engine.

## Settings Tables Overview

Android stores system settings in three separate XML files:

1. **settings_secure.xml** - Secure settings (user-specific)
2. **settings_system.xml** - System settings (user-specific)
3. **settings_global.xml** - Global settings (device-wide)

## File Locations

All settings files are located in the user directory:
```
/data/system/users/0/settings_secure.xml
/data/system/users/0/settings_system.xml
/data/system/users/0/settings_global.xml
```

Note: User 0 is the primary user. Multi-user devices have separate secure/system settings per user, but global settings are shared.

## Settings Secure

**Purpose**: Security-sensitive settings that are user-specific

**Common Settings to Score**:

```json
"SettingsSecure": {
  "lock_screen_show_notifications": 0,
  "screensaver_enabled": 0,
  "install_non_market_apps": 0,
  "adb_enabled": 0,
  "bluetooth_on": 0,
  "location_mode": 0
}
```

**Examples**:
- `lock_screen_show_notifications` - Show notifications on lock screen (0=off, 1=on)
- `install_non_market_apps` - Allow installation from unknown sources (0=off, 1=on)
- `adb_enabled` - ADB debugging enabled (0=off, 1=on)
- `location_mode` - Location services mode (0=off, 1=sensors only, 2=battery saving, 3=high accuracy)
- `screensaver_enabled` - Screen saver enabled (0=off, 1=on)

## Settings System

**Purpose**: User-specific system preferences

**Common Settings to Score**:

```json
"SettingsSystem": {
  "show_password": 0,
  "screen_off_timeout": 30000,
  "accelerometer_rotation": 0,
  "notification_sound": "",
  "haptic_feedback_enabled": 0
}
```

**Examples**:
- `show_password` - Show password while typing (0=off, 1=on)
- `screen_off_timeout` - Screen timeout in milliseconds (e.g., 30000 = 30 seconds)
- `accelerometer_rotation` - Auto-rotate screen (0=off, 1=on)
- `haptic_feedback_enabled` - Vibration feedback (0=off, 1=on)
- `notification_sound` - Notification sound URI

## Settings Global

**Purpose**: Device-wide settings that apply to all users

**Common Settings to Score**:

```json
"SettingsGlobal": {
  "adb_enabled": 0,
  "development_settings_enabled": 0,
  "stay_on_while_plugged_in": 0,
  "usb_mass_storage_enabled": 0,
  "airplane_mode_on": 0,
  "wifi_on": 0,
  "bluetooth_on": 0,
  "data_roaming": 0
}
```

**Examples**:
- `adb_enabled` - ADB debugging (0=off, 1=on)
- `development_settings_enabled` - Developer options enabled (0=off, 1=on)
- `stay_on_while_plugged_in` - Keep screen on while charging (0=off, 1=USB, 2=AC, 3=wireless, 7=all)
- `airplane_mode_on` - Airplane mode (0=off, 1=on)
- `wifi_on` - WiFi enabled (0=off, 1=on)
- `bluetooth_on` - Bluetooth enabled (0=off, 1=on)
- `data_roaming` - Data roaming (0=off, 1=on)
- `usb_mass_storage_enabled` - USB mass storage (0=off, 1=on)

## Configuration Format

In your scoring configuration JSON:

```json
{
  "SettingsSecure": {
    "setting_name": expected_value,
    "another_setting": expected_value
  },
  "SettingsSystem": {
    "setting_name": expected_value
  },
  "SettingsGlobal": {
    "setting_name": expected_value
  },
  "penaltiesandPoints": {
    "settingsPoints": 3
  }
}
```

**Important Notes**:
- All values are compared as integers
- String values in XML are converted to integers for comparison
- Empty sections (`{}`) are valid if you don't want to score that category
- Each matching setting awards `settingsPoints` points

## Finding Available Settings

### View All Settings on Device

```bash
# Secure settings
adb shell su -c "cat /data/system/users/0/settings_secure.xml"

# System settings
adb shell su -c "cat /data/system/users/0/settings_system.xml"

# Global settings
adb shell su -c "cat /data/system/users/0/settings_global.xml"
```

### Using Settings Command

```bash
# List all secure settings
adb shell settings list secure

# List all system settings
adb shell settings list system

# List all global settings
adb shell settings list global

# Get specific setting
adb shell settings get secure adb_enabled
adb shell settings get system show_password
adb shell settings get global development_settings_enabled
```

## Common Security Scoring Scenarios

### Disable Developer Options

```json
"SettingsSecure": {
  "adb_enabled": 0
},
"SettingsGlobal": {
  "development_settings_enabled": 0,
  "adb_enabled": 0
}
```

### Lock Down Display Settings

```json
"SettingsSecure": {
  "lock_screen_show_notifications": 0,
  "screensaver_enabled": 0
},
"SettingsSystem": {
  "show_password": 0,
  "screen_off_timeout": 30000
}
```

### Disable Connectivity Features

```json
"SettingsGlobal": {
  "bluetooth_on": 0,
  "wifi_on": 0,
  "data_roaming": 0,
  "airplane_mode_on": 1
}
```

### Restrict Installation Sources

```json
"SettingsSecure": {
  "install_non_market_apps": 0
},
"SettingsGlobal": {
  "usb_mass_storage_enabled": 0
}
```

## Differences Between Tables

| Feature | Secure | System | Global |
|---------|--------|--------|--------|
| Scope | Per-user | Per-user | Device-wide |
| Security Level | High | Medium | High |
| Backup | Yes | Yes | No |
| Multi-user | Separate | Separate | Shared |
| Examples | ADB, Location | Display, Sound | WiFi, Bluetooth |

## XML Format

Settings are stored in XML format:

```xml
<settings version="123">
  <setting id="1" name="adb_enabled" value="0" package="android" />
  <setting id="2" name="bluetooth_on" value="1" package="android" />
  <setting id="3" name="screen_off_timeout" value="30000" package="android" />
</settings>
```

The scoring engine reads the `name` and `value` attributes.

## Troubleshooting

### Setting Not Found

If a setting doesn't exist in the XML:
- It may not be set yet (uses default value)
- It may be in a different settings table
- It may be device/manufacturer specific

### Value Mismatch

If scoring doesn't work:
- Check the actual value: `adb shell settings get <table> <setting>`
- Verify the value is an integer
- Some settings use strings (not supported for comparison)

### Permission Denied

If the app can't read settings:
- Ensure root access is granted
- Check file permissions: `adb shell su -c "ls -la /data/system/users/0/settings_*.xml"`
- Make files readable: `adb shell su -c "chmod 644 /data/system/users/0/settings_*.xml"`

## Best Practices

1. **Test Settings First**: Verify settings exist on your target device
2. **Use Appropriate Table**: Choose secure/system/global based on setting scope
3. **Document Expectations**: Comment why each setting is scored
4. **Check Defaults**: Some settings may not appear until changed from default
5. **Device Variations**: Manufacturer-specific settings may not be portable
