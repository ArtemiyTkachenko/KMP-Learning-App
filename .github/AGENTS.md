# GitHub Project Automation Guide

This subtree owns backlog and GitHub Project automation, not application behavior.

- Treat `.github/project/backlog.yml` as version-controlled backlog definition data.
- Preserve stable `E##` epic keys and `E##-##` child issue keys.
- Keep issue titles free of the key; the synchronizer prefixes child issue titles.
- Preserve the generated body structure: `Issue`, `Approach`, and `Acceptance criteria`.
- Keep `Priority`, `Size`, and `initial_status` in `backlog.yml`; do not add mutable execution state such as Iteration.
- Current allowed values are enforced by `.github/project/validate_backlog.py`:
  - Priority: `P0`, `P1`, `P2`, `P3`
  - Size: `XS`, `S`, `M`, `L`
  - Initial status: `Backlog`, `Ready`, `In Progress`, `Done`
- Do not modify GitHub workflows or backlog sync behavior while implementing product code unless the issue explicitly calls for workflow work.
- For backlog changes, validate with `python .github/project/validate_backlog.py .github/project/backlog.yml`. If local `PyYAML` is missing, report that instead of pretending validation passed.
