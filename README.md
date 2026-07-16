# PixelPantry

Android app for on-device ingredient recognition, local pantry inventory, and recipe generation from what you already have.

Formerly prototyped as **Find Me A Recipe**. This repository is the canonical development home.

## Status

Implementation branch `impl/mvp-core`: Home / One-off mode chooser, inventory CRUD, confirm (save or session), on-device Leap text recipes, product VL scan→confirm path, and recipe list/detail. Leap model quality still needs a physical device. Research spike UI (`VlmSpikeActivity`) remains available. Planning docs are local/gitignored under `docs/`.

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
