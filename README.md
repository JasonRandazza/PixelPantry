# PixelPantry

Android app for on-device ingredient recognition, local pantry inventory, and recipe generation from what you already have.

Formerly prototyped as **Find Me A Recipe**. This repository is the canonical development home.

## Status

Early spike: Koog (cloud) recipe agent and Leap on-device text model wiring exist. No launcher UI, camera pipeline, or local inventory database yet. Product plans live under a local `docs/` folder (gitignored).

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
