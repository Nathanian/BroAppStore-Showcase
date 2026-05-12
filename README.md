# Android App Store & Update Manager (Sanitized Public Showcase)

This repository is a **sanitized public-demo version** of an Android application distribution and update-management system for managed/embedded Android devices.

It preserves the technical architecture for:
- remote catalog sync
- APK download + installation flow
- signed URL retrieval
- update checks
- rollback-ready package handling patterns
- background-friendly download orchestration and status tracking
- notification and UI update states

All confidential customer, infrastructure, and production-specific values have been replaced with placeholders.

## Sanitization Highlights

Sensitive or internal values were replaced with placeholders such as:
- `YOUR_SERVER_URL`
- `YOUR_UPDATE_ENDPOINT`
- `YOUR_API_KEY`
- `YOUR_BUCKET_NAME`
- `YOUR_PACKAGE_NAME`

Additionally:
- Internal domains/IPs and production backend references were removed.
- Firebase/Google service values were replaced with sample placeholders.
- Package identifiers were changed to a demo-safe namespace (`com.example.appstoredemo`).
- Internal file-provider and download directory naming was sanitized.
- Generated/local Gradle cache artifacts were removed from versioned project content where applicable.

## Architecture Overview

- `activities/MainActivity`  
  Hosts core navigation and app lifecycle wiring.

- `fragments/`  
  UI for home, discover, settings, and installed-app workflows.

- `viewmodels/`  
  MVVM state handling for catalog loading, app list filtering, and update state exposure.

- `repositories/`  
  Encapsulates catalog loading, download execution, APK file handling, and install triggers.

- `network/`  
  Retrofit service interface for remote metadata and signed URL endpoints.

- `managers/`  
  Update and remote-app orchestration logic.

- `utils/`  
  Shared constants, installer helpers, version compare utilities, and threading/executors.

## Configuration

Use placeholder-driven configuration and replace values for your environment:

1. Update constants in:
   - `app/src/main/java/com/bro/brostore/utils/Constants.java`
2. Replace sample Google services values in:
   - `app/google-services.json`
3. Update package/application IDs as needed in:
   - `app/build.gradle`
   - `app/src/main/AndroidManifest.xml`

See [`config.example.env`](config.example.env) for a quick template.

## Build

```bash
./gradlew assembleDebug
```

## Notes for Evaluators / Employers

This showcase focuses on engineering patterns used in real-world managed Android deployment systems:
- structured repository + MVVM architecture
- resilient asynchronous download/update flows
- package-version comparison and installation orchestration
- clear separation of UI, data access, network, and update management

No private infrastructure or customer-specific operational data is included.
