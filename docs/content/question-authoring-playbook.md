# Question Authoring Playbook

## Purpose

`docs/content/content-authoring.md` is the editorial **contract**: it defines the rules a
question must satisfy. This playbook is the **method** for satisfying them. It
records how questions are actually written and audited, and it exists because the
contract's rule "incorrect answers should be plausible distractors" turned out to
be the hardest one to follow and the easiest one to believe you had followed.

Read the contract first. Where the two documents disagree, the contract wins.

`docs/content/question-validation.md` is the third piece: the acceptance standard that says
when a finished question is allowed to ship. This playbook tells you how to write one; that
document tells you how to prove it is correct.

This playbook applies to authoring new questions and to reviewing existing ones.
For *what the bank already covers* — per-subtopic counts, the concept index,
which empty subtopics are intentional, and the current audit baselines — see
`docs/content/question-bank-coverage.md` rather than re-reading the JSON.

## The failure mode this exists to prevent

A full audit of the initial 90-question bank found that the correct answers were
generally well written, but the distractors were not. The result was an
assessment that could be passed without Android knowledge.

Two cues were measurable across the whole bank:

| Cue | Measured |
| --- | --- |
| Correct answer was the longest option | 68 of 86 single-answer questions (79%), against ~25% by chance |
| Correct answer ÷ mean distractor length | 1.50 |
| Absolute words in distractors ("always", "never", "only", "every", "automatically", "cannot") | 142 occurrences |
| Absolute words in correct answers | **0 occurrences, across all 95** |

The second pair is the serious one. "Never pick the option containing an
absolute" was a *perfect* discriminator, and combined with "prefer the longest
survivor" it produced a reliable strategy requiring no subject knowledge.

The underlying cause was a recurring distractor pattern: filler that named
unrelated technology. An OkHttp question offered *"Declaring Room entities"*; a
Room DAO question offered *"Choosing Gradle build variants"*; a background-sync
question offered *"Whether the feature uses a specific button color."* Each is
dismissible on sight, so a four-option question effectively had two options.

**The lesson: distractor quality is not a finishing touch. It is most of the
work.** Budget accordingly — expect to spend more time on the three wrong
answers than on the question and the right answer combined.

## Part 1 — The question stem

A good stem:

- tests **one** identifiable concept;
- is answerable from the stem alone, without reading the options to work out what
  is being asked;
- states any assumption that changes the answer — API level, target SDK, whether
  the app is offline-first, whether the flow is hot or cold;
- prefers a decision a senior engineer actually faces over recall of a method
  name;
- carries no irrelevant detail, and no deliberate misdirection.

Prefer a short scenario to a bare definition. *"A long-lived repository needs a
Context only to access application resources. Which Context is safest to
retain?"* is better than *"What is the application Context?"* — it forces a
judgment rather than a recital.

Do not lengthen a stem to make a question appear deeper. Interview depth must
come from the required technical reasoning, not from parsing.

**Ambiguity check.** Read the stem and ask whether a defensible case exists for
any option other than the intended one. If it does, tighten the stem rather than
weakening the distractor. During the review, `storage_selection_001` asked for
state that must "survive process death" — which made `SavedStateHandle`
arguably correct. The stem became "remain available across app restarts", which
excludes it cleanly without changing what the question tests.

## Part 2 — The answer set

### The test every answer set must pass

> A candidate who does **not** know the concept should find several options
> plausible.
> A candidate who **does** know it should be able to say exactly why the correct
> option is right and why each distractor is wrong.

If a distractor fails the first half, it is filler. If it fails the second half,
it is ambiguous. Both are defects.

### Where good distractors come from

Every distractor should be a belief a competent developer could actually hold.
Productive sources, with examples from the bank:

**Adjacent APIs that solve a neighbouring problem.**
Room DAO vs `@Entity` vs `@Database`; OkHttp interceptor vs converter factory vs
Retrofit annotations; navigation back stack vs navigation graph vs the system
task stack.

**The other member of a pair.** Where a concept has a natural opposite, the
opposite's properties are ideal distractors. For "what makes a Flow cold?", all
three distractors are genuine *hot*-flow properties: replays the latest value,
shares one producer, keeps emitting with no collectors. A candidate who does not
know the distinction has nothing to eliminate.

**A real API misapplied.** `StrictMode` "blocks disk access on the main thread in
release builds" — StrictMode is real and related, but it reports in development
rather than enforcing in release. `onTrimMemory()` "clears static caches on the
app's behalf" — real callback, but the app must implement it.

**Documented behaviour inverted.** `SideEffect` running *before* composition is
applied; `onDestroyView()` being skipped when a Fragment goes onto the back
stack; hoisting state as high as possible to *reduce* recomposition.

