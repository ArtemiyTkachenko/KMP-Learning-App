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

REQUIRED_PROJECT_FIELDS = {
    "Priority": {"P0", "P1", "P2", "P3"},
    "Size": {"XS", "S", "M", "L"},
    "Status": {"Backlog", "Ready", "In Progress", "Done"},
}


class SyncError(Exception):
    pass


def fail(message: str) -> None:
    raise SyncError(message)


def run_gh(*args: str) -> str:
    result = subprocess.run(
        ["gh", *args],
        text=True,
        capture_output=True,
    )

    if result.returncode != 0:
        fail(
            f"GitHub CLI command failed:\n"
            f"  gh {' '.join(args)}\n\n"
            f"{result.stderr.strip()}"
        )

    return result.stdout.strip()


def load_backlog(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as file:
        data = yaml.safe_load(file)

    if not isinstance(data, dict):
        fail("Backlog root must be an object.")

    return data


def parse_collection(
    output: str,
    key: str,
) -> list[dict[str, Any]]:
    data = json.loads(output)

    if isinstance(data, list):
        return data

    if isinstance(data, dict) and isinstance(data.get(key), list):
        return data[key]

    fail(f"Unexpected GitHub response for '{key}'.")


def load_repository_issues(
    repository: str,
) -> list[dict[str, Any]]:
    return parse_collection(
        run_gh(
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
        ),
        "issues",
    )


def backlog_marker(key: str) -> str:
    return f"<!-- backlog-key: {key} -->"


def find_child(
    definition: dict[str, Any],
    github_issues: list[dict[str, Any]],
) -> dict[str, Any]:
    key = definition["key"]
    marker = backlog_marker(key)
    title_prefix = f"{key} "

    matches = [
        issue
        for issue in github_issues
        if marker in (issue.get("body") or "")
        or (issue.get("title") or "").startswith(title_prefix)
    ]

    if not matches:
        fail(f"{key}: GitHub issue was not found.")

    if len(matches) > 1:
        numbers = ", ".join(
            f"#{issue['number']}"
            for issue in matches
        )
        fail(
            f"{key}: multiple GitHub issues match this key: {numbers}"
        )

    return matches[0]


def resolve_project_number(project_title: str) -> int:
    projects = parse_collection(
        run_gh(
            "project",
            "list",
            "--owner",
            "@me",
            "--limit",
            "100",
            "--format",
            "json",
        ),
        "projects",
    )

    matches = [
        project
        for project in projects
        if project.get("title") == project_title
    ]

    if not matches:
        fail(f"Project '{project_title}' was not found.")

    if len(matches) > 1:
        fail(f"Multiple Projects are titled '{project_title}'.")

    number = matches[0].get("number")

    if not isinstance(number, int):
        fail(
            f"Project '{project_title}' has no valid project number."
        )

    return number


def load_single_select_fields(
    project_number: int,
) -> dict[str, set[str]]:
    query = """
    query($number: Int!) {
      viewer {
        projectV2(number: $number) {
          fields(first: 100) {
            nodes {
              __typename
              ... on ProjectV2SingleSelectField {
                name
                options {
                  name
                }
              }
            }
          }
        }
      }
    }
    """

    output = run_gh(
        "api",
        "graphql",
        "-f",
        f"query={query}",
        "-F",
        f"number={project_number}",
    )

    data = json.loads(output)
    project = (
        data.get("data", {})
        .get("viewer", {})
        .get("projectV2")
    )

    if not isinstance(project, dict):
        fail(
            f"Unable to read Project #{project_number} "
            "field configuration."
        )

    result: dict[str, set[str]] = {}

    for node in project["fields"]["nodes"]:
        if (
            node.get("__typename")
            != "ProjectV2SingleSelectField"
        ):
            continue

        result[node["name"]] = {
            option["name"]
            for option in node.get("options", [])
        }

    return result


def validate_project_fields(project_number: int) -> None:
    fields = load_single_select_fields(project_number)

    for field_name, required_options in REQUIRED_PROJECT_FIELDS.items():
        options = fields.get(field_name)

        if options is None:
            fail(
                f"Project field '{field_name}' was not found "
                "as a single-select field."
            )

        missing = required_options - options

        if missing:
            fail(
                f"Project field '{field_name}' is missing option(s): "
                f"{', '.join(sorted(missing))}"
            )


def set_field(
    project_number: int,
    issue_url: str,
    field_name: str,
    value: str,
) -> None:
    run_gh(
        "project",
        "item-edit",
        str(project_number),
        "--owner",
        "@me",
        "--url",
        issue_url,
        "--field",
        field_name,
        "--value",
        value,
    )


def mark_initialized(
    repository: str,
    github_issue: dict[str, Any],
) -> None:
    body = (github_issue.get("body") or "").rstrip()

    updated_body = (
        f"{body}\n\n"
        f"{FIELDS_INITIALIZED_MARKER}\n"
    )

    with tempfile.NamedTemporaryFile(
        mode="w",
        encoding="utf-8",
        suffix=".md",
        delete=False,
    ) as file:
        file.write(updated_body)
        path = Path(file.name)

    try:
        run_gh(
            "issue",
            "edit",
            str(github_issue["number"]),
            "--repo",
            repository,
            "--body-file",
            str(path),
        )
    finally:
        path.unlink(missing_ok=True)


def main() -> int:
    backlog_path = (
        Path(sys.argv[1])
        if len(sys.argv) > 1
        else Path(".github/project/backlog.yml")
    )

    repository = os.environ.get(
        "GITHUB_REPOSITORY",
        "",
    ).strip()

    project_title = os.environ.get(
        "PROJECT_TITLE",
        "",
    ).strip()

    if not repository or not project_title:
        print("PROJECT FIELD SYNC FAILED")
        print(
            "GITHUB_REPOSITORY and PROJECT_TITLE "
            "must be configured."
        )
        return 1

    try:
        backlog = load_backlog(backlog_path)
        github_issues = load_repository_issues(repository)

        # Resolve all issues before making any changes.
        resolved: list[
            tuple[dict[str, Any], dict[str, Any]]
        ] = []

        for epic in backlog["epics"]:
            for definition in epic["issues"]:
                resolved.append(
                    (
                        definition,
                        find_child(
                            definition,
                            github_issues,
                        ),
                    )
                )

        project_number = resolve_project_number(
            project_title
        )

        validate_project_fields(project_number)

        print(
            f"Resolved Project '{project_title}' "
            f"as #{project_number}."
        )
        print(
            f"Resolved {len(resolved)} backlog issues."
        )
        print("Project-field preflight passed.")

        initialized_count = 0

        for definition, github_issue in resolved:
            key = definition["key"]
            issue_url = github_issue["url"]
            body = github_issue.get("body") or ""

            already_initialized = (
                FIELDS_INITIALIZED_MARKER in body
            )

            print(
                f"{key}: "
                f"Priority={definition['priority']}, "
                f"Size={definition['size']}"
            )

            # These remain authoritative in backlog.yml.
            set_field(
                project_number,
                issue_url,
                "Priority",
                definition["priority"],
            )

            set_field(
                project_number,
                issue_url,
                "Size",
                definition["size"],
            )

            # Status is initialized exactly once.
            if not already_initialized:
                print(
                    "  initialize "
                    f"Status={definition['initial_status']}"
                )

                set_field(
                    project_number,
                    issue_url,
                    "Status",
                    definition["initial_status"],
                )

                mark_initialized(
                    repository,
                    github_issue,
                )

                initialized_count += 1

        print()
        print("PROJECT FIELD SYNC COMPLETED")
        print(
            f"Issues synchronized: {len(resolved)}"
        )
        print(
            f"Statuses initialized: {initialized_count}"
        )
        print("Iteration was not modified.")

        return 0

    except (
        OSError,
        yaml.YAMLError,
        json.JSONDecodeError,
        SyncError,
    ) as error:
        print()
        print("PROJECT FIELD SYNC FAILED")
        print(error)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
