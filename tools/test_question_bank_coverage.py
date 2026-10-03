"""Tests for the question-bank coverage generator.

Fixtures carry the derivation and patching cases: the properties worth protecting —
ACTIVE-only coverage, generic distributions, byte-for-byte preservation of human prose,
marker integrity — need input shapes the shipped bank does not all contain, and a test that
only passes because of today's content would stop testing anything once the content changed.
The tests that read the shipped files check currency and staleness, which is exactly what
production state is for.

Run from the repository root:

    python3 -m unittest discover -s tools -p 'test_*.py'
"""

from __future__ import annotations

import copy
import unittest

import question_bank_coverage as coverage


def question(
    question_id: str,
    subtopic_id: str,
    topic_id: str = "topic_one",
    status: str = "ACTIVE",
    selection_mode: str = "SINGLE",
    answers: int = 4,
    correct: int = 1,
    urls: tuple[str, ...] = ("https://example.com/a",),
) -> dict:
    return {
        "id": question_id,
        "topicId": topic_id,
        "subtopicId": subtopic_id,
        "text": f"Question {question_id}?",
        "answers": [{"id": f"{question_id}_{n}", "text": "Option."} for n in range(answers)],
        "selectionMode": selection_mode,
        "level": "FOUNDATION",
        "correctAnswerIds": [f"{question_id}_{n}" for n in range(correct)],
        "explanation": "Explanation.",
        "sources": [{"title": "Source", "url": url} for url in urls],
        "status": status,
    }


def subtopic(subtopic_id: str, topic_id: str, status: str = "ACTIVE") -> dict:
    return {
        "id": subtopic_id,
        "topicId": topic_id,
        "name": f"Name {subtopic_id}",
        "status": status,
    }


def curriculum_fixture() -> dict:
    # Authored order deliberately differs from alphabetical order at every level.
    return {
        "topics": [
            {"id": "topic_one", "name": "Topic One", "status": "ACTIVE"},
            {"id": "topic_two", "name": "Topic Two", "status": "ACTIVE"},
        ],
        "subtopics": [
            subtopic("s_zeta", "topic_one"),
            subtopic("s_alpha", "topic_one"),
            subtopic("s_empty", "topic_one"),
            subtopic("s_two", "topic_two"),
            subtopic("s_retired", "topic_two", status="DEPRECATED"),
        ],
        "questions": [
            question("q_zeta_2", "s_zeta", urls=("https://example.com/a", "https://example.com/b")),
            question("q_zeta_1", "s_zeta", selection_mode="MULTIPLE", correct=2, answers=5),
            question("q_alpha_1", "s_alpha", selection_mode="MULTIPLE", correct=1),
            question("q_alpha_old", "s_alpha", status="DEPRECATED", answers=3),
            question("q_empty_old", "s_empty", status="DEPRECATED", urls=()),
            question("q_two_1", "s_two", topic_id="topic_two"),
            question("q_retired_old", "s_retired", topic_id="topic_two", status="DEPRECATED"),
        ],
    }


def blocks(curriculum: dict) -> dict[str, str]:
    return coverage.render_blocks(curriculum)


def marked_document(bodies: dict[str, str] | None = None) -> str:
    """A small snapshot with human prose before, between and after every region."""
    parts = ["# Title\n\nHuman intro,  with  odd   spacing.\t\n\n"]
    for name in coverage.BLOCK_RENDERERS:
        body = (bodies or {}).get(name, "\nplaceholder\n\n")
        parts.append(
            f"<!-- BEGIN GENERATED: {name} -->\n{body}<!-- END GENERATED: {name} -->\n"
            f"\nHuman prose after {name}.\n\n",
        )
    parts.append("Closing human prose without a trailing newline")

    return "".join(parts)


def human_prose(document: str) -> list[str]:
    """Everything outside the region bodies, in order."""
    regions = coverage.find_regions(document, list(coverage.BLOCK_RENDERERS))
    pieces, cursor = [], 0
    for region in sorted(regions.values(), key=lambda region: region.body_start):
        pieces.append(document[cursor:region.body_start])
        cursor = region.body_end
    pieces.append(document[cursor:])

    return pieces


