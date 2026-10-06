"""Turn Gradle's JUnit XML test results into a browsable report and a CI run summary.

CI tooling. Gradle's own HTML test report is a folder of linked pages, so CI can only ship
it as a zip that has to be downloaded and unpacked before anyone can see which test failed.
This tool reads the JUnit XML files Gradle writes for every test task and produces:

- a single self-contained HTML file (inline CSS, no script, no fonts, no external resources)
  that a non-zipped `actions/upload-artifact` upload opens directly in the browser;
- Markdown appended to `$GITHUB_STEP_SUMMARY`, which GitHub renders on the run's Summary page.

Each subdirectory of the results directory is one Gradle test task (`jvmTest`,
`testAndroidHostTest`, `jsBrowserTest`, `wasmJsBrowserTest`, ...) and is reported as a
*target*. Failures come first, grouped by target; passing tests are collapsed.

The tool reports, it never judges: it exits 0 whenever its outputs were written, failing tests
included, because the Gradle step has already failed the job. A malformed report file is listed
and skipped, and a missing results directory produces a "no test results" message, so this step
can neither hide the real failure nor become one. It is non-zero only for a usage error or an
output it could not write. Standard library only: CI runs it on the runner's Python 3 with no
installation step, and the report must not depend on third-party code or URLs.

Usage:

    python3 tools/ci_test_report.py [results-dir] --summary <markdown-path> --html <html-path>
    python3 tools/ci_test_report.py shared/build/test-results \\
        --summary /tmp/summary.md --html /tmp/test-report.html
"""

from __future__ import annotations

import argparse
import html
import os
import re
import sys
import xml.etree.ElementTree as ElementTree
from collections.abc import Iterable, Mapping
from dataclasses import dataclass, field
from pathlib import Path

DEFAULT_RESULTS_DIR = "shared/build/test-results"

NO_RESULTS_MESSAGE = (
    "No test results were produced; the build failed before tests ran. See the Gradle step log."
)

# Gradle stops at the first failing task, so a failed build can leave some test tasks unrun.
COVERAGE_NOTE = (
    "Only test tasks that wrote results are listed; a task that did not run is absent."
)

# GitHub rejects a step summary above 1 MiB. Trace excerpts stop at this budget, which keeps
# the Summary page readable and the whole summary well under the limit.
SUMMARY_BYTE_BUDGET = 256 * 1024
SUMMARY_FAILURE_ROWS = 50
SUMMARY_TRACE_LINES = 15
SUMMARY_CELL_CHARS = 200
SUMMARY_UNREADABLE_ROWS = 20

# Per-value caps for the HTML report. Full stack traces are kept up to a generous size;
# captured output can be megabytes of logging, so it is cut much shorter.
HTML_TRACE_CHARS = 64 * 1024
HTML_OUTPUT_CHARS = 16 * 1024


@dataclass(frozen=True)
class Problem:
    """One `<failure>` or `<error>` element. A test case can carry several."""

    kind: str
    type: str
    message: str
    detail: str


@dataclass
class TestCase:
    target: str
    class_name: str
    name: str
    time: float
    skipped: bool = False
    problems: list[Problem] = field(default_factory=list)
    system_out: str = ""
    system_err: str = ""

    @property
    def failed(self) -> bool:
        return bool(self.problems)

    @property
    def errored(self) -> bool:
        return any(problem.kind == "error" for problem in self.problems)


@dataclass
class TargetTotals:
    total: int = 0
    passed: int = 0
    failed: int = 0
    errors: int = 0
    skipped: int = 0
    time: float = 0.0

    def add(self, case: TestCase) -> None:
        self.total += 1
        self.time += case.time
        if case.failed:
            self.failed += 1
            if case.errored:
                self.errors += 1
        elif case.skipped:
            self.skipped += 1
        else:
            self.passed += 1


@dataclass
class Results:
    cases: list[TestCase] = field(default_factory=list)
    # Repository- or results-relative paths of report files that could not be parsed.
    unreadable: list[str] = field(default_factory=list)
    report_files: int = 0

    @property
    def has_results(self) -> bool:
        return self.report_files > 0

    def by_target(self) -> dict[str, TargetTotals]:
        totals: dict[str, TargetTotals] = {}
        for case in self.cases:
            totals.setdefault(case.target, TargetTotals()).add(case)
        return dict(sorted(totals.items()))

    def overall(self) -> TargetTotals:
        overall = TargetTotals()
        for case in self.cases:
            overall.add(case)
        return overall

    def failures(self) -> list[TestCase]:
        return sorted(
            (case for case in self.cases if case.failed),
            key=lambda case: (case.target, case.class_name, case.name),
        )


