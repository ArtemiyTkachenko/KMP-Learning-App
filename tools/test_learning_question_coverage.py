"""Tests for the learning-to-question coverage generator.

Fixtures rather than production content carry most of these cases: the properties worth
protecting — unit-level deduplication, supporting isolation, active-only counting — need
input shapes the current curriculum does not all contain, and a test that only passes
because of today's authored data would stop testing anything the moment the content
changed. The two tests that do read the shipped files check reproducibility and staleness,
which is exactly what production state is for.

Run from the repository root:

    python3 -m unittest discover -s tools -p 'test_*.py'
"""

from __future__ import annotations

import copy
import unittest

import learning_question_coverage as coverage


def question(
    question_id: str,
    subtopic_id: str,
    level: str = "FOUNDATION",
    status: str = "ACTIVE",
) -> dict:
    return {
        "id": question_id,
        "topicId": "topic_one",
        "subtopicId": subtopic_id,
        "text": f"Question {question_id}?",
        "answers": [],
        "selectionMode": "SINGLE",
        "level": level,
        "correctAnswerIds": [],
        "explanation": "Explanation.",
        "sources": [],
        "status": status,
    }


def lesson(
    lesson_id: str,
    primary: list[str],
    supporting: list[str] | None = None,
    status: str = "ACTIVE",
) -> dict:
    return {
        "id": lesson_id,
        "title": f"Lesson {lesson_id}",
        "summary": "Summary.",
        "primarySubtopicIds": primary,
        "supportingSubtopicIds": supporting or [],
        "sections": [],
        "relatedLessonIds": [],
        "sources": [],
        "status": status,
    }


def curriculum_fixture() -> dict:
    return {
        "topics": [{"id": "topic_one", "name": "Topic One", "status": "ACTIVE"}],
        "subtopics": [
            {"id": "primary_a", "topicId": "topic_one", "name": "Primary A", "status": "ACTIVE"},
            {"id": "primary_b", "topicId": "topic_one", "name": "Primary B", "status": "ACTIVE"},
            {"id": "supporting_a", "topicId": "topic_one", "name": "Supporting A", "status": "ACTIVE"},
            {"id": "unassessed", "topicId": "topic_one", "name": "Unassessed", "status": "ACTIVE"},
        ],
        "questions": [
            question("q_primary_a_1", "primary_a", "FOUNDATION"),
            question("q_primary_a_2", "primary_a", "APPLIED"),
            question("q_primary_a_retired", "primary_a", "ADVANCED", status="DEPRECATED"),
            question("q_primary_b_1", "primary_b", "ADVANCED"),
            question("q_supporting_a_1", "supporting_a", "FOUNDATION"),
            question("q_supporting_a_2", "supporting_a", "APPLIED"),
        ],
    }


def learning_fixture() -> dict:
    return {
        "units": [
            {
                "id": "unit_one",
                "topicId": "topic_one",
                "title": "Unit One",
                "summary": "Summary.",
                "lessons": [
                    # Two lessons deliberately share `primary_a`, the real shape Unit 1 of
                    # the Compose curriculum has.
                    lesson("lesson_one", ["primary_a"], ["supporting_a"]),
                    lesson("lesson_two", ["primary_a"]),
                    lesson("lesson_three", ["primary_b"]),
                ],
                "status": "ACTIVE",
            },
        ],
    }


