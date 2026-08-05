# Mitra 0.23.1 — Room FTS compile fix

## Fixed

`PageKnowledgeFtsDao.search()` previously used `SELECT *` against the FTS4 table.
SQLite's `*` projection does not include the hidden FTS `rowid`, but
`PageKnowledgeFtsEntity` declares `rowid` as its non-null primary key. Room KSP
therefore rejected the DAO result mapping.

The query now explicitly returns every entity column, including `rowid`:

```sql
SELECT rowid, pageKnowledgeId, bookId, chapterId, pageNumberText, content
FROM page_knowledge_fts
WHERE page_knowledge_fts MATCH :query
LIMIT :limit
```

No Room schema migration is needed. The database schema remains version 6.

- App version: 0.23.1
- Version code: 45
