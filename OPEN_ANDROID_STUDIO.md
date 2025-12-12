# Opening Android Studio for Time Protector

## ✅ Your app is built and synced!

The web app has been built and synced to the Android project. Now you just need to open it in Android Studio.

## 📱 Option 1: Open Manually (Easiest)

1. **Open Android Studio** (if installed)
2. Click **File** → **Open**
3. Navigate to: `C:\Users\mdmar\Documents\GitHub\TimeBlock\android`
4. Click **OK**
5. Wait for Gradle sync to complete
6. Build your APK!

## 🔧 Option 2: Set Android Studio Path

If Android Studio is installed but not in PATH, set the environment variable:

### Windows PowerShell (Current Session):
```powershell
$env:CAPACITOR_ANDROID_STUDIO_PATH = "C:\Program Files\Android\Android Studio\bin\studio64.exe"
```

### Windows Command Prompt:
```cmd
set CAPACITOR_ANDROID_STUDIO_PATH=C:\Program Files\Android\Android Studio\bin\studio64.exe
```

### Permanent (System-wide):
1. Press `Win + X` → **System**
2. Click **Advanced system settings**
3. Click **Environment Variables**
4. Under **User variables**, click **New**
5. Variable name: `CAPACITOR_ANDROID_STUDIO_PATH`
6. Variable value: `C:\Program Files\Android\Android Studio\bin\studio64.exe`
   (Adjust path if Android Studio is installed elsewhere)

Common locations:
- `C:\Program Files\Android\Android Studio\bin\studio64.exe`
- `C:\Program Files (x86)\Android\Android Studio\bin\studio64.exe`
- `%LOCALAPPDATA%\Programs\Android\Android Studio\bin\studio64.exe`

## 📥 Option 3: Install Android Studio

If Android Studio is not installed:

1. **Download Android Studio:**
   - Visit: https://developer.android.com/studio
   - Download the Windows installer

2. **Install:**
   - Run the installer
   - Follow the setup wizard
   - Make sure to install Android SDK

3. **Open the project:**
   - Open Android Studio
   - File → Open → Navigate to `android` folder

## 🏗️ Building the APK

Once Android Studio is open:

1. **Wait for Gradle Sync** (may take a few minutes on first run)
2. **Build APK:**
   - Go to: **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
3. **Find your APK:**
   - Location: `android\app\build\outputs\apk\debug\app-debug.apk`

## 📝 Quick Reference

- Project location: `C:\Users\mdmar\Documents\GitHub\TimeBlock\android`
- APK output: `android\app\build\outputs\apk\debug\app-debug.apk`
- App ID: `com.timeprotector.app`
- App Name: `Time Protector`

Your app is ready! Just open it in Android Studio and build! 🚀

