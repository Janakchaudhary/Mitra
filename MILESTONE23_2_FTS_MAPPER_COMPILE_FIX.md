# Mitra 0.23.2 — FTS Mapper Compile Fix

## Fixed

`LocalBookKnowledgeRepository` used a callable reference to a member extension:

```kotlin
pages.map(PageKnowledgeEntity::toFts)
```

Kotlin prohibits references to declarations that are both members and extensions. The mapper is now a normal private member function and is invoked through an explicit lambda:

```kotlin
pages.map { page -> toFtsEntity(page) }
```

This preserves the FTS row-id generation and indexed content while removing the compiler ambiguity.

- Version code: 46
- Version name: 0.23.2
- Room schema: unchanged at 6
