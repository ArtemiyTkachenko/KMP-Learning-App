from __future__ import annotations

import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any

import yaml


FIELDS_INITIALIZED_MARKER = "<!-- backlog-fields-initialized -->"


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


def load_repository_issues(
    repository: str,
) -> list[dict[str, Any]]:
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
        "number,title,body,url,parent",
    )

    try:
        data = json.loads(output)

    except json.JSONDecodeError as error:
        fail(
            "Unable to parse repository issues:\n"
            f"{error}"
        )

    if not isinstance(data, list):
        fail(
            "Unexpected response while loading "
            "repository issues."
        )

    return data


# ------------------------------------------------------------------
# Stable identifiers
# ------------------------------------------------------------------

def build_epic_marker(epic_key: str) -> str:
    return f"<!-- backlog-epic-key: {epic_key} -->"


def build_issue_marker(issue_key: str) -> str:
    return f"<!-- backlog-key: {issue_key} -->"


def build_epic_title_prefix(epic_key: str) -> str:
    epic_number = epic_key[1:]
    return f"EPIC-{epic_number} "


def build_issue_title(
    issue: dict[str, Any],
) -> str:
    return f"{issue['key']} {issue['title']}"


# ------------------------------------------------------------------
# Body generation
# ------------------------------------------------------------------

def build_epic_body(
    epic: dict[str, Any],
    existing_body: str = "",
) -> str:
    marker = build_epic_marker(epic["key"])
    goal = epic.get("goal")

    # Epics with a goal are fully managed by backlog.yml.
    if isinstance(goal, str) and goal.strip():
        return (
            f"{marker}\n\n"
            "## Goal\n\n"
            f"{goal.strip()}\n"
        )

    # Historical epics E01-E04 predate managed epic bodies.
    # Preserve their existing content and only add the stable marker.
    existing_body = existing_body.strip()

    if marker in existing_body:
        return f"{existing_body}\n"

    if existing_body:
        return (
            f"{marker}\n\n"
            f"{existing_body}\n"
        )

    return f"{marker}\n"


def build_issue_body(
    issue: dict[str, Any],
    existing_body: str = "",
) -> str:
    markers = [
        build_issue_marker(issue["key"])
    ]

    if FIELDS_INITIALIZED_MARKER in existing_body:
        markers.append(
            FIELDS_INITIALIZED_MARKER
        )

    marker_block = "\n".join(markers)

    acceptance_criteria = "\n".join(
        f"- [ ] {criterion}"
        for criterion in issue[
            "acceptance_criteria"
        ]
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


# ------------------------------------------------------------------
# Existing GitHub issue discovery
# ------------------------------------------------------------------

def find_existing_epic(
    epic: dict[str, Any],
    repository_issues: list[dict[str, Any]],
) -> dict[str, Any] | None:
    epic_key = epic["key"]
    marker = build_epic_marker(epic_key)
    title_prefix = build_epic_title_prefix(
        epic_key
    )

    matches_by_number: dict[
        int,
        dict[str, Any],
    ] = {}

    for github_issue in repository_issues:
        body = github_issue.get("body") or ""
        title = github_issue.get("title") or ""

        if (
            marker in body
            or title.startswith(title_prefix)
        ):
            matches_by_number[
                github_issue["number"]
            ] = github_issue

    matches = list(
        matches_by_number.values()
    )

    if len(matches) > 1:
        numbers = ", ".join(
            f"#{github_issue['number']}"
            for github_issue in matches
        )

        fail(
            f"{epic_key}: multiple existing GitHub "
            f"issues match this epic key: {numbers}"
        )

    if not matches:
        return None

    return matches[0]


def find_existing_child(
    issue: dict[str, Any],
    repository_issues: list[dict[str, Any]],
) -> dict[str, Any] | None:
    issue_key = issue["key"]
    marker = build_issue_marker(issue_key)
    title_prefix = f"{issue_key} "

    matches_by_number: dict[
        int,
        dict[str, Any],
    ] = {}

    for github_issue in repository_issues:
        body = github_issue.get("body") or ""
        title = github_issue.get("title") or ""

        if (
            marker in body
            or title.startswith(title_prefix)
        ):
            matches_by_number[
                github_issue["number"]
            ] = github_issue

    matches = list(
        matches_by_number.values()
    )

    if len(matches) > 1:
        numbers = ", ".join(
            f"#{github_issue['number']}"
            for github_issue in matches
        )

        fail(
            f"{issue_key}: multiple existing GitHub "
            f"issues match this backlog key: {numbers}"
        )

    if not matches:
        return None

    return matches[0]


# ------------------------------------------------------------------
# Preflight
# ------------------------------------------------------------------

def preflight(
    backlog: dict[str, Any],
    repository_issues: list[dict[str, Any]],
) -> tuple[
    dict[str, dict[str, Any] | None],
    dict[str, dict[str, Any] | None],
]:
    print(
        "Running GitHub preflight checks..."
    )

    existing_epics: dict[
        str,
        dict[str, Any] | None,
    ] = {}

    existing_children: dict[
        str,
        dict[str, Any] | None,
    ] = {}

    for epic in backlog["epics"]:
        epic_key = epic["key"]

        existing_epics[epic_key] = (
            find_existing_epic(
                epic,
                repository_issues,
            )
        )

        for issue in epic["issues"]:
            existing_children[
                issue["key"]
            ] = find_existing_child(
                issue,
                repository_issues,
            )

    planned_epics = len(existing_epics)

    existing_epic_count = sum(
        epic is not None
        for epic in existing_epics.values()
    )

    planned_children = len(
        existing_children
    )

    existing_child_count = sum(
        child is not None
        for child in existing_children.values()
    )

    print(
        f"Planned epics: {planned_epics}"
    )

    print(
        f"Existing epics: "
        f"{existing_epic_count}"
    )

    print(
        f"Missing epics: "
        f"{planned_epics - existing_epic_count}"
    )

    print(
        f"Planned child issues: "
        f"{planned_children}"
    )

    print(
        f"Existing child issues: "
        f"{existing_child_count}"
    )

    print(
        f"Missing child issues: "
        f"{planned_children - existing_child_count}"
    )

    return (
        existing_epics,
        existing_children,
    )


# ------------------------------------------------------------------
# Temporary body files
# ------------------------------------------------------------------

def write_temp_body(body: str) -> str:
    temp_file = (
        tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            suffix=".md",
            delete=False,
        )
    )

    try:
        temp_file.write(body)
        temp_file.flush()
        return temp_file.name

    finally:
        temp_file.close()


