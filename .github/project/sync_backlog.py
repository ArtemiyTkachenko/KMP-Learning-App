from __future__ import annotations

FIELDS_INITIALIZED_MARKER = "<!-- backlog-fields-initialized -->"

import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

import yaml


class SyncError(Exception):
    pass


def fail(message: str) -> None:
    raise SyncError(message)


def run_gh(*args: str) -> str:
    command = ["gh", *args]

    result = subprocess.run(
        command,
        text=True,
        capture_output=True,
    )

    if result.returncode != 0:
        fail(
            "GitHub CLI command failed:\n"
            f"  {' '.join(command)}\n\n"
            f"{result.stderr.strip()}"
        )

    return result.stdout.strip()


def load_backlog(path: Path) -> dict[str, Any]:
    try:
        with path.open("r", encoding="utf-8") as file:
            data = yaml.safe_load(file)
    except (OSError, yaml.YAMLError) as error:
        fail(f"Unable to read backlog: {error}")

    if not isinstance(data, dict):
        fail("Backlog root must be an object.")

    return data


def load_repository_issues(repository: str) -> list[dict[str, Any]]:
    output = run_gh(
        "issue",
        "list",
        "--repo",
        repository,
        "--state",
        "all",
        "--limit",
        "1000",
        "--json",
        "number,title,body,url",
    )

    data = json.loads(output)

    if not isinstance(data, list):
        fail("Unexpected response while loading repository issues.")

    return data


def build_issue_title(issue: dict[str, Any]) -> str:
    return f"{issue['key']} {issue['title']}"


def build_issue_marker(issue_key: str) -> str:
    return f"<!-- backlog-key: {issue_key} -->"


def build_issue_body(
    issue: dict[str, Any],
    existing_body: str = "",
) -> str:
    markers = [build_issue_marker(issue["key"])]

    if FIELDS_INITIALIZED_MARKER in existing_body:
        markers.append(FIELDS_INITIALIZED_MARKER)

    marker_block = "\n".join(markers)

    acceptance_criteria = "\n".join(
        f"- [ ] {criterion}"
        for criterion in issue["acceptance_criteria"]
    )

    return f"""\
{marker_block}

## Issue

{issue["issue"].strip()}

## Approach

{issue["approach"].strip()}

## Acceptance criteria

{acceptance_criteria}
"""


def resolve_parent_epics(
    backlog: dict[str, Any],
    repository_issues: list[dict[str, Any]],
) -> dict[str, int]:
    parents: dict[str, int] = {}

    for epic in backlog["epics"]:
        epic_key = epic["key"]
        expected_title = epic["github_title"]

        matches = [
            issue
            for issue in repository_issues
            if issue["title"] == expected_title
        ]

        if not matches:
            fail(
                f"{epic_key}: parent epic was not found in GitHub.\n"
                f"Expected exact title: {expected_title}"
            )

        if len(matches) > 1:
            numbers = ", ".join(
                f"#{issue['number']}"
                for issue in matches
            )

            fail(
                f"{epic_key}: multiple GitHub issues match the parent "
                f"title '{expected_title}': {numbers}"
            )

        parents[epic_key] = matches[0]["number"]

    return parents


def find_existing_child(
    issue: dict[str, Any],
    repository_issues: list[dict[str, Any]],
) -> dict[str, Any] | None:
    issue_key = issue["key"]
    marker = build_issue_marker(issue_key)
    title_prefix = f"{issue_key} "

    matches_by_number: dict[int, dict[str, Any]] = {}

    for github_issue in repository_issues:
        body = github_issue.get("body") or ""
        title = github_issue.get("title") or ""

        if marker in body or title.startswith(title_prefix):
            matches_by_number[github_issue["number"]] = github_issue

    matches = list(matches_by_number.values())

    if len(matches) > 1:
        numbers = ", ".join(
            f"#{github_issue['number']}"
            for github_issue in matches
        )

        fail(
            f"{issue_key}: multiple existing GitHub issues match "
            f"this backlog key: {numbers}"
        )

    if not matches:
        return None

    return matches[0]


def preflight(
    backlog: dict[str, Any],
    repository_issues: list[dict[str, Any]],
) -> tuple[dict[str, int], dict[str, dict[str, Any] | None]]:
    print("Running GitHub preflight checks...")

    parent_numbers = resolve_parent_epics(
        backlog,
        repository_issues,
    )

    existing_children: dict[str, dict[str, Any] | None] = {}

    for epic in backlog["epics"]:
        for issue in epic["issues"]:
            existing_children[issue["key"]] = find_existing_child(
                issue,
                repository_issues,
            )

    print(f"Resolved {len(parent_numbers)} parent epics.")

    existing_count = sum(
        child is not None
        for child in existing_children.values()
    )

    planned_count = len(existing_children)

    print(f"Planned child issues: {planned_count}")
    print(f"Existing child issues: {existing_count}")
    print(f"Missing child issues: {planned_count - existing_count}")

    return parent_numbers, existing_children


