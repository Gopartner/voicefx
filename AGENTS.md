# VoiceFX — AGENTS.md

## Project Overview

VoiceFX is a production-ready Android application for:

- WhatsApp
- WhatsApp Business

Main goal:

Users can:

- record voice
- pick audio from storage
- pick WhatsApp historical voice notes
- choose voice preset
- convert remotely
- preview
- send back to WhatsApp as native voice note

Receiver must see:

- waveform
- play button
- duration

as a normal WhatsApp voice note.

Android device must stay lightweight.

Heavy processing must run remotely.

---

# Target Android

Current supported:

| Setting     | Value |
| ----------- | ----: |
| Min SDK     |    26 |
| Target SDK  |    35 |
| Compile SDK |    35 |

Android support:

```txt
Android 8 → Android 15
```

Target packages:

```txt
com.whatsapp
com.whatsapp.w4b
```

---

# Architecture

```txt
┌──────────────────────┐
│ Android App          │
│ Kotlin + Compose     │
└──────────┬───────────┘
           │
           │ upload
           ▼
┌──────────────────────┐
│ GitHub API           │
│ Releases + Dispatch  │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ GitHub Actions       │
│ Build + Voice Engine │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Download Result      │
└──────────────────────┘
```

---

# Local Laptop Policy

Primary goal:

Laptop stays lightweight.

Laptop responsibilities:

- edit code
- refactor
- generate code
- review
- AGENTS.md update
- git commit
- git push

Allowed:

```bash
git status
git add .
git commit
git push

./gradlew tasks
./gradlew help
./gradlew dependencies
```

Never auto-run locally:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
./gradlew lint
./gradlew test
./gradlew connectedCheck
```

unless user explicitly asks.

OpenCode must prioritize:

- low RAM
- low CPU
- fast response

---

# GitHub Actions Policy

All heavy work runs remotely.

GitHub Actions handles:

Android:

- assembleDebug
- assembleRelease
- bundleRelease
- lint
- unit tests
- dependency cache
- APK upload
- Release upload

Voice:

- ffmpeg
- OpenVoice v2
- RVC
- OGG/Opus encoding

Never heavy build locally.

---

# Required Workflows

```txt
.github/workflows/

android-debug.yml
android-release.yml
voice-convert.yml
```

---

# android-debug.yml

Trigger:

```txt
push
pull_request
workflow_dispatch
```

Tasks:

- setup JDK 17
- Android SDK
- Gradle cache
- assembleDebug
- upload APK artifact

Artifact:

```txt
voicefx-debug.apk
```

---

# android-release.yml

Trigger:

```txt
workflow_dispatch
tag push
```

Tasks:

- assembleRelease
- bundleRelease
- sign APK
- upload GitHub Release

Assets:

```txt
voicefx-release.apk
voicefx-release.aab
mapping.txt
```

---

# Voice Conversion Engine

Heavy conversion must run remotely only.

Android never runs inference.

Priority:

## 1 — ffmpeg

MVP

Use:

- pitch shift
- formant shift
- normalize
- encode

Fast and stable.

---

## 2 — OpenVoice v2

Primary AI engine.

Use for:

- male → female
- child
- teen
- adult female
- user reference audio
- WhatsApp history reference

---

## 3 — RVC

Optional advanced cloning.

Use for:

- user voice from storage
- user voice from SD card
- WhatsApp history voice note

---

## 4 — XTTS

Future only.

Not required now.

Output:

```txt
audio/ogg
audio/opus
```

WhatsApp compatible.

---

# Android Responsibilities

Allowed:

- UI
- overlay
- recorder
- preview
- upload
- download
- cache
- Room DB
- WorkManager
- CameraX
- location
- permission handling
- WhatsApp share

Forbidden:

- local AI inference
- TensorFlow Lite
- PyTorch Android
- heavy DSP
- model download

---

# Overlay Requirements

Create:

```txt
VoiceFxOverlayService
```

Permission:

```txt
SYSTEM_ALERT_WINDOW
FOREGROUND_SERVICE
POST_NOTIFICATIONS
```

Rules:

- draggable
- restore position
- long press hide
- show only when WhatsApp foreground
- show for WA Business
- persistent

Quick panel:

```txt
🎙 VoiceFX

