"""Generate the learning-to-question coverage snapshot.

Authoring-time tooling. It reads the two authored curricula and reports which active
interview questions are *structurally* associated with each active lesson through the
lesson's primary subtopic mappings. It never runs at application runtime, never touches
Room or a repository, and never writes to either source curriculum.

The relationship the report derives is:

    LearningLesson.primarySubtopicIds -> Curriculum subtopics -> ACTIVE questions

Lessons therefore keep no question-id lists of their own: mappings stay on stable subtopic
concepts, and the question association is recomputed from the current bank every time.

Structural association is not semantic coverage. That a lesson and a question name the same
subtopic says nothing about whether the lesson teaches enough reasoning to answer it; that
judgement stays with the author and reviewer, which is why nothing here inspects prose.

Usage:

    python3 tools/learning_question_coverage.py --write
    python3 tools/learning_question_coverage.py --check
"""

from __future__ import annotations

import argparse
import hashlib
import json
from dataclasses import dataclass
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]

CURRICULUM_DIR = REPO_ROOT / "shared/src/commonMain/composeResources/files/curriculum"
LEARNING_CURRICULUM_PATH = CURRICULUM_DIR / "learning_curriculum.json"
QUESTION_CURRICULUM_PATH = CURRICULUM_DIR / "initial_curriculum.json"
SNAPSHOT_PATH = REPO_ROOT / "docs/content/learning-question-coverage.md"

ACTIVE = "ACTIVE"
KNOWN_STATUSES = (ACTIVE, "DEPRECATED")
# Authored order, which is also the order every level table in the report uses.
QUESTION_LEVELS = ("FOUNDATION", "APPLIED", "ADVANCED")


class CoverageError(Exception):
    """Raised when the authored input cannot support a trustworthy report."""


def fail(message: str) -> None:
    raise CoverageError(message)


@dataclass(frozen=True)
class QuestionRef:
    """The only question fields the report needs. Answer text is deliberately absent."""

    id: str
    subtopic_id: str
    level: str


@dataclass(frozen=True)
class Subtopic:
    id: str
    name: str
    topic_id: str


@dataclass(frozen=True)
class CurriculumIndex:
    topic_names: dict[str, str]
    subtopics: dict[str, Subtopic]
    active_questions_by_subtopic: dict[str, tuple[QuestionRef, ...]]
    active_question_count: int
    deprecated_question_count: int


# ---------------------------------------------------------------------------------------
# Input
# ---------------------------------------------------------------------------------------


def load_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))

    except FileNotFoundError:
        fail(f"Authored input is missing: {path}")

    except json.JSONDecodeError as error:
        fail(f"Authored input {path} is not valid JSON: {error}")


def index_curriculum(curriculum: dict[str, Any]) -> CurriculumIndex:
    """Index the assessment curriculum, failing on anything that would mislead a reader.

    These checks are narrow on purpose: they cover what this report displays and counts.
    `LearningCurriculumValidator` (E20-03) remains the canonical validator for authored
    learning content, and duplicating it here would give two answers to one question.
    """
    topic_names = {topic["id"]: topic["name"] for topic in curriculum["topics"]}

    subtopics: dict[str, Subtopic] = {}
    for subtopic in curriculum["subtopics"]:
        subtopics[subtopic["id"]] = Subtopic(
            id=subtopic["id"],
            name=subtopic["name"],
            topic_id=subtopic["topicId"],
        )

    active_by_subtopic: dict[str, list[QuestionRef]] = {}
    seen_ids: set[str] = set()
    active_count = 0
    deprecated_count = 0

    for question in curriculum["questions"]:
        question_id = question["id"]
        # Unit-level aggregation deduplicates by question id, so a repeated id would
        # silently undercount rather than fail visibly.
        if question_id in seen_ids:
            fail(f"Question id '{question_id}' appears more than once in the question bank.")
        seen_ids.add(question_id)

        status = question["status"]
        if status not in KNOWN_STATUSES:
            fail(f"Question '{question_id}' has unrecognised status '{status}'.")

        level = question["level"]
        # Level is read, never inferred: an unknown level is a bank change this report has
        # not been taught to classify, and bucketing it as "other" would hide that.
        if level not in QUESTION_LEVELS:
            fail(f"Question '{question_id}' has unrecognised level '{level}'.")

        if status != ACTIVE:
            deprecated_count += 1
            continue

        active_count += 1
        active_by_subtopic.setdefault(question["subtopicId"], []).append(
            QuestionRef(id=question_id, subtopic_id=question["subtopicId"], level=level),
        )

    return CurriculumIndex(
        topic_names=topic_names,
        subtopics=subtopics,
        # Sorted by question id, which is the report's documented question ordering.
        active_questions_by_subtopic={
            subtopic_id: tuple(sorted(questions, key=lambda question: question.id))
            for subtopic_id, questions in active_by_subtopic.items()
        },
        active_question_count=active_count,
        deprecated_question_count=deprecated_count,
    )


