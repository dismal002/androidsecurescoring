# Forensics Questions Guide

The Security Scoring Engine includes a forensics questions feature that allows you to test users' investigative skills with custom questions and answers.

## Overview

Forensics questions are:
- Displayed in a dedicated "Forensics" screen
- Answered through text input
- Protected by a 2-minute cooldown between incorrect attempts
- Persistently tracked (answered questions remain answered across app restarts)
- Scored when answered correctly

## Configuration Format

Add forensics questions to your scoring configuration JSON:

```json
{
  "forensicsQuestions": {
    "forensicsQuestion1": [
      "Question text goes here",
      "Answer goes here"
    ],
    "forensicsQuestion2": [
      "Another question text",
      "Another answer"
    ]
  },
  "penaltiesandPoints": {
    "forensicsPoints": 5
  }
}
```

### Format Details

- **Key**: Unique identifier for the question (e.g., `forensicsQuestion1`, `forensicsQuestion2`)
- **Value**: Array with exactly 2 elements:
  - Index 0: Question text (displayed to user)
  - Index 1: Correct answer (case-insensitive comparison)

## User Experience

### Accessing Forensics Questions

1. Open the app
2. Tap the "Forensics" button on the main screen
3. View all configured questions

### Answering Questions

1. Read the question text
2. Type your answer in the input box
3. Tap "Submit Answer"
4. If correct: Question marked as answered, points awarded
5. If incorrect: 2-minute cooldown before next attempt

### Cooldown System

- **Duration**: 2 minutes (120 seconds)
- **Trigger**: Incorrect answer submission
- **Display**: Live countdown timer shows remaining time
- **Button State**: Submit button disabled during cooldown
- **Persistence**: Cooldowns survive app restarts

### Answered Questions

Once a question is answered correctly:
- Shows "✓ Answered Correctly" status
- Input box and submit button removed
- Cannot be re-answered
- Points permanently awarded
- Status persists across app restarts and reboots

## Example Questions

### Basic Information Discovery

```json
"forensicsQuestion1": [
  "What is the name of the hidden user account created on this device?",
  "Max Verstappen"
]
```

### Package Investigation

```json
"forensicsQuestion2": [
  "What is the package name of the unauthorized network scanning app?",
  "de.csicar.ning"
]
```

### Configuration Analysis

```json
"forensicsQuestion3": [
  "What port number is the Frida server configured to listen on?",
  "27042"
]
```

### File System Forensics

```json
"forensicsQuestion4": [
  "What is the MD5 hash of the suspicious file in /sdcard/Download/?",
  "5d41402abc4b2a76b9719d911017c592"
]
```

### Log Analysis

```json
"forensicsQuestion5": [
  "According to the system logs, what time was ADB debugging enabled? (Format: HH:MM)",
  "14:23"
]
```

## Answer Matching

- **Case Insensitive**: "answer", "ANSWER", and "Answer" all match
- **Whitespace**: Leading/trailing spaces are trimmed
- **Exact Match**: Answer must match exactly (after trimming and case normalization)

### Tips for Creating Answers

**Good Answers** (specific, unambiguous):
- Package names: `com.example.app`
- Numbers: `27042`
- Hashes: `5d41402abc4b2a76b9719d911017c592`
- Specific names: `Max Verstappen`

**Avoid** (ambiguous, multiple valid formats):
- Dates without format specification
- Answers with multiple valid spellings
- Answers requiring specific punctuation

## Scoring Integration

Forensics questions integrate with the main scoring system:

1. **Points Awarded**: When question answered correctly
2. **Score Display**: Shows in main scoring report under "FORENSICS" category
3. **Max Points**: Included in total possible points
4. **Persistence**: Answered questions contribute to score across restarts

### Scoring Report Example

```
=== SCORING REPORT ===

--- FORENSICS ---
Forensics question 'forensicsQuestion1' answered correctly - +5 Points
Forensics question 'forensicsQuestion3' answered correctly - +5 Points

--- APPS ---
de.csicar.ning has been deleted - +2 Points

======================
Total: 12 / 50 Points
```

## Data Persistence

