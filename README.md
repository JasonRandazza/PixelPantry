# PixelPantry

Android app for on-device ingredient recognition, local pantry inventory, and recipe generation from what you already have.

Formerly prototyped as **Find Me A Recipe**. This repository is the canonical development home.

## Status

Product stub: the launcher opens fixture detections for confirmation, then shows saved Room inventory. The Leap VL research UI (`VlmSpikeActivity`) remains available for device-only model runs but is not the launcher. Planning docs live under local gitignored `docs/`.

**Locality:** Prefer on-device inference; network is expected for model download and some lookups (not a pure-offline app).

## Requirements

- Android Studio (recent stable)
- JDK 17+
- Android SDK with `compileSdk` / `targetSdk` 36
- Optional: `ANTHROPIC_API_KEY` in `local.properties` for the Koog cloud spike

## Setup

1. Open this directory in Android Studio.
2. Copy or create `local.properties` with your Android SDK path (Android Studio usually generates this).
3. If exercising the cloud recipe agent spike, add:

   ```properties
   ANTHROPIC_API_KEY=your_key_here
   ```

4. Sync Gradle and build the `:app` module.

Do **not** commit `local.properties`.

## Package

- Application ID / namespace: `com.jasonrandazza.pixelpantry`
- Min SDK: 31

## Local docs

Planning snapshots and project orientation live in `docs/` (ignored by git). They are local working copies for development; the Obsidian vault is treated as read-only and is not modified by this repo.

## License / model notes

On-device models (Leap / Liquid AI) and any cloud LLM usage are subject to their respective licenses and terms. Prefer local-first processing for inventory images and personal data.
