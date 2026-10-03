"""Maintain the generated regions of the question-bank coverage snapshot.

Authoring-time tooling. It reads the assessment curriculum and derives the structural facts
`docs/content/question-bank-coverage.md` reports about the bank itself: headline counts,
per-Topic density, the deprecated-Question table, the empty-Subtopic count and the full
Subtopic index. It never runs at application runtime, never touches Room or a repository,
and never writes to the curriculum.

Unlike `learning_question_coverage.py`, this tool does not own the whole document. The
snapshot deliberately mixes derived structure with human editorial judgement — the
empty-Subtopic triage, the concept-coverage prose, the audit baselines and the expansion
notes — so it owns only the regions fenced by explicit markers:

    <!-- BEGIN GENERATED: <block-name> -->
    ...
    <!-- END GENERATED: <block-name> -->

`--write` replaces the body of each marked region and leaves every byte outside them
untouched. `--check` proves the marked regions match the current curriculum. It does not,
and cannot, prove that the prose around them is still true; that stays with the author and
reviewer.

Usage:

    python3 tools/question_bank_coverage.py --write
    python3 tools/question_bank_coverage.py --check
"""

from __future__ import annotations

import argparse
import json
import re
import textwrap
from collections import Counter
from dataclasses import dataclass
from fractions import Fraction
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[1]

QUESTION_CURRICULUM_PATH = (
    REPO_ROOT / "shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json"
)
SNAPSHOT_PATH = REPO_ROOT / "docs/content/question-bank-coverage.md"

ACTIVE = "ACTIVE"
DEPRECATED = "DEPRECATED"
KNOWN_STATUSES = (ACTIVE, DEPRECATED)
SELECTION_MODES = ("SINGLE", "MULTIPLE")

# The prose the generator wraps is kept to the snapshot's own line length.
WRAP_WIDTH = 80

BEGIN_MARKER = re.compile(r"^<!-- BEGIN GENERATED: ([a-z0-9-]+) -->$", re.MULTILINE)
END_MARKER = re.compile(r"^<!-- END GENERATED: ([a-z0-9-]+) -->$", re.MULTILINE)


class CoverageError(Exception):
    """Raised when the curriculum or the snapshot cannot support a trustworthy report."""


def fail(message: str) -> None:
    raise CoverageError(message)


@dataclass(frozen=True)
class Subtopic:
    id: str
    name: str
    topic_id: str
    status: str


@dataclass(frozen=True)
class Question:
    """The only Question fields the report needs. Question and answer text are absent."""

    id: str
    topic_id: str
    subtopic_id: str
    status: str
    selection_mode: str
    answer_count: int
    correct_answer_count: int
    source_urls: tuple[str, ...]


@dataclass(frozen=True)
class Bank:
    """The curriculum in authored order, with ACTIVE and DEPRECATED ids per Subtopic."""

    topics: tuple[tuple[str, str], ...]
    subtopics: tuple[Subtopic, ...]
    questions: tuple[Question, ...]
    active_ids_by_subtopic: dict[str, tuple[str, ...]]
    deprecated_ids_by_subtopic: dict[str, tuple[str, ...]]

    def subtopics_of(self, topic_id: str) -> tuple[Subtopic, ...]:
        return tuple(subtopic for subtopic in self.subtopics if subtopic.topic_id == topic_id)

    def active_count(self, subtopic_id: str) -> int:
        return len(self.active_ids_by_subtopic.get(subtopic_id, ()))


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


def unique_ids(records: list[dict[str, Any]], kind: str) -> None:
    seen: set[str] = set()
    for record in records:
        if record["id"] in seen:
            fail(f"{kind} id '{record['id']}' appears more than once in the curriculum.")
        seen.add(record["id"])