# ---------------------------------------------------------------------------------------
# Input
# ---------------------------------------------------------------------------------------


def target_of(report: Path, results_dir: Path) -> str:
    parts = report.relative_to(results_dir).parts
    return parts[0] if len(parts) > 1 else results_dir.name


def parse_time(value: str | None) -> float:
    try:
        return max(float(value), 0.0) if value else 0.0
    except ValueError:
        return 0.0


def text_of(element: ElementTree.Element | None) -> str:
    return (element.text or "") if element is not None else ""


def parse_suite(suite: ElementTree.Element, target: str) -> list[TestCase]:
    # Gradle writes captured output once per suite unless outputPerTestCase is enabled, so a
    # failing test without its own output is shown with its suite's.
    suite_out = text_of(suite.find("system-out"))
    suite_err = text_of(suite.find("system-err"))
    cases: list[TestCase] = []
    for element in suite.iter("testcase"):
        case = TestCase(
            target=target,
            class_name=element.get("classname") or suite.get("name") or "(unknown class)",
            name=element.get("name") or "(unnamed test)",
            time=parse_time(element.get("time")),
            skipped=element.find("skipped") is not None,
            system_out=text_of(element.find("system-out")) or suite_out,
            system_err=text_of(element.find("system-err")) or suite_err,
        )
        for problem in element:
            if problem.tag in ("failure", "error"):
                case.problems.append(
                    Problem(
                        kind=problem.tag,
                        type=problem.get("type") or "",
                        message=problem.get("message") or "",
                        detail=problem.text or "",
                    )
                )
        cases.append(case)
    return cases


def read_results(results_dir: Path) -> Results:
    results = Results()
    if not results_dir.is_dir():
        return results
    for report in sorted(results_dir.glob("**/TEST-*.xml")):
        if not report.is_file():
            continue
        results.report_files += 1
        display = report.relative_to(results_dir).as_posix()
        try:
            root = ElementTree.parse(report).getroot()
        except (ElementTree.ParseError, OSError, UnicodeDecodeError, ValueError):
            results.unreadable.append(display)
            continue
        if root.tag == "testsuite":
            suites: Iterable[ElementTree.Element] = [root]
        elif root.tag == "testsuites":
            suites = root.iter("testsuite")
        else:
            results.unreadable.append(display)
            continue
        target = target_of(report, results_dir)
        for suite in suites:
            results.cases.extend(parse_suite(suite, target))
    return results


# ---------------------------------------------------------------------------------------
# Shared formatting
# ---------------------------------------------------------------------------------------


def seconds(value: float) -> str:
    if value >= 60:
        minutes, rest = divmod(value, 60)
        return f"{int(minutes)}m {rest:.1f}s"
    return f"{value:.2f}s" if value < 10 else f"{value:.1f}s"


def first_line(text: str) -> str:
    for line in text.splitlines():
        if line.strip():
            return line.strip()
    return ""


def headline(case: TestCase) -> str:
    """The most useful one-line description of why a test failed."""
    for problem in case.problems:
        line = first_line(problem.message) or first_line(problem.detail) or problem.type
        if line:
            return line
    return "(no failure message)"


def truncate(text: str, limit: int) -> tuple[str, bool]:
    if len(text) <= limit:
        return text, False
    return text[:limit], True


@dataclass(frozen=True)
class RunContext:
    repository: str
    sha: str
    ref_name: str
    run_url: str

    @property
    def is_local(self) -> bool:
        return not (self.repository or self.sha or self.ref_name or self.run_url)


def run_context(environ: Mapping[str, str]) -> RunContext:
    repository = environ.get("GITHUB_REPOSITORY", "")
    server = environ.get("GITHUB_SERVER_URL", "")
    run_id = environ.get("GITHUB_RUN_ID", "")
    run_url = ""
    # Only an https server URL becomes a link; anything else would be an odd href to emit.
    if server.startswith("https://") and repository and run_id:
        run_url = f"{server.rstrip('/')}/{repository}/actions/runs/{run_id}"
    return RunContext(
        repository=repository,
        sha=environ.get("GITHUB_SHA", ""),
        ref_name=environ.get("GITHUB_REF_NAME", ""),
        run_url=run_url,
    )


