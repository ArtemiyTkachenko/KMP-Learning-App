#!/usr/bin/env python3
"""Print bounded diagnostics from a failed Claude Code execution file."""

import json
import os
import re
import sys
from pathlib import Path

path = os.environ.get("CLAUDE_EXECUTION_FILE", "").strip()
if not path:
    print("Claude action failed without exposing an execution_file output.", file=sys.stderr)
    sys.exit(1)

execution_file = Path(path)
if not execution_file.is_file():
    print(f"Claude execution file is unavailable: {execution_file}", file=sys.stderr)
    sys.exit(1)

try:
    messages = json.loads(execution_file.read_text(encoding="utf-8"))
except Exception as exc:
    print(f"Could not parse Claude execution file: {exc}", file=sys.stderr)
    sys.exit(1)

results = [
    message for message in messages
    if isinstance(message, dict) and message.get("type") == "result"
]
if not results:
    print("Claude execution file contains no result message.", file=sys.stderr)
    sys.exit(1)

result = results[-1]
print("Claude provider diagnostic:")
for key in (
    "subtype",
    "is_error",
    "api_error_status",
    "num_turns",
    "total_cost_usd",
    "permission_denials_count",
):
    if key in result:
        print(f"  {key}: {result[key]!r}")

def redact(value: str) -> str:
    value = re.sub(r"sk-ant-[A-Za-z0-9_-]+", "sk-ant-[REDACTED]", value)
    value = re.sub(r"github_pat_[A-Za-z0-9_]+", "github_pat_[REDACTED]", value)
    value = re.sub(r"gh[pousr]_[A-Za-z0-9]+", "gh_[REDACTED]", value)
    value = re.sub(r"Bearer\s+[A-Za-z0-9._~+/-]+", "Bearer [REDACTED]", value, flags=re.I)
    return value.replace("\x00", "")[:1200]

errors = result.get("errors")
if errors:
    print("  errors:", redact(json.dumps(errors, ensure_ascii=False)))

message = result.get("result")
if isinstance(message, str) and message.strip():
    print("  untrusted_result_excerpt:", redact(message.strip()))

print(
    "Claude failed before returning the required structured output. "
    "Use api_error_status and the excerpt above to diagnose the provider/account failure.",
    file=sys.stderr,
)
sys.exit(1)
