# Milestone 10.2 — Cloudflare Free Provider + System Bar Insets

## Added

- OpenAI remains the default remote provider.
- Cloudflare Workers AI can be selected as a second provider in Parent → AI provider.
- Cloudflare credentials are stored separately from the OpenAI key.
- Cloudflare mode accepts only `@cf/...` Workers AI models.
- Default Cloudflare model: `@cf/google/gemma-4-26b-a4b-it`.
- Book contents detection, chapter preparation, textbook-derived activities, and grounded study Q&A route through the selected provider.
- Built-in Standard 2 skills stay local/offline regardless of provider.
- Android edge-to-edge system bar handling now applies safe drawing insets globally to prevent content overlapping time/network/battery/navigation system UI.

## Cloudflare setup

1. Create/sign in to a Cloudflare account.
2. Open Workers AI.
3. Choose **Use REST API**.
4. Copy the Account ID.
5. Create/copy a Workers AI API token.
6. In Mitra Parent → AI provider, enable remote AI and choose **Cloudflare Free**.
7. Enter Account ID and token, then Test connection.

Cloudflare free-tier quotas are provider-controlled and can change. The app does not embed credentials.