class CountTest(unittest.TestCase):
    def setUp(self) -> None:
        self.blocks = blocks(curriculum_fixture())

    def test_headline_counts(self) -> None:
        headline = self.blocks["question-bank-headline"]

        self.assertIn("| Total questions | 7 |", headline)
        self.assertIn("| ACTIVE | 4 |", headline)
        self.assertIn("| DEPRECATED | 3 |", headline)
        self.assertIn("| Topics | 2 |", headline)
        self.assertIn("| Subtopics | 5 (4 ACTIVE, 1 DEPRECATED) |", headline)
        self.assertIn("| SINGLE | 5 |", headline)
        self.assertIn("| MULTIPLE | 2 |", headline)
        self.assertIn("| — of which exactly one correct answer | 1 |", headline)
        self.assertIn("| Source references | 7 across 2 unique URLs |", headline)

    def test_covered_and_empty_subtopics(self) -> None:
        headline = self.blocks["question-bank-headline"]

        self.assertIn("| Subtopics with ≥1 active question | 3 (60%) |", headline)
        self.assertIn(
            "| Subtopics with 0 active questions | 2 (1 ACTIVE, plus the DEPRECATED `s_retired`) |",
            headline,
        )
        self.assertEqual(
            "\n2 subtopics have no active question: 1 ACTIVE subtopic and the DEPRECATED\n"
            "`s_retired`.\n\n",
            self.blocks["question-bank-empty-subtopics"],
        )

    def test_answer_option_distribution_is_derived(self) -> None:
        self.assertIn(
            "| Answer options | 28 (1 question with 3 options, 5 with 4, 1 with 5) |",
            self.blocks["question-bank-headline"],
        )

    def test_depth_distribution_is_derived_in_ascending_order(self) -> None:
        self.assertIn(
            "Subtopic depth distribution: **2** subtopics have 0 questions, **2** have 1, and\n"
            "**1** has 2.",
            self.blocks["question-bank-headline"],
        )

    def test_several_deprecated_empty_subtopics_render_generically(self) -> None:
        curriculum = curriculum_fixture()
        curriculum["subtopics"].append(subtopic("s_retired_too", "topic_two", status="DEPRECATED"))

        rendered = blocks(curriculum)

        self.assertIn(
            "1 ACTIVE subtopic and 2 DEPRECATED ones (`s_retired`, `s_retired_too`).",
            rendered["question-bank-empty-subtopics"].replace("\n", " "),
        )
        self.assertIn(
            "(1 ACTIVE, plus 2 DEPRECATED ones (`s_retired`, `s_retired_too`))",
            rendered["question-bank-headline"],
        )

    def test_unknown_selection_mode_fails(self) -> None:
        curriculum = curriculum_fixture()
        curriculum["questions"][0]["selectionMode"] = "ORDERED"

        with self.assertRaises(coverage.CoverageError):
            blocks(curriculum)


class ActiveSemanticsTest(unittest.TestCase):
    def test_a_deprecated_question_never_covers_a_subtopic(self) -> None:
        # `s_empty` holds only a DEPRECATED Question.
        rendered = blocks(curriculum_fixture())

        self.assertIn(
            "| `s_empty` — Name s_empty | 0 | —_(deprecated: `q_empty_old`)_ |",
            rendered["question-bank-subtopic-index"],
        )
        self.assertIn(
            "| Topic One | `topic_one` | 3 | 3 | 2 | 1 | 1.00 |",
            rendered["question-bank-topic-coverage"],
        )

    def test_deprecating_a_question_changes_coverage(self) -> None:
        curriculum = curriculum_fixture()
        before = blocks(curriculum)
        curriculum["questions"][5]["status"] = "DEPRECATED"  # q_two_1, the only one in s_two
        after = blocks(curriculum)

        for name in coverage.BLOCK_RENDERERS:
            self.assertNotEqual(before[name], after[name], name)
        self.assertIn(
            "| Topic Two | `topic_two` | 0 | 2 | 0 | 2 | 0.00 |",
            after["question-bank-topic-coverage"],
        )


