# SaveTrick — TikTok Video Downloader & Saver
**Version 2.5.7**  
**Developer:** Prachurjo Sorkar Porosh  
**Website:** [https://prachurjo.pro.bd/](https://prachurjo.pro.bd/)  
**Copyright:** © 2026 SaveTrick. All rights reserved.

---

## Overview
SaveTrick is a production-quality Android application built with Kotlin and Jetpack Compose for saving TikTok videos, photo slideshows, and audio directly to the user's device.

### Supported Formats
- **VIDEO**: TikTok video preview with Media3 ExoPlayer, aspect-ratio preservation, video download, and audio stream extraction.
- **PHOTO_SLIDESHOW**: Vertical image presentation with original aspect ratios, individual image download, and "Download All" (generating individual records).
- **AUDIO**: High quality audio extraction when a real audio stream exists.

### Architecture & Tech Stack
- **Kotlin & Jetpack Compose**: Material 3 theming (Light, Dark, System Default) with Plus Jakarta Sans typography.
- **Room Database**: Persistent download queue, stats, and history.
- **Real Streaming Download Engine**: OkHttp byte-streaming, real progress tracking, accurate transfer speeds, and duplicate/cancellation management.
- **Localization**: Full English and Bengali language support.
- **Admin Portal**: Hidden 4-tap trigger, email/password Firebase Auth with Firestore role verification, user management, audit logging, and payment verification.

## Building the APK
To build the debug APK locally:
```bash
gradle assembleDebug
```
The resulting APK will be placed in `app/build/outputs/apk/debug/`.