def write_temp_body(body: str) -> str:
    temp_file = tempfile.NamedTemporaryFile(
        mode="w",
        encoding="utf-8",
        suffix=".md",
        delete=False,
    )

    try:
        temp_file.write(body)
        temp_file.flush()
        return temp_file.name
    finally:
        temp_file.close()


def create_child_issue(
    *,
    repository: str,
    project_title: str,
    parent_number: int,
    issue: dict[str, Any],
) -> int:
    title = build_issue_title(issue)
    body = build_issue_body(issue)

    body_file = write_temp_body(body)

    try:
        output = run_gh(
            "issue",
            "create",
            "--repo",
            repository,
            "--title",
            title,
            "--body-file",
            body_file,
            "--parent",
            str(parent_number),
            "--project",
            project_title,
        )
    finally:
        Path(body_file).unlink(missing_ok=True)

    # gh issue create returns the created issue URL.
    issue_url = output.splitlines()[-1].strip()

    if not issue_url:
        fail(f"{issue['key']}: GitHub did not return an issue URL.")

    issue_number_text = issue_url.rstrip("/").split("/")[-1]

    try:
        return int(issue_number_text)
    except ValueError:
        fail(
            f"{issue['key']}: unable to determine issue number "
            f"from GitHub response: {issue_url}"
        )


def update_child_issue(
    *,
    repository: str,
    project_title: str,
    parent_number: int,
    github_issue_number: int,
    issue: dict[str, Any],
    existing_body: str,
) -> None:
    title = build_issue_title(issue)
    body = build_issue_body(issue, existing_body)

    body_file = write_temp_body(body)

    try:
        run_gh(
            "issue",
            "edit",
            str(github_issue_number),
            "--repo",
            repository,
            "--title",
            title,
            "--body-file",
            body_file,
            "--parent",
            str(parent_number),
            "--add-project",
            project_title,
        )
    finally:
        Path(body_file).unlink(missing_ok=True)


def synchronize(
    backlog: dict[str, Any],
    repository: str,
    project_title: str,
    parent_numbers: dict[str, int],
    existing_children: dict[str, dict[str, Any] | None],
) -> tuple[int, int]:
    created = 0
    updated = 0

    for epic in backlog["epics"]:
        epic_key = epic["key"]
        parent_number = parent_numbers[epic_key]

        print()
        print(f"{epic_key}: {epic['github_title']}")

        for issue in epic["issues"]:
            issue_key = issue["key"]
            existing = existing_children[issue_key]

            if existing is None:
                print(f"  CREATE {issue_key}: {issue['title']}")

                number = create_child_issue(
                    repository=repository,
                    project_title=project_title,
                    parent_number=parent_number,
                    issue=issue,
                )

                print(f"         created as #{number}")
                created += 1

            else:
                number = existing["number"]

                print(
                    f"  UPDATE {issue_key}: "
                    f"existing issue #{number}"
                )

                update_child_issue(
                    repository=repository,
                    project_title=project_title,
                    parent_number=parent_number,
                    github_issue_number=number,
                    issue=issue,
                    existing_body=existing.get("body") or "",
                )

                updated += 1

    return created, updated


def main() -> int:
    backlog_path = (
        Path(sys.argv[1])
        if len(sys.argv) > 1
        else Path(".github/project/backlog.yml")
    )

    repository = os.environ.get("GITHUB_REPOSITORY", "").strip()
    project_title = os.environ.get("PROJECT_TITLE", "").strip()

    if not repository:
        print("SYNC FAILED")
        print("GITHUB_REPOSITORY is not available.")
        return 1

    if not project_title:
        print("SYNC FAILED")
        print("PROJECT_TITLE is not configured.")
        return 1

    try:
        backlog = load_backlog(backlog_path)

        # Important: all parent and duplicate checks happen
        # before the first GitHub mutation.
        repository_issues = load_repository_issues(repository)

        parent_numbers, existing_children = preflight(
            backlog,
            repository_issues,
        )

        print()
        print("Preflight passed.")
        print("Starting synchronization...")

        created, updated = synchronize(
            backlog=backlog,
            repository=repository,
            project_title=project_title,
            parent_numbers=parent_numbers,
            existing_children=existing_children,
        )

    except SyncError as error:
        print()
        print("BACKLOG SYNC FAILED")
        print(error)
        return 1

    print()
    print("BACKLOG SYNC COMPLETED")
    print(f"Created: {created}")
    print(f"Updated: {updated}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
