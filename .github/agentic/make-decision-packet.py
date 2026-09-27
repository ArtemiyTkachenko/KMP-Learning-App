#!/usr/bin/env python3
"""Render the final Codex/Claude disagreement as a compact PR-ready Markdown packet."""
from __future__ import annotations
import json
import sys
from pathlib import Path

def load(path: str) -> dict:
    return json.loads(Path(path).read_text(encoding="utf-8"))

def main() -> None:
    if len(sys.argv) != 5:
        raise SystemExit("usage: make-decision-packet.py TASK CODEX_REVIEW CLAUDE_POSITION OUTPUT")
    task = load(sys.argv[1])
    codex = load(sys.argv[2])
    claude = load(sys.argv[3])
    active = [
        f for f in codex["findings"]
        if f["status"] != "WITHDRAWN" and f["severity"] in {"BLOCKING", "IMPORTANT"}
    ]
    responses = {r["findingId"]: r for r in claude["findingResponses"]}
    lines = [
        "## Human decision required", "",
        "Five Codex ↔ Claude negotiation iterations completed without common ground.",
        "No automatic merge will be attempted.", "",
        "### Task", "", f"**{task['title']}**", "", task["objective"], "",
        "### Codex position", "", codex["positionBrief"].strip(), "",
        "### Claude position", "", claude["positionBrief"].strip(), "",
        "### Remaining disputed findings", "",
    ]
    if not active:
        lines.append(
            "The final structured review still reports no consensus, but contains no active "
            "blocking/important findings. Inspect the two position briefs before deciding."
        )
    else:
        for finding in active:
            response = responses.get(finding["id"])
            lines.extend([
                f"#### {finding['id']} — {finding['severity']}", "",
                f"**Codex:** {finding['problem']}", "",
                f"**Requested change:** {finding['requestedChange']}", "",
                f"**Codex rationale:** {finding['rationale']}", "",
            ])
            if response:
                lines.extend([
                    f"**Claude stance:** {response['stance']}", "",
                    f"**Claude reasoning:** {response['reasoning']}", "",
                    f"**Claude action:** {response['actionTaken']}", "",
                ])
    lines.extend([
        "### Decision", "",
        "Review the disputed points and decide which position to accept, or define a "
        "third resolution. The pipeline intentionally stops here.", "",
    ])
    Path(sys.argv[4]).write_text("\n".join(lines), encoding="utf-8")

if __name__ == "__main__":
    main()