### Stored Data

The app stores:
- **Answered Questions**: Which questions have been answered correctly
- **Cooldowns**: Active cooldown timers with expiration times

### Storage Location

```
/data/data/com.security.scoringengine/shared_prefs/ForensicsPrefs.xml
```

### Data Format

```xml
<map>
    <string name="answered_questions">{"forensicsQuestion1":true,"forensicsQuestion2":true}</string>
    <string name="cooldowns">{"forensicsQuestion3":1732825200000}</string>
</map>
```

## Security Considerations

### Answer Protection

Answers are stored in the encrypted configuration file:
- Not visible in the APK
- Not accessible without the app
- Hardware-backed encryption

### Preventing Cheating

The cooldown system prevents:
- Rapid brute-force attempts
- Automated answer testing
- Quick trial-and-error

### Forensics Data Protection

Answered questions are stored in app-private storage:
- Requires root to access
- Stored as JSON in SharedPreferences
- Can be cleared by resetting configuration

## Best Practices

### Question Design

1. **Clear and Specific**: Questions should have one correct answer
2. **Investigative**: Require actual system investigation
3. **Verifiable**: Answers should be discoverable on the device
4. **Appropriate Difficulty**: Match skill level of users

### Answer Format

1. **Consistent**: Use consistent formats (e.g., all lowercase for package names)
2. **Documented**: Provide format hints in question text if needed
3. **Testable**: Verify answers are discoverable before deployment

### Point Values

1. **Difficulty-Based**: Harder questions = more points
2. **Balanced**: Don't make forensics worth too much of total score
3. **Consistent**: Similar difficulty questions should have similar points

## Troubleshooting

### Question Not Appearing

- Check JSON syntax is valid
- Verify question has both text and answer
- Ensure `forensicsQuestions` key is present

### Answer Not Accepted

- Check for typos in configured answer
- Verify case-insensitive matching is working
- Check for extra whitespace in answer

### Cooldown Not Working

- Verify system time is correct
- Check SharedPreferences are being saved
- Restart app to reload cooldown state

### Points Not Awarded

- Verify `forensicsPoints` is set in `penaltiesandPoints`
- Check scoring service is running
- Tap "Refresh Score" to trigger update

## Advanced Usage

### Dynamic Questions

Questions can reference:
- System properties
- File contents
- App configurations
- Network settings
- User accounts

### Multi-Part Investigations

Create question sequences:
```json
"forensicsQuestion1": ["Find the suspicious app package name", "com.evil.app"],
"forensicsQuestion2": ["What permission does this app request?", "READ_CONTACTS"],
"forensicsQuestion3": ["What data does it exfiltrate?", "contacts.db"]
```

### Hint System

Include hints in question text:
```json
"forensicsQuestion1": [
  "What is the hidden user's name? (Hint: Check /data/system/users/*/accounts.db)",
  "HiddenUser"
]
```

## Example Configuration

Complete example with multiple question types:

```json
{
  "forensicsQuestions": {
    "forensicsQuestion1": [
      "What is the name of the unauthorized user account?",
      "Max Verstappen"
    ],
    "forensicsQuestion2": [
      "What package name is used by the network scanner app?",
      "de.csicar.ning"
    ],
    "forensicsQuestion3": [
      "What port is Frida server listening on?",
      "27042"
    ],
    "forensicsQuestion4": [
      "What is the filename of the hidden music file in /sdcard/Music/?",
      ".tu-tu-tu-du-max-verstappen.mp3"
    ],
    "forensicsQuestion5": [
      "How many unauthorized apps were pre-installed? (number only)",
      "2"
    ]
  },
  "penaltiesandPoints": {
    "forensicsPoints": 5,
    "updatePoints": 2,
    "appInstallPoints": 2,
    "appDeletionsPoints": 2,
    "fileDeletionPoints": 4,
    "settingsPoints": 3,
    "policyPoints": 3,
    "userPoints": 3,
    "userPenalty": 3,
    "appPenalty": 3
  }
}
```

This configuration creates 5 forensics questions, each worth 5 points, for a total of 25 possible forensics points.
