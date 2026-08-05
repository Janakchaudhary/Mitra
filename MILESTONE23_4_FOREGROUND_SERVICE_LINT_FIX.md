# Mitra 0.23.4 — WorkManager foreground-service lint fix

## Problem

Android Lint reported `SpecifyForegroundServiceType` for `BookPreparationWorker`.
The worker already returned `ForegroundInfo` with
`ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC`, and the app already declared
`FOREGROUND_SERVICE_DATA_SYNC`, but WorkManager's shared
`SystemForegroundService` was not overridden in the app manifest with the same
service type.

## Fix

`AndroidManifest.xml` now merges this declaration into WorkManager's service:

```xml
<service
    android:name="androidx.work.impl.foreground.SystemForegroundService"
    android:foregroundServiceType="dataSync"
    tools:node="merge" />
```

The manifest also declares the `tools` namespace. The existing permissions and
runtime foreground type remain unchanged.

## Version

- Version name: `0.23.4`
- Version code: `48`
- Room schema: `6` (unchanged)
