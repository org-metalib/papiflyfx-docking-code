# PapiflyFX Code Editor Search/Replace — Unified Implementation Plan

This plan consolidates `review0-codex.md` (functional gaps) and `review0-gemini.md` (visual polish) into a single actionable spec for delivering a professional, IntelliJ-grade search/replace experience.

## Goals

1. **Visual parity** — compact floating overlay with modern iconography, depth, and stateful controls.
2. **Functional completeness** — preserve-case replace, in-selection scope, exclude/skip action, search history.
3. **Theme integrity** — all new elements driven by `CodeEditorTheme`, live-switching dark/light without reopen.

## Design Constraints

1. JavaFX-only; no heavy new dependencies.
2. Search behavior (query execution, shortcuts, replace semantics) remains stable.
3. Theme-aware via `CodeEditorTheme`; null/invalid theme falls back to `CodeEditorTheme.dark()`.
4. No layout jitter when toggling replace mode.
5. Keyboard flow preserved: `Enter`, `Shift+Enter`, `Escape`, `Cmd/Ctrl+F`, `Cmd+Option+F` / `Ctrl+H`.

## Priority Tiers

| Tier | What ships | PR |
|------|------------|----|
| P0 | Compact layout, toggle chips, SVG icons, stateful CSS, theme wiring | PR-1 |
| P1 | Elevation/shadow, no-results warning style, open/expand animation | PR-2 |
| P2 | Search/replace history, preserve-case, in-selection scope, exclude action | PR-3 (on product-owner approval) |

---

## Phase A — Layout & Compactness (P0)

### A1. Tighten overlay bounds

**File:** `SearchController.java`

- Set `minWidth ~520`, `prefWidth ~620`, `maxWidth ~760` so the overlay never reads as an edge-to-edge strip.
- Reduce internal padding from `(4, 8, 4, 8)` to `(2, 4, 2, 4)`.
- Reduce `HBox` spacing to `2` (use borders/separators for visual separation).
- Enforce fixed field height (22–24 px) and font size `11`–`12` for toolbar density.
- Keep top-right anchor behavior currently set in `CodeEditor`.

**Acceptance:**
- Overlay is compact and right-aligned even on wide editors.
- Expanded (replace) mode feels structured, not crowded.
- Match-count label remains legible but not visually dominant.

### A2. Rebalance control grouping

**File:** `SearchController.java`

- Left zone: query / replace input fields.
- Center zone: mode chips + result label.
- Right zone: navigation (prev/next) + close + replace actions.
- Replace row slightly denser/secondary than search row.

---

## Phase B — Stateful Visual System (P0)

### B1. Dedicated CSS stylesheet

**New file:** `search-overlay.css`
**Files:** `SearchController.java`, `CodeEditor.java`

- Add style classes: overlay root, text fields, chip toggles, icon buttons, action buttons, result label.
- Define pseudo-class rules: `:hover`, `:focused`, `:selected`, `:disabled`.
- Remove default JavaFX focus rings; implement custom focus highlight (accent-color border on active field).
- Load stylesheet once from editor/search controller init (no repeated accumulation).

**Acceptance:**
- Hover/focus/selected states are immediately visible.
- Disabled `Replace`/`Replace All` are visually distinct.

### B2. Extended theme tokens

**Files:** `CodeEditorTheme.java`, `CodeEditorThemeMapper.java`, `CodeEditorThemeMapperTest.java`

New tokens:

| Token | Purpose |
|-------|---------|
| `searchOverlayPanelBorder` | Overlay border color |
| `searchOverlayControlHoverBackground` | Hover state on buttons/chips |
| `searchOverlayControlActiveBackground` | Selected/active chip background |
| `searchOverlayControlFocusedBorder` | Focused field ring |
| `searchOverlayControlDisabledText` | Disabled control text |
| `searchOverlayNoResultsBorder` | Warning border when 0 matches |
| `searchOverlayShadowColor` | Drop-shadow color (used in P1) |
| `searchOverlayIntegratedToggleActive` | ON state for Cc/W/.* chips |
| `searchOverlayErrorBackground` | No-results field background tint |

