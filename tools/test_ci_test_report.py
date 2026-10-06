"""Tests for the CI test-report renderer.

Two fixture sets under `tools/testdata/ci_test_report/`: `gradle/` holds trimmed copies of real
JUnit XML that Gradle wrote for each shared test task (all passing), so the parser is pinned to
the format CI actually produces; `failing/` is hand-written in the same shape to reach the cases
a healthy build never produces — a failure, an error, a skipped test, several failures in one
test case, hostile message text, an empty suite, missing attributes and a malformed file. The
empty and missing results directories are built in a temporary directory, since Git cannot
commit an empty directory.

Run from the repository root:

    python3 -m unittest discover -s tools -p 'test_*.py'
"""

from __future__ import annotations

import contextlib
import io
import re
import tempfile
import unittest
from pathlib import Path

import ci_test_report as report

FIXTURES = Path(__file__).resolve().parent / "testdata" / "ci_test_report"
GRADLE = FIXTURES / "gradle"
FAILING = FIXTURES / "failing"

GITHUB_ENV = {
    "GITHUB_REPOSITORY": "owner/repo",
    "GITHUB_SHA": "0123456789abcdef0123456789abcdef01234567",
    "GITHUB_REF_NAME": "main",
    "GITHUB_SERVER_URL": "https://github.com",
    "GITHUB_RUN_ID": "42",
}


class Outputs:
    """Runs the command-line entry point into a temporary directory."""

    def __init__(self, results_dir: Path, environ: dict[str, str] | None = None) -> None:
        self._temp = tempfile.TemporaryDirectory()
        self.dir = Path(self._temp.name)
        self.summary_path = self.dir / "summary.md"
        self.html_path = self.dir / "report.html"
        self.exit_code = report.main(
            [str(results_dir), "--summary", str(self.summary_path), "--html", str(self.html_path)],
            environ={} if environ is None else environ,
        )
        self.summary = self.summary_path.read_text(encoding="utf-8")
        self.html = self.html_path.read_text(encoding="utf-8")

    def close(self) -> None:
        self._temp.cleanup()


class RealGradleReportsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.outputs = Outputs(GRADLE)
        self.addCleanup(self.outputs.close)

    def test_every_target_is_counted(self) -> None:
        totals = report.read_results(GRADLE).by_target()
        self.assertEqual(
            ["jsBrowserTest", "jvmTest", "testAndroidHostTest", "wasmJsBrowserTest"],
            list(totals),
        )
        self.assertEqual(3, totals["jsBrowserTest"].total)
        self.assertEqual(2, totals["jvmTest"].passed)
        self.assertEqual(3, totals["testAndroidHostTest"].passed)
        self.assertEqual(3, totals["wasmJsBrowserTest"].passed)
        self.assertAlmostEqual(0.102, totals["jvmTest"].time)

    def test_success_summary_is_a_result_line_and_the_target_table(self) -> None:
        self.assertEqual(0, self.outputs.exit_code)
        self.assertIn("✅ Shared tests: All 11 tests passed", self.outputs.summary)
        self.assertIn("| Target | Total | Passed | Failed | Skipped | Time |", self.outputs.summary)
        self.assertIn("| wasmJsBrowserTest | 3 | 3 | 0 | 0 |", self.outputs.summary)
        self.assertNotIn("Failed tests", self.outputs.summary)
        self.assertIn(report.COVERAGE_NOTE, self.outputs.summary)
        self.assertIn(report.COVERAGE_NOTE, self.outputs.html)

    def test_local_run_has_no_links(self) -> None:
        self.assertIn("Local run", self.outputs.html)
        self.assertNotIn("http", self.outputs.html)


class FailingReportsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.results = report.read_results(FAILING)
        self.outputs = Outputs(FAILING, GITHUB_ENV)
        self.addCleanup(self.outputs.close)

    def test_failures_and_errors_both_count_as_failed(self) -> None:
        jvm = self.results.by_target()["jvmTest"]
        self.assertEqual((6, 1, 4, 1, 1), (jvm.total, jvm.passed, jvm.failed, jvm.errors, jvm.skipped))
        overall = self.results.overall()
        self.assertEqual((8, 3, 4), (overall.total, overall.passed, overall.failed))

    def test_several_failures_in_one_test_case_are_all_kept(self) -> None:
        cart = next(case for case in self.results.cases if case.name == "cartIsConsistent[jvm]")
        self.assertEqual(
            ["first: item count differs", "second: total differs"],
            [problem.message for problem in cart.problems],
        )
        self.assertIn("second: total differs", self.outputs.html)

    def test_missing_attributes_fall_back(self) -> None:
        settings = [case for case in self.results.cases if case.target == "testAndroidHostTest"]
        self.assertEqual({"example.SettingsTest"}, {case.class_name for case in settings})
        self.assertEqual({0.0}, {case.time for case in settings})

    def test_malformed_report_is_listed_and_processing_continues(self) -> None:
        self.assertEqual(
            ["testAndroidHostTest/TEST-example.Truncated.xml"], self.results.unreadable
        )
        self.assertEqual(0, self.outputs.exit_code)
        expected = "unreadable report: testAndroidHostTest/TEST-example.Truncated.xml"
        self.assertIn(expected, self.outputs.html)
        self.assertIn(expected.replace("_", "\\_"), self.outputs.summary)

    def test_html_lists_failures_before_passes_and_collapses_traces(self) -> None:
        html = self.outputs.html
        self.assertLess(html.index("discountIsApplied"), html.index("totalIncludesTax"))
        self.assertLess(html.index("<h2>Failures</h2>"), html.index("<h2>Passing tests</h2>"))
        self.assertIn("<details><summary>Stack trace</summary><pre>java.lang.AssertionError", html)
        self.assertIn("<summary>Standard output</summary>", html)

    def test_passing_tests_carry_no_captured_output(self) -> None:
        passing_section = self.outputs.html.split("<h2>Passing tests</h2>", 1)[1]
        self.assertNotIn("log line before failure", passing_section)

    def test_html_header_carries_the_run_context(self) -> None:
        html = self.outputs.html
        self.assertIn('<a href="https://github.com/owner/repo/actions/runs/42">', html)
        self.assertIn("commit 0123456789ab", html)
        self.assertIn("ref main", html)
        self.assertNotIn("Local run", html)

    def test_html_is_self_contained(self) -> None:
        html = self.outputs.html.lower()
        self.assertNotIn("<script", html)
        self.assertNotIn("<link", html)
        self.assertNotIn("<img", html)
        self.assertNotIn("@import", html)
        self.assertNotIn("url(", html)
        # The fixture's message text contains "src=", escaped; no real tag may carry one.
        self.assertIsNone(re.search(r"<[^>]*\ssrc\s*=", html))
        # The only reference anywhere is the link back to the workflow run.
        self.assertEqual(
            ["https://github.com/owner/repo/actions/runs/42"],
            re.findall(r"\shref\s*=\s*\"([^\"]*)\"", html),
        )
        self.assertIn("prefers-color-scheme: dark", html)

    def test_html_escapes_every_xml_value(self) -> None:
        html = self.outputs.html
        self.assertIn("&lt;/details&gt;&lt;script&gt;alert(1)&lt;/script&gt;", html)
        self.assertIn("&lt;img src=x onerror=alert(2)&gt;", html)
        self.assertIn("hostile &lt;b&gt;name&lt;/b&gt; | pipe[jvm]", html)
        self.assertIn("&lt;script&gt;alert(3)&lt;/script&gt;", html)
        self.assertIn("Grüße — 日本語 ✓", html)
        self.assertNotIn("<b>name", html)
        self.assertEqual(html.count("<details>"), html.count("</details>"))

    def test_summary_lists_failures_with_neutralised_cells(self) -> None:
        summary = self.outputs.summary
        self.assertIn("❌ Shared tests: 4 of 8 tests failed", summary)
        self.assertIn("| Target | Class | Test | Message |", summary)
        self.assertIn(
            "| jvmTest | example.CheckoutTest | discountIsApplied\\[jvm\\] "
            "| java.lang.AssertionError: expected:&lt;90&gt; but was:&lt;100&gt; |",
            summary,
        )
        hostile_row = next(line for line in summary.splitlines() if "hostile" in line)
        # Exactly the five delimiters of a four-column row: no pipe from the data survives.
        self.assertEqual(5, hostile_row.count("|"))
        self.assertIn("&lt;/details&gt;&lt;script&gt;", hostile_row)
        self.assertIn("a&#124;b \\*\\*bold\\*\\* \\[x\\](https://example.com)", hostile_row)
        self.assertIn("Grüße — 日本語 ✓", hostile_row)
        self.assertNotIn("<script", summary)
        self.assertNotIn("<img", summary)

    def test_summary_trace_excerpts_stay_one_inert_html_block(self) -> None:
        summary = self.outputs.summary
        self.assertEqual(summary.count("<details>"), summary.count("</details>"))
        self.assertEqual(4, summary.count("<details>"))
        self.assertIn("&lt;/pre&gt;&lt;/details&gt;&lt;img src=x onerror=alert(2)&gt;", summary)
        for block in re.findall(r"<details>.*?</details>", summary, re.DOTALL):
            self.assertNotIn("\n\n", block)

    def test_skipped_tests_are_reported_separately(self) -> None:
        self.assertIn("<h2>Skipped tests</h2>", self.outputs.html)
        self.assertIn("refundIsIgnored[jvm]", self.outputs.html)
        self.assertIn("1 skipped", self.outputs.summary)


