#!/usr/bin/env python3
"""
VoiceFX - GitHub Actions voice conversion runner.

Receives audio from a GitHub release, processes it with the selected
voice preset, and uploads the result back to the same release.

Environment variables:
  GH_TOKEN: GitHub personal access token
  RELEASE_ID: GitHub release ID to download from / upload to
  PRESET: Voice preset (child, teen, adult_female)
  SESSION_ID: Unique session identifier
  GITHUB_REPO: Repository full name (owner/repo)
"""

import os
import sys
import json
import time
import subprocess
import urllib.request
import urllib.error

API_BASE = "https://api.github.com"


def api_request(method, path, data=None, binary=False):
    """Make a GitHub API request."""
    token = os.environ["GH_TOKEN"]
    url = f"{API_BASE}{path}"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github.v3+json",
        "User-Agent": "VoiceFX-Runner",
    }
    if not binary and data is not None:
        headers["Content-Type"] = "application/json"
        data = json.dumps(data).encode("utf-8")
    elif binary and data is not None:
        headers["Content-Type"] = "application/octet-stream"

    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        print(f"API error {e.code}: {body}", flush=True)
        sys.exit(1)


def get_release_assets(release_id):
    """List all assets for a release."""
    repo = os.environ["GITHUB_REPO"]
    return api_request("GET", f"/repos/{repo}/releases/{release_id}/assets")


def download_asset(download_url, output_path):
    """Download a release asset to a file."""
    token = os.environ["GH_TOKEN"]
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/octet-stream",
        "User-Agent": "VoiceFX-Runner",
    }
    req = urllib.request.Request(download_url, headers=headers)
    with urllib.request.urlopen(req, timeout=120) as resp:
        with open(output_path, "wb") as f:
            f.write(resp.read())
    print(f"Downloaded to {output_path} ({os.path.getsize(output_path)} bytes)", flush=True)


def upload_asset(release_id, file_path, content_type="audio/ogg"):
    """Upload a file as a release asset."""
    repo = os.environ["GITHUB_REPO"]
    token = os.environ["GH_TOKEN"]
    file_name = os.path.basename(file_path)

    # Get upload URL from release
    release = api_request("GET", f"/repos/{repo}/releases/{release_id}")
    upload_url_template = release["upload_url"]
    upload_url = upload_url_template.replace(
        "{?name,label}", f"?name={file_name}"
    )

    with open(file_path, "rb") as f:
        data = f.read()

    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github.v3+json",
        "Content-Type": content_type,
        "Content-Length": str(len(data)),
        "User-Agent": "VoiceFX-Runner",
    }

    req = urllib.request.Request(upload_url, data=data, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=300) as resp:
            result = json.loads(resp.read().decode("utf-8"))
            print(f"Uploaded {file_name} (id: {result['id']})", flush=True)
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8", errors="replace")
        print(f"Upload error {e.code}: {body}", flush=True)
        sys.exit(1)


def get_audio_duration(file_path):
    """Get audio duration in seconds using ffprobe."""
    try:
        result = subprocess.run(
            ["ffprobe", "-v", "error", "-show_entries",
             "format=duration", "-of", "default=noprint_wrappers=1:nokey=1",
             file_path],
            capture_output=True, text=True, timeout=30,
        )
        return float(result.stdout.strip())
    except (subprocess.TimeoutExpired, ValueError, FileNotFoundError):
        return 0.0


def convert_audio(input_path, output_path, preset):
    """
    Convert audio based on voice preset.
    MVP: simple pitch shift via ffmpeg atempo + asetrate.
    Production: call OpenVoice/RVC/XTTS here.
    """
    duration = get_audio_duration(input_path)
    print(f"Input duration: {duration:.1f}s, preset: {preset}", flush=True)

    # Pitch factors matching Android VoicePreset
    pitch_factors = {
        "original": 1.0,
        "child": 1.6,
        "teen": 1.3,
        "adult_female": 1.12,
    }
    factor = pitch_factors.get(preset, 1.0)

    if factor == 1.0:
        # Original: copy directly to OGG/Opus
        subprocess.run(
            ["ffmpeg", "-y", "-i", input_path,
             "-c:a", "libopus", "-b:a", "24k",
             "-application", "voip", output_path],
            check=True, capture_output=True, timeout=120,
        )
        print(f"Copied to {output_path} (no pitch change)", flush=True)
    else:
        # Pitch shift using resampling technique
        # asetrate adjusts sample rate (pitch), atempo adjusts speed to compensate
        original_rate = 44100
        new_rate = int(original_rate * factor)

        # Step 1: change sample rate (pitch shift)
        # Step 2: compensate duration with atempo
        compensation = 1.0 / factor

        filter_complex = (
            f"[0:a]asetrate={new_rate},aresample={original_rate},atempo={compensation}[a]"
        )

        subprocess.run(
            ["ffmpeg", "-y", "-i", input_path,
             "-af", filter_complex,
             "-c:a", "libopus", "-b:a", "24k",
             "-application", "voip",
             "-ar", str(original_rate),
             "-ac", "1",
             output_path],
            check=True, capture_output=True, timeout=120,
        )
        print(f"Pitch-shifted ({factor}x) to {output_path}", flush=True)


def main():
    release_id = os.environ.get("RELEASE_ID")
    preset = os.environ.get("PRESET", "original").lower()
    session_id = os.environ.get("SESSION_ID", "unknown")

    if not release_id:
        print("ERROR: RELEASE_ID not set", flush=True)
        sys.exit(1)

    print(f"Starting conversion | session={session_id} preset={preset} release={release_id}", flush=True)

    # Find and download input asset
    assets = get_release_assets(release_id)
    input_asset = None
    for asset in assets:
        name = asset["name"].lower()
        if name.startswith("input"):
            input_asset = asset
            break

    if not input_asset:
        print("ERROR: No input asset found in release", flush=True)
        sys.exit(1)

    input_path = f"input_{session_id}"
    download_asset(input_asset["browser_download_url"], input_path)

    # Determine audio format from input asset name
    input_name = input_asset["name"].lower()
    output_path = f"output_{session_id}.ogg"

    # Convert
    try:
        convert_audio(input_path, output_path, preset)
    except subprocess.CalledProcessError as e:
        print(f"Conversion error: {e.stderr.decode()}", flush=True)
        # Fallback: copy input as output
        subprocess.run(
            ["ffmpeg", "-y", "-i", input_path, "-c:a", "libopus", output_path],
            check=True, capture_output=True, timeout=60,
        )
        print("Fallback: copied input as output", flush=True)

    # Upload result
    upload_asset(release_id, output_path, "audio/ogg")

    # Cleanup
    for f in [input_path, output_path]:
        if os.path.exists(f):
            os.remove(f)

    print(f"Conversion complete | session={session_id}", flush=True)


if __name__ == "__main__":
    main()