def index_bank(curriculum: dict[str, Any]) -> Bank:
    """Index the curriculum, failing on anything that would make the report misleading.

    These checks are narrow on purpose: they cover what the generated regions count and
    attribute. `CurriculumValidator` remains the canonical validator for authored content,
    and duplicating it here would give two answers to one question.
    """
    unique_ids(curriculum["topics"], "Topic")
    unique_ids(curriculum["subtopics"], "Subtopic")
    unique_ids(curriculum["questions"], "Question")

    topics = tuple((topic["id"], topic["name"]) for topic in curriculum["topics"])
    topic_ids = {topic_id for topic_id, _ in topics}

    subtopics: list[Subtopic] = []
    for record in curriculum["subtopics"]:
        if record["topicId"] not in topic_ids:
            fail(f"Subtopic '{record['id']}' references unknown Topic '{record['topicId']}'.")
        # Status decides the "(deprecated subtopic)" annotation and the empty-Subtopic split.
        if record["status"] not in KNOWN_STATUSES:
            fail(f"Subtopic '{record['id']}' has unrecognised status '{record['status']}'.")
        subtopics.append(
            Subtopic(
                id=record["id"],
                name=record["name"],
                topic_id=record["topicId"],
                status=record["status"],
            ),
        )
    subtopic_by_id = {subtopic.id: subtopic for subtopic in subtopics}

    for topic_id, _ in topics:
        if not any(subtopic.topic_id == topic_id for subtopic in subtopics):
            fail(f"Topic '{topic_id}' has no Subtopics, so its density is undefined.")

    questions: list[Question] = []
    active: dict[str, list[str]] = {}
    deprecated: dict[str, list[str]] = {}

    for record in curriculum["questions"]:
        question_id = record["id"]

        if record["topicId"] not in topic_ids:
            fail(f"Question '{question_id}' references unknown Topic '{record['topicId']}'.")

        subtopic = subtopic_by_id.get(record["subtopicId"])
        if subtopic is None:
            fail(f"Question '{question_id}' references unknown Subtopic '{record['subtopicId']}'.")

        # Coverage is attributed through the Subtopic, and the deprecated table prints the
        # Question's own Topic: the two must agree or the report contradicts itself.
        if subtopic.topic_id != record["topicId"]:
            fail(
                f"Question '{question_id}' names Topic '{record['topicId']}' but its Subtopic "
                f"'{subtopic.id}' belongs to Topic '{subtopic.topic_id}'.",
            )

        status = record["status"]
        if status not in KNOWN_STATUSES:
            fail(f"Question '{question_id}' has unrecognised status '{status}'.")

        # Read, never inferred: an unknown mode is a bank change the headline has not been
        # taught to count, and bucketing it silently would hide that.
        selection_mode = record["selectionMode"]
        if selection_mode not in SELECTION_MODES:
            fail(f"Question '{question_id}' has unrecognised selectionMode '{selection_mode}'.")

        questions.append(
            Question(
                id=question_id,
                topic_id=record["topicId"],
                subtopic_id=subtopic.id,
                status=status,
                selection_mode=selection_mode,
                answer_count=len(record["answers"]),
                correct_answer_count=len(record["correctAnswerIds"]),
                source_urls=tuple(source["url"] for source in record["sources"]),
            ),
        )
        (active if status == ACTIVE else deprecated).setdefault(subtopic.id, []).append(question_id)

    return Bank(
        topics=topics,
        subtopics=tuple(subtopics),
        questions=tuple(questions),
        # Authored Question order within each Subtopic, as the index has always shown it.
        active_ids_by_subtopic={key: tuple(ids) for key, ids in active.items()},
        deprecated_ids_by_subtopic={key: tuple(ids) for key, ids in deprecated.items()},
    )


# ---------------------------------------------------------------------------------------
# Rendering helpers
# ---------------------------------------------------------------------------------------


def cell(text: str) -> str:
    """Keep an authored name containing a pipe from breaking the table it sits in."""
    return text.replace("|", "\\|")


def plural(count: int, singular: str, plural_form: str | None = None) -> str:
    return singular if count == 1 else (plural_form or f"{singular}s")


def series(items: list[str]) -> str:
    """Join with commas and a final "and", as the snapshot's prose does."""
    if len(items) <= 2:
        return " and ".join(items)

    return ", ".join(items[:-1]) + ", and " + items[-1]


def code_list(ids: list[str] | tuple[str, ...]) -> str:
    return ", ".join(f"`{item}`" for item in ids)


def wrap(paragraph: str) -> list[str]:
    return textwrap.wrap(
        paragraph,
        width=WRAP_WIDTH,
        break_long_words=False,
        break_on_hyphens=False,
    )


def density(active: int, subtopics: int) -> Fraction:
    # Exact, so that two Topics with equal density tie instead of differing in the last bit.
    return Fraction(active, subtopics)


def empty_subtopics(bank: Bank) -> list[Subtopic]:
    """Subtopics with no ACTIVE Question. A DEPRECATED Question never covers a Subtopic."""
    return [subtopic for subtopic in bank.subtopics if bank.active_count(subtopic.id) == 0]


def empty_split(bank: Bank) -> tuple[int, list[str]]:
    """ACTIVE empty Subtopic count, and the ids of DEPRECATED empty ones in authored order."""
    empty = empty_subtopics(bank)
    deprecated = [subtopic.id for subtopic in empty if subtopic.status == DEPRECATED]

    return len(empty) - len(deprecated), deprecated


