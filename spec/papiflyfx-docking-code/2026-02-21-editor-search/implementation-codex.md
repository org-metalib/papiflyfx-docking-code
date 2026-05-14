# PapiflyFX Search/Replace Visual Refresh - Implementation Plan (Codex)

This plan consolidates:

- `spec/papiflyfx-docking-code-editor-search/review0-codex.md`
- `spec/papiflyfx-docking-code-editor-search/review0-gemini.md`

Goal: move the current search/replace overlay from "functional but flat" to a compact, IDE-grade experience comparable to IntelliJ while preserving current behavior.

## 1. Scope and Priorities

## P0 (Must ship first)

1. Compact bounded overlay (no full-width toolbar look).
2. Strong visual hierarchy (field-first, toggles/actions secondary).
3. Replace `CheckBox` mode toggles with chip-like toggle controls.
4. Consistent iconography for chevron, prev/next, close.
5. Proper visual states: hover, focused, selected, disabled.
6. Expanded mode spacing/rhythm cleanup.

## P1 (High-value follow-up)

1. Subtle elevation (shadow + rounded floating surface).
2. "No results" warning state styling.
3. Optional open/expand animation (short, non-distracting).

## P2 (Optional advanced parity)

1. Search and replace history dropdown.
2. Replace-row preserve-case toggle (`Aa`) if product scope accepts it.
3. "Exclude" action for selective replace workflows.
4. Optional in-selection search scope.

## 2. Design Constraints

1. Keep JavaFX-only solution; avoid heavy new dependencies.
2. Keep search behavior stable (query execution, shortcuts, replace semantics).
3. Theme-aware in both dark/light modes via `CodeEditorTheme`.
4. Avoid layout jitter when toggling replace mode.
5. Preserve keyboard flow (`Enter`, `Shift+Enter`, `Escape`, open shortcuts).

## 3. Architecture Decisions

## 3.1 Styling model

Move search overlay styling away from fully imperative inline Java painting to CSS-class-driven styling where statefulness matters.

- Keep `CodeEditorTheme` as the source of colors.
- Bridge theme values to CSS via style classes + inline `setStyle(...)` variables or targeted setter methods.
- Use pseudo-classes for hover/focus/selected/disabled states.

## 3.2 Icons

Use one icon strategy for all compact actions.

- Preferred: JavaFX `SVGPath` icons in a small utility.
- Fallback: packaged monochrome PNG/SVG resources.
- Do not mix glyph text arrows with icon nodes.

## 3.3 Control composition

- Search row stays primary.
- Replace row stays secondary and collapsible.
- Mode toggles (`Aa`, `W`, `.*`) become `ToggleButton` chips (not `CheckBox`).
- Maintain current semantics from `SearchModel`.

## 3.4 Theme integration contract

Theme integration must be explicit and runtime-safe:

1. Source of truth remains docking `Theme` bound via `CodeEditor.bindThemeProperty(...)`.
2. Conversion flow remains:
   - `Theme` -> `CodeEditorThemeMapper.map(...)` -> `CodeEditorTheme`.
3. `CodeEditor.setEditorTheme(...)` remains the single fan-out point that updates:
   - `Viewport`
   - `GutterView`
   - `SearchController`
4. `SearchController` exposes and updates CSS variables/style state from `CodeEditorTheme` (not hardcoded colors).
5. Theme changes are live; opening/closing search overlay is not required to refresh styles.
6. Null/invalid theme fallback remains deterministic (`CodeEditorTheme.dark()`).

## 4. File-Level Implementation Plan

## Phase A - Overlay Structure and Layout (P0)

### A1. Tighten panel bounds and width behavior

Files:

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`

Changes:

1. Introduce stable width policy for overlay:
   - `minWidth` around 520.
   - `prefWidth` around 620.
   - `maxWidth` around 760.
2. Ensure root does not visually read as edge-to-edge strip.
3. Keep top-right anchor behavior currently set in `CodeEditor`.

Acceptance:

1. Search overlay no longer spans nearly full editor width.
2. On wide editor sizes, overlay remains compact and aligned top-right.

### A2. Rebalance spacing and rhythm

Files:

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`

