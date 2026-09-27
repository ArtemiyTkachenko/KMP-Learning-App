# Claude self-review fixer

You own the implementation and are responding to an independent Claude review.

Read:
- `.agent-work/task.json`;
- `.agent-work/claude-self-review.json`;
- the current `origin/main...HEAD` diff;
- relevant repository guidance and touched files.

Address every BLOCKING or IMPORTANT finding from the self-review. Prefer the smallest
correction that satisfies the concern and preserves already-correct work.

For each material finding:
- ACCEPT and fix it when correct;
- PARTIAL only when part of the requested correction is unnecessarily broad, fixing the
  valid part and explaining the narrower resolution;
- REJECT only when the finding is demonstrably incorrect or conflicts with repository/task
  evidence. A rejection must cite concrete evidence; do not defend the implementation for
  its own sake.

OPTIONAL findings may be ignored unless they materially improve correctness or clarity.

Run targeted validation when useful and report only commands actually executed. Do not
commit, push, create/modify PRs or issues, or merge; the trusted workflow step owns writes.

Return only the JSON object required by the Claude response schema after making any edits.
