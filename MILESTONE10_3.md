# Mitra 0.10.3 — Cloudflare response compatibility fix

- Supports OpenAI-compatible Workers AI responses and native Cloudflare REST envelopes (`result.response`).
- If `/ai/v1/chat/completions` returns HTTP success but no final assistant text, retries the same Workers AI model through `/ai/run/@cf/...`.
- Structured JSON generation retries without `response_format` when model/endpoint JSON-schema behavior differs.
- Uses `max_completion_tokens` rather than deprecated `max_tokens` for Cloudflare requests.
- No Room migration.
