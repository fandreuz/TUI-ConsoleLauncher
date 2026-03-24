# T-UI Launcher Development & Deployment Guide

This document summarizes the essential commands for building, installing, and managing the T-UI Linux CLI Launcher fork.

## 🛠 Building the APK

To perform a clean build of the F-Droid version (includes SMS permissions):

```bash
# Ensure gradlew is executable
chmod +x gradlew

# Build the F-Droid Debug APK
./gradlew assembleFdroidDebug
```

**Output Path:** `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk`

---

## 📱 Emulator Deployment (ADB)

To install the APK on your Pixel 9 Pro emulator (or any physical device connected via USB):

```bash
# 1. Start the emulator (if not already running)
emulator -avd Pixel_9_Pro -gpu host -accel on &

# 2. Wait for the device and install (overwriting existing)
adb wait-for-device
adb install -r app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```

---

## ☀️ Weather Configuration

The `weather` command requires a personal API key from [OpenWeatherMap](https://openweathermap.org/) for manual updates.

### Setting your Key
1.  Register for a free account on the OpenWeatherMap website.
2.  Generate an API key in your account settings.
3.  In the T-UI terminal, type:
    ```bash
    weather -set_key [YOUR_API_KEY]
    ```
4.  Enable weather updates:
    ```bash
    weather -enable
    ```
    *(If it shows a location error, ensure Location permission is granted in Android settings)*

---

## 🔍 Useful ADB Debugging Commands

```bash
# View real-time logs (filtered for T-UI)
adb logcat | grep ohi.andre

# Uninstall the launcher via ADB
adb uninstall ohi.andre.consolelauncher

# Push a file to the launcher's internal storage
adb push local_file.txt /data/user/0/ohi.andre.consolelauncher/files/
```

