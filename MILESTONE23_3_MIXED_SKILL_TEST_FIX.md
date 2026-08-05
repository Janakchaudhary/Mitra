# Mitra 0.23.3 — Mixed Skill Session Test Fix

## Failure corrected

`DefaultLearningEngineTest.skillSessionMixesConceptsAndIncludesCarryWork` could fail because the adaptive planner gave a small score advantage to vocabulary, spelling and word-problem activity types. In a six-question session, those questions could fill every slot before a carry/borrow question was selected.

## Changes

- Added `MixedSkillCoveragePolicy` after adaptive ranking.
- A mixed skill session now guarantees one regrouping question when the generated pool contains one.
- The session keeps at least four distinct learning concepts when enough concepts are available.
- Fingerprints remain unique.
- The movement/off-screen break no longer replaces the only carry/borrow question or unnecessarily removes concept diversity.
- The movement break remains near the fourth activity.
- Added a focused regression test for carry, diversity, uniqueness and movement-break preservation.

## Version

- Version name: `0.23.3`
- Version code: `47`
- Room schema: unchanged at `6`