Original
Child
Teen
Adult Female

📁 Internal Storage
💾 SD Card
💬 WhatsApp Voice Notes

📍 Location
📷 Camera

⚙ Settings
```

---

# Background Execution

Must continue when:

- app closed
- user in WhatsApp
- screen locked

Use:

```txt
ForegroundService
WorkManager
BootReceiver
```

Permissions:

```txt
RECEIVE_BOOT_COMPLETED
WAKE_LOCK
```

Restore overlay after reboot.

Restore upload queue.

Continue polling.

---

# Audio Sources

Allow:

```txt
wav
ogg
opus
aac
m4a
mp3
```

Sources:

- microphone
- internal storage
- SD card
- WhatsApp history

---

# WhatsApp History Scan

Search:

```txt
/storage/emulated/0/Android/media/com.whatsapp/

/storage/emulated/0/Android/media/com.whatsapp.w4b/
```

Read:

```txt
.opus
```

Always:

```txt
ContentResolver
DocumentFile
MediaStore
```

Never:

```txt
File(uri.path)
```

---

# WhatsApp Output

Must send:

```txt
audio/ogg
audio/opus
```

Use:

```txt
Intent.ACTION_SEND
```

Packages:

```txt
com.whatsapp
com.whatsapp.w4b
```

Receiver sees normal WhatsApp voice note.

---

# Location

Use:

```txt
FusedLocationProviderClient
```

Permissions:

```txt
ACCESS_COARSE_LOCATION
ACCESS_FINE_LOCATION
FOREGROUND_SERVICE_LOCATION
```

Rules:

- ask only when needed
- cache last known
- low power first
- fallback if denied

---

# Camera

Use:

```txt
CameraX
```

Dependencies:

- camera-core
- camera-camera2
- camera-lifecycle
- camera-view

Permission:

```txt
CAMERA
```

Rules:

- runtime request
- preview
- capture
- cache
- cleanup

---

# Local Cache

Use:

```txt
/Android/data/com.voicefx/cache/
```

Store:

- temp record
- temp upload
- temp download
- preview
- camera

Auto cleanup.

---

# Tech Stack

| Component              | Choice        |
| ---------------------- | ------------- |
| Kotlin                 | 1.9 stable    |
| Compose                | latest stable |
| Material3              | latest stable |
| Navigation             | latest stable |
| Hilt                   | stable        |
| Room                   | stable        |
| OkHttp                 | stable        |
| Media3                 | stable        |
| CameraX                | stable        |
| Play Services Location | stable        |
| WorkManager            | stable        |
| Gradle                 | latest stable |

---

# Project Structure

```txt
voicefx/

.github/
  workflows/
  scripts/

app/

core/
audio/
camera/
location/
overlay/
storage/
whatsapp/
network/
queue/
model/

data/
local/
repository/

domain/
repository/
usecase/

ui/
home/
picker/
history/
recorder/
upload/
preview/
settings/
```

---

# State Rules

Use:

```txt
sealed interface
StateFlow
```

Examples:

```txt
RecordingState
UploadState
OverlayState
PermissionState
PreviewState
```

No UI business logic inside Compose.

---

# Coroutines

Use:

```txt
viewModelScope
Dispatchers.IO
```

Never:

```txt
GlobalScope
```

---

# Error Handling

Always handle:

- permission denied
- GitHub rate limit
- upload fail
- download fail
- invalid URI
- WhatsApp missing
- playback fail
- network fail

Repository returns:

```txt
Result<T>
```

---

# Documentation Rule

AGENTS.md is source of truth.

When changing:

- architecture
- dependencies
- SDK
- workflow
- permissions
- engine
- feature

Always:

1 update AGENTS.md

2 update code

Never leave AGENTS.md outdated.

---

# AI Agent Rules

When working:

1. Read AGENTS.md first
2. Follow architecture
3. Keep Android lightweight
4. Never heavy build local
5. Use GitHub Actions
6. Use BuildConfig/env vars
7. Never hardcode token
8. Keep Material3
9. Use latest stable APIs
10. Use full runnable code
11. No pseudo code
12. Support WhatsApp + Business
13. Preserve native WhatsApp voice-note compatibility
14. Ask minimal clarification
15. Update AGENTS.md when project changes
16. Production-ready only
