# Milestone 11 CI Fix — v0.11.1

The multiplication-table generator intentionally produces two visual/activity forms:

- direct table drills (`ActivityType.TABLES`)
- table-based story problems (`ActivityType.WORD_PROBLEM`)

The previous unit test incorrectly required every generated activity to be `TABLES`, so it failed after question-variety improvements were added. The test now verifies that:

- every table concept creates five questions
- every question is either a direct table drill or a table word problem
- both forms occur in the generated set
- every question is numeric and has an expected answer
- every question remains attached to the requested table concept

No production behavior or Room schema changed.