class RehomingTest(unittest.TestCase):
    def test_moving_an_active_question_changes_the_affected_blocks(self) -> None:
        curriculum = curriculum_fixture()
        before = blocks(curriculum)
        moved = curriculum["questions"][1]  # q_zeta_1 → s_empty, inside the same Topic
        moved["subtopicId"] = "s_empty"
        after = blocks(curriculum)

        self.assertIn(
            "| `s_empty` — Name s_empty | 1 | `q_zeta_1` _(deprecated: `q_empty_old`)_ |",
            after["question-bank-subtopic-index"],
        )
        self.assertIn("| `s_zeta` — Name s_zeta | 1 | `q_zeta_2` |", after["question-bank-subtopic-index"])
        # The Topic's totals hold, but one more Subtopic is now covered.
        self.assertIn("| Topic One | `topic_one` | 3 | 3 | 3 | 0 | 1.00 |", after["question-bank-topic-coverage"])
        self.assertNotEqual(before["question-bank-headline"], after["question-bank-headline"])
        self.assertNotEqual(before["question-bank-empty-subtopics"], after["question-bank-empty-subtopics"])

    def test_question_topic_must_match_its_subtopic(self) -> None:
        curriculum = curriculum_fixture()
        curriculum["questions"][0]["subtopicId"] = "s_two"  # still names topic_one

        with self.assertRaises(coverage.CoverageError):
            blocks(curriculum)


class DeprecatedTableTest(unittest.TestCase):
    def test_lists_all_and_only_deprecated_questions(self) -> None:
        table = blocks(curriculum_fixture())["question-bank-deprecated-questions"]

        self.assertTrue(table.startswith("\n3 questions are `DEPRECATED`."))
        for question_id in ("q_alpha_old", "q_empty_old", "q_retired_old"):
            self.assertIn(f"`{question_id}`", table)
        for question_id in ("q_zeta_1", "q_zeta_2", "q_alpha_1", "q_two_1"):
            self.assertNotIn(question_id, table)
        self.assertIn("| `q_retired_old` | `topic_two` / `s_retired` |", table)

    def test_groups_by_topic_then_authored_question_order(self) -> None:
        curriculum = curriculum_fixture()
        # Appended last, as a later batch would be, but belongs to the first Topic.
        curriculum["questions"].append(question("q_late_old", "s_zeta", status="DEPRECATED"))

        table = blocks(curriculum)["question-bank-deprecated-questions"]
        order = [line.split("`")[1] for line in table.splitlines() if line.startswith("| `")]

        self.assertEqual(["q_alpha_old", "q_empty_old", "q_late_old", "q_retired_old"], order)

    def test_no_deprecated_questions_renders_the_count_alone(self) -> None:
        curriculum = curriculum_fixture()
        curriculum["questions"] = [q for q in curriculum["questions"] if q["status"] == "ACTIVE"]

        table = blocks(curriculum)["question-bank-deprecated-questions"]

        self.assertIn("0 questions are `DEPRECATED`.", table)
        self.assertNotIn("| Question |", table)


class SubtopicIndexTest(unittest.TestCase):
    def setUp(self) -> None:
        self.index = blocks(curriculum_fixture())["question-bank-subtopic-index"]

    def test_active_ids_precede_the_deprecated_annotation(self) -> None:
        self.assertIn(
            "| `s_alpha` — Name s_alpha | 1 | `q_alpha_1` _(deprecated: `q_alpha_old`)_ |",
            self.index,
        )

    def test_active_ids_keep_authored_question_order(self) -> None:
        self.assertIn("| `s_zeta` — Name s_zeta | 2 | `q_zeta_2`, `q_zeta_1` |", self.index)

    def test_deprecated_subtopic_is_marked(self) -> None:
        self.assertIn(
            "| `s_retired` — Name s_retired _(deprecated subtopic)_ | 0 | "
            "—_(deprecated: `q_retired_old`)_ |",
            self.index,
        )

    def test_topics_and_subtopics_keep_authored_order(self) -> None:
        positions = [
            self.index.index(marker)
            for marker in ("### Topic One", "`s_zeta`", "`s_alpha`", "`s_empty`", "### Topic Two", "`s_two`")
        ]

        self.assertEqual(sorted(positions), positions)
        self.assertIn("`topic_one` — **3 active** across 3 subtopics (2 covered, 1 empty)", self.index)


