"""Reject question-bank changes that would re-point historical assessment data.

Authoring-time and CI tooling. `CurriculumValidator` checks one curriculum document; it cannot
see that between two accepted revisions an author removed a Question or changed what an
existing Question ID means. Persisted `QuestionAttempt` rows reference Questions and
AnswerOptions by ID, so those changes would make a learner's history describe something they
never answered. This tool compares the working-tree assessment curriculum against the same
file at an earlier Git revision and fails when, for any Question present in that revision:

- its `Question.id` no longer exists;
- its `selectionMode` changed (SINGLE <-> MULTIPLE);
- its set of `AnswerOption.id`s changed;
- its set of `correctAnswerIds` changed.

New Questions are always allowed. Status, level, Topic/Subtopic placement, wording,
explanation and sources are deliberately not compared: they can change under a stable ID
without changing what a historical answer meant. Answer order is not identity either, so
reordering options is allowed. Whether an editorial change is *material* stays a review
judgement — see `docs/content/content-authoring.md`.

Git history is the baseline. There is no committed baseline file and no write mode: a
baseline that lives in the same pull request as the change could be rewritten alongside the
violation it is meant to catch. The tool is read-only.

Usage:

    python3 tools/question_bank_identity.py --against <git-ref>
    python3 tools/question_bank_identity.py --against HEAD    # uncommitted local edits
"""

from __future__ import annotations

import argparse
import json
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]

# Repository-relative and POSIX-separated: it is also the path `git show <ref>:<path>` takes.
CURRICULUM_PATH = "shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json"


class IdentityError(Exception):
    """Raised when either revision cannot support a trustworthy comparison."""


def fail(message: str) -> None:
    raise IdentityError(message)


@dataclass(frozen=True)
class QuestionIdentity:
    """The Question fields a persisted attempt depends on. Everything else may change."""

    selection_mode: str
    answer_ids: frozenset[str]
    correct_answer_ids: frozenset[str]


# ---------------------------------------------------------------------------------------
# Input
# ---------------------------------------------------------------------------------------


def parse_curriculum(text: str, origin: str) -> Any:
    try:
        return json.loads(text)

    except json.JSONDecodeError as error:
        fail(f"{origin} is not valid JSON: {error}")


def read_current(repo_root: Path = REPO_ROOT) -> Any:
    path = repo_root / CURRICULUM_PATH
    try:
        text = path.read_text(encoding="utf-8")

    except FileNotFoundError:
        fail(f"The current curriculum is missing: {CURRICULUM_PATH}")

    return parse_curriculum(text, f"The current {CURRICULUM_PATH}")


def run_git(arguments: list[str], repo_root: Path) -> subprocess.CompletedProcess[bytes]:
    try:
        return subprocess.run(["git", *arguments], cwd=repo_root, capture_output=True, check=False)

    except FileNotFoundError:
        fail("git is not available on PATH.")


def read_baseline(ref: str, repo_root: Path = REPO_ROOT) -> Any:
    """Read the curriculum as it was at `ref`, failing on anything that is not a clean read.

    A missing ref, a missing file or unreadable JSON is an error rather than a skipped
    check: a gate that quietly passes when it has nothing to compare against is no gate.
    """
    if not ref or ref.startswith("-"):
        fail(f"'{ref}' is not a Git revision.")

    resolved = run_git(["rev-parse", "--verify", "--quiet", f"{ref}^{{commit}}"], repo_root)
    if resolved.returncode != 0:
        fail(f"Git revision '{ref}' does not exist in this clone. Fetch it before comparing.")
    commit = resolved.stdout.decode("utf-8").strip()

    shown = run_git(["show", f"{commit}:{CURRICULUM_PATH}"], repo_root)
    if shown.returncode != 0:
        detail = shown.stderr.decode("utf-8", errors="replace").strip()
        fail(f"Could not read {CURRICULUM_PATH} at {ref} ({commit[:12]}): {detail}")

    try:
        text = shown.stdout.decode("utf-8")

    except UnicodeDecodeError as error:
        fail(f"{CURRICULUM_PATH} at {ref} is not UTF-8: {error}")

    return parse_curriculum(text, f"{CURRICULUM_PATH} at {ref} ({commit[:12]})")


# ---------------------------------------------------------------------------------------
# Indexing
# ---------------------------------------------------------------------------------------


