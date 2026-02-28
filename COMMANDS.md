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

## 🐧 BusyBox Management

The launcher now features a built-in BusyBox manager to enable standard Linux commands.

### Installation
In the T-UI terminal, type:
```bash
bbman -install
```
This downloads the architecture-specific ELF binary, verifies its **SHA-256 hash**, and sets up the environment.

### Usage
Once installed, you can use any Linux command directly:
```bash
ls -la
grep "search_term" file.txt
busybox --list
```

### Removal
To clean up the environment:
```bash
bbman -remove
```

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

---

## 🛡 Security Note
All binaries are verified using hardcoded SHA-256 hashes found in `app/src/main/java/ohi/andre/consolelauncher/tuils/BusyBoxInstaller.java`. All network transport is forced over HTTPS.
