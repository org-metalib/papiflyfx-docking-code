# papiflyfx-docking-code

Extracted from the PapiflyFX Docking monorepo.

## Modules

- `papiflyfx-docking-code`
- `papiflyfx-docking-code-java`
- `papiflyfx-docking-code-javascript`
- `papiflyfx-docking-code-json`
- `papiflyfx-docking-code-yaml`
- `papiflyfx-docking-code-markdown`

## Build

Use the split-local Maven repository so cross-repo snapshots resolve from the extraction workspace:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -Dtestfx.headless=true clean verify
```

Lead agent: `@feature-dev`.

## Notes

- Benchmark tests remain excluded by the `surefire.excludedGroups=benchmark` setting in the code module.