def string_ids(values: Any, where: str) -> list[str]:
    if not isinstance(values, list) or not all(isinstance(value, str) for value in values):
        fail(f"{where} must be a list of string IDs.")

    return values


def index_identities(curriculum: Any, origin: str) -> dict[str, QuestionIdentity]:
    """Map each Question ID to its identity, in authored order.

    The shape checks are only those the comparison needs to be trustworthy;
    `CurriculumValidator` remains the canonical validator for authored content.
    """
    if not isinstance(curriculum, dict) or not isinstance(curriculum.get("questions"), list):
        fail(f"{origin} has no 'questions' list.")

    identities: dict[str, QuestionIdentity] = {}
    for position, question in enumerate(curriculum["questions"]):
        if not isinstance(question, dict) or not isinstance(question.get("id"), str):
            fail(f"{origin}: question at position {position} has no string 'id'.")

        question_id = question["id"]
        if question_id in identities:
            fail(f"{origin}: Question id '{question_id}' appears more than once.")

        selection_mode = question.get("selectionMode")
        if not isinstance(selection_mode, str):
            fail(f"{origin}: Question '{question_id}' has no string 'selectionMode'.")

        answers = question.get("answers")
        if not isinstance(answers, list) or not all(isinstance(answer, dict) for answer in answers):
            fail(f"{origin}: Question '{question_id}' has no 'answers' list.")

        answer_ids = string_ids([answer.get("id") for answer in answers], f"{origin}: answers of '{question_id}'")
        correct_ids = string_ids(question.get("correctAnswerIds"), f"{origin}: correctAnswerIds of '{question_id}'")

        identities[question_id] = QuestionIdentity(
            selection_mode=selection_mode,
            answer_ids=frozenset(answer_ids),
            correct_answer_ids=frozenset(correct_ids),
        )

    return identities


# ---------------------------------------------------------------------------------------
# Comparison
# ---------------------------------------------------------------------------------------


def id_list(ids: frozenset[str]) -> str:
    return ", ".join(sorted(ids)) or "(none)"


def set_change(before: frozenset[str], after: frozenset[str]) -> str:
    parts = []
    if before - after:
        parts.append(f"removed {id_list(before - after)}")
    if after - before:
        parts.append(f"added {id_list(after - before)}")

    return "; ".join(parts)


def identity_violations(baseline: Any, current: Any) -> list[str]:
    """Every identity rule the current curriculum breaks, in the baseline's authored order."""
    before = index_identities(baseline, "Baseline curriculum")
    after = index_identities(current, "Current curriculum")

    violations: list[str] = []
    for question_id, old in before.items():
        new = after.get(question_id)
        if new is None:
            violations.append(f"{question_id}: Question removed. Keep it and mark it DEPRECATED instead.")
            continue

        if old.selection_mode != new.selection_mode:
            violations.append(
                f"{question_id}: selectionMode changed {old.selection_mode} -> {new.selection_mode}."
            )
        if old.answer_ids != new.answer_ids:
            violations.append(
                f"{question_id}: AnswerOption IDs changed ({set_change(old.answer_ids, new.answer_ids)})."
            )
        if old.correct_answer_ids != new.correct_answer_ids:
            violations.append(
                f"{question_id}: correctAnswerIds changed "
                f"({set_change(old.correct_answer_ids, new.correct_answer_ids)})."
            )

    return violations


# ---------------------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------------------


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument(
        "--against",
        required=True,
        metavar="GIT_REF",
        help="the accepted revision whose curriculum the working tree must stay compatible with",
    )
    arguments = parser.parse_args(argv)

    try:
        violations = identity_violations(read_baseline(arguments.against), read_current())

    except IdentityError as error:
        print("QUESTION BANK IDENTITY CHECK FAILED")
        print(f"Error: {error}")

        return 1

    if violations:
        print("QUESTION BANK IDENTITY CHECK FAILED")
        print(f"{len(violations)} change(s) against {arguments.against} would re-point historical attempts:")
        for violation in violations:
            print(f"  - {violation}")
        print("Add a replacement Question with a new ID and mark the old one DEPRECATED.")
        print("See docs/content/content-authoring.md, 'Stable Question Identity'.")

        return 1

    print(f"Question bank identity is stable against {arguments.against}.")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