Update `dark()` and `light()` static constructors with appropriate values.

**Acceptance:**
- Dark/light themes provide coherent contrast for all states.
- Mapper tests updated and passing.

### B3. Runtime theme propagation

**Files:** `CodeEditor.java`, `SearchController.java`, `search-overlay.css`

- Bridge theme tokens to CSS variables (`-pf-search-bg`, `-pf-search-border`, `-pf-search-control-bg`, etc.).
- `SearchController.setTheme(...)` updates CSS variable values on root node.
- SVG icons use `fillProperty().bind()` or are repainted on theme change.
- Theme update path fires on both `bindThemeProperty` new value and direct `setEditorTheme(...)`.
- No stale colors after dark ↔ light switch; no listener leaks or duplicate stylesheet entries.

**Acceptance:**
- Live theme switching updates overlay visuals without reopen.
- Integration test asserts style values change after theme switch.

---

## Phase C — Controls & Iconography (P0)

### C1. Toggle chips replace checkboxes

**File:** `SearchController.java`

- Replace `CheckBox` controls with `ToggleButton` for `Aa` (case-sensitive), `W` (whole word), `.*` (regex).
- Keep existing model wiring (`setCaseSensitive`, `setWholeWord`, `setRegexMode`).
- Selected chip state styled via CSS pseudo-class `:selected`; keyboard accessible.

**Acceptance:**
- No form-checkbox visuals remain.
- Behavior identical to current toggles.

### C2. SVG icon system

**New file:** `SearchIcons.java`
**File:** `SearchController.java`

SVG path constants:

| Icon | Usage |
|------|-------|
| Magnifying glass | Search field decoration (left-inside or leading) |
| Chevron right / down | Expand/collapse replace row |
| Arrow up / down | Previous / next match |
| Close (X) | Dismiss overlay |
| Filter (optional) | In-selection scope toggle (P2) |

Helper: `createIcon(String svgPath)` → sized `SVGPath` or `Region`.

- Replace text glyph buttons (`▲`, `▼`, `▶`, `✕`) with icon-only buttons.
- Buttons transparent by default, background on hover only.
- Hit targets remain usable (min 20×20 px).
- Icons legible in both dark and light themes (theme-driven fill).

---

## Phase D — Surface Polish (P1)

### D1. Floating panel appearance

**Files:** `SearchController.java`, `CodeEditorTheme.java`

- Rounded corners (6–8 px radius).
- Subtle `DropShadow` using `searchOverlayShadowColor` (e.g. `Color.rgb(0, 0, 0, 0.2)`).
- Remove or soften full-width accent-line look.

**Acceptance:**
- Panel reads as a floating card, not a toolbar strip.
- Edge separation visible against both theme backgrounds.

### D2. No-results and focus feedback

**Files:** `SearchController.java`, `search-overlay.css`

- When query is non-empty and match count is zero: apply warning border + tinted background on search field.
- When field is focused: strong accent-color border ring.

**Acceptance:**
- "No results" state is visible without reading label text.
- Keyboard focus is obvious during navigation.

### D3. Open/expand animation

**File:** `SearchController.java`

- Short `FadeTransition` or `TranslateTransition` on `open()` and replace-row expand/collapse.
- Duration 100–160 ms; skip animation when reopened within debounce window.

---

## Phase E — Advanced Features (P2)

### E1. Search/replace history

**Files:** `SearchModel.java`, `SearchController.java`

- `List<String> searchHistory` / `replaceHistory` in model, capped at 20 items, deduplicated.
- Small history-dropdown arrow icon next to each field.
- Clicking shows `ContextMenu` with recent queries; selecting updates field and executes search.

### E2. Preserve-case replace

**Files:** `SearchModel.java`, `SearchController.java`, `SearchModelTest.java`

- `boolean preserveCase` flag in model.
- `Aa` toggle chip in replace row.
- Casing rules applied to replacement text before insertion:
  - all lower → lower
  - Initial Capital → Initial Capital
  - ALL UPPER → ALL UPPER

**Acceptance:** Unit tests cover all three casing patterns.

### E3. In-selection search scope

**Files:** `SearchModel.java`, `SearchController.java`