def deprecated_suffix(deprecated_ids: list[str], lead: str) -> str:
    if not deprecated_ids:
        return ""

    if len(deprecated_ids) == 1:
        return f"{lead}the DEPRECATED `{deprecated_ids[0]}`"

    return f"{lead}{len(deprecated_ids)} DEPRECATED ones ({code_list(deprecated_ids)})"


# ---------------------------------------------------------------------------------------
# Generated blocks
# ---------------------------------------------------------------------------------------


def render_headline(bank: Bank) -> list[str]:
    questions = bank.questions
    active_count = sum(1 for question in questions if question.status == ACTIVE)
    modes = Counter(question.selection_mode for question in questions)
    single_correct_multiple = sum(
        1
        for question in questions
        if question.selection_mode == "MULTIPLE" and question.correct_answer_count == 1
    )

    subtopic_count = len(bank.subtopics)
    deprecated_subtopics = sum(1 for subtopic in bank.subtopics if subtopic.status == DEPRECATED)
    empty_active, empty_deprecated = empty_split(bank)
    empty_count = empty_active + len(empty_deprecated)
    covered = subtopic_count - empty_count

    option_buckets = sorted(Counter(question.answer_count for question in questions).items())
    option_parts = [
        f"{count} {plural(count, 'question')} with {options} {plural(options, 'option')}"
        if position == 0
        else f"{count} with {options}"
        for position, (options, count) in enumerate(option_buckets)
    ]
    option_total = sum(question.answer_count for question in questions)

    urls = [url for question in questions for url in question.source_urls]

    lines = [
        "| Metric | Value |",
        "|---|---:|",
        f"| Total questions | {len(questions)} |",
        f"| ACTIVE | {active_count} |",
        f"| DEPRECATED | {len(questions) - active_count} |",
        f"| Topics | {len(bank.topics)} |",
        f"| Subtopics | {subtopic_count} ({subtopic_count - deprecated_subtopics} ACTIVE, "
        f"{deprecated_subtopics} DEPRECATED) |",
        f"| Subtopics with ≥1 active question | {covered} ({covered / subtopic_count:.0%}) |",
        f"| Subtopics with 0 active questions | {empty_count} ({empty_active} ACTIVE"
        f"{deprecated_suffix(empty_deprecated, ', plus ')}) |",
        f"| SINGLE | {modes['SINGLE']} |",
        f"| MULTIPLE | {modes['MULTIPLE']} |",
        f"| — of which exactly one correct answer | {single_correct_multiple} |",
        f"| Answer options | {option_total} ({', '.join(option_parts)}) |",
        f"| Source references | {len(urls)} across {len(set(urls))} unique URLs |",
        "",
    ]

    depths = sorted(Counter(bank.active_count(subtopic.id) for subtopic in bank.subtopics).items())
    depth_parts = [
        f"**{count}** {plural(count, 'subtopic')} {plural(count, 'has', 'have')} {depth} "
        f"{plural(depth, 'question')}"
        if position == 0
        else f"**{count}** {plural(count, 'has', 'have')} {depth}"
        for position, (depth, count) in enumerate(depths)
    ]
    lines.extend(wrap(f"Subtopic depth distribution: {series(depth_parts)}."))

    return lines


def render_topic_coverage(bank: Bank) -> list[str]:
    rows = []
    for position, (topic_id, name) in enumerate(bank.topics):
        subtopics = bank.subtopics_of(topic_id)
        active = sum(bank.active_count(subtopic.id) for subtopic in subtopics)
        covered = sum(1 for subtopic in subtopics if bank.active_count(subtopic.id))
        rows.append((density(active, len(subtopics)), position, name, topic_id, active, subtopics, covered))

    # Densest first; authored Topic order breaks an exact tie.
    rows.sort(key=lambda row: (-row[0], row[1]))

    lines = [
        "| Topic | `topicId` | Active | Subtopics | Covered | Empty | Density |",
        "|---|---|---:|---:|---:|---:|---:|",
    ]
    for topic_density, _, name, topic_id, active, subtopics, covered in rows:
        lines.append(
            f"| {cell(name)} | `{topic_id}` | {active} | {len(subtopics)} | {covered} | "
            f"{len(subtopics) - covered} | {float(topic_density):.2f} |",
        )

    total_active = sum(row[4] for row in rows)
    total_subtopics = len(bank.subtopics)
    total_covered = sum(row[6] for row in rows)
    # Bank-wide ratio, not an average of Topic densities, which would weight small Topics up.
    total_density = float(density(total_active, total_subtopics))
    lines.append(
        f"| **Total** | | **{total_active}** | **{total_subtopics}** | **{total_covered}** | "
        f"**{total_subtopics - total_covered}** | **{total_density:.2f}** |",
    )

    return lines


