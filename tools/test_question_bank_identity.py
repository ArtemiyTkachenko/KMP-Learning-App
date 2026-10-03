"""Tests for the question-bank identity gate.

Comparison cases use fixtures: each rule needs a before/after pair the shipped history does not
all contain, and the allowed-change cases matter as much as the rejected ones — a gate that
rejects a status flip or a re-homed Question would block legitimate authoring. Baseline loading
is tested against a throwaway Git repository, because the failure modes worth pinning (unknown
ref, file absent at the ref, malformed JSON) cannot be produced from this repository's history.

Run from the repository root:

    python3 -m unittest discover -s tools -p 'test_*.py'
"""

from __future__ import annotations

import copy
import json
import subprocess
import tempfile
import unittest
from pathlib import Path

import question_bank_identity as identity


def question(question_id: str, answers: int = 4, correct: tuple[int, ...] = (0,)) -> dict:
    return {
        "id": question_id,
        "topicId": "topic_one",
        "subtopicId": "subtopic_one",
        "text": f"Question {question_id}?",
        "answers": [{"id": f"{question_id}_{n}", "text": f"Option {n}."} for n in range(answers)],
        "selectionMode": "MULTIPLE" if len(correct) > 1 else "SINGLE",
        "level": "FOUNDATION",
        "correctAnswerIds": [f"{question_id}_{n}" for n in correct],
        "explanation": "Explanation.",
        "sources": [{"title": "Source", "url": "https://example.com/a"}],
        "status": "ACTIVE",
    }


def curriculum(*questions: dict) -> dict:
    return {"topics": [], "subtopics": [], "questions": list(questions)}


def baseline() -> dict:
    return curriculum(question("q_single"), question("q_multiple", correct=(0, 2)))


def edited(change) -> dict:
    current = copy.deepcopy(baseline())
    change({entry["id"]: entry for entry in current["questions"]})
    return current


class AllowedChangesTest(unittest.TestCase):
    def assert_allowed(self, change) -> None:
        self.assertEqual([], identity.identity_violations(baseline(), edited(change)))

    def test_unchanged_curriculum_passes(self) -> None:
        self.assertEqual([], identity.identity_violations(baseline(), baseline()))

    def test_new_question_is_allowed(self) -> None:
        current = baseline()
        current["questions"].append(question("q_new"))

        self.assertEqual([], identity.identity_violations(baseline(), current))

    def test_status_change_in_either_direction_is_allowed(self) -> None:
        def deprecate(questions):
            questions["q_single"]["status"] = "DEPRECATED"

        self.assert_allowed(deprecate)

        reactivated = edited(deprecate)
        reactivated_from = copy.deepcopy(reactivated)
        reactivated["questions"][0]["status"] = "ACTIVE"
        self.assertEqual([], identity.identity_violations(reactivated_from, reactivated))

    def test_taxonomy_re_homing_is_allowed(self) -> None:
        def re_home(questions):
            questions["q_single"]["topicId"] = "topic_two"
            questions["q_single"]["subtopicId"] = "subtopic_two"

        self.assert_allowed(re_home)

    def test_level_change_is_allowed(self) -> None:
        def raise_level(questions):
            questions["q_single"]["level"] = "ADVANCED"

        self.assert_allowed(raise_level)

    def test_editorial_changes_are_allowed(self) -> None:
        def reword(questions):
            entry = questions["q_single"]
            entry["text"] = "Reworded?"
            entry["answers"][1]["text"] = "Clearer distractor."
            entry["explanation"] = "Better explanation."
            entry["sources"] = [{"title": "New title", "url": "https://example.com/b"}]

        self.assert_allowed(reword)

    def test_reordering_answers_and_keys_is_allowed(self) -> None:
        def reorder(questions):
            entry = questions["q_multiple"]
            entry["answers"].reverse()
            entry["correctAnswerIds"].reverse()

        self.assert_allowed(reorder)

    def test_reordering_questions_is_allowed(self) -> None:
        current = baseline()
        current["questions"].reverse()

        self.assertEqual([], identity.identity_violations(baseline(), current))