- `boolean searchInSelection` flag in model.
- Toggle button with filter/selection icon in search row.
- When ON, `search()` restricts matches to `match.start >= selection.start && match.end <= selection.end`.

### E4. Exclude (skip) action

**Files:** `SearchController.java`, `SearchModel.java` (optional)

- "Exclude" / "Skip" button next to "Replace" in replace row.
- Skips current match (moves to next) without replacing — useful for manual selective-replace workflows.

---

## Testing Strategy

### Unit tests

| File | Coverage |
|------|----------|
| `SearchModelTest.java` | Preserve-case rules (E2), history behavior (E1) |
| `CodeEditorThemeMapperTest.java` | New token mappings (B2) |

### UI integration tests

| File | Coverage |
|------|----------|
| `SearchControllerFxTest.java` (new) | Overlay width bounds, toggle-chip behavior, disabled/active states, icon button wiring, focus + no-results class transitions |
| `CodeEditorIntegrationTest.java` (update) | Theme switch updates overlay styles |

### Manual QA checklist

1. `Cmd/Ctrl+F` opens compact find panel top-right.
2. `Cmd+Option+F` (mac) / `Ctrl+H` (win/linux) opens expanded replace mode.
3. Dark/light theme switching keeps contrast and icon visibility.
4. Very narrow editor widths degrade gracefully (no control overlap).
5. Search panel does not excessively occlude editor content.

---

## Architecture Decisions

### Styling model

Move from fully imperative inline Java painting to CSS-class-driven styling for stateful controls. `CodeEditorTheme` remains the color source; values are bridged to CSS via style classes + inline `setStyle(...)` variables. Use pseudo-classes for hover/focus/selected/disabled states.

### Icon strategy

JavaFX `SVGPath` icons in a dedicated `SearchIcons` utility. No mixing of glyph text arrows with icon nodes. Fallback: packaged monochrome PNG/SVG resources if needed.

### Integrated field controls (optional enhancement)

For maximum compactness, toggles (`Cc`, `W`, `.*`) can be placed *inside* the search field (right-aligned), and the magnifying glass icon *inside* (left-aligned). This requires a `StackPane`-wrapping approach or `ControlsFX` `CustomTextField`. Evaluate complexity vs. benefit during Phase C implementation.

### Theme integration contract

1. Source of truth: docking `Theme` bound via `CodeEditor.bindThemeProperty(...)`.
2. Conversion: `Theme` → `CodeEditorThemeMapper.map(...)` → `CodeEditorTheme`.
3. `CodeEditor.setEditorTheme(...)` is the single fan-out point updating `Viewport`, `GutterView`, `SearchController`.
4. `SearchController` updates CSS variables from `CodeEditorTheme` — no hardcoded colors.
5. Theme changes are live; no reopen required.
6. Null/invalid fallback: `CodeEditorTheme.dark()`.

---

## Execution Order

| Step | Content | Deliverable |
|------|---------|-------------|
| 1 | Phase A + B + C | PR-1: visual baseline, stateful controls, iconography |
| 2 | Phase D | PR-2: polish pass, validate against screenshots |
| 3 | Phase E | PR-3: advanced features (on product-owner approval) |

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| CSS + programmatic styling conflicts | Centralize style ownership in CSS; Java updates limited to semantic classes/state toggles |
| Icon visibility issues across themes | Theme-driven icon fill + explicit dark/light snapshots in tests |
| New tokens break mapper tests | Update `CodeEditorThemeMapperTest` in same PR as token additions |
| Animation causing perceived latency | Cap duration at 160 ms; skip when focus/typing happens immediately |
| Integrated-field-controls complexity | Evaluate during Phase C; fall back to adjacent chips if too complex |

## Definition of Done

1. P0 acceptance criteria fully met.
2. Tests pass: `mvn test -pl papiflyfx-docking-code`.
3. Visual diff against `intellij-idea-search-screenshot.png` shows: compact bounded panel, coherent iconography, obvious state styling, improved expanded-row rhythm.
4. No regressions in existing search/replace keyboard behavior.
5. Live theme switching updates search overlay without reopen/restart.