def render_deprecated_questions(bank: Bank) -> list[str]:
    # Grouped by authored Topic order, authored Question order within a Topic. Batches append
    # to the end of the bank, so plain authored order would scatter a Topic's retirements;
    # the grouping is the one the hand-maintained table used before this tool existed.
    topic_position = {topic_id: position for position, (topic_id, _) in enumerate(bank.topics)}
    deprecated = sorted(
        (question for question in bank.questions if question.status == DEPRECATED),
        key=lambda question: topic_position[question.topic_id],
    )

    lines = wrap(
        f"{len(deprecated)} {plural(len(deprecated), 'question is', 'questions are')} "
        "`DEPRECATED`. They are retained for stable identity and historical attempts, and "
        "are excluded from active selection.",
    )
    lines.append("")

    if not deprecated:
        return lines

    lines.extend(["| Question | Topic / Subtopic |", "|---|---|"])
    for question in deprecated:
        lines.append(f"| `{question.id}` | `{question.topic_id}` / `{question.subtopic_id}` |")

    return lines


def render_empty_summary(bank: Bank) -> list[str]:
    empty_active, empty_deprecated = empty_split(bank)
    empty_count = empty_active + len(empty_deprecated)
    head = (
        f"{empty_count} {plural(empty_count, 'subtopic has', 'subtopics have')} no active "
        "question"
    )

    if not empty_deprecated:
        return wrap(f"{head}, all of them ACTIVE.")

    return wrap(
        f"{head}: {empty_active} ACTIVE {plural(empty_active, 'subtopic')}"
        f"{deprecated_suffix(empty_deprecated, ' and ')}.",
    )


def render_subtopic_index(bank: Bank) -> list[str]:
    lines: list[str] = []

    for topic_id, name in bank.topics:
        subtopics = bank.subtopics_of(topic_id)
        active = sum(bank.active_count(subtopic.id) for subtopic in subtopics)
        empty = sum(1 for subtopic in subtopics if bank.active_count(subtopic.id) == 0)

        if lines:
            lines.append("")
        lines.extend(
            [
                f"### {name}",
                "",
                f"`{topic_id}` — **{active} active** across {len(subtopics)} subtopics "
                f"({len(subtopics) - empty} covered, {empty} empty)",
                "",
                "| Subtopic | n | Question IDs |",
                "|---|---:|---|",
            ],
        )

        for subtopic in subtopics:
            active_ids = bank.active_ids_by_subtopic.get(subtopic.id, ())
            deprecated_ids = bank.deprecated_ids_by_subtopic.get(subtopic.id, ())

            ids = code_list(active_ids) if active_ids else "—"
            if deprecated_ids:
                ids += (" " if active_ids else "") + f"_(deprecated: {code_list(deprecated_ids)})_"

            label = cell(subtopic.name)
            if subtopic.status == DEPRECATED:
                label += " _(deprecated subtopic)_"

            lines.append(f"| `{subtopic.id}` — {label} | {len(active_ids)} | {ids} |")

    return lines


# Document order. Each name must appear exactly once as a marked region in the snapshot.
BLOCK_RENDERERS = {
    "question-bank-headline": render_headline,
    "question-bank-topic-coverage": render_topic_coverage,
    "question-bank-deprecated-questions": render_deprecated_questions,
    "question-bank-empty-subtopics": render_empty_summary,
    "question-bank-subtopic-index": render_subtopic_index,
}


def render_blocks(curriculum: dict[str, Any]) -> dict[str, str]:
    """Every generated region body, keyed by block name.

    A body is framed by blank lines so each marker stays its own Markdown block and a table
    never absorbs the END marker as a row.
    """
    bank = index_bank(curriculum)

    return {
        name: "\n" + "\n".join(renderer(bank)).rstrip("\n") + "\n\n"
        for name, renderer in BLOCK_RENDERERS.items()
    }


# ---------------------------------------------------------------------------------------
# Generated-region patching
# ---------------------------------------------------------------------------------------


@dataclass(frozen=True)
class Region:
    name: str
    body_start: int  # first character after the BEGIN marker's line break
    body_end: int  # first character of the END marker