def active_units(learning_curriculum: dict[str, Any]) -> list[dict[str, Any]]:
    return [unit for unit in learning_curriculum["units"] if unit["status"] == ACTIVE]


def active_lessons(unit: dict[str, Any]) -> list[dict[str, Any]]:
    return [lesson for lesson in unit["lessons"] if lesson["status"] == ACTIVE]


def check_learning_references(
    learning_curriculum: dict[str, Any],
    index: CurriculumIndex,
) -> None:
    """Fail rather than report coverage for a concept the taxonomy does not contain."""
    for unit in active_units(learning_curriculum):
        if unit["topicId"] not in index.topic_names:
            fail(f"Learning unit '{unit['id']}' references unknown home topic '{unit['topicId']}'.")

        for lesson in active_lessons(unit):
            for role in ("primarySubtopicIds", "supportingSubtopicIds"):
                for subtopic_id in lesson[role]:
                    if subtopic_id not in index.subtopics:
                        fail(
                            f"Lesson '{lesson['id']}' references unknown subtopic "
                            f"'{subtopic_id}' in {role}.",
                        )


# ---------------------------------------------------------------------------------------
# Fingerprints
# ---------------------------------------------------------------------------------------


def canonical_json(value: Any) -> str:
    """Formatting-independent JSON: sorted keys, no incidental whitespace, authored lists.

    Hashing raw file bytes would let a reformat invalidate the snapshot without any
    authored content having changed.
    """
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def sha256_of(value: Any) -> str:
    return hashlib.sha256(canonical_json(value).encode("utf-8")).hexdigest()


def learning_fingerprint(learning_curriculum: dict[str, Any]) -> str:
    """Hash the whole learning document, not only its mappings.

    Editing lesson prose can change whether the material actually teaches an associated
    question, so a content edit must invalidate this snapshot too.
    """
    return sha256_of(learning_curriculum)


def question_fingerprint(curriculum: dict[str, Any]) -> str:
    """Hash the taxonomy plus every ACTIVE question record.

    Deprecated questions are excluded because they never reach the report. Whole active
    records are included so that editing a question the report lists invalidates the
    snapshot even when every count stays the same.
    """
    return sha256_of(
        {
            "topics": curriculum["topics"],
            "subtopics": curriculum["subtopics"],
            "activeQuestions": [
                question for question in curriculum["questions"] if question["status"] == ACTIVE
            ],
        },
    )


# ---------------------------------------------------------------------------------------
# Derivation
# ---------------------------------------------------------------------------------------


def questions_for(index: CurriculumIndex, subtopic_id: str) -> tuple[QuestionRef, ...]:
    return index.active_questions_by_subtopic.get(subtopic_id, ())


def unique_questions(
    index: CurriculumIndex,
    subtopic_ids: list[str],
) -> tuple[QuestionRef, ...]:
    """Questions reached through the given subtopics, each counted once.

    Two lessons in a unit may legitimately share a primary subtopic, so summing lesson
    counts would inflate the unit. Identity is the stable question id.
    """
    collected: dict[str, QuestionRef] = {}
    for subtopic_id in subtopic_ids:
        for question in questions_for(index, subtopic_id):
            collected[question.id] = question

    return tuple(sorted(collected.values(), key=lambda question: question.id))


