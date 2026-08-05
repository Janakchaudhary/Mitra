# Mitra 0.22.1 compile fix

This patch fixes two Kotlin compilation failures in Milestone 22.

## Fixes

1. `LocalBookRepository.renderCover()` is now a `suspend` function because it calls the suspend `PdfPageRenderer.render()` API. Both call sites already execute inside the suspend `importBook()` flow on `Dispatchers.IO`.
2. Removed the explicit `androidx.compose.foundation.layout.weight` import from `BookListScreen.kt`. `Modifier.weight()` is resolved from the `RowScope` receiver inside the `Row` content block; the explicit import selected an inaccessible internal symbol with the Compose version used by the project.

## Version

- versionName: 0.22.1
- versionCode: 43