class DerivationTest(unittest.TestCase):
    def setUp(self) -> None:
        self.curriculum = curriculum_fixture()
        self.learning = learning_fixture()
        self.index = coverage.index_curriculum(self.curriculum)

    def test_each_lesson_shows_the_questions_of_its_own_primary_subtopic(self) -> None:
        reached = coverage.unique_questions(self.index, ["primary_a"])

        self.assertEqual(["q_primary_a_1", "q_primary_a_2"], [q.id for q in reached])

    def test_unit_totals_count_a_shared_question_once(self) -> None:
        # lesson_one and lesson_two both reach the two `primary_a` questions, and
        # lesson_three reaches one more. Summing lessons would give 5.
        primary_ids = coverage.ordered_distinct(
            [
                subtopic_id
                for unit_lesson in self.learning["units"][0]["lessons"]
                for subtopic_id in unit_lesson["primarySubtopicIds"]
            ],
        )
        reached = coverage.unique_questions(self.index, primary_ids)

        self.assertEqual(
            ["q_primary_a_1", "q_primary_a_2", "q_primary_b_1"],
            [q.id for q in reached],
        )
        self.assertEqual(
            {"FOUNDATION": 1, "APPLIED": 1, "ADVANCED": 1},
            coverage.level_counts(reached),
        )

    def test_supporting_questions_never_enter_primary_counts(self) -> None:
        report = coverage.render_report(self.learning, self.curriculum)
        primary_ids = ["primary_a", "primary_b"]
        reached = coverage.unique_questions(self.index, primary_ids)

        self.assertNotIn("q_supporting_a_1", [q.id for q in reached])
        # The supporting subtopic is reported, and its questions are only ever counted.
        self.assertIn("`supporting_a` — Supporting A", report)
        self.assertNotIn("q_supporting_a_1", report)

    def test_supporting_role_is_local_to_the_lesson(self) -> None:
        # `supporting_a` is supporting in lesson_one and primary in a second unit's lesson;
        # both roles must survive rather than one winning globally.
        learning = copy.deepcopy(self.learning)
        learning["units"].append(
            {
                "id": "unit_two",
                "topicId": "topic_one",
                "title": "Unit Two",
                "summary": "Summary.",
                "lessons": [lesson("lesson_four", ["supporting_a"])],
                "status": "ACTIVE",
            },
        )

        report = coverage.render_report(learning, self.curriculum)

        self.assertIn("| Unit Two (`unit_two`) | 1 | 1 | 1 | 1 | 0 | 2 |", report)
        self.assertIn("q_supporting_a_1", report)

    def test_deprecated_questions_are_excluded(self) -> None:
        reached = coverage.unique_questions(self.index, ["primary_a"])

        self.assertNotIn("q_primary_a_retired", [q.id for q in reached])
        self.assertEqual(0, coverage.level_counts(reached)["ADVANCED"])
        self.assertEqual(1, self.index.deprecated_question_count)

    def test_deprecated_lessons_and_units_are_excluded(self) -> None:
        learning = copy.deepcopy(self.learning)
        learning["units"][0]["lessons"][2]["status"] = "DEPRECATED"

        report = coverage.render_report(learning, self.curriculum)

        self.assertNotIn("lesson_three", report)
        self.assertNotIn("q_primary_b_1", report)

    def test_primary_subtopics_without_active_questions_are_reported(self) -> None:
        learning = copy.deepcopy(self.learning)
        learning["units"][0]["lessons"][2]["primarySubtopicIds"] = ["unassessed"]

        gaps = coverage.primary_subtopics_without_questions(learning, self.index)

        self.assertEqual(["unassessed"], gaps)
        self.assertIn("`unassessed` — Unassessed", coverage.render_report(learning, self.curriculum))

    def test_levels_come_from_the_authored_field(self) -> None:
        curriculum = curriculum_fixture()
        curriculum["questions"][0]["level"] = "FOUNDATIONAL"

        with self.assertRaises(coverage.CoverageError):
            coverage.index_curriculum(curriculum)

    def test_unknown_question_status_fails(self) -> None:
        curriculum = curriculum_fixture()
        curriculum["questions"][0]["status"] = "DRAFT"

        with self.assertRaises(coverage.CoverageError):
            coverage.index_curriculum(curriculum)

    def test_duplicate_question_id_fails(self) -> None:
        curriculum = curriculum_fixture()
        curriculum["questions"].append(question("q_primary_a_1", "primary_b"))

        with self.assertRaises(coverage.CoverageError):
            coverage.index_curriculum(curriculum)

    def test_unknown_primary_subtopic_fails_instead_of_reporting_partially(self) -> None:
        learning = copy.deepcopy(self.learning)
        learning["units"][0]["lessons"][0]["primarySubtopicIds"] = ["does_not_exist"]

        with self.assertRaises(coverage.CoverageError):
            coverage.render_report(learning, self.curriculum)

    def test_report_is_deterministic(self) -> None:
        first = coverage.render_report(self.learning, self.curriculum)
        second = coverage.render_report(learning_fixture(), curriculum_fixture())

        self.assertEqual(first, second)


