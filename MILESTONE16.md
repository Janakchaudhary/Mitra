# Milestone 16 — Resilient Study Talk, Offline Local AI, and Vertical Carry Entry

## Cloudflare Study Talk reliability

- Study Talk asks Cloudflare for a short plain-text Gujarati answer instead of requiring JSON for conversational turns.
- The response parser accepts OpenAI-compatible `choices[].message.content`, text-part arrays, native Workers AI `result.response`, nested output envelopes, and common final-text keys.
- `<think>` and `<analysis>` blocks are removed before child display.
- If both Cloudflare endpoints fail or return an unreadable answer, Mitra uses a grounded local extractive answer from prepared textbook pages instead of displaying a provider/parser error.
- Vague follow-up phrases such as `એના વિષે વાત કરીએ` combine with the previous child question for source retrieval.

## Third provider: Offline Local

Parent Settings now offers:

1. OpenAI (default cloud provider)
2. Cloudflare Workers AI
3. Offline Local

Offline Local has two levels:

- **Always available extractive mode:** chooses relevant sentences from page knowledge already prepared and stored on the phone. It does not invent facts.
- **Optional on-device LLM:** the parent can import a compatible `.litertlm` file. Mitra copies it into private app storage and runs it through LiteRT-LM, trying GPU first and CPU as fallback.

The model is not bundled in the APK because compatible models range from hundreds of MB to several GB. Importing/replacing/removing the model is parent-only. Replacing the file invalidates the warm engine so the new model is loaded on the next turn.

### Current limitation

Offline Local does not analyze arbitrary new scanned PDF page images. Use OpenAI/Cloudflare once to prepare those pages, or enter chapter ranges manually. Once page knowledge exists, grounded textbook chat and cached activities can work offline.

## Guided vertical maths

- Place-value order remains `દશક` then `એકમ`.
- The carry/borrow input is directly above the `દશક` input.
- The child still works in the teaching sequence: `એકમ → કેરિ/ઉધાર → દશક`.

## Build notes

- LiteRT-LM Android dependency: `com.google.ai.edge.litertlm:litertlm-android:0.14.0`.
- GitHub Actions uses Java 21 so the current LiteRT-LM artifact can be compiled and packaged.
- Optional GPU native-library declarations are included; runtime falls back to CPU when GPU initialization fails.

## Upgrade

- `versionCode = 28`
- `versionName = 0.16.1`
- Room schema is unchanged; no database migration is required.
