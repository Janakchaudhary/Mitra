# Milestone 5 CI fix — v0.5.1

## Fixed failing `OpenAiResponseParserTest`

The previous test fixture used `\\"` inside a Kotlin raw string. That generated invalid JSON for the mocked Responses API body and caused `JsonDecodingException` before the parser could inspect the output.

The test now constructs the mock response with `kotlinx.serialization` JSON builders so escaping is handled correctly.

`OpenAiResponseParser.outputText()` also accepts:

1. the canonical Responses API shape: `output[].content[].type == "output_text"` and its `text` field; and
2. a top-level `output_text` convenience field, for compatible adapters/clients.

Production structured-output parsing remains unchanged: the extracted text must parse to a JSON object.
