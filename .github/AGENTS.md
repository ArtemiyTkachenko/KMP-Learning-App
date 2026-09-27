# GitHub Project Automation Guide

This subtree owns backlog data, GitHub Project synchronization, and CI — not application
behavior. Do not modify workflows or backlog sync behavior while implementing product code
unless the issue explicitly calls for workflow work.

## Backlog Data

`.github/project/backlog.yml` is version-controlled backlog definition data.

- Preserve stable `E##` epic keys and `E##-##` child issue keys.
- Keep issue titles free of the key; the synchronizer prefixes child issue titles.
- Preserve the generated body structure: `Issue`, `Approach`, and `Acceptance criteria`.
- Keep `Priority`, `Size`, and `initial_status` in `backlog.yml`. Do not add mutable
  execution state such as Iteration.
- Allowed values are enforced by `.github/project/validate_backlog.py`:
  - Priority: `P0`, `P1`, `P2`, `P3`
  - Size: `XS`, `S`, `M`, `L`
  - Initial status: `Backlog`, `Ready`, `In Progress`, `Done`

Validate a backlog change with:

```sh
python .github/project/validate_backlog.py .github/project/backlog.yml
```

If local `PyYAML` is missing, report that instead of pretending validation passed.

## Related Documentation

How a backlog key drives an implementation task:
[backlog workflow](../docs/workflows/backlog.md). What the workflows in this subtree do:
[CI](../docs/workflows/ci.md).