def level_counts(questions: tuple[QuestionRef, ...]) -> dict[str, int]:
    return {
        level: sum(1 for question in questions if question.level == level)
        for level in QUESTION_LEVELS
    }


def primary_subtopics_without_questions(
    learning_curriculum: dict[str, Any],
    index: CurriculumIndex,
) -> list[str]:
    """Primary concepts of active lessons that no active question currently assesses.

    Authored order of first appearance, so the list is stable and reads in curriculum
    order rather than alphabetically.
    """
    gaps: list[str] = []
    for unit in active_units(learning_curriculum):
        for lesson in active_lessons(unit):
            for subtopic_id in lesson["primarySubtopicIds"]:
                if not questions_for(index, subtopic_id) and subtopic_id not in gaps:
                    gaps.append(subtopic_id)

    return gaps


def ordered_distinct(values: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for value in values:
        if value not in seen:
            seen.add(value)
            result.append(value)

    return result


# ---------------------------------------------------------------------------------------
# Rendering
# ---------------------------------------------------------------------------------------


def cell(text: str) -> str:
    """Keep an authored name containing a pipe from breaking the table it sits in."""
    return text.replace("|", "\\|")


def subtopic_label(index: CurriculumIndex, subtopic_id: str) -> str:
    return f"`{subtopic_id}` — {cell(index.subtopics[subtopic_id].name)}"


def question_id_list(questions: tuple[QuestionRef, ...]) -> str:
    if not questions:
        return "—"

    return ", ".join(f"`{question.id}`" for question in questions)


def render_header() -> list[str]:
    return [
        "# Learning-to-Question Coverage Snapshot",
        "",
        "Generated by `tools/learning_question_coverage.py`. Do not edit this file by hand:",
        "it is regenerated in full, and `--check` compares it byte for byte against the",
        "current authored content.",
        "",
        "## Purpose",
        "",
        "This report connects the authored learning curriculum to the interview question bank",
        "through the stable subtopic concepts each lesson already declares. It answers four",
        "factual questions: which active questions are structurally associated with a lesson,",
        "how those questions divide across Foundation, Applied and Advanced, which primary",
        "concepts no active question currently assesses, and which subtopics a lesson carries",
        "only as supporting context.",
        "",
        "It exists so that a lesson author can see the assessment material a lesson is expected",
        "to make answerable without reading the whole bank. It is authoring-time tooling: no",
        "part of it runs in the application, and nothing here is persisted or exposed through a",
        "repository.",
        "",
        "## Interpretation",
        "",
        "Structural association is not semantic coverage:",
        "",
        "> A lesson maps to a subtopic, and a question maps to the same subtopic. That does",
        "> **not** establish that the lesson teaches enough reasoning to answer the question.",
        "",
        "This report answers *are these pieces structurally related?* It cannot answer *is this",
        "lesson pedagogically sufficient for this question?* Nothing here inspects lesson prose,",
        "and nothing here compares wording against a question — that would replace editorial",
        "judgement with string matching. The semantic review stays with the author and reviewer,",
        "following Rule 10 of `learning-content-authoring.md`:",
        "",
        "- **Foundation** questions — check that the lesson's Core material teaches the",
        "  understanding the question needs.",
        "- **Applied** questions — check that the Practical material establishes the required",
        "  reasoning.",
        "- **Advanced** questions — check that the necessary mechanism or trade-off material",
        "  exists, in this lesson or in a later unit that deliberately owns it. An Advanced",
        "  question and no Senior section is a prompt to look, not a defect.",
        "",
        "The counts below are not a score. There is no denominator that would make a coverage",
        "percentage mean anything about lesson quality or interview readiness, so none is",
        "reported.",
        "",
    ]


def render_inputs(
    learning_curriculum: dict[str, Any],
    curriculum: dict[str, Any],
) -> list[str]:
    return [
        "## Inputs and fingerprints",
        "",
        "Every number in this report is derived from exactly these two authored documents.",
        "The fingerprint is the SHA-256 of a canonical rendering of the relevant JSON — sorted",
        "keys, fixed separators, authored list order — so reformatting a file does not",
        "invalidate the snapshot, but changing what it says does.",
        "",
        "| Input | Path | Fingerprint (SHA-256 of canonical JSON) |",
        "|---|---|---|",
        "| Learning curriculum (whole document) | "
        "`shared/src/commonMain/composeResources/files/curriculum/learning_curriculum.json` | "
        f"`{learning_fingerprint(learning_curriculum)}` |",
        "| Question curriculum (topics, subtopics, ACTIVE questions) | "
        "`shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json` | "
        f"`{question_fingerprint(curriculum)}` |",
        "",
        "The learning fingerprint covers the whole document rather than the mappings alone,",
        "because editing lesson content can change whether the material still teaches an",
        "associated question. The question fingerprint covers whole active question records,",
        "so editing a listed question invalidates the snapshot even when the counts are",
        "unchanged. Deprecated questions are outside both the report and the fingerprint.",
        "",
    ]


def render_headline(
    learning_curriculum: dict[str, Any],
    index: CurriculumIndex,
) -> list[str]:
    units = active_units(learning_curriculum)
    lessons = [lesson for unit in units for lesson in active_lessons(unit)]

    primary_ids = ordered_distinct(
        [subtopic_id for lesson in lessons for subtopic_id in lesson["primarySubtopicIds"]],
    )
    supporting_ids = ordered_distinct(
        [subtopic_id for lesson in lessons for subtopic_id in lesson["supportingSubtopicIds"]],
    )
    reached = unique_questions(index, primary_ids)
    gaps = primary_subtopics_without_questions(learning_curriculum, index)

    return [
        "## Headline structural coverage",
        "",
        "Current authored learning content against the current active question bank.",
        "Deprecated units, lessons and questions are excluded throughout.",
        "",
        "| Measure | Count |",
        "|---|---:|",
        f"| Active learning units | {len(units)} |",
        f"| Active lessons in those units | {len(lessons)} |",
        f"| Distinct primary subtopics | {len(primary_ids)} |",
        f"| Distinct supporting subtopics | {len(supporting_ids)} |",
        f"| Unique active questions reached through primary mappings | {len(reached)} |",
        f"| Primary subtopics with at least one active question | {len(primary_ids) - len(gaps)} |",
        f"| Primary subtopics with no active question | {len(gaps)} |",
        f"| Active questions in the bank | {index.active_question_count} |",
        f"| Deprecated questions excluded from this report | {index.deprecated_question_count} |",
        "",
        "The two bank-size rows are context, not a target. The learning curriculum is",
        "authored unit by unit, so most of the bank is not yet reachable from any lesson.",
        "That is the expected state while the Compose blueprint is only partly authored,",
        "not a coverage failure.",
        "",
    ]


def render_unit_summary(
    learning_curriculum: dict[str, Any],
    index: CurriculumIndex,
) -> list[str]:
    lines = [
        "## Unit summary",
        "",
        "Question counts are unique per unit: two lessons may share a primary subtopic, so",
        "questions are deduplicated by question id before the levels are counted. Summing the",
        "lesson tables further down will therefore overcount a unit on purpose.",
        "",
        "| Unit | Lessons | Primary subtopics | Foundation | Applied | Advanced | Unique active questions |",
        "|---|---:|---:|---:|---:|---:|---:|",
    ]

    for unit in active_units(learning_curriculum):
        lessons = active_lessons(unit)
        primary_ids = ordered_distinct(
            [subtopic_id for lesson in lessons for subtopic_id in lesson["primarySubtopicIds"]],
        )
        questions = unique_questions(index, primary_ids)
        counts = level_counts(questions)
        lines.append(
            f"| {cell(unit['title'])} (`{unit['id']}`) | {len(lessons)} | {len(primary_ids)} | "
            f"{counts['FOUNDATION']} | {counts['APPLIED']} | {counts['ADVANCED']} | "
            f"{len(questions)} |",
        )

    lines.append("")

    return lines


def render_lesson(lesson: dict[str, Any], index: CurriculumIndex) -> list[str]:
    primary_ids = lesson["primarySubtopicIds"]
    supporting_ids = lesson["supportingSubtopicIds"]

    lines = [
        f"#### {cell(lesson['title'])} (`{lesson['id']}`)",
        "",
        "Primary concepts:",
        "",
        "| Primary subtopic | Foundation | Applied | Advanced | Total | Active question IDs |",
        "|---|---:|---:|---:|---:|---|",
    ]

    for subtopic_id in primary_ids:
        questions = questions_for(index, subtopic_id)
        counts = level_counts(questions)
        lines.append(
            f"| {subtopic_label(index, subtopic_id)} | {counts['FOUNDATION']} | "
            f"{counts['APPLIED']} | {counts['ADVANCED']} | {len(questions)} | "
            f"{question_id_list(questions)} |",
        )

    if len(primary_ids) > 1:
        questions = unique_questions(index, primary_ids)
        counts = level_counts(questions)
        lines.append(
            f"| **Lesson total (unique)** | {counts['FOUNDATION']} | {counts['APPLIED']} | "
            f"{counts['ADVANCED']} | {len(questions)} | {question_id_list(questions)} |",
        )

    lines.extend(["", "Supporting context — not primary coverage:", ""])

    if supporting_ids:
        lines.extend(
            [
                "| Supporting subtopic | Active questions | Owning topic |",
                "|---|---:|---|",
            ],
        )
        for subtopic_id in supporting_ids:
            questions = questions_for(index, subtopic_id)
            owning_topic = index.subtopics[subtopic_id].topic_id
            lines.append(
                f"| {subtopic_label(index, subtopic_id)} | {len(questions)} | "
                f"`{owning_topic}` — {cell(index.topic_names[owning_topic])} |",
            )
    else:
        lines.append("This lesson declares no supporting subtopics.")

    lines.append("")

    return lines


def render_unit_detail(
    learning_curriculum: dict[str, Any],
    index: CurriculumIndex,
) -> list[str]:
    lines = [
        "## Unit and lesson detail",
        "",
        "Units and lessons appear in authored order; subtopics appear in the order the lesson",
        "declares them; question ids are sorted by id.",
        "",
        "Each lesson lists two tables, and the difference between them is the point. **Primary",
        "concepts** are what the lesson is responsible for teaching, and their active questions",
        "are the ones a reader who understood the lesson should be able to reason through.",
        "**Supporting context** is what the lesson explains only far enough to stay",
        "understandable; those questions belong to whichever lesson owns the concept as",
        "primary, and they are excluded from every primary count in this report. A subtopic may",
        "be primary in one lesson and supporting in another — the role is local to the lesson",
        "that declares it.",
        "",
    ]

    for unit in active_units(learning_curriculum):
        lessons = active_lessons(unit)
        primary_ids = ordered_distinct(
            [subtopic_id for lesson in lessons for subtopic_id in lesson["primarySubtopicIds"]],
        )
        questions = unique_questions(index, primary_ids)
        topic_id = unit["topicId"]

        lines.extend(
            [
                f"### {cell(unit['title'])} (`{unit['id']}`)",
                "",
                f"Home topic: `{topic_id}` — {cell(index.topic_names[topic_id])}.",
                "",
                "Unique active questions reached through this unit's primary concepts, counted",
                "once each however many lessons share the concept.",
                "",
                "| Level | Count | Question IDs |",
                "|---|---:|---|",
            ],
        )

        for level in QUESTION_LEVELS:
            at_level = tuple(question for question in questions if question.level == level)
            lines.append(f"| {level} | {len(at_level)} | {question_id_list(at_level)} |")

        lines.append("")

        for lesson in lessons:
            lines.extend(render_lesson(lesson, index))

    return lines


def render_gaps(
    learning_curriculum: dict[str, Any],
    index: CurriculumIndex,
) -> list[str]:
    gaps = primary_subtopics_without_questions(learning_curriculum, index)

    lines = [
        "## Primary assessment gaps",
        "",
        "Primary concepts of active lessons that currently have no active question.",
        "",
        "A gap here is a candidate for question authoring, not automatically a defect. The",
        "subtopic taxonomy is deliberately finer-grained than the bank — see",
        "`question-bank-coverage.md`, whose empty-subtopic triage already separates real gaps",
        "from subtopics that are empty on purpose. This report surfaces the gap; whether it",
        "matters is editorial judgement.",
        "",
    ]

    if not gaps:
        lines.extend(["Every primary concept of every active lesson has at least one active question.", ""])

        return lines

    lines.extend(["| Primary subtopic | Owning topic |", "|---|---|"])
    for subtopic_id in gaps:
        owning_topic = index.subtopics[subtopic_id].topic_id
        lines.append(
            f"| {subtopic_label(index, subtopic_id)} | `{owning_topic}` — "
            f"{cell(index.topic_names[owning_topic])} |",
        )
    lines.append("")

    return lines


def render_footer() -> list[str]:
    return [
        "## Semantic review reminder",
        "",
        "Everything above is structural evidence. It shows that a lesson and a question are",
        "attached to the same concept, and nothing more. Before treating a lesson as covering",
        "its associated questions, read them: a reader who understood the lesson should be able",
        "to reason through the question without having seen it. A lesson that leaves its",
        "associated questions unanswerable has under-taught something, and a lesson written so",
        "that a particular question becomes answerable has over-fitted to the bank. Both are",
        "authoring decisions this report cannot make.",
        "",
        "## Regeneration",
        "",
        "Run from the repository root. Regenerate in the same change that alters learning",
        "content, learning mappings, or any active question.",
        "",
        "```sh",
        "python3 tools/learning_question_coverage.py --write   # rewrite this snapshot",
        "python3 tools/learning_question_coverage.py --check   # fail if it is stale",
        "python3 -m unittest discover -s tools -p 'test_*.py'  # generator tests",
        "```",
        "",
        "`--check` never writes. It regenerates the report in memory and compares it with this",
        "file, exiting non-zero when they differ.",
    ]


def render_report(
    learning_curriculum: dict[str, Any],
    curriculum: dict[str, Any],
) -> str:
    index = index_curriculum(curriculum)
    check_learning_references(learning_curriculum, index)

    lines = [
        *render_header(),
        *render_inputs(learning_curriculum, curriculum),
        *render_headline(learning_curriculum, index),
        *render_unit_summary(learning_curriculum, index),
        *render_unit_detail(learning_curriculum, index),
        *render_gaps(learning_curriculum, index),
        *render_footer(),
    ]

    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------------------


def build_report_from_repository() -> str:
    return render_report(
        load_json(LEARNING_CURRICULUM_PATH),
        load_json(QUESTION_CURRICULUM_PATH),
    )


def relative(path: Path) -> str:
    return str(path.relative_to(REPO_ROOT))


def write_snapshot(report: str) -> int:
    SNAPSHOT_PATH.write_text(report, encoding="utf-8")
    print(f"Wrote {relative(SNAPSHOT_PATH)} ({len(report.splitlines())} lines).")

    return 0


def check_snapshot(report: str) -> int:
    if not SNAPSHOT_PATH.exists():
        print(f"Coverage snapshot {relative(SNAPSHOT_PATH)} is missing.")
        print("Run: python3 tools/learning_question_coverage.py --write")

        return 1

    if SNAPSHOT_PATH.read_text(encoding="utf-8") != report:
        print(f"Coverage snapshot {relative(SNAPSHOT_PATH)} is stale.")
        print("The learning curriculum or the active question bank has changed since it was generated.")
        print("Run: python3 tools/learning_question_coverage.py --write")

        return 1

    print(f"Coverage snapshot {relative(SNAPSHOT_PATH)} is current.")

    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--write", action="store_true", help="regenerate and write the snapshot")
    mode.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed snapshot no longer matches the authored content",
    )
    arguments = parser.parse_args()

    try:
        report = build_report_from_repository()

    except CoverageError as error:
        print("LEARNING COVERAGE AUDIT FAILED")
        print(f"Error: {error}")
        print("No file was written.")

        return 1

    return write_snapshot(report) if arguments.write else check_snapshot(report)


if __name__ == "__main__":
    raise SystemExit(main())