class FingerprintTest(unittest.TestCase):
    def test_formatting_alone_does_not_change_a_fingerprint(self) -> None:
        learning = learning_fixture()
        reordered_keys = {"units": learning["units"]}

        self.assertEqual(
            coverage.learning_fingerprint(learning),
            coverage.learning_fingerprint(reordered_keys),
        )

    def test_changing_a_primary_mapping_changes_the_learning_fingerprint(self) -> None:
        learning = learning_fixture()
        changed = copy.deepcopy(learning)
        changed["units"][0]["lessons"][0]["primarySubtopicIds"] = ["primary_b"]

        self.assertNotEqual(
            coverage.learning_fingerprint(learning),
            coverage.learning_fingerprint(changed),
        )

    def test_editing_lesson_content_changes_the_learning_fingerprint(self) -> None:
        learning = learning_fixture()
        changed = copy.deepcopy(learning)
        changed["units"][0]["lessons"][0]["summary"] = "A different summary."

        self.assertNotEqual(
            coverage.learning_fingerprint(learning),
            coverage.learning_fingerprint(changed),
        )

    def test_editing_an_active_question_changes_the_question_fingerprint(self) -> None:
        curriculum = curriculum_fixture()
        changed = copy.deepcopy(curriculum)
        changed["questions"][0]["text"] = "A reworded question?"

        self.assertNotEqual(
            coverage.question_fingerprint(curriculum),
            coverage.question_fingerprint(changed),
        )

    def test_editing_a_deprecated_question_does_not_change_the_question_fingerprint(self) -> None:
        curriculum = curriculum_fixture()
        changed = copy.deepcopy(curriculum)
        changed["questions"][2]["text"] = "A reworded retired question?"

        self.assertEqual(
            coverage.question_fingerprint(curriculum),
            coverage.question_fingerprint(changed),
        )


class ShippedContentTest(unittest.TestCase):
    """The staleness contract, checked against the two documents actually shipped."""

    def setUp(self) -> None:
        self.learning = coverage.load_json(coverage.LEARNING_CURRICULUM_PATH)
        self.curriculum = coverage.load_json(coverage.QUESTION_CURRICULUM_PATH)

    def test_committed_snapshot_matches_the_current_authored_content(self) -> None:
        expected = coverage.render_report(self.learning, self.curriculum)

        self.assertEqual(
            expected,
            coverage.SNAPSHOT_PATH.read_text(encoding="utf-8"),
            "docs/content/learning-question-coverage.md is stale. "
            "Run: python3 tools/learning_question_coverage.py --write",
        )

    def test_changing_a_learning_mapping_makes_the_snapshot_stale(self) -> None:
        # Copies only: neither source curriculum is written to by this tooling.
        changed = copy.deepcopy(self.learning)
        changed["units"][0]["lessons"][0]["primarySubtopicIds"] = ["compose_recomposition"]

        self.assertNotEqual(
            coverage.SNAPSHOT_PATH.read_text(encoding="utf-8"),
            coverage.render_report(changed, self.curriculum),
        )

    def test_changing_an_active_question_makes_the_snapshot_stale(self) -> None:
        changed = copy.deepcopy(self.curriculum)
        for candidate in changed["questions"]:
            if candidate["status"] == "ACTIVE" and candidate["subtopicId"] == "compose_udf":
                candidate["text"] = "A reworded question?"
                break
        else:  # pragma: no cover - the fixture guarantees such a question exists
            self.fail("No active compose_udf question to modify.")

        self.assertNotEqual(
            coverage.SNAPSHOT_PATH.read_text(encoding="utf-8"),
            coverage.render_report(self.learning, changed),
        )

    def test_unit_one_deduplicates_its_shared_primary_subtopic(self) -> None:
        # Regression for the real current shape: `compose_fundamentals` is primary in two
        # Unit 1 lessons, and its questions must count once for the unit.
        index = coverage.index_curriculum(self.curriculum)
        unit = coverage.active_units(self.learning)[0]
        lessons = coverage.active_lessons(unit)
        primary_ids = [
            subtopic_id
            for unit_lesson in lessons
            for subtopic_id in unit_lesson["primarySubtopicIds"]
        ]

        self.assertGreater(
            len(primary_ids),
            len(coverage.ordered_distinct(primary_ids)),
            "Expected at least one primary subtopic shared by two lessons.",
        )

        per_lesson_total = sum(
            len(coverage.unique_questions(index, unit_lesson["primarySubtopicIds"]))
            for unit_lesson in lessons
        )
        unit_total = len(coverage.unique_questions(index, coverage.ordered_distinct(primary_ids)))

        self.assertLess(unit_total, per_lesson_total)


if __name__ == "__main__":
    unittest.main()