Changes:

1. Reduce vertical padding/row spacing to IDE toolbar density.
2. Explicitly group controls:
   - Left: query/replace input.
   - Middle: mode chips + result label.
   - Right: navigation + close + replace actions.
3. Keep replace row slightly denser/secondary than search row.

Acceptance:

1. Expanded mode feels structured rather than crowded.
2. Match count remains legible and not visually dominant.

## Phase B - Stateful Visual System (P0)

### B1. Add dedicated CSS for search overlay

Files:

- New: `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/search/search-overlay.css`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`

Changes:

1. Add style classes for:
   - overlay root
   - text fields
   - chip toggles
   - icon buttons
   - action buttons
   - result label
2. Define pseudo-class styles:
   - `:hover`
   - `:focused`
   - `:selected`
   - `:disabled`
3. Load stylesheet once from editor/search controller initialization.

Acceptance:

1. Hover/focus/selected states are immediately visible.
2. Disabled `Replace`/`Replace All` are visually distinct.

### B2. Extend theme tokens for overlay states

Files:

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`

Changes:

Add tokens for stateful styling, for example:

1. `searchOverlayPanelBorder`
2. `searchOverlayControlHoverBackground`
3. `searchOverlayControlActiveBackground`
4. `searchOverlayControlFocusedBorder`
5. `searchOverlayControlDisabledText`
6. `searchOverlayNoResultsBorder`
7. `searchOverlayShadowColor` (used in P1)

Acceptance:

1. Dark/light themes provide coherent contrast for all states.
2. Theme mapper tests updated and passing.

### B3. Wire runtime theme propagation to search CSS variables

Files:

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/search/search-overlay.css`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeIntegrationTest.java`

Changes:

1. Add CSS variable bridge for search overlay tokens (for example `-pf-search-bg`, `-pf-search-border`, `-pf-search-control-bg`, etc.).
2. In `SearchController.setTheme(...)`, update:
   - CSS variable values on root node
   - pseudo-class state where needed (`no-results`, `replace-mode`)
3. Ensure stylesheet is loaded exactly once per scene/editor instance (no repeated stylesheet accumulation).
4. Ensure theme update path is executed when:
   - `bindThemeProperty` receives new `Theme`
   - `setEditorTheme(...)` is called directly
5. Add integration tests that assert search overlay style values change after theme switch.

Acceptance:

1. Switching docking theme dark <-> light immediately updates search overlay visuals.
2. No stale colors remain on toggles/buttons/labels after theme change.
3. Rebinding/unbinding theme property does not leak listeners or duplicate stylesheet entries.

## Phase C - Controls and Iconography (P0)

### C1. Replace mode checkboxes with toggle chips

