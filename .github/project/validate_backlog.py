from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Any

import yaml


EPIC_KEY_PATTERN = re.compile(r"^E\d{2}$")
ISSUE_KEY_PATTERN = re.compile(r"^E\d{2}-\d{2}$")

ALLOWED_PRIORITIES = {"P0", "P1", "P2", "P3"}
ALLOWED_SIZES = {"XS", "S", "M", "L"}
ALLOWED_INITIAL_STATUSES = {"Backlog", "Ready", "In Progress", "Done"}

ROOT_FIELDS = {"epics"}

EPIC_FIELDS = {
    "key",
    "github_title",
    "issues",
}

ISSUE_FIELDS = {
    "key",
    "title",
    "issue",
    "approach",
    "acceptance_criteria",
    "priority",
    "size",
    "initial_status",
}


class ValidationError(Exception):
    pass


def fail(message: str) -> None:
    raise ValidationError(message)


def require_mapping(value: Any, location: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        fail(f"{location} must be a mapping/object.")
    return value


def require_list(value: Any, location: str) -> list[Any]:
    if not isinstance(value, list):
        fail(f"{location} must be a list.")
    return value


def require_non_empty_string(value: Any, location: str) -> str:
    if not isinstance(value, str):
        fail(f"{location} must be a string.")

    value = value.strip()

    if not value:
        fail(f"{location} must not be blank.")

    return value


def validate_allowed_fields(
    data: dict[str, Any],
    allowed_fields: set[str],
    location: str,
) -> None:
    unknown_fields = set(data) - allowed_fields

    if unknown_fields:
        fields = ", ".join(sorted(unknown_fields))
        fail(f"{location} contains unknown field(s): {fields}")


def validate_required_fields(
    data: dict[str, Any],
    required_fields: set[str],
    location: str,
) -> None:
    missing_fields = required_fields - set(data)

    if missing_fields:
        fields = ", ".join(sorted(missing_fields))
        fail(f"{location} is missing required field(s): {fields}")


def validate_acceptance_criteria(
    criteria: Any,
    issue_key: str,
) -> None:
    criteria = require_list(
        criteria,
        f"{issue_key}.acceptance_criteria",
    )

    if not criteria:
        fail(f"{issue_key}.acceptance_criteria must contain at least one item.")

    normalized_criteria: list[str] = []

    for index, criterion in enumerate(criteria, start=1):
        text = require_non_empty_string(
            criterion,
            f"{issue_key}.acceptance_criteria[{index}]",
        )
        normalized_criteria.append(text)

    if len(normalized_criteria) != len(set(normalized_criteria)):
        fail(f"{issue_key}.acceptance_criteria contains duplicate items.")


def validate_issue(
    issue: Any,
    parent_epic_key: str,
    seen_issue_keys: set[str],
) -> None:
    issue = require_mapping(issue, f"Issue under {parent_epic_key}")

    validate_allowed_fields(issue, ISSUE_FIELDS, f"Issue under {parent_epic_key}")
    validate_required_fields(issue, ISSUE_FIELDS, f"Issue under {parent_epic_key}")

    issue_key = require_non_empty_string(
        issue["key"],
        f"Issue under {parent_epic_key}.key",
    )

    if not ISSUE_KEY_PATTERN.fullmatch(issue_key):
        fail(
            f"{issue_key}: invalid issue key. "
            "Expected format E##-##, for example E03-04."
        )

    expected_prefix = f"{parent_epic_key}-"

    if not issue_key.startswith(expected_prefix):
        fail(
            f"{issue_key}: key does not match parent epic {parent_epic_key}."
        )

    if issue_key in seen_issue_keys:
        fail(f"Duplicate issue key: {issue_key}")

    seen_issue_keys.add(issue_key)

    title = require_non_empty_string(
        issue["title"],
        f"{issue_key}.title",
    )

    if issue_key in title:
        fail(
            f"{issue_key}.title must not contain the issue key. "
            "The synchronizer will add it automatically."
        )

    require_non_empty_string(
        issue["issue"],
        f"{issue_key}.issue",
    )

    require_non_empty_string(
        issue["approach"],
        f"{issue_key}.approach",
    )

    validate_acceptance_criteria(
        issue["acceptance_criteria"],
        issue_key,
    )

    priority = require_non_empty_string(
        issue["priority"],
        f"{issue_key}.priority",
    )

    if priority not in ALLOWED_PRIORITIES:
        fail(
            f"{issue_key}.priority has invalid value '{priority}'. "
            f"Allowed values: {', '.join(sorted(ALLOWED_PRIORITIES))}"
        )

    size = require_non_empty_string(
        issue["size"],
        f"{issue_key}.size",
    )

    if size not in ALLOWED_SIZES:
        fail(
            f"{issue_key}.size has invalid value '{size}'. "
            "Allowed values: XS, S, M, L"
        )

    initial_status = require_non_empty_string(
        issue["initial_status"],
        f"{issue_key}.initial_status",
    )

    if initial_status not in ALLOWED_INITIAL_STATUSES:
        fail(
            f"{issue_key}.initial_status has invalid value "
            f"'{initial_status}'. Allowed values: "
            "Backlog, Ready, In Progress, Done"
        )


def validate_epic(
    epic: Any,
    seen_epic_keys: set[str],
    seen_issue_keys: set[str],
) -> int:
    epic = require_mapping(epic, "Epic")

    validate_allowed_fields(epic, EPIC_FIELDS, "Epic")
    validate_required_fields(epic, EPIC_FIELDS, "Epic")

    epic_key = require_non_empty_string(
        epic["key"],
        "Epic.key",
    )

    if not EPIC_KEY_PATTERN.fullmatch(epic_key):
        fail(
            f"{epic_key}: invalid epic key. "
            "Expected format E##, for example E03."
        )

    if epic_key in seen_epic_keys:
        fail(f"Duplicate epic key: {epic_key}")

    seen_epic_keys.add(epic_key)

    github_title = require_non_empty_string(
        epic["github_title"],
        f"{epic_key}.github_title",
    )

    epic_number = epic_key[1:]
    expected_prefix = f"EPIC-{epic_number} "

    if not github_title.startswith(expected_prefix):
        fail(
            f"{epic_key}.github_title must start with "
            f"'{expected_prefix}'. Found: '{github_title}'"
        )

    issues = require_list(
        epic["issues"],
        f"{epic_key}.issues",
    )

    for issue in issues:
        validate_issue(
            issue,
            parent_epic_key=epic_key,
            seen_issue_keys=seen_issue_keys,
        )

    return len(issues)


def validate_backlog(path: Path) -> tuple[int, int]:
    if not path.exists():
        fail(f"Backlog file does not exist: {path}")

    try:
        with path.open("r", encoding="utf-8") as file:
            data = yaml.safe_load(file)
    except yaml.YAMLError as error:
        fail(f"Unable to parse YAML:\n{error}")

    root = require_mapping(data, "Root")

    validate_allowed_fields(root, ROOT_FIELDS, "Root")
    validate_required_fields(root, ROOT_FIELDS, "Root")

    epics = require_list(root["epics"], "epics")

    if not epics:
        fail("epics must contain at least one epic.")

    seen_epic_keys: set[str] = set()
    seen_issue_keys: set[str] = set()

    issue_count = 0

    for epic in epics:
        issue_count += validate_epic(
            epic,
            seen_epic_keys,
            seen_issue_keys,
        )

    return len(epics), issue_count


def main() -> int:
    backlog_path = (
        Path(sys.argv[1])
        if len(sys.argv) > 1
        else Path(".github/project/backlog.yml")
    )

    print(f"Validating {backlog_path}...")

    try:
        epic_count, issue_count = validate_backlog(backlog_path)
    except ValidationError as error:
        print()
        print("BACKLOG VALIDATION FAILED")
        print(f"Error: {error}")
        print()
        print("No GitHub changes were made.")
        return 1

    print()
    print("BACKLOG VALIDATION PASSED")
    print(f"Epics:  {epic_count}")
    print(f"Issues: {issue_count}")
    print()
    print("No GitHub changes were made.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
