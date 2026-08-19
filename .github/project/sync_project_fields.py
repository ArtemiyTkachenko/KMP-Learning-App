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


def run_graphql(
    query: str,
    variables: dict[str, str],
) -> dict[str, Any]:
    args = [
        "api",
        "graphql",
        "-f",
        f"query={query}",
    ]

    for name, value in variables.items():
        args.extend(
            [
                "-f",
                f"{name}={value}",
            ]
        )

    output = run_gh(*args)

    try:
        data = json.loads(output)
    except json.JSONDecodeError as error:
        fail(
            "GitHub returned invalid JSON from GraphQL:\n"
            f"{error}"
        )

    errors = data.get("errors")

    if errors:
        fail(
            "GitHub GraphQL request failed:\n"
            + json.dumps(errors, indent=2)
        )

    return data


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
        "id,number,title,body,url",
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
            "Unexpected GitHub response while "
            "loading repository issues."
        )

    return data


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
        github_issue
        for github_issue in github_issues
        if (
            marker in (github_issue.get("body") or "")
            or (github_issue.get("title") or "").startswith(
                title_prefix
            )
        )
    ]

    if not matches:
        fail(
            f"{key}: corresponding GitHub issue "
            "was not found."
        )

    if len(matches) > 1:
        numbers = ", ".join(
            f"#{github_issue['number']}"
            for github_issue in matches
        )

        fail(
            f"{key}: multiple GitHub issues "
            f"match this backlog key: {numbers}"
        )

    github_issue = matches[0]

    if not github_issue.get("id"):
        fail(
            f"{key}: GitHub issue does not have "
            "a GraphQL node ID."
        )

    return github_issue


def resolve_project(
    project_title: str,
    repository: str,
) -> dict[str, Any]:
    owner = repository.split("/", 1)[0]

    query = """
    query($owner: String!, $title: String!) {
      user(login: $owner) {
        projectsV2(first: 100, query: $title) {
          nodes {
            id
            number
            title
          }
        }
      }
    }
    """

    response = run_graphql(
        query,
        {
            "owner": owner,
            "title": project_title,
        },
    )

    user = (
        response.get("data", {})
        .get("user")
    )

    if not isinstance(user, dict):
        fail(
            f"Unable to resolve GitHub user "
            f"'{owner}'."
        )

    projects = (
        user.get("projectsV2", {})
        .get("nodes", [])
    )

    matches = [
        project
        for project in projects
        if project.get("title") == project_title
    ]

    if not matches:
        fail(
            f"Project '{project_title}' was not found "
            f"under GitHub user '{owner}'."
        )

    if len(matches) > 1:
        fail(
            f"Multiple Projects are titled "
            f"'{project_title}'."
        )

    project = matches[0]

    if not project.get("id"):
        fail(
            f"Project '{project_title}' does not "
            "have a GraphQL node ID."
        )

    return project


def load_project_fields(
    project_id: str,
) -> dict[str, dict[str, Any]]:
    query = """
    query($project: ID!) {
      node(id: $project) {
        ... on ProjectV2 {
          fields(first: 100) {
            nodes {
              ... on ProjectV2SingleSelectField {
                id
                name
                options {
                  id
                  name
                }
              }
            }
          }
        }
      }
    }
    """

    response = run_graphql(
        query,
        {
            "project": project_id,
        },
    )

    project = (
        response.get("data", {})
        .get("node")
    )

    if not isinstance(project, dict):
        fail(
            "Unable to load Project field "
            "configuration."
        )

    field_nodes = (
        project.get("fields", {})
        .get("nodes", [])
    )

    fields: dict[str, dict[str, Any]] = {}

    for field in field_nodes:
        field_id = field.get("id")
        field_name = field.get("name")

        if not field_id or not field_name:
            continue

        options = {
            option["name"]: option["id"]
            for option in field.get("options", [])
            if option.get("name") and option.get("id")
        }

        fields[field_name] = {
            "id": field_id,
            "options": options,
        }

    return fields


def validate_project_fields(
    fields: dict[str, dict[str, Any]],
) -> None:
    for field_name, required_options in (
        REQUIRED_PROJECT_FIELDS.items()
    ):
        field = fields.get(field_name)

        if field is None:
            fail(
                f"Project field '{field_name}' "
                "was not found as a single-select field."
            )

        available_options = set(
            field["options"].keys()
        )

        missing_options = (
            required_options - available_options
        )

        if missing_options:
            fail(
                f"Project field '{field_name}' "
                "is missing option(s): "
                f"{', '.join(sorted(missing_options))}"
            )


def ensure_project_item(
    project_id: str,
    content_id: str,
) -> str:
    query = """
    mutation($project: ID!, $content: ID!) {
      addProjectV2ItemById(
        input: {
          projectId: $project
          contentId: $content
        }
      ) {
        item {
          id
        }
      }
    }
    """

    response = run_graphql(
        query,
        {
            "project": project_id,
            "content": content_id,
        },
    )

    item = (
        response.get("data", {})
        .get("addProjectV2ItemById", {})
        .get("item")
    )

    if not isinstance(item, dict):
        fail(
            "Unable to resolve Project item."
        )

    item_id = item.get("id")

    if not item_id:
        fail(
            "GitHub did not return a Project "
            "item node ID."
        )

    return item_id