class SummaryLimitsTest(unittest.TestCase):
    def many_failures(self, directory: Path, count: int, trace_lines: int) -> None:
        target = directory / "jvmTest"
        target.mkdir()
        trace = "\n".join(f"\tat example.Frame{n}.call(Frame.kt:{n}) " + "x" * 250
                          for n in range(trace_lines))
        cases = "".join(
            f'<testcase name="test{n}" classname="example.Big" time="0.1">'
            f'<failure message="boom {n}">{trace}</failure></testcase>'
            for n in range(count)
        )
        (target / "TEST-example.Big.xml").write_text(
            f'<testsuite name="example.Big">{cases}</testsuite>', encoding="utf-8"
        )

    def test_failure_rows_and_size_are_capped(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            results = Path(temp) / "results"
            results.mkdir()
            self.many_failures(results, count=500, trace_lines=400)
            outputs = Outputs(results)
            self.addCleanup(outputs.close)

            summary = outputs.summary
            self.assertEqual(0, outputs.exit_code)
            self.assertLessEqual(len(summary.encode("utf-8")), report.SUMMARY_BYTE_BUDGET + 1024)
            self.assertEqual(report.SUMMARY_FAILURE_ROWS, summary.count("| example.Big |"))
            self.assertIn("Showing the first 50 of 500 failed tests", summary)
            self.assertIn("more lines in the HTML report", summary)
            self.assertIn("test499", outputs.html)

    def test_trace_excerpts_stop_at_the_byte_budget(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            results = Path(temp) / "results"
            results.mkdir()
            self.many_failures(results, count=50, trace_lines=15)
            original = report.SUMMARY_BYTE_BUDGET
            report.SUMMARY_BYTE_BUDGET = 20 * 1024
            self.addCleanup(setattr, report, "SUMMARY_BYTE_BUDGET", original)
            outputs = Outputs(results)
            self.addCleanup(outputs.close)

            self.assertLessEqual(len(outputs.summary.encode("utf-8")), 20 * 1024 + 2)
            self.assertIn("Stack-trace excerpts stop here", outputs.summary)
            self.assertEqual(
                outputs.summary.count("<details>"), outputs.summary.count("</details>")
            )


class NoResultsTest(unittest.TestCase):
    def assert_no_results(self, results_dir: Path) -> None:
        outputs = Outputs(results_dir)
        self.addCleanup(outputs.close)
        self.assertEqual(0, outputs.exit_code)
        self.assertIn(report.NO_RESULTS_MESSAGE, outputs.summary)
        self.assertIn(report.NO_RESULTS_MESSAGE, outputs.html)

    def test_empty_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            # A stray non-report file, like Gradle's binary/ output, is not a result.
            (Path(temp) / "jvmTest").mkdir()
            (Path(temp) / "jvmTest" / "output.bin").write_bytes(b"\x00")
            self.assert_no_results(Path(temp))

    def test_missing_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            self.assert_no_results(Path(temp) / "never-created")


class OutputHandlingTest(unittest.TestCase):
    def test_summary_is_appended_not_overwritten(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            summary = Path(temp) / "summary.md"
            summary.write_text("## Earlier step\n", encoding="utf-8")
            self.assertEqual(0, report.main([str(GRADLE), "--summary", str(summary)], environ={}))
            self.assertEqual(0, report.main([str(GRADLE), "--summary", str(summary)], environ={}))
            text = summary.read_text(encoding="utf-8")
            self.assertTrue(text.startswith("## Earlier step\n"))
            self.assertEqual(2, text.count("All 11 tests passed"))

    def test_unwritable_output_is_the_only_failing_exit(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            missing_parent = Path(temp) / "absent" / "report.html"
            with contextlib.redirect_stderr(io.StringIO()) as stderr:
                exit_code = report.main([str(GRADLE), "--html", str(missing_parent)], environ={})
            self.assertEqual(1, exit_code)
            self.assertIn("cannot write HTML report", stderr.getvalue())
            self.assertFalse(missing_parent.parent.exists())

    def test_an_output_is_required(self) -> None:
        with self.assertRaises(SystemExit) as raised, contextlib.redirect_stderr(io.StringIO()):
            report.main([str(GRADLE)], environ={})
        self.assertNotEqual(0, raised.exception.code)


if __name__ == "__main__":
    unittest.main()