class TopicOrderingTest(unittest.TestCase):
    def test_densest_topic_first_and_total_is_a_bank_ratio(self) -> None:
        curriculum = curriculum_fixture()
        curriculum["questions"].extend(
            [question(f"q_two_extra_{n}", "s_two", topic_id="topic_two") for n in range(3)],
        )

        table = blocks(curriculum)["question-bank-topic-coverage"]
        rows = [line for line in table.splitlines() if line.startswith("| Topic T") or line.startswith("| Topic O")]

        # topic_two 4/2 = 2.00 overtakes topic_one 3/3 = 1.00.
        self.assertEqual(["| Topic Two", "| Topic One"], [row[:11] for row in rows])
        # 7 / 5 = 1.40, not the mean of 2.00 and 1.00.
        self.assertIn("| **Total** | | **7** | **5** | **3** | **2** | **1.40** |", table)

    def test_equal_density_falls_back_to_authored_topic_order(self) -> None:
        curriculum = curriculum_fixture()
        # topic_one is 3 active / 3 subtopics; a second topic_two Question makes it 2 / 2.
        curriculum["questions"].append(question("q_two_2", "s_two", topic_id="topic_two"))

        table = blocks(curriculum)["question-bank-topic-coverage"]
        rows = [line for line in table.splitlines() if line.startswith("| Topic T") or line.startswith("| Topic O")]

        self.assertEqual(["| Topic One", "| Topic Two"], [row[:11] for row in rows])
        self.assertTrue(all(row.endswith("| 1.00 |") for row in rows))


class ValidationTest(unittest.TestCase):
    def assert_fails(self, mutate) -> None:
        curriculum = curriculum_fixture()
        mutate(curriculum)

        with self.assertRaises(coverage.CoverageError):
            blocks(curriculum)

    def test_duplicate_topic_id(self) -> None:
        self.assert_fails(lambda c: c["topics"].append(dict(c["topics"][0])))

    def test_duplicate_subtopic_id(self) -> None:
        self.assert_fails(lambda c: c["subtopics"].append(dict(c["subtopics"][0])))

    def test_duplicate_question_id(self) -> None:
        self.assert_fails(lambda c: c["questions"].append(question("q_zeta_1", "s_alpha")))

    def test_question_with_unknown_topic(self) -> None:
        self.assert_fails(lambda c: c["questions"][0].update(topicId="missing"))

    def test_question_with_unknown_subtopic(self) -> None:
        self.assert_fails(lambda c: c["questions"][0].update(subtopicId="missing"))

    def test_subtopic_with_unknown_topic(self) -> None:
        self.assert_fails(lambda c: c["subtopics"][0].update(topicId="missing"))

    def test_unknown_question_status(self) -> None:
        self.assert_fails(lambda c: c["questions"][0].update(status="DRAFT"))

    def test_unknown_subtopic_status(self) -> None:
        self.assert_fails(lambda c: c["subtopics"][0].update(status="HIDDEN"))

    def test_topic_without_subtopics(self) -> None:
        self.assert_fails(lambda c: c["topics"].append({"id": "bare", "name": "Bare", "status": "ACTIVE"}))


