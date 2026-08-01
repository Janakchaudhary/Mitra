# Milestone 12 CI Fix — v0.12.1

Fixed `MitraBackupService.restoreFrom()` returning an inferred `Result<Boolean>` because `File.deleteRecursively()` was the final expression in `runCatching`.

The restore block now ends with explicit `Unit`, matching the declared `Result<Unit>` return type.

No Room migration or behavior change is required.