def status_text(results: Results, overall: TargetTotals) -> str:
    if not results.has_results:
        return "No test results"
    if overall.failed:
        return f"{overall.failed} of {overall.total} tests failed"
    if results.unreadable:
        return f"{overall.passed} tests passed; {len(results.unreadable)} reports unreadable"
    return f"All {overall.passed} tests passed"


# ---------------------------------------------------------------------------------------
# HTML report
# ---------------------------------------------------------------------------------------

# Light and dark through prefers-color-scheme. No fonts are loaded: the system stacks are
# enough, and the report must make no network request when opened.
HTML_STYLE = """
:root {
  color-scheme: light dark;
  --bg: #ffffff; --fg: #1f2328; --muted: #59636e; --border: #d1d9e0;
  --surface: #f6f8fa; --fail: #cf222e; --fail-bg: #ffebe9; --pass: #1a7f37;
  --pass-bg: #dafbe1; --warn: #9a6700; --warn-bg: #fff8c5; --link: #0969da;
}
@media (prefers-color-scheme: dark) {
  :root {
    --bg: #0d1117; --fg: #e6edf3; --muted: #9198a1; --border: #3d444d;
    --surface: #151b23; --fail: #ff7b72; --fail-bg: #3c1618; --pass: #3fb950;
    --pass-bg: #12261e; --warn: #d29922; --warn-bg: #2e2a1a; --link: #4493f8;
  }
}
* { box-sizing: border-box; }
body {
  margin: 0 auto; padding: 24px 16px 48px; max-width: 1100px;
  background: var(--bg); color: var(--fg);
  font: 15px/1.5 system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
}
a { color: var(--link); }
h1 { font-size: 1.5rem; margin: 0 0 8px; }
h2 { font-size: 1.2rem; margin: 32px 0 12px; }
h3 { font-size: 1.05rem; margin: 20px 0 8px; }
.banner { border-radius: 8px; padding: 16px; margin-bottom: 16px; border: 1px solid var(--border); }
.banner.fail { background: var(--fail-bg); border-color: var(--fail); }
.banner.pass { background: var(--pass-bg); border-color: var(--pass); }
.banner.warn { background: var(--warn-bg); border-color: var(--warn); }
.status { font-size: 1.25rem; font-weight: 600; }
.totals { display: flex; flex-wrap: wrap; gap: 8px 24px; margin-top: 8px; }
.context { color: var(--muted); font-size: 0.9rem; margin-top: 8px; overflow-wrap: anywhere; }
.table-wrap { overflow-x: auto; }
table { border-collapse: collapse; width: 100%; font-size: 0.95rem; }
th, td { text-align: left; padding: 6px 10px; border-bottom: 1px solid var(--border); }
th { background: var(--surface); }
td.num, th.num { text-align: right; font-variant-numeric: tabular-nums; }
.failure { border: 1px solid var(--border); border-left: 4px solid var(--fail);
  border-radius: 6px; padding: 12px; margin: 12px 0; background: var(--surface); }
.failure .test { font-weight: 600; overflow-wrap: anywhere; }
.failure .class { color: var(--muted); font-size: 0.9rem; overflow-wrap: anywhere; }
.message { white-space: pre-wrap; overflow-wrap: anywhere; margin: 8px 0 0; color: var(--fail); }
.kind { display: inline-block; font-size: 0.75rem; font-weight: 600; text-transform: uppercase;
  padding: 0 6px; border-radius: 4px; border: 1px solid var(--fail); color: var(--fail); }
details { margin: 8px 0; }
summary { cursor: pointer; }
pre { background: var(--bg); border: 1px solid var(--border); border-radius: 6px; padding: 10px;
  overflow-x: auto; font: 12.5px/1.45 ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  white-space: pre; }
.note { color: var(--warn); font-size: 0.9rem; }
.muted { color: var(--muted); }
ul.tests { margin: 4px 0 12px; padding-left: 20px; }
ul.tests li { overflow-wrap: anywhere; }
"""