**A correct statement about the wrong subject.** For a plain `Job` parent, "keeps
its other children running and reports the failure to a handler" is exactly what
`SupervisorJob` does — right mechanism, wrong question.

**A claim that fails on one condition.** `copy()` on a data class performing a
*deep* copy; a data class generating `equals()` over properties declared in the
class *body*.

### What disqualifies a distractor

- **Off-topic technology.** If the option names a technology the question is not
  about, it is almost always filler. This single pattern accounted for the
  majority of weak distractors in the original bank.
- **Impossible platform behaviour.** *"The garbage collector never runs in
  Android apps."*
- **Contradicting basic programming knowledge.** *"Because logs cannot contain
  strings."*
- **Joke or throwaway options.** *"Whether the feature uses a specific button
  color."*
- **Invented APIs.** Every type, function, and permission named must exist. Check
  it.
- **Defensibly correct.** Plausible is not the same as arguable. If a senior
  engineer could mount a reasonable defence of a distractor under the stem as
  written, the question is broken — fix the stem.

### Category parity

All options must answer the same *kind* of question. If the stem asks which
mechanism should own state, every option must be a state-ownership mechanism.

The worst offender in the bank was a storage question whose options were
*"An Activity field" / "DataStore" / "A RecyclerView adapter" / "A Compose
remember block only"* — a mix of a field, a library, a UI widget, and an API,
where only one was a storage mechanism at all. It became four storage options:
`SavedStateHandle`, `DataStore`, `Room`, and a file in `cacheDir` — each a real
choice a developer might make, each wrong for a specific, teachable reason.

### Style parity

