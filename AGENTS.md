# VoiceFX — Agents Guide

## Project Overview

VoiceFX is an Android application that integrates with WhatsApp/WhatsApp Business to record, process, and send voice notes with selectable voice presets (Original, Child, Teen, Adult Female). Heavy voice processing is offloaded to GitHub Actions Runner; the Android device stays lightweight.

## Architecture

```
┌─────────────────┐     upload audio via GitHub Release API     ┌──────────────────────┐
│  Android App     │ ──────────────────────────────────────────> │  GitHub Actions       │
│  (Kotlin/Compose) │ <────────────────────────────────────────── │  Runner (convert.py)  │
│  (light client)  │     download result + cleanup Release       │  (ffmpeg pitch shift) │
└─────────────────┘                                              │  (future: OpenVoice)  │
                                                                 └──────────────────────┘
```

### Android Responsibilities (LOCAL)
- UI rendering (Jetpack Compose + Material 3)
- Audio recording (MediaRecorder AAC)
- File picking (Storage Access Framework)
- WhatsApp history scanning (MediaStore + direct fs)
- Audio preview (Media3 ExoPlayer)
- WhatsApp share (Intent ACTION_SEND)
- Queue management & upload (OkHttp → GitHub API)
- Result polling & download
- Room DB caching (voice notes + job history)

### GitHub Runner Responsibilities (REMOTE)
- Voice conversion (ffmpeg pitch shift — MVP)
- AI inference (OpenVoice/RVC/XTTS — future)
- OGG/Opus encoding for WhatsApp
- Temporary artifact storage via Releases

## Tech Stack

| Component | Choice |
|-----------|--------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt 2.50 |
| DB | Room 2.6.1 |
| Audio Record | MediaRecorder (AAC) |
| Audio Playback | Media3 ExoPlayer 1.2.1 |
| Network | OkHttp 4.12 |
| Navigation | Navigation Compose 2.7.7 |
| Build | Gradle 8.5 + KSP |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| GitHub Runner | Python 3.12 + ffmpeg |

## Project Structure

```
voicefx/
├── .github/
│   ├── workflows/voice-convert.yml
│   └── scripts/convert.py
├── app/
│   └── src/main/java/com/voicefx/
│       ├── di/              ← Hilt modules
│       ├── navigation/      ← NavGraph (7 routes)
│       ├── core/
│       │   ├── audio/       ← AudioRecorder, AudioPlayer
│       │   ├── storage/     ← FilePickerHelper
│       │   ├── whatsapp/     ← HistoryScanner, ShareHelper
│       │   ├── network/     ← GitHubApiService
│       │   ├── queue/       ← UploadQueueManager
│       │   └── model/       ← VoicePreset, VoiceNote, etc.
│       ├── data/
│       │   ├── local/       ← Room DB, DAOs, Entities
│       │   └── repository/  ← Repository implementations
│       ├── domain/
│       │   ├── repository/  ← Repository interfaces
│       │   └── usecase/     ← Business logic
│       └── ui/
│           ├── theme/       ← Colors, Typography, Theme
│           ├── home/        ← Home screen
│           ├── picker/      ← File picker screen
│           ├── history/     ← WhatsApp history screen
│           ├── recorder/    ← Recording screen
│           ├── upload/      ← Upload/progress screen
│           └── preview/     ← Preview + share screen
```

## Workflow & States

### User Flow
```
Home → Select Preset → [Original → Preview] / [Child/Teen/Adult → Record]
                                                          ↓
                                                    Upload (GitHub)
                                                          ↓
                                                    Poll for result
                                                          ↓
                                                    Download → Preview
                                                          ↓
                                              Share to WhatsApp (Intent)
```

### Recording States
```
Idle → Recording → Recorded → [Delete → Idle] / [Use → Preview/Upload]
```

