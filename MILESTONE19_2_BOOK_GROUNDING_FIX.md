# Mitra 0.19.2 - prepared-book grounding fix

- Converts printed page numbers from a contents page into physical PDF page numbers.
- Example: contents on physical PDF page 12 and lesson starts at printed page 1 => chapter starts at PDF page 13.
- Stops unrelated prepared pages from being passed to the answerer when no query word matches.
- Adds a grounded Gujarati definition path for young-child vocabulary, including `દંગોરો`.
- Existing wrongly prepared chapters must be detected/saved again and re-prepared once.