def issue_number_from_create_output(
    *,
    output: str,
    key: str,
) -> int:
    issue_url = (
        output.splitlines()[-1].strip()
        if output.strip()
        else ""
    )

    if not issue_url:
        fail(
            f"{key}: GitHub did not "
            "return an issue URL."
        )

    number_text = (
        issue_url
        .rstrip("/")
        .split("/")[-1]
    )

    try:
        return int(number_text)

    except ValueError:
        fail(
            f"{key}: unable to determine "
            "issue number from GitHub response: "
            f"{issue_url}"
        )


# ------------------------------------------------------------------
# Epic synchronization
# ------------------------------------------------------------------

def create_epic_issue(
    *,
    repository: str,
    project_title: str,
    epic: dict[str, Any],
) -> int:
    title = epic["github_title"]
    body = build_epic_body(epic)

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
            "--label",
            "epic",
            "--project",
            project_title,
        )

    finally:
        Path(body_file).unlink(
            missing_ok=True
        )

    return issue_number_from_create_output(
        output=output,
        key=epic["key"],
    )


def update_epic_issue(
    *,
    repository: str,
    project_title: str,
    github_issue: dict[str, Any],
    epic: dict[str, Any],
) -> int:
    github_issue_number = (
        github_issue["number"]
    )

    existing_body = (
        github_issue.get("body") or ""
    )

    body = build_epic_body(
        epic,
        existing_body,
    )

    body_file = write_temp_body(body)

    try:
        run_gh(
            "issue",
            "edit",
            str(github_issue_number),
            "--repo",
            repository,
            "--title",
            epic["github_title"],
            "--body-file",
            body_file,
            "--add-label",
            "epic",
            "--add-project",
            project_title,
        )

    finally:
        Path(body_file).unlink(
            missing_ok=True
        )

    return github_issue_number


def synchronize_epics(
    *,
    backlog: dict[str, Any],
    repository: str,
    project_title: str,
    existing_epics: dict[
        str,
        dict[str, Any] | None,
    ],
) -> tuple[
    dict[str, int],
    int,
    int,
]:
    parent_numbers: dict[str, int] = {}

    created = 0
    updated = 0

    print()
    print("Synchronizing parent epics...")

    for epic in backlog["epics"]:
        epic_key = epic["key"]
        existing = existing_epics[
            epic_key
        ]

        if existing is None:
            print(
                f"  CREATE {epic_key}: "
                f"{epic['github_title']}"
            )

            number = create_epic_issue(
                repository=repository,
                project_title=project_title,
                epic=epic,
            )

            print(
                f"         created as #{number}"
            )

            created += 1

        else:
            number = existing["number"]

            print(
                f"  UPDATE {epic_key}: "
                f"existing issue #{number}"
            )

            number = update_epic_issue(
                repository=repository,
                project_title=project_title,
                github_issue=existing,
                epic=epic,
            )

            updated += 1

        parent_numbers[
            epic_key
        ] = number

    return (
        parent_numbers,
        created,
        updated,
    )


