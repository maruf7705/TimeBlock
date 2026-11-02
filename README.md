# Focus - Minimalist Time-Blocking Android App

A refined, high-performance daily time-blocking application built with **100% Kotlin**, **Jetpack Compose (Material 3)**, **Room SQLite Database**, and **WorkManager**.

---

## 🌟 Key Features

### 1. Minimalist Editorial Timeline
- **Natural Temporal Perception**: Time blocks flow seamlessly from morning to night.
- **Decoupled Keyboard Architecture**: Zero layout thrashing during keyboard open/close, ensuring smooth hardware-accelerated typing.
- **Smooth 1-Minute Wheel Precision**: Interactive touch/drag time selector with ±1 minute precision and ±5m / ±15m quick jump pills.
- **Zen Washi & Sumi Palette**: Custom warm paper palette crafted for zero eye strain during prolonged focus sessions.

### 2. Intelligent Check-In Notification System
- **Relative Cadence**:
  - `+45 min`: Initial mindful check-in alert.
  - `+15 min`: First gentle follow-up.
  - `+15 min`: Second follow-up.
  - `+45 min`: Final graceful summary and auto-stop.
- **Quick Action ("একই কাজ চলছে")**: Extend current block without opening the app directly from the notification.
- **Alarm Stream Vibration**: Strong 3-pulse cadence that remains noticeable even during active cellular/WhatsApp calls.
- **Double-Chime Audio**: High-clarity 2-pulse melodic chime (`checkin_45.mp3`).
- **Sleep & Quiet Hours**: Completely silent from 11:00 PM to 6:00 AM and during active sleep blocks.

### 3. Next-Day 00:00 Auto-Reset & Gap Recovery
- Next day automatically resets the active logging timer cleanly to `00:00`.
- Unlogged night hours trigger an elegant **Yesterday Gap BottomSheet** allowing one-tap "Save as Sleep" or custom labeling.

### 4. Zero Data Loss & Localhost Backup
- **Persistent State Store**: Draft text and timer state survive OS low-memory process kills.
- **Localhost Backup & Restore**: One-tap JSON export and import using Android Storage Access Framework (SAF).

---

## 🚀 Download & Installation

The latest pre-built APK is available directly in the repository root:
- [`Focus.apk`](./Focus.apk)

Or download from GitHub Actions Artifacts via the automated build workflow.

---

## 🛠️ Build from Source

### Prerequisites
- Android Studio Ladybug / Meerkat or Command Line Tools
- JDK 17 or JDK 21
- Android SDK Platform 35

### Building via Terminal
```bash
# Clone the repository
git clone https://github.com/maruf7705/TimeBlock.git
cd TimeBlock

# Build Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```