class RejectedChangesTest(unittest.TestCase):
    def violations(self, change) -> list[str]:
        return identity.identity_violations(baseline(), edited(change))

    def test_removed_question_is_rejected(self) -> None:
        current = baseline()
        del current["questions"][0]

        violations = identity.identity_violations(baseline(), current)

        self.assertEqual(1, len(violations))
        self.assertIn("q_single: Question removed", violations[0])

    def test_selection_mode_change_is_rejected(self) -> None:
        def to_multiple(questions):
            questions["q_single"]["selectionMode"] = "MULTIPLE"

        self.assertEqual(
            ["q_single: selectionMode changed SINGLE -> MULTIPLE."],
            self.violations(to_multiple),
        )

    def test_correct_answer_change_is_rejected(self) -> None:
        def re_key(questions):
            questions["q_single"]["correctAnswerIds"] = ["q_single_1"]

        self.assertEqual(
            ["q_single: correctAnswerIds changed (removed q_single_0; added q_single_1)."],
            self.violations(re_key),
        )

    def test_added_answer_option_is_rejected(self) -> None:
        def add_option(questions):
            questions["q_single"]["answers"].append({"id": "q_single_9", "text": "New."})

        self.assertEqual(
            ["q_single: AnswerOption IDs changed (added q_single_9)."],
            self.violations(add_option),
        )

    def test_removed_answer_option_is_rejected(self) -> None:
        def remove_option(questions):
            del questions["q_single"]["answers"][3]

        self.assertEqual(
            ["q_single: AnswerOption IDs changed (removed q_single_3)."],
            self.violations(remove_option),
        )

    def test_renamed_key_reports_both_option_and_key_change(self) -> None:
        def rename_key(questions):
            entry = questions["q_single"]
            entry["answers"][0]["id"] = "q_single_new"
            entry["correctAnswerIds"] = ["q_single_new"]

        self.assertEqual(
            [
                "q_single: AnswerOption IDs changed (removed q_single_0; added q_single_new).",
                "q_single: correctAnswerIds changed (removed q_single_0; added q_single_new).",
            ],
            self.violations(rename_key),
        )

    def test_violations_follow_baseline_authored_order(self) -> None:
        current = baseline()
        current["questions"].reverse()
        current["questions"][0]["selectionMode"] = "SINGLE"  # q_multiple
        current["questions"][1]["selectionMode"] = "MULTIPLE"  # q_single

        violations = identity.identity_violations(baseline(), current)

        self.assertEqual(["q_single", "q_multiple"], [entry.split(":")[0] for entry in violations])


class MalformedInputTest(unittest.TestCase):
    def test_duplicate_question_id_fails(self) -> None:
        duplicated = curriculum(question("q_single"), question("q_single"))

        with self.assertRaisesRegex(identity.IdentityError, "appears more than once"):
            identity.identity_violations(duplicated, baseline())

    def test_missing_questions_list_fails(self) -> None:
        with self.assertRaisesRegex(identity.IdentityError, "no 'questions' list"):
            identity.identity_violations({"topics": []}, baseline())

    def test_non_string_answer_id_fails(self) -> None:
        current = baseline()
        current["questions"][0]["answers"][0]["id"] = 7

        with self.assertRaisesRegex(identity.IdentityError, "list of string IDs"):
            identity.identity_violations(baseline(), current)


class BaselineLoadingTest(unittest.TestCase):
    def setUp(self) -> None:
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.git("init", "--quiet")
        self.git("config", "user.email", "test@example.com")
        self.git("config", "user.name", "Test")
        self.git("config", "commit.gpgsign", "false")

    def git(self, *arguments: str) -> None:
        subprocess.run(["git", *arguments], cwd=self.root, check=True, capture_output=True)

    def commit_curriculum(self, text: str) -> None:
        path = self.root / identity.CURRICULUM_PATH
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        self.git("add", "--all")
        self.git("commit", "--quiet", "--message", "curriculum")

    def test_reads_the_curriculum_at_the_ref(self) -> None:
        self.commit_curriculum(json.dumps(baseline()))

        self.assertEqual(baseline(), identity.read_baseline("HEAD", self.root))

    def test_unknown_ref_fails(self) -> None:
        self.commit_curriculum(json.dumps(baseline()))

        with self.assertRaisesRegex(identity.IdentityError, "does not exist"):
            identity.read_baseline("no-such-ref", self.root)

    def test_option_like_ref_is_refused(self) -> None:
        with self.assertRaisesRegex(identity.IdentityError, "not a Git revision"):
            identity.read_baseline("--output=/tmp/x", self.root)

    def test_curriculum_absent_at_ref_fails(self) -> None:
        (self.root / "README").write_text("empty", encoding="utf-8")
        self.git("add", "--all")
        self.git("commit", "--quiet", "--message", "no curriculum")

        with self.assertRaisesRegex(identity.IdentityError, "Could not read"):
            identity.read_baseline("HEAD", self.root)

    def test_malformed_baseline_json_fails(self) -> None:
        self.commit_curriculum("{ not json")

        with self.assertRaisesRegex(identity.IdentityError, "not valid JSON"):
            identity.read_baseline("HEAD", self.root)

    def test_missing_current_curriculum_fails(self) -> None:
        with self.assertRaisesRegex(identity.IdentityError, "current curriculum is missing"):
            identity.read_current(self.root)


if __name__ == "__main__":
    unittest.main()