def esc(value: object) -> str:
    return html.escape(str(value), quote=True)


def html_totals_table(totals: Mapping[str, TargetTotals]) -> str:
    rows = "".join(
        f"<tr><td>{esc(target)}</td><td class=num>{t.total}</td><td class=num>{t.passed}</td>"
        f"<td class=num>{t.failed}</td><td class=num>{t.skipped}</td>"
        f"<td class=num>{esc(seconds(t.time))}</td></tr>"
        for target, t in totals.items()
    )
    return (
        "<div class=table-wrap><table><thead><tr><th>Target</th><th class=num>Total</th>"
        "<th class=num>Passed</th><th class=num>Failed</th><th class=num>Skipped</th>"
        f"<th class=num>Time</th></tr></thead><tbody>{rows}</tbody></table></div>"
    )


def html_capped_pre(text: str, limit: int, label: str) -> str:
    shown, cut = truncate(text, limit)
    note = (
        f"<p class=note>Truncated: showing the first {limit:,} of {len(text):,} characters.</p>"
        if cut
        else ""
    )
    return f"<details><summary>{esc(label)}</summary><pre>{esc(shown)}</pre>{note}</details>"


def html_failure(case: TestCase) -> str:
    parts = [
        "<div class=failure>",
        f"<div class=test>{esc(case.name)}</div>",
        f"<div class=class>{esc(case.class_name)} &middot; {esc(seconds(case.time))}</div>",
    ]
    for problem in case.problems:
        message = problem.message or first_line(problem.detail) or "(no failure message)"
        type_note = f" <span class=muted>{esc(problem.type)}</span>" if problem.type else ""
        parts.append(
            f"<p class=message><span class=kind>{esc(problem.kind)}</span>{type_note}\n"
            f"{esc(message)}</p>"
        )
        if problem.detail.strip():
            parts.append(html_capped_pre(problem.detail, HTML_TRACE_CHARS, "Stack trace"))
    if case.system_out.strip():
        parts.append(html_capped_pre(case.system_out, HTML_OUTPUT_CHARS, "Standard output"))
    if case.system_err.strip():
        parts.append(html_capped_pre(case.system_err, HTML_OUTPUT_CHARS, "Standard error"))
    parts.append("</div>")
    return "".join(parts)


def html_grouped_list(cases: list[TestCase], heading: str) -> str:
    by_target: dict[str, dict[str, list[TestCase]]] = {}
    for case in sorted(cases, key=lambda c: (c.target, c.class_name, c.name)):
        by_target.setdefault(case.target, {}).setdefault(case.class_name, []).append(case)
    sections = []
    for target, classes in by_target.items():
        count = sum(len(tests) for tests in classes.values())
        body = "".join(
            f"<h3>{esc(class_name)}</h3><ul class=tests>"
            + "".join(
                f"<li>{esc(case.name)} <span class=muted>{esc(seconds(case.time))}</span></li>"
                for case in tests
            )
            + "</ul>"
            for class_name, tests in classes.items()
        )
        sections.append(f"<details><summary>{esc(target)} ({count})</summary>{body}</details>")
    return (
        f"<details><summary><strong>{esc(heading)} ({len(cases)})</strong></summary>"
        f"{''.join(sections)}</details>"
    )