class RegionPatchingTest(unittest.TestCase):
    def setUp(self) -> None:
        self.blocks = blocks(curriculum_fixture())

    def test_human_prose_before_between_and_after_regions_survives_byte_for_byte(self) -> None:
        document = marked_document()

        updated = coverage.apply_blocks(document, self.blocks)

        self.assertEqual(human_prose(document), human_prose(updated))
        self.assertTrue(updated.startswith("# Title\n\nHuman intro,  with  odd   spacing.\t\n\n"))
        self.assertTrue(updated.endswith("Closing human prose without a trailing newline"))
        for name, body in self.blocks.items():
            self.assertIn(f"<!-- BEGIN GENERATED: {name} -->\n{body}<!-- END GENERATED: {name} -->", updated)

    def test_update_is_idempotent(self) -> None:
        once = coverage.apply_blocks(marked_document(), self.blocks)
        twice = coverage.apply_blocks(once, self.blocks)

        self.assertEqual(once, twice)
        self.assertEqual([], coverage.stale_blocks(once, self.blocks))

    def test_stale_region_is_named(self) -> None:
        current = coverage.apply_blocks(marked_document(), self.blocks)
        tampered = current.replace("| Total questions | 7 |", "| Total questions | 8 |")

        self.assertEqual(["question-bank-headline"], coverage.stale_blocks(tampered, self.blocks))

    def test_missing_region_fails_instead_of_being_appended(self) -> None:
        document = marked_document().replace(
            "<!-- BEGIN GENERATED: question-bank-topic-coverage -->\n\nplaceholder\n\n"
            "<!-- END GENERATED: question-bank-topic-coverage -->\n",
            "",
        )

        with self.assertRaisesRegex(coverage.CoverageError, "missing generated region"):
            coverage.apply_blocks(document, self.blocks)

    def test_duplicate_region_fails(self) -> None:
        document = marked_document() + (
            "\n<!-- BEGIN GENERATED: question-bank-headline -->\n"
            "<!-- END GENERATED: question-bank-headline -->\n"
        )

        with self.assertRaisesRegex(coverage.CoverageError, "more than once"):
            coverage.apply_blocks(document, self.blocks)

    def test_end_marker_without_begin_fails(self) -> None:
        document = marked_document().replace(
            "<!-- BEGIN GENERATED: question-bank-headline -->\n",
            "",
        )

        with self.assertRaisesRegex(coverage.CoverageError, "no matching BEGIN"):
            coverage.apply_blocks(document, self.blocks)

    def test_begin_marker_without_end_fails(self) -> None:
        document = marked_document().replace(
            "<!-- END GENERATED: question-bank-subtopic-index -->\n",
            "",
        )

        with self.assertRaisesRegex(coverage.CoverageError, "no END marker"):
            coverage.apply_blocks(document, self.blocks)

    def test_mismatched_end_marker_fails(self) -> None:
        document = marked_document().replace(
            "<!-- END GENERATED: question-bank-headline -->",
            "<!-- END GENERATED: question-bank-topic-coverage -->",
        )

        with self.assertRaises(coverage.CoverageError):
            coverage.apply_blocks(document, self.blocks)

    def test_unknown_region_fails(self) -> None:
        document = marked_document() + (
            "\n<!-- BEGIN GENERATED: question-bank-mystery -->\n"
            "<!-- END GENERATED: question-bank-mystery -->\n"
        )

        with self.assertRaisesRegex(coverage.CoverageError, "does not generate"):
            coverage.apply_blocks(document, self.blocks)


class ShippedContentTest(unittest.TestCase):
    """The staleness contract, checked against the files actually shipped."""

    def setUp(self) -> None:
        self.curriculum = coverage.load_json(coverage.QUESTION_CURRICULUM_PATH)
        self.snapshot = coverage.SNAPSHOT_PATH.read_text(encoding="utf-8")

    def test_committed_generated_regions_match_the_current_curriculum(self) -> None:
        stale = coverage.stale_blocks(self.snapshot, coverage.render_blocks(self.curriculum))

        self.assertEqual(
            [],
            stale,
            "docs/content/question-bank-coverage.md has stale generated regions. "
            "Run: python3 tools/question_bank_coverage.py --write",
        )

    def test_deprecating_an_active_question_makes_the_snapshot_stale(self) -> None:
        # Copies only: the curriculum is never written to by this tooling.
        changed = copy.deepcopy(self.curriculum)
        target = next(q for q in changed["questions"] if q["status"] == "ACTIVE")
        target["status"] = "DEPRECATED"

        stale = coverage.stale_blocks(self.snapshot, coverage.render_blocks(changed))

        self.assertIn("question-bank-headline", stale)
        self.assertIn("question-bank-deprecated-questions", stale)
        self.assertIn("question-bank-subtopic-index", stale)

    def test_rehoming_an_active_question_makes_the_snapshot_stale(self) -> None:
        changed = copy.deepcopy(self.curriculum)
        target = next(q for q in changed["questions"] if q["status"] == "ACTIVE")
        destination = next(
            s for s in changed["subtopics"]
            if s["topicId"] == target["topicId"] and s["id"] != target["subtopicId"]
        )
        target["subtopicId"] = destination["id"]

        stale = coverage.stale_blocks(self.snapshot, coverage.render_blocks(changed))

        self.assertIn("question-bank-subtopic-index", stale)


if __name__ == "__main__":
    unittest.main()