Options should match in grammatical form, technical vocabulary, specificity, and
degree of qualification. If the correct answer needs a qualifier ("depending on
the dispatcher", "unless configured otherwise"), give at least one distractor a
qualifier of comparable weight.

Aim for **stylistic parity, not uniform word counts.** Do not pad an option to hit
a character target.

## Classifying Question Interview Level

The authoritative definitions and boundary rules live in the
[Question Interview Level](content-authoring.md#question-interview-level)
section of the content-authoring contract. This section is the working method
for applying that contract. The finished question must author the resulting
required `level` value in JSON; the one-sentence review justification remains
reporting-only and is not stored in production curriculum.

Classify the finished question after its stem, correct answer, and plausible
distractors are stable, and before the final anti-cue review. The real options
matter: a stem that sounds architectural may require only a direct contract if
its distractors can be eliminated without deeper reasoning.

### Minimum-sufficient-knowledge test

Write one sentence answering:

> What is the minimum technical reasoning a candidate needs to eliminate the
> distractors and justify the correct answer?

Then classify that reasoning:

- one direct documented contract -> `FOUNDATION` candidate;
- known behavior applied to realistic constraints -> `APPLIED` candidate;
- interacting mechanisms or constraints, substantial trade-offs, or a subtle
  failure trace -> `ADVANCED` candidate.

Do not classify by everything an expert could discuss after answering. The
question's explanation may teach more than the candidate needed to select the
answer.

### Classification workflow

1. **Name the primary concept.** If it cannot be named clearly, fix the question
   before assigning a level.
2. **State the minimum reasoning.** Describe the shortest technically adequate
   path from the stem and options to the answer.
3. **Classify the reasoning.** Decide whether it is primarily a documented
   contract, application to a scenario, or interaction/trade-off/failure-mode
   reasoning.
4. **Remove false signals.** Ask whether the apparent level comes only from long
   wording, answer length, explanation depth, obscure terminology, a rare API,
   sophisticated distractors, `MULTIPLE` selection, answer count, Topic, or
   familiarity. None of those raises the level.
5. **Check the nearest boundary.** Compare against the rules and same-concept
   progressions below and in the contract.
6. **Record a one-sentence justification for review.** It is review evidence,
   not a production JSON property.

### Quick boundary checks

**Foundation or Applied?** Remove the scenario details mentally. If one direct
contract still supplies the answer, use `FOUNDATION`. If the details determine
which known mechanism, owner, or correction fits, use `APPLIED`.

**Applied or Advanced?** Ask whether the candidate is applying one known pattern
under direct requirements, or must combine mechanisms, trace a subtle failure,
or compare viable choices across several consequences. Use `ADVANCED` only for
the latter.

**Deep fact or advanced reasoning?** Recall of one obscure internal fact is not
advanced. Common mechanisms can require advanced reasoning when their
interaction must be traced. Obscurity and depth are independent.

**Debugging?** A direct symptom-to-contract mapping can be foundation. Choosing
the likely cause or fix in a realistic situation is applied. Tracing interacting
lifecycle, state, concurrency, or system constraints can be advanced.

**System design?** A layer-ownership choice can be applied. Use advanced only
when several explicit concerns, such as offline behavior, consistency,
background work, recovery, modularity, or testability, materially determine the
answer.

**Single or multiple selection?** Ignore selection mode when determining level.
`MULTIPLE` does not rank above `SINGLE`, and the number of correct options is not
a depth signal.

### Same-concept calibration

| Concept | `FOUNDATION` | `APPLIED` | `ADVANCED` |
| --- | --- | --- | --- |
| Coroutine failure | Predict child cancellation for an ordinary parent `Job`. | Choose supervision because independent siblings must continue. | Trace nested supervision, `async`, awaiting, sibling lifetime, and exception observation. |
| State restoration | Identify the mechanism for a small restorable screen value. | Split a form's small saved state from large reloadable data. | Reconcile saved state, repository persistence, navigation state, and conflict rules. |
| Compose recomposition | Predict what follows when an observed state read changes. | Place keyed memoization or derived state for a described repeated computation. | Redesign state reads and invalidation boundaries under frequency, consistency, and performance constraints. |

The progression is in the required reasoning, not the word count. A concise
advanced concept must still become an unambiguous stem with enough constraints
to make its keyed answer defensible.

### Resolving reviewer disagreement

Review the stem and real options together:

1. agree on the minimum sufficient reasoning;
2. identify which scenario details actually affect the decision;
3. identify any mechanisms or constraints that truly must be combined;
4. compare the closest calibration example;
5. choose the lower level when the higher-level analysis is educational but not
   necessary to answer correctly.

If sophisticated distractors are obviously unrelated, repair them before
classification. If several answers remain defensible, tighten the requirements;
do not use ambiguity as evidence of advanced depth.

For complete-bank classification work, keep a temporary review table with the
question ID, proposed level, one-line justification, and an ambiguity flag. Use
it to make disagreement visible, but do not copy justification or ambiguity
into production curriculum. Do not adjust honest classifications to reach an
arbitrary distribution.

## Part 3 — Anti-cue audits

Run these before opening a PR. They are mechanical and catch what review misses.

### Length

Target: the correct answer is not conspicuously longest, and no option is a
significant outlier.

- **Hard limit:** the correct answer must not exceed the longest distractor by
  more than ~10%.
- **Bank-level target:** mean ratio of correct ÷ average distractor at or near
  1.0.

"Correct is longest" landing somewhat above 25% across a bank is acceptable
*provided* margins are a few characters. After revision the bank sits at 47%
with a mean ratio of 1.05 and zero questions over the 10% limit — the correct
answer is nominally longest in many questions, but never by a perceptible amount.
Chasing exactly 25% would mean padding options, which is a worse defect.

### Absolute words

Watch `always`, `never`, `only`, `every`, `completely`, `automatically`,
`guaranteed`, `cannot`, `impossible`.

The rule is **not** "remove them from distractors". Often the absolute is the
precise quantifier that makes a claim false — *"an implicit Intent matches only
filters declared in the same app"* is a good distractor *because* of "only".

The rule is: **the distribution must not be a signal.** If distractors use
absolutes and correct answers never do, the audit fails no matter how good each
individual option is. Fix it by using absolutes in correct answers **wherever
they are technically accurate**:

- "Its producer block runs **only** when a collector starts collecting" — true of
  a cold flow.
- "Cancellation is cooperative and busy code **only** stops at a check" — true.
- "A declaration that **every** platform implements itself" — true of `expect`.

Never add an absolute to a correct answer that is not literally true.

### Position

Correct answers should be spread across positions. Check the bank periodically
rather than per question; answer identity is by ID, so reordering options is
free and never changes an ID.

### Grammar

Read the stem followed by each option in turn. Singular/plural agreement, tense,
and article use must not single one out.

### The audit script

Run from the repository root:

```python
import json, re, statistics
from collections import Counter

PATH = 'shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json'
ABS = r'\b(always|never|completely|automatically|guaranteed|only|every|cannot|impossible)\b'

qs = json.load(open(PATH, encoding='utf-8'))['questions']
single = [q for q in qs if len(q['correctAnswerIds']) == 1]

longest = over = 0
ratios = []
for q in single:
    cid = q['correctAnswerIds'][0]
    lens = {a['id']: len(a['text']) for a in q['answers']}
    cl, others = lens[cid], [v for k, v in lens.items() if k != cid]
    if cl > max(others):
        longest += 1
    if cl > max(others) * 1.10:
        over += 1
        print(f'  LENGTH  {q["id"]}: correct={cl} longest_distractor={max(others)}')
    ratios.append(cl / (sum(others) / len(others)))

print(f'correct-longest {longest}/{len(single)} ({100*longest/len(single):.0f}%), '
      f'mean ratio {statistics.mean(ratios):.2f}, over 10% limit: {over}')

d = c = dn = cn = 0
for q in qs:
    correct = set(q['correctAnswerIds'])
    for a in q['answers']:
        hits = len(re.findall(ABS, a['text'], re.I))
        if a['id'] in correct:
            c += hits; cn += 1
        else:
            d += hits; dn += 1
print(f'absolutes: distractors {d/dn:.2f}/opt, correct {c/cn:.2f}/opt '
      f'(correct must not be 0.00 while distractors are high)')

pos = Counter()
for q in qs:
    ids = [a['id'] for a in q['answers']]
    for cid in q['correctAnswerIds']:
        pos[ids.index(cid)] += 1
total = sum(pos.values())
print('position:', {k: f'{100*v/total:.0f}%' for k, v in sorted(pos.items())})
```

Passing this script is necessary, not sufficient. It cannot judge whether a
distractor is plausible, whether a stem is ambiguous, or whether a source
supports its claim. Those need a human read.

## Part 4 — Correct answers

- Exactly one defensibly correct option in a `SINGLE` question.
- State the claim plainly. Do not hedge it into safety — hedging is itself a cue.
- Do not make it the most carefully worded option. If it reads as the only
  professionally written answer, that is a defect.
- Verify it against current documentation before writing the distractors. Writing
  three good distractors around a wrong answer wastes the effort.

## Part 5 — Multiple-selection questions

Use `MULTIPLE` when the candidate is intentionally allowed to select several
options. This may legitimately produce one or several correct answers; the
interaction mode must be authored independently from the answer-key count.

- The stem must say **"Select all that apply."**
- Every correct option must be independently true and every incorrect option
  independently false, each judged on its own.
- No option may depend on, imply, or overlap another.
- Keep correct and incorrect options at comparable length; the length audit
  applies here too.

## Part 6 — Explanations

An explanation teaches; it does not restate the answer.

- Say why the correct answer is correct and name the mechanism.
- Address the **most tempting** distractor explicitly. When a distractor is
  strengthened, update the explanation to disarm it — otherwise the review screen
  leaves the candidate believing the trap.
- Do not walk through all three wrong answers mechanically.
- Keep it to a few sentences; it renders in a results screen.
- Claim nothing the listed sources do not support.

Example: the cold-flow explanation ends by naming replay, shared producers, and
emitting without collectors as *hot*-flow properties — which is precisely what
the three distractors asserted.

## Part 7 — Sources

Follow the contract's hierarchy: Android Developers / AndroidX, kotlinlang.org,
Gradle, Firebase, and official library documentation. Never SEO interview sites,
blogs, or forum answers.

Two rules the review showed need stating explicitly:

**The source must establish the specific claim.** A landing page is not enough
when a precise page exists. `guide/components/fundamentals` was cited for
resource qualifiers, `ViewGroup`, the rendering pipeline, and intents — a page
that discusses none of them in the required detail. Each now points at the page
that does.

**Verify the link resolves.** During the review, `square.github.io/okhttp/` and
`square.github.io/retrofit/` were both found to return 404 — three questions were
citing dead hosts, and three replacement URLs drafted from memory *also* 404'd.
Never trust a remembered URL. Check every one:

```bash
python3 -c "
import json
d = json.load(open('shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json', encoding='utf-8'))
print('\n'.join(sorted({s['url'] for q in d['questions'] for s in q['sources']})))
" | while read -r u; do
  code=$(curl -s -o /dev/null -w '%{http_code}' -L --max-time 25 "$u")
  [ "$code" = "200" ] || echo "DEAD $code $u"
done
```

**Verify the fragment resolves too.** A URL ending in `#some-section` returns 200 even when
that section no longer exists — the reader silently lands on the top of the page. The
226–275 review ran the check below for the first time and found **12 dead anchors**, none of
which the loop above could see. Four of them pointed into `dagger.dev/dev-guide/`, whose
content had moved wholesale to `/dev-guide/basic-usage`.

```bash
python3 - <<'EOF'
import json, re, urllib.request, urllib.parse
P = 'shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json'
d = json.load(open(P, encoding='utf-8'))
anchored = {}
for i, q in enumerate(d['questions'], 1):
    for s in q['sources']:
        if '#' in s['url']:
            anchored.setdefault(s['url'], []).append(i)
cache = {}
for url, ns in sorted(anchored.items()):
    base, frag = url.split('#', 1)
    frag = urllib.parse.unquote(frag)
    if base not in cache:
        req = urllib.request.Request(base, headers={'User-Agent': 'Mozilla/5.0'})
        try:
            cache[base] = urllib.request.urlopen(req, timeout=40).read().decode('utf-8', 'ignore')
        except Exception as e:
            cache[base] = 'ERR:' + str(e)
    doc = cache[base]
    # GitHub prefixes rendered-markdown anchors with "user-content-"; both forms are valid.
    forms = [frag, 'user-content-' + frag]
    if doc.startswith('ERR:'):
        print(f'FETCH-FAIL {url}')
    elif not any(p in doc for f in forms for p in (f'id="{f}"', f"id='{f}'", f'name="{f}"')):
        print(f'MISSING-ANCHOR {url}  (questions {ns})')
EOF
```

**And check the page is not empty.** Three vendor pages in this bank returned 200 while rendering
nothing at all — `kotlinlang.org/docs/cancellation-and-timeouts.html`, and the whole
`kotlinlang.org/docs/multiplatform-*.html` set after those pages moved under
`/docs/multiplatform/`. Eleven questions cited the multiplatform pages and every check the
repository had passed them. Measure the rendered text:

```bash
python3 - <<'EOF'
import json, re, html, urllib.request, concurrent.futures
P = 'shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json'
d = json.load(open(P, encoding='utf-8'))
byurl = {}
for i, q in enumerate(d['questions'], 1):
    for s in q['sources']:
        byurl.setdefault(s['url'].split('#')[0], set()).add(i)

def body_len(u):
    try:
        req = urllib.request.Request(u, headers={'User-Agent': 'Mozilla/5.0'})
        t = urllib.request.urlopen(req, timeout=45).read().decode('utf-8', 'ignore')
    except Exception as e:
        return u, -1
    t = re.sub(r'<script.*?</script>|<style.*?</style>', '', t, flags=re.S)
    m = (re.search(r'<devsite-content.*?</devsite-content>', t, flags=re.S)
         or re.search(r'<article.*?</article>', t, flags=re.S)
         or re.search(r'<main.*?</main>', t, flags=re.S))
    b = re.sub(r'<[^>]+>', ' ', m.group(0) if m else t)
    return u, len(re.sub(r'\s+', ' ', html.unescape(b)).strip())

with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    for u, n in ex.map(body_len, sorted(byurl)):
        if n < 800:
            print(f'EMPTY-OR-STUB {n:6d}  {u}  (questions {sorted(byurl[u])})')
EOF
```

Where a project's documentation site has moved, the canonical docs in its
repository are an acceptable primary source. A third-party mirror is not: it can
disappear or drift without notice, and nothing marks it as authoritative.

**A 200 is not a verified source.** The first-100 review of 2026-09-04 found the
loop above passing two pages it should not have:
`kotlinlang.org/docs/cancellation-and-timeouts.html` returns 200 and renders
nothing at all, and `kotlinlang.org/docs/coroutines-flow.html` still returns 200
after the sections documenting `conflate` and `collectLatest` were removed from
it. Four questions cited the first and one cited the second, and every one of
them looked healthy to the script.

Two habits follow. Open the page and locate the sentence, rather than trusting
that the URL used to be right. And where a question turns on a named parameter or
a specific contract — `stopTimeoutMillis`, `conflate` versus `collectLatest`,
`Dispatchers.IO` parallelism — cite the API reference rather than the narrative
guide. Guides get restructured; reference pages track the code.

## Part 8 — Stable identity

Identity rules are in the contract; this is how they apply in practice.

**New questions.** Choose a `Question.id` naming the concept, not the wording —
`compose_recomposition_state_read`, not `compose_question_7`. Answer IDs follow
`<question_id>_a…_d`.

**Editing an existing question.** Keep `Question.id` for wording, clarity,
distractor, explanation, and source changes. A new ID plus deprecation of the old
question is for a changed concept or changed correct answer, and should be rare —
the 90-question review needed none.

Changing between `SINGLE` and `MULTIPLE` can materially change how the candidate
interacts with a question. Review stable identity in that case; adding explicit
metadata for an interaction that was already intended does not itself require a
new ID.

**Editing an answer.** This is the rule most easily got wrong:

- Keep the `AnswerOption.id` when only the wording changes and the claim is the
  same.
- Issue a **new** ID when the claim changes — replacing an implausible distractor
  with a different misconception is a new claim, even in the same slot.

Historical `QuestionAttempt` rows store selected answer IDs, so reusing an ID for
a different assertion silently corrupts past attempts. The review issued 253 new
answer IDs (suffixes `_e`, `_f`, `_g`) for exactly this reason, while 45 answers
kept their ID because only their wording changed.

Never treat list position as identity. Reordering options is safe; renaming IDs
to match a new order is not.

**Renaming an answer ID is a data migration, not just an edit.** The old row does
not disappear when the bundle stops authoring it: if any past attempt selected it,
the import must keep it to preserve the foreign key from
`question_attempt_selected_answer`. It is therefore marked `DEPRECATED` and
excluded from active curriculum queries, while `getQuestionById` still returns it
so the attempt stays reviewable. This was not true when the 90-question review
first landed, and the consequence was that retired options — the very filler the
review removed — reappeared as extra choices for upgrading users. When a change
retires answer IDs at scale, verify that the active path excludes them and that
the review path does not.

## Part 9 — Claim precision

Part 2 exists because an audit found the distractors were the weak point. The
next large batch — 90 questions added in one PR — was reviewed after that lesson
had been absorbed, and the finding was different. Nine defects were raised across
two review rounds and **none of them was a distractor-plausibility problem**.
Every one was a claim that was too broad, too vague, or simply wrong:

| Cluster | Count | Example |
| --- | --- | --- |
| Keyed answer true only under an unstated condition | 4 | "an anonymous class declared in the Activity" leaks it — only if it captures it |
| Explanation asserting something false or unsourceable | 4 | "ViewModels survive *Don't keep activities*" — they do not |
| Stem premise false, or API named for the wrong type | 2 | stem said `Worker`, keyed option used `CoroutineWorker`'s `setForeground()` |

(The clusters overlap; one question had two defects.)

**The lesson: once the distractors are good, the remaining risk moves into the
sentences you write most quickly** — the qualifying clause in a correct answer,
the setup clause in a stem, and the last two sentences of an explanation. Those
are the ones this part is about.

### The keyed answer must be true under every reading of the stem

Part 2 says no distractor may be defensibly correct. The mirror rule is just as
important and is the single largest cluster above:

> No keyed answer may be defensibly **wrong**.

A candidate who knows the material and declines a keyed option is being marked
incorrect for being right. That is worse than an easy question.

Three that failed it:

| Shipped | Why a knowledgeable candidate could decline it | Fixed to |
| --- | --- | --- |
| "Data and UI models have to be mapped to and from domain models at each layer boundary." | The dependency rule forbids the domain *naming* framework types; it does not mandate a separate model per layer. An outer layer may use a domain type directly. | "A Room entity or network DTO cannot double as the domain model, so mapping appears there." |
| "A listener registered on an app-wide singleton using an anonymous class declared in the Activity." | An object expression retains the enclosing instance only when it references it. | "…by an anonymous class that calls into the Activity." |
| Stem: "a file in `src/debug/res` that also exists under the same name in `src/main/res`" | A drawable is replaced; `values/strings.xml` is *merged* per resource name. Two different correct answers. | Stem names a drawable; the explanation uses `values/` as the contrast. |

**The test.** Read the stem, then ask of every keyed option: *can a competent
engineer construct a case, consistent with the stem, in which this is false?* If
yes, either state the missing condition in the stem or narrow the option.
Narrowing the option is usually better — it keeps the stem short and puts the
precision where the marking happens.

Watch particularly for `have to`, `must`, `always`, `every`, `cannot`, and `is
required` in a keyed answer. Part 3 says absolutes belong in correct answers
*where they are literally true*; this is the other half of that rule. An absolute
that is merely usually true is a defect, not a style choice.

### The stem's premise must itself be true

A stem is not neutral scaffolding. Every clause in it is a claim the candidate
is invited to accept, so a false premise teaches a wrong default even when the
keyed answer is right.

> "A shared Kotlin Multiplatform module **adds an iOS target**. What does the
> build produce for the Xcode project to consume?"

Adding a target registers a Kotlin/Native compilation; it produces no framework
until `binaries.framework` is configured — as this repository's own
`shared/build.gradle.kts` does. The answer set was fine; the setup was not. The
stem now says the target is declared *and* the framework binary configured.

### Name the exact type when you name an API

If an option names a method, the stem must fix the receiver, because a learner
who copies the option will try to compile it.

> Stem: "A **Worker** uploading large videos…"
>
> Keyed option: "Make it a long-running worker with `setForeground()`…"

`setForeground()` is `CoroutineWorker`'s suspending API. `Worker` and
`ListenableWorker` use `setForegroundAsync(ForegroundInfo)`. The stem now says
`CoroutineWorker`, which makes the keyed option exactly right.

The same applies to a method named only in an explanation. The explanation for
that question also offered `getForegroundInfo()` as the CoroutineWorker
alternative, which is neither — it is the callback WorkManager invokes *on* the
worker.

### Every sentence of an explanation is a claim, including the ones about distractors

The second-largest cluster, and the errors were concentrated in the closing
sentences that disarm the distractors — written last, checked least, and rarely
re-read against a source.

| Shipped | Reality |
| --- | --- |
| "*Don't keep activities* … so ViewModels survive and the bug stays hidden." | `ComponentActivity` clears its `ViewModelStore` on destroy when it is not a configuration change, so they do not survive. |
| "a finished producer closes the flow" | `callbackFlow` throws `IllegalStateException` when the block returns with the channel still open. |
| "Fragments are recreated … on every return through the back stack" | A back-stacked Fragment loses its view, not its instance — and another question in the same batch said so. |

Three rules follow.

**Name the observable failure.** When the question is about something going
wrong, say what the engineer will actually see: the exception type, the log line,
the symptom. "Closes the flow" and "throws `IllegalStateException` telling you to
call `awaitClose`" are not the same lesson, and only one of them is recognisable
at 2am.

**Delete what you cannot cite.** Part 7 governs explanations, not only answers. A
claim that is true but unsupported by any listed source is still a defect,
because nothing in review can check it. Two questions in this batch rested on
widely-repeated behaviour that no primary document states; both were reframed
onto documented behaviour rather than sourced to a blog. If the precise mechanism
is not citable, find a different true statement that is — the question usually
survives the change.

**Distinguish "wrong" from "understated".** An explanation that is directionally
right but soft is still teaching the wrong thing, and it reads as authoritative
because it sits next to correct material.

### Check the new question against its neighbours

The back-stack error above is the one worth generalising: it was not wrong in
isolation, it was wrong *relative to another question in the same PR*. Two
questions teaching opposite things is worse than either being wrong alone,
because the bank stops being trustworthy rather than merely incomplete.

Semantic-duplicate scanning does not find these — contradictions score low on
overall similarity precisely because the wording differs. What does find them is
pairing questions that share **rare** vocabulary, which is a proxy for "these
talk about the same narrow subject", and then reading each pair for agreement.

Run from the repository root before opening the PR:

```python
import json, re, subprocess
from collections import Counter

PATH = 'shared/src/commonMain/composeResources/files/curriculum/initial_curriculum.json'
BASE = 'main'          # the commit this PR branched from
THRESHOLD = 5          # shared rare terms; lower to 4 to roughly double the list

STOP = set('''a an the of to in for is are and or that this it its what which why how when does do
with on at as be by from not but if then than can may must should will would there their they them
these those into over under one two both each any all some more most other another such same own so
no nor only just also very much many'''.split())
tok = lambda t: {w for w in re.findall(r'[a-z][a-z-]{3,}', t.lower()) if w not in STOP}

cur = json.load(open(PATH, encoding='utf-8'))
base = json.loads(subprocess.run(['git', 'show', f'{BASE}:{PATH}'],
                                 capture_output=True, text=True).stdout)
old = {q['id'] for q in base['questions']}
qs = [q for q in cur['questions'] if q['status'] == 'ACTIVE']
new = {q['id'] for q in qs if q['id'] not in old}

docs = [(q['id'], tok(q['text'] + ' ' + q['explanation'])) for q in qs]
df = Counter(w for _, t in docs for w in t)

pairs = []
for i, a in enumerate(docs):
    for b in docs[i + 1:]:
        if a[0] not in new and b[0] not in new:
            continue
        shared = {w for w in a[1] & b[1] if 2 <= df[w] <= 12}
        if len(shared) >= THRESHOLD:
            pairs.append((len(shared), a[0], b[0], sorted(shared)))

print(f'{len(pairs)} pairs to read\n')
for n, x, y, shared in sorted(pairs, reverse=True):
    print(f'{n}  {x}\n   {y}\n   {shared}\n')
```

Read each pair and ask one question: *do these two say anything incompatible
about the shared subject?* Related questions are expected and fine — most pairs
will be a definition and a scenario about the same API, which is exactly the
distinct-depth the contract encourages. You are looking only for disagreement.

On the 90-question batch, compared against the commit it branched from, this
printed **33 pairs** — and the real contradiction was entry 22. That is a few
minutes of reading against a defect that otherwise reached review.

Point `BASE` at the actual branch point. Aiming it at a stale `main` marks
already-merged questions as new and roughly doubles the list without adding
signal.

## Part 10 — Pre-merge checklist

Structural (automated by the validator and existing tests):

- [ ] JSON parses; `CurriculumValidator` returns no errors
- [ ] IDs unique; every `correctAnswerId` references a real option
- [ ] At least two options, one correct answer, one source per question
- [ ] `topicId` / `subtopicId` valid and consistent
- [ ] `selectionMode` is explicitly authored
- [ ] `SINGLE` does not contain several correct answers

Editorial (human review — the validator cannot check these):

- [ ] Stem tests one concept and is answerable without reading the options
- [ ] Assumptions that change the answer are explicit
- [ ] When interview level is under review, the finished question has a
      defensible one-sentence classification justification
- [ ] The classification describes required reasoning, not learner ability,
      Topic, familiarity, selection mode, length, or obscurity
- [ ] Scenario, mechanism, and trade-off depth support the proposed level
- [ ] An `ADVANCED` classification requires deeper reasoning to choose correctly,
      not merely deeper detail in the explanation
- [ ] Every distractor is a belief a real developer could hold
- [ ] No off-topic technology, impossible behaviour, or invented API
- [ ] No distractor is defensibly correct under the stem as written
- [ ] No keyed answer is defensibly wrong under the stem as written
- [ ] Every clause of the stem is itself true, including the setup
- [ ] Any method named in an option or explanation belongs to the type the
      stem specifies
- [ ] All options share category, grammatical form, and register
- [ ] Length audit passes; correct answer within ~10% of longest distractor
- [ ] Absolute words are not a signal; correct answers use them where true
- [ ] Wording matches the authored selection interaction
- [ ] `MULTIPLE` stems say "Select all that apply."
- [ ] `MULTIPLE` may legitimately contain exactly one correct answer
- [ ] Explanation teaches and disarms the strongest distractor
- [ ] Explanation names the observable failure, not a softer paraphrase
- [ ] Every sentence of the explanation is supported by a listed source,
      including the sentences about distractors
- [ ] Neighbour scan (Part 9) read; no pair disagrees
- [ ] Every source establishes its specific claim and returns 200
- [ ] Answer IDs: preserved for wording changes, new for changed claims
- [ ] Question count and per-topic distribution match the pinned tests, or those
      tests were updated deliberately

## Appendix — Worked examples

### Off-topic filler → adjacent-API confusion

*What is a common use of an OkHttp interceptor?* (correct answer unchanged:
adding headers, observing or modifying requests and responses)

| Before | After |
| --- | --- |
| Declaring Room entities. | Defining the endpoint paths and HTTP methods for a service. |
| Rendering Compose layouts. | Converting response bodies into Kotlin data classes. |
| Replacing Gradle dependency resolution. | Configuring the connection and read timeouts for the client. |

Before, three options could be dismissed without knowing what an interceptor is.
After, all three are real responsibilities in the same stack — held by Retrofit
annotations, a converter factory, and the client builder respectively — so
answering requires knowing which layer owns which job.

### Category mismatch → parallel mechanisms

*Which storage option is appropriate for small key-value preferences that must
remain available across app restarts?* (correct: `DataStore`)

| Before | After |
| --- | --- |
| An Activity field. | `SavedStateHandle`, which restores screen state after recreation. |
| A RecyclerView adapter. | `Room`, which stores structured relational data with queries. |
| A Compose `remember` block only. | A file in the app's `cacheDir`, which the system may clear. |

Every option is now a real persistence choice, each wrong for a distinct and
teachable reason. The stem was also tightened from "survive process death" so
that `SavedStateHandle` is unambiguously excluded.

### Unsourceable claim → documented mechanism

*A Compose test asserts on a node that appears while a continuously repeating
animation is running, and the test hangs instead of failing. What is happening?*
(correct: the rule waits for the app to be idle and an endless animation never
lets that happen)

The keyed answer is very likely true. It is also stated by no Android
documentation — neither the synchronization page nor the animation-testing page
mentions infinite animations at all, and the only supporting material is forum
and blog writing. Under Part 7 that makes the question indefensible in review:
nothing a reviewer can consult decides it.

The fix was not a better source, because none exists. It was a different
question about the same subtopic, resting on behaviour the docs *do* specify
with a code sample:

*A Compose test must assert on a colour part-way through a 250 ms animation, but
the default synchronization lets the animation finish before the assertion runs.
What lets the test observe an intermediate frame?* (correct: set
`mainClock.autoAdvance` to false and step the clock with `advanceTimeBy()`)

The distractors improved as a side effect — `waitForIdle()` and `runOnIdle()`
both do the opposite of what is needed, and `waitUntil()` polls a condition
without stopping the clock, so all three are real APIs failing for teachable
reasons. When a claim cannot be cited, look for the adjacent question that can
be; it is usually the better question anyway.

### The opposite concept as distractor set

*What makes a basic Kotlin Flow cold?* (correct: its producer block runs only
when a collector starts collecting)

All three distractors became genuine hot-flow properties — replays the most
recent value to each new collector; shares a single active producer; continues
emitting after its last collector stops. Nothing can be eliminated without
knowing the hot/cold distinction, which is exactly the concept under test.