def set_single_select_field(
    *,
    project_id: str,
    item_id: str,
    field_id: str,
    option_id: str,
) -> None:
    query = """
    mutation(
      $project: ID!
      $item: ID!
      $field: ID!
      $option: String!
    ) {
      updateProjectV2ItemFieldValue(
        input: {
          projectId: $project
          itemId: $item
          fieldId: $field
          value: {
            singleSelectOptionId: $option
          }
        }
      ) {
        projectV2Item {
          id
        }
      }
    }
    """

    run_graphql(
        query,
        {
            "project": project_id,
            "item": item_id,
            "field": field_id,
            "option": option_id,
        },
    )


def set_named_field(
    *,
    project_id: str,
    item_id: str,
    fields: dict[str, dict[str, Any]],
    field_name: str,
    value: str,
) -> None:
    field = fields.get(field_name)

    if field is None:
        fail(
            f"Project field '{field_name}' "
            "was not found."
        )

    option_id = (
        field.get("options", {})
        .get(value)
    )

    if not option_id:
        fail(
            f"Project field '{field_name}' "
            f"does not contain option '{value}'."
        )

    set_single_select_field(
        project_id=project_id,
        item_id=item_id,
        field_id=field["id"],
        option_id=option_id,
    )


def mark_initialized(
    repository: str,
    github_issue: dict[str, Any],
) -> None:
    body = (
        github_issue.get("body") or ""
    ).rstrip()

    if FIELDS_INITIALIZED_MARKER in body:
        return

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
        body_path = Path(file.name)

    try:
        run_gh(
            "issue",
            "edit",
            str(github_issue["number"]),
            "--repo",
            repository,
            "--body-file",
            str(body_path),
        )
    finally:
        body_path.unlink(
            missing_ok=True
        )


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
        print(
            "PROJECT FIELD SYNC FAILED"
        )
        print(
            "GITHUB_REPOSITORY is not configured."
        )
        return 1

    if not project_title:
        print(
            "PROJECT FIELD SYNC FAILED"
        )
        print(
            "PROJECT_TITLE is not configured."
        )
        return 1

    try:
        backlog = load_backlog(
            backlog_path
        )

        github_issues = (
            load_repository_issues(
                repository
            )
        )

        # Resolve every backlog child before
        # starting Project field mutations.
        resolved: list[
            tuple[
                dict[str, Any],
                dict[str, Any],
            ]
        ] = []

        for epic in backlog["epics"]:
            for definition in epic["issues"]:
                github_issue = find_child(
                    definition,
                    github_issues,
                )

                resolved.append(
                    (
                        definition,
                        github_issue,
                    )
                )

        project = resolve_project(
            project_title,
            repository,
        )

        project_id = project["id"]
        project_number = project["number"]

        fields = load_project_fields(
            project_id
        )

        validate_project_fields(
            fields
        )

        print(
            f"Resolved Project "
            f"'{project_title}' "
            f"as #{project_number}."
        )

        print(
            f"Resolved {len(resolved)} "
            "backlog issues."
        )

        print(
            "Project-field preflight passed."
        )

        initialized_count = 0

        for (
            definition,
            github_issue,
        ) in resolved:
            key = definition["key"]

            body = (
                github_issue.get("body")
                or ""
            )

            already_initialized = (
                FIELDS_INITIALIZED_MARKER
                in body
            )

            print(
                f"{key}: "
                f"Priority={definition['priority']}, "
                f"Size={definition['size']}"
            )

            # This mutation is safe even if the
            # issue is already present in the
            # Project: GitHub returns the existing
            # Project item.
            item_id = ensure_project_item(
                project_id=project_id,
                content_id=github_issue["id"],
            )

            # Priority remains authoritative
            # in backlog.yml.
            set_named_field(
                project_id=project_id,
                item_id=item_id,
                fields=fields,
                field_name="Priority",
                value=definition["priority"],
            )

            # Size remains authoritative
            # in backlog.yml.
            set_named_field(
                project_id=project_id,
                item_id=item_id,
                fields=fields,
                field_name="Size",
                value=definition["size"],
            )

            # Status is only initialized once.
            if not already_initialized:
                initial_status = (
                    definition[
                        "initial_status"
                    ]
                )

                print(
                    "  initialize "
                    f"Status={initial_status}"
                )

                set_named_field(
                    project_id=project_id,
                    item_id=item_id,
                    fields=fields,
                    field_name="Status",
                    value=initial_status,
                )

                mark_initialized(
                    repository,
                    github_issue,
                )

                initialized_count += 1

        print()
        print(
            "PROJECT FIELD SYNC COMPLETED"
        )

        print(
            f"Issues synchronized: "
            f"{len(resolved)}"
        )

        print(
            f"Statuses initialized: "
            f"{initialized_count}"
        )

        print(
            "Iteration was not modified."
        )

        return 0

    except (
        OSError,
        yaml.YAMLError,
        json.JSONDecodeError,
        SyncError,
    ) as error:
        print()
        print(
            "PROJECT FIELD SYNC FAILED"
        )
        print(error)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