def render_html(results: Results, context: RunContext) -> str:
    overall = results.overall()
    totals = results.by_target()
    failures = results.failures()

    if not results.has_results:
        tone = "warn"
    elif overall.failed:
        tone = "fail"
    elif results.unreadable:
        tone = "warn"
    else:
        tone = "pass"

    if context.is_local:
        context_line = "Local run"
    else:
        bits = []
        if context.repository:
            bits.append(esc(context.repository))
        if context.ref_name:
            bits.append(f"ref {esc(context.ref_name)}")
        if context.sha:
            bits.append(f"commit {esc(context.sha[:12])}")
        if context.run_url:
            bits.append(f'<a href="{esc(context.run_url)}">workflow run</a>')
        context_line = " &middot; ".join(bits)

    body = [
        "<h1>Shared test report</h1>",
        f"<div class='banner {tone}'><div class=status>{esc(status_text(results, overall))}</div>",
    ]
    if results.has_results:
        # A test with an <error> counts as failed too; "errors" says how many of the failed did.
        body.append(
            "<div class=totals>"
            f"<span><strong>{overall.total}</strong> tests</span>"
            f"<span><strong>{overall.failed}</strong> failed</span>"
            f"<span><strong>{overall.errors}</strong> errors</span>"
            f"<span><strong>{overall.skipped}</strong> skipped</span>"
            f"<span><strong>{esc(seconds(overall.time))}</strong> total test time</span>"
            "</div>"
        )
    body.append(f"<div class=context>{context_line}</div></div>")

    if not results.has_results:
        body.append(f"<p>{esc(NO_RESULTS_MESSAGE)}</p>")

    if failures:
        body.append("<h2>Failures</h2>")
        current_target = None
        for case in failures:
            if case.target != current_target:
                current_target = case.target
                body.append(f"<h3>{esc(case.target)}</h3>")
            body.append(html_failure(case))

    if results.unreadable:
        body.append("<h2>Unreadable reports</h2><ul>")
        body.extend(f"<li>unreadable report: {esc(path)}</li>" for path in results.unreadable)
        body.append("</ul>")

    if totals:
        body.append("<h2>By target</h2>")
        body.append(html_totals_table(totals))
        body.append(f"<p class=muted>{esc(COVERAGE_NOTE)}</p>")

    skipped = [case for case in results.cases if case.skipped and not case.failed]
    if skipped:
        body.append("<h2>Skipped tests</h2>")
        body.append(html_grouped_list(skipped, "Skipped tests"))

    passed = [case for case in results.cases if not case.failed and not case.skipped]
    if passed:
        body.append("<h2>Passing tests</h2>")
        body.append(html_grouped_list(passed, "Passing tests"))

    return (
        "<!doctype html>\n<html lang=en>\n<head>\n<meta charset=utf-8>\n"
        '<meta name=viewport content="width=device-width, initial-scale=1">\n'
        f"<title>{esc('Test report — ' + status_text(results, overall))}</title>\n"
        f"<style>{HTML_STYLE}</style>\n</head>\n<body>\n"
        + "\n".join(body)
        + "\n</body>\n</html>\n"
    )


# ---------------------------------------------------------------------------------------
# Markdown summary
# ---------------------------------------------------------------------------------------

# Characters that start inline Markdown constructs: emphasis, code spans, links and
# strikethrough. `<`, `>` and `&` become entities instead, and `|` would end a table cell.
MARKDOWN_INLINE = re.compile(r"([\\`*_\[\]~])")


def md_cell(value: str, limit: int = SUMMARY_CELL_CHARS) -> str:
    """Render untrusted text as inert, single-line table-cell content."""
    text = " ".join(value.split())
    text, cut = truncate(text, limit)
    text = MARKDOWN_INLINE.sub(r"\\\1", text)
    text = html.escape(text, quote=False).replace("|", "&#124;")
    return text + ("…" if cut else "")


def md_totals_table(totals: Mapping[str, TargetTotals]) -> list[str]:
    lines = [
        "| Target | Total | Passed | Failed | Skipped | Time |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    lines.extend(
        f"| {md_cell(target)} | {t.total} | {t.passed} | {t.failed} | {t.skipped} "
        f"| {seconds(t.time)} |"
        for target, t in totals.items()
    )
    return lines


def md_trace_block(case: TestCase) -> str:
    """A collapsed trace excerpt that stays one raw-HTML block.

    The excerpt is HTML-escaped inside `<pre>`, so nothing in it is markup. Blank lines are
    dropped because a blank line would end the HTML block and hand the remaining trace lines
    to the Markdown parser.
    """
    detail = "\n".join(problem.detail or problem.message for problem in case.problems)
    lines = [line for line in detail.splitlines() if line.strip()]
    excerpt = [truncate(line, 300)[0] for line in lines[:SUMMARY_TRACE_LINES]]
    if len(lines) > SUMMARY_TRACE_LINES:
        excerpt.append(f"... {len(lines) - SUMMARY_TRACE_LINES} more lines in the HTML report")
    label = f"{case.target} · {case.class_name} · {case.name}"
    return (
        f"<details><summary>{html.escape(label)}</summary>\n"
        f"<pre>{html.escape(chr(10).join(excerpt) or '(no stack trace)')}</pre>\n"
        "</details>\n"
    )