# ------------------------------------------------------------------
# Child synchronization
# ------------------------------------------------------------------

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
        Path(body_file).unlink(
            missing_ok=True
        )

    return issue_number_from_create_output(
        output=output,
        key=issue["key"],
    )


def update_child_issue(
    *,
    repository: str,
    project_title: str,
    parent_number: int,
    github_issue: dict[str, Any],
    issue: dict[str, Any],
) -> None:
    github_issue_number = (
        github_issue["number"]
    )

    title = build_issue_title(issue)

    existing_body = (
        github_issue.get("body") or ""
    )

    body = build_issue_body(
        issue,
        existing_body,
    )

    current_parent = (
        github_issue.get("parent")
    )

    current_parent_number = (
        current_parent.get("number")
        if isinstance(
            current_parent,
            dict,
        )
        else None
    )

    body_file = write_temp_body(body)

    try:
        # Keep ordinary issue metadata synchronized.
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
            "--add-project",
            project_title,
        )

        # Only modify hierarchy when necessary.
        if (
            current_parent_number
            == parent_number
        ):
            print(
                "         parent already "
                f"correct: #{parent_number}"
            )

        elif current_parent_number is None:
            print(
                "         setting parent: "
                f"#{parent_number}"
            )

            run_gh(
                "issue",
                "edit",
                str(github_issue_number),
                "--repo",
                repository,
                "--parent",
                str(parent_number),
            )

        else:
            print(
                "         changing parent: "
                f"#{current_parent_number} "
                f"-> #{parent_number}"
            )

            run_gh(
                "issue",
                "edit",
                str(github_issue_number),
                "--repo",
                repository,
                "--remove-parent",
            )

            run_gh(
                "issue",
                "edit",
                str(github_issue_number),
                "--repo",
                repository,
                "--parent",
                str(parent_number),
            )

    finally:
        Path(body_file).unlink(
            missing_ok=True
        )


def synchronize_children(
    *,
    backlog: dict[str, Any],
    repository: str,
    project_title: str,
    parent_numbers: dict[str, int],
    existing_children: dict[
        str,
        dict[str, Any] | None,
    ],
) -> tuple[int, int]:
    created = 0
    updated = 0

    print()
    print("Synchronizing child issues...")

    for epic in backlog["epics"]:
        epic_key = epic["key"]
        parent_number = (
            parent_numbers[epic_key]
        )

        print()
        print(
            f"{epic_key}: "
            f"{epic['github_title']}"
        )

        if not epic["issues"]:
            print(
                "  No child issues defined."
            )
            continue

        for issue in epic["issues"]:
            issue_key = issue["key"]

            existing = (
                existing_children[
                    issue_key
                ]
            )

            if existing is None:
                print(
                    f"  CREATE {issue_key}: "
                    f"{issue['title']}"
                )

                number = create_child_issue(
                    repository=repository,
                    project_title=project_title,
                    parent_number=parent_number,
                    issue=issue,
                )

                print(
                    f"         created as "
                    f"#{number}"
                )

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
                    github_issue=existing,
                    issue=issue,
                )

                updated += 1

    return created, updated


# ------------------------------------------------------------------
# Main
# ------------------------------------------------------------------

def main() -> int:
    backlog_path = (
        Path(sys.argv[1])
        if len(sys.argv) > 1
        else Path(
            ".github/project/backlog.yml"
        )
    )

    repository = os.environ.get(
        "GITHUB_REPOSITORY",
        "",
    ).strip()

    project_title = os.environ.get(
        "PROJECT_TITLE",
        "",
    ).strip()

    if not repository:
        print("SYNC FAILED")
        print(
            "GITHUB_REPOSITORY is "
            "not available."
        )
        return 1

    if not project_title:
        print("SYNC FAILED")
        print(
            "PROJECT_TITLE is "
            "not configured."
        )
        return 1

    try:
        backlog = load_backlog(
            backlog_path
        )

        # All duplicate discovery happens before
        # the first GitHub mutation.
        repository_issues = (
            load_repository_issues(
                repository
            )
        )

        (
            existing_epics,
            existing_children,
        ) = preflight(
            backlog,
            repository_issues,
        )

        print()
        print("Preflight passed.")
        print(
            "Starting synchronization..."
        )

        (
            parent_numbers,
            epics_created,
            epics_updated,
        ) = synchronize_epics(
            backlog=backlog,
            repository=repository,
            project_title=project_title,
            existing_epics=existing_epics,
        )

        (
            children_created,
            children_updated,
        ) = synchronize_children(
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
    print(
        f"Epics created: "
        f"{epics_created}"
    )
    print(
        f"Epics updated: "
        f"{epics_updated}"
    )
    print(
        f"Child issues created: "
        f"{children_created}"
    )
    print(
        f"Child issues updated: "
        f"{children_updated}"
    )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())