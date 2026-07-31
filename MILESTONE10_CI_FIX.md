# Milestone 10 CI Fix — v0.10.1

## Kotlin compile fix

GitHub Actions reported an overload-resolution ambiguity for `Iterable.sumOf` in
`StudyContextService.score()`. The selector returned only integer literals, which Kotlin 2.1.20
could not disambiguate between the `Int` and `Long` overloads in this context.

The scoring aggregation now uses an explicitly `Int`-typed `fold(0)`:

```kotlin
return tokens.fold(0) { total, token ->
    total + when {
        haystack.contains(" $token ") -> 5
        haystack.contains(token) -> 2
        else -> 0
    }
}
```

This preserves the exact scoring behavior while removing numeric-overload inference.

Version bumped to `0.10.1` / code 14. No Room migration is required.