def render_summary(results: Results) -> str:
    overall = results.overall()
    totals = results.by_target()
    failures = results.failures()

    if not results.has_results:
        return f"### ⚠️ Shared tests: no results\n\n{NO_RESULTS_MESSAGE}\n"

    icon = "❌" if overall.failed else ("⚠️" if results.unreadable else "✅")
    extras = []
    if overall.errors:
        extras.append(f"{overall.errors} with errors")
    if overall.skipped:
        extras.append(f"{overall.skipped} skipped")
    extra = f" ({', '.join(extras)})" if extras else ""
    lines = [
        f"### {icon} Shared tests: {status_text(results, overall)}{extra}"
        f" in {seconds(overall.time)}",
        "",
    ]
    lines.extend(md_totals_table(totals))
    lines.append("")
    lines.append(COVERAGE_NOTE)
    lines.append("")

    if results.unreadable:
        lines.append("**Unreadable reports**")
        lines.append("")
        lines.extend(
            f"- unreadable report: {md_cell(path)}"
            for path in results.unreadable[:SUMMARY_UNREADABLE_ROWS]
        )
        hidden = len(results.unreadable) - SUMMARY_UNREADABLE_ROWS
        if hidden > 0:
            lines.append(f"- … and {hidden} more; see the HTML report.")
        lines.append("")

    if not failures:
        return "\n".join(lines)

    lines.extend(
        [
            "#### Failed tests",
            "",
            "| Target | Class | Test | Message |",
            "| --- | --- | --- | --- |",
        ]
    )
    shown = failures[:SUMMARY_FAILURE_ROWS]
    lines.extend(
        f"| {md_cell(case.target)} | {md_cell(case.class_name)} | {md_cell(case.name)} "
        f"| {md_cell(headline(case))} |"
        for case in shown
    )
    lines.append("")
    if len(failures) > len(shown):
        lines.append(
            f"Showing the first {len(shown)} of {len(failures)} failed tests. "
            "The browsable HTML report lists every failure."
        )
        lines.append("")

    text = "\n".join(lines)
    budget_note = (
        "\nStack-trace excerpts stop here to keep this summary small; "
        "the browsable HTML report has every trace.\n"
    )
    blocks = []
    used = len(text.encode("utf-8")) + len(budget_note.encode("utf-8"))
    for case in shown:
        block = md_trace_block(case)
        size = len(block.encode("utf-8")) + 1
        if used + size > SUMMARY_BYTE_BUDGET:
            return text + "\n" + "\n".join(blocks) + budget_note
        blocks.append(block)
        used += size
    return text + "\n" + "\n".join(blocks)


# ---------------------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------------------


def write_outputs(
    results: Results,
    context: RunContext,
    summary_path: Path | None,
    html_path: Path | None,
) -> list[str]:
    """Write each requested output; return a description of every write that failed."""
    errors = []
    if html_path is not None:
        try:
            html_path.write_text(render_html(results, context), encoding="utf-8")
        except OSError as error:
            errors.append(f"cannot write HTML report {html_path}: {error}")
    if summary_path is not None:
        try:
            # Appended, never overwritten: $GITHUB_STEP_SUMMARY may already hold other output.
            with summary_path.open("a", encoding="utf-8") as summary:
                summary.write("\n" + render_summary(results).rstrip("\n") + "\n")
        except OSError as error:
            errors.append(f"cannot write summary {summary_path}: {error}")
    return errors


def main(argv: list[str] | None = None, environ: Mapping[str, str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Render Gradle JUnit XML results as an HTML report and a Markdown summary."
    )
    parser.add_argument(
        "results_dir",
        nargs="?",
        default=DEFAULT_RESULTS_DIR,
        type=Path,
        help=f"directory holding <task>/TEST-*.xml files (default: {DEFAULT_RESULTS_DIR})",
    )
    parser.add_argument("--summary", type=Path, help="Markdown file to append the summary to")
    parser.add_argument("--html", type=Path, help="path of the self-contained HTML report")
    args = parser.parse_args(argv)
    if args.summary is None and args.html is None:
        parser.error("at least one of --summary or --html is required")

    results = read_results(args.results_dir)
    context = run_context(os.environ if environ is None else environ)
    errors = write_outputs(results, context, args.summary, args.html)
    for error in errors:
        print(f"ci_test_report: {error}", file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