### Upload (Job) States
```
UPLOADING → QUEUED → PROCESSING → COMPLETED
                                      ↓ (if fail)
                                    FAILED → Retry
```

## Code Conventions

1. **Clean Architecture**: core → data → domain → ui (one-way dependency)
2. **Hilt DI**: All dependencies in `di/AppModule.kt` or `@Inject constructor`
3. **ViewModels**: `AndroidViewModel` or `ViewModel` with Hilt, expose `StateFlow`
4. **State**: Use `sealed interface` for complex states (see `RecordingState`)
5. **Error Handling**: Return `Result<T>` from repositories, catch in ViewModels
6. **Coroutines**: `viewModelScope.launch` for UI, `Dispatchers.IO` for network/disk
7. **Compose**: Stateless screens + state hoisting to ViewModel
8. **Navigation**: 7 routes defined in `NavGraph.kt`, pass primitives only
9. **No local AI processing**: Never run heavy inference on device

## Build & Run

```bash
# Debug APK
./gradlew assembleDebug

# Install to device
./gradlew installDebug

# Clean build
./gradlew clean assembleDebug

# Release APK (requires signing config)
./gradlew assembleRelease
```

### Prerequisites
- JDK 17
- Android SDK 34
- `local.properties` with `github.token`, `github.owner`, `sdk.dir`
- GitHub Personal Access Token with `repo` and `workflow` scopes

## GitHub Actions

### voice-convert.yml
- Trigger: `repository_dispatch` with type `voice-convert`
- Receives: `release_id`, `preset`, `session_id` via `client_payload`
- Steps:
  1. Checkout + Setup Python + Install ffmpeg
  2. Download input from Release asset
  3. Run `convert.py` (ffmpeg pitch shift)
  4. Upload result back to Release
  5. Upload artifact fallback

### Secret Required
- `VOICEFX_TOKEN`: GitHub PAT with `repo` scope

### convert.py
- Downloads input asset from Release by `release_id`
- Applies pitch shift via ffmpeg (atempo + asetrate filters)
- Uploads result OGG/Opus as new Release asset
- Cleanup: removes temp files

## Voice Presets

| Preset | Pitch Factor | Processing Location |
|--------|-------------|-------------------|
| Original | 1.0x | Local (none) |
| Child | 1.6x | GitHub Runner |
| Teen | 1.3x | GitHub Runner |
| Adult Female | 1.12x | GitHub Runner |

Original preset bypasses upload entirely — goes straight to preview.

## MVP Scope

Done:
- [x] Voice preset selection
- [x] Audio recording with timer
- [x] File picking from storage/SD card
- [x] WhatsApp history scanning
- [x] Upload to GitHub + poll + download
- [x] GitHub Actions workflow + conversion script
- [x] Preview with play/pause + progress
- [x] Share to WhatsApp as voice note
- [x] Room DB for history caching

Post-MVP:
- [ ] Floating overlay for quick access
- [ ] Custom user presets
- [ ] OpenVoice / RVC / XTTS integration
- [ ] Magisk / Zygisk plugin
- [ ] Multiple language support
- [ ] Settings screen for GitHub config

## Role: AI Agent Instructions

When modifying code in this project:

1. **NEVER** add heavy processing to the Android app. All voice processing must go through GitHub Actions.
2. **NEVER** hardcode `github.token` — always use BuildConfig fields from `local.properties`.
3. **ALWAYS** use `ContentResolver` for content:// URIs (not `File(uri.path)`).
4. **KEEP** the ViewModel → UseCase → Repository → DataSource pattern.
5. **ADD** error handling for network failures, permission denials, and file access.
6. **USE** Material 3 components for all UI.
7. **MAINTAIN** the sealed interface pattern for states.
8. **PRESERVE** the existing navigation routes when adding new screens.
9. **TEST** with preset `ORIGINAL` first (no network needed) before testing server processing.
10. **ENSURE** all Coroutines use proper scopes — no `GlobalScope`.