def find_regions(document: str, expected: list[str]) -> dict[str, Region]:
    """Locate every marked region, failing on any structure that makes ownership ambiguous.

    Markers define ownership: a missing region is an error rather than something to append,
    because the tool cannot know where a human intended the block to sit.
    """
    events = sorted(
        [(match.start(), match.end(), "BEGIN", match.group(1)) for match in BEGIN_MARKER.finditer(document)]
        + [(match.start(), match.end(), "END", match.group(1)) for match in END_MARKER.finditer(document)],
    )

    regions: dict[str, Region] = {}
    open_name: str | None = None
    open_body_start = 0

    for start, end, kind, name in events:
        if kind == "BEGIN":
            if open_name is not None:
                fail(f"BEGIN marker for '{name}' appears inside the unclosed region '{open_name}'.")
            if name in regions:
                fail(f"Generated region '{name}' is marked more than once.")
            if end >= len(document) or document[end] != "\n":
                fail(f"BEGIN marker for '{name}' must be followed by a line break.")
            open_name = name
            open_body_start = end + 1
        else:
            if open_name is None:
                fail(f"END marker for '{name}' has no matching BEGIN marker.")
            if name != open_name:
                fail(f"END marker for '{name}' closes the region opened as '{open_name}'.")
            regions[name] = Region(name=name, body_start=open_body_start, body_end=start)
            open_name = None

    if open_name is not None:
        fail(f"Generated region '{open_name}' has no END marker.")

    missing = [name for name in expected if name not in regions]
    if missing:
        fail(f"Snapshot is missing generated region(s): {', '.join(missing)}.")

    unknown = sorted(name for name in regions if name not in expected)
    if unknown:
        fail(f"Snapshot marks region(s) this tool does not generate: {', '.join(unknown)}.")

    return regions


def apply_blocks(document: str, blocks: dict[str, str]) -> str:
    """Replace each region body; every character outside the bodies is kept as it was."""
    regions = find_regions(document, list(blocks))

    pieces: list[str] = []
    cursor = 0
    for region in sorted(regions.values(), key=lambda region: region.body_start):
        pieces.append(document[cursor:region.body_start])
        pieces.append(blocks[region.name])
        cursor = region.body_end
    pieces.append(document[cursor:])

    return "".join(pieces)


def stale_blocks(document: str, blocks: dict[str, str]) -> list[str]:
    regions = find_regions(document, list(blocks))

    return [
        name
        for name, body in blocks.items()
        if document[regions[name].body_start:regions[name].body_end] != body
    ]


# ---------------------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------------------


def relative(path: Path) -> str:
    return str(path.relative_to(REPO_ROOT))


def read_snapshot() -> str:
    if not SNAPSHOT_PATH.exists():
        fail(f"Coverage snapshot {relative(SNAPSHOT_PATH)} is missing.")

    return SNAPSHOT_PATH.read_text(encoding="utf-8")


def write_snapshot(blocks: dict[str, str]) -> int:
    document = read_snapshot()
    # Fully assembled before anything is written, so a failure never leaves a partial file.
    updated = apply_blocks(document, blocks)

    if updated == document:
        print(f"Coverage snapshot {relative(SNAPSHOT_PATH)} is already current; nothing written.")

        return 0

    SNAPSHOT_PATH.write_text(updated, encoding="utf-8")
    print(f"Updated the generated regions of {relative(SNAPSHOT_PATH)}.")
    print("Prose outside the generated regions is unchanged; review it against the new numbers.")

    return 0


def check_snapshot(blocks: dict[str, str]) -> int:
    stale = stale_blocks(read_snapshot(), blocks)

    if stale:
        print(f"Coverage snapshot {relative(SNAPSHOT_PATH)} is stale.")
        print(f"Generated region(s) no longer match the curriculum: {', '.join(stale)}.")
        print("Run: python3 tools/question_bank_coverage.py --write")

        return 1

    print(f"Coverage snapshot {relative(SNAPSHOT_PATH)} generated regions are current.")
    print("Editorial prose outside those regions is not checked.")

    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument(
        "--write",
        action="store_true",
        help="regenerate the generated regions of the snapshot in place",
    )
    mode.add_argument(
        "--check",
        action="store_true",
        help="fail if a generated region no longer matches the curriculum",
    )
    arguments = parser.parse_args()

    try:
        blocks = render_blocks(load_json(QUESTION_CURRICULUM_PATH))

        return write_snapshot(blocks) if arguments.write else check_snapshot(blocks)

    except CoverageError as error:
        print("QUESTION BANK COVERAGE FAILED")
        print(f"Error: {error}")
        print("No file was written.")

        return 1


if __name__ == "__main__":
    raise SystemExit(main())