Files:

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`

Changes:

1. Replace `CheckBox` controls with `ToggleButton` for `Aa`, `W`, `.*`.
2. Keep existing model wiring:
   - `setCaseSensitive(...)`
   - `setWholeWord(...)`
   - `setRegexMode(...)`
3. Ensure selected chip state is styled and keyboard accessible.

Acceptance:

1. No form-checkbox visuals remain for these options.
2. Behavior remains identical to current toggles.

### C2. Unify icon controls

Files:

- New: `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchIcons.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`

Changes:

1. Create reusable icon nodes (search, chevron-right, chevron-down, arrow-up/down, close).
2. Replace text glyph buttons (`▲`, `▼`, `▶`, `✕`) with icon-only buttons.
3. Normalize icon size, button padding, and hit target size.

Acceptance:

1. All compact controls use one icon language.
2. Icons are legible in dark/light themes.

## Phase D - Surface Polish (P1)

### D1. Floating panel appearance

Files:

- `SearchController.java`
- `CodeEditorTheme.java`

Changes:

1. Add rounded corners (target 6-8 px radius).
2. Add subtle drop shadow using theme-driven color/intensity.
3. Remove or soften full-width accent-line look.

Acceptance:

1. Panel clearly reads as floating card, not top toolbar strip.
2. Edge separation remains visible against both theme backgrounds.

### D2. No-results and focus feedback

Files:

- `SearchController.java`
- `search-overlay.css`

Changes:

1. Apply warning border/background when query is non-empty and count is zero.
2. Apply strong focused border/ring for active field.

Acceptance:

1. "No results" state is visible without reading label text.
2. Keyboard focus is obvious during navigation.

### D3. Optional animation

Files:

- `SearchController.java`

Changes:

1. Add short fade/slide for `open()` and replace-row expand/collapse.
2. Keep durations short (100-160ms) and disable animation when reopened quickly.

Acceptance:

1. Motion feels smooth and does not delay typing/navigation.

## Phase E - Advanced UX Features (P2)

### E1. Search/replace history

Files:

- `SearchModel.java`
- `SearchController.java`
- New optional helper for history popup

Changes:

1. Store capped recent queries/replacements (for example max 20).
2. Add history dropdown affordance near both fields.
3. Selecting history item updates field and executes search.

Acceptance:

1. Recent entries are deduplicated and capped.
2. History interaction does not steal core keyboard flow.

### E2. Preserve-case toggle in replace row

Files:

- `SearchModel.java`
- `SearchController.java`
- `SearchModelTest.java`

Changes:

1. Add `preserveCase` model flag.
2. Add replace-row `Aa` chip.
3. Apply casing transform rules to replacement text:
   - all lower -> lower
   - Initial capital -> Initial capital
   - all upper -> upper

Acceptance:

1. Unit tests cover all three casing patterns.

### E3. Exclude action

Files:

- `SearchController.java`
- optional additions in `SearchModel.java`

Changes:

1. Add `Exclude` button in replace row.
2. Exclude moves current match to next without replacing.
3. Define exact semantics for repeated manual replace sequences.

Acceptance:

1. Excluded match is skipped in current replace flow as specified.

## 5. Testing Strategy

## 5.1 Unit tests

Files:

- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/search/SearchModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`

Coverage:

1. Preserve-case rules (if E2 enabled).
2. History list behavior (if E1 enabled).
3. Theme token mapping additions.

## 5.2 UI integration tests

Files:

- New: `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/search/SearchControllerFxTest.java`
- Update: `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java`

Coverage:

1. Overlay width bounds in collapsed and expanded mode.
2. Toggle chip behavior and selected visuals.
3. Disabled/active action button states.
4. Icon buttons wired correctly (next/prev/close/chevron).
5. Focus and no-results state class transitions.

## 5.3 Manual QA checklist

1. `Cmd/Ctrl+F` opens compact find panel top-right.
2. `Cmd+Option+F` (mac) / `Ctrl+H` (win/linux) opens expanded replace mode.
3. Dark/light theme switching keeps contrast and icon visibility.
4. Very narrow editor widths degrade gracefully (no control overlap).
5. Search panel does not occlude editor content excessively.

## 6. Rollout Plan

## Step 1 (PR-1, P0 core)

1. Phase A + Phase B (including B3 theme wiring) + Phase C.
2. Ship visual baseline and stateful controls.

## Step 2 (PR-2, P1 polish)

1. Phase D.
2. Validate against screenshots and usability pass.

## Step 3 (PR-3, P2 optional)

1. Phase E only if product owner confirms advanced parity scope.

## 7. Risks and Mitigations

1. Risk: CSS + programmatic styling conflicts.
   Mitigation: centralize style ownership in CSS; keep Java updates limited to semantic classes/state toggles.
2. Risk: icon visibility issues across themes.
   Mitigation: theme-driven icon paint and explicit light/dark snapshots in tests.
3. Risk: added tokens break mapper tests.
   Mitigation: update `CodeEditorThemeMapperTest` in same PR as token additions.
4. Risk: animation causing perceived latency.
   Mitigation: cap animation duration and skip when focus/typing happens immediately.

## 8. Definition of Done

1. P0 acceptance criteria fully met.
2. New/updated tests pass with:
   - `mvn test -pl papiflyfx-docking-code`
3. Visual diff against `intellij-idea-search-screenshot.png` shows:
   - compact bounded panel
   - coherent iconography
   - obvious state styling
   - improved expanded-row rhythm
4. No regressions in existing search/replace keyboard behavior.
5. Live theme switching updates search overlay without reopen/restart.
