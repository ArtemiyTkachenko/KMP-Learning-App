#!/usr/bin/env python3
"""Resolve one E##-## backlog key into a compact JSON task context."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import yaml


KEY_RE = re.compile(r"^E\d{2}-\d{2}$")
BACKLOG_PATH = Path(".github/project/backlog.yml")


def fail(message: str) -> "NoReturn":
    print(message, file=sys.stderr)
    raise SystemExit(2)


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: resolve_backlog.py E##-##")

    key = sys.argv[1].strip().upper()
    if not KEY_RE.fullmatch(key):
        fail(f"invalid backlog key: {key!r}; expected E##-##")

    data = yaml.safe_load(BACKLOG_PATH.read_text(encoding="utf-8"))
    for epic in data.get("epics", []):
        for issue in epic.get("issues", []):
            if issue.get("key") != key:
                continue

            result = {
                "key": key,
                "epic": {
                    "key": epic.get("key"),
                    "title": epic.get("github_title"),
                },
                "title": issue.get("title"),
                "issue": issue.get("issue"),
                "approach": issue.get("approach"),
                "acceptance_criteria": issue.get("acceptance_criteria", []),
                "priority": issue.get("priority"),
                "size": issue.get("size"),
                "initial_status": issue.get("initial_status"),
            }
            json.dump(result, sys.stdout, indent=2, ensure_ascii=False)
            sys.stdout.write("\n")
            return

    fail(f"backlog key {key} was not found in {BACKLOG_PATH}")


if __name__ == "__main__":
    main()
