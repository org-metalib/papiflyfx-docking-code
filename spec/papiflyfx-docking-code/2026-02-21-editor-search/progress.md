# PapiflyFX Code Editor Search/Replace Progress

**Date:** 2026-02-21  
**Reference:** `spec/papiflyfx-docking-code-editor-search/implementation-claude.md`  
**Status:** P0 complete, P1 partial, P2 partial

## Completed

### Phase A (P0) Layout & compactness

- Refactored `SearchController` into a compact bounded overlay (`min/pref/max`: 520/620/760).
- Reorganized controls into grouped zones (inputs, mode chips/result, navigation/actions).
- Tightened spacing/padding and control heights for dense toolbar behavior.

### Phase B (P0) Stateful visuals + theme wiring

- Added dedicated stylesheet:  
  `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/search/search-overlay.css`
- Replaced imperative paint/border wiring with class-based CSS + pseudo-class state (`hover`, `focused`, `selected`, `disabled`, `no-results`).
- Added runtime CSS variable bridge from `CodeEditorTheme` in `SearchController.setTheme(...)`.
- Added new `CodeEditorTheme` tokens and mapper propagation:
  - `searchOverlayPanelBorder`
  - `searchOverlayControlHoverBackground`
  - `searchOverlayControlActiveBackground`
  - `searchOverlayControlFocusedBorder`
  - `searchOverlayControlDisabledText`
  - `searchOverlayNoResultsBorder`
  - `searchOverlayShadowColor`
  - `searchOverlayIntegratedToggleActive`
  - `searchOverlayErrorBackground`
- Added integration assertion that search overlay style variables update after theme switch.

### Phase C (P0) Controls & iconography

- Replaced checkbox toggles with chip `ToggleButton`s (`Aa`, `W`, `.*`).
- Added icon system (`SearchIcons`) and icon-only controls for:
  - expand/collapse
  - previous/next
  - close
  - search leading icon
  - in-selection filter icon
- Added styles for icon buttons and action buttons with explicit disabled/hover/focus behavior.

### Phase D (P1) Surface polish (partial)

- Added floating-card look with rounded border + shadow token usage.
- Added no-results warning state on search field (border + tinted background).

### Phase E (P2) Advanced features (partial)

- Implemented preserve-case replacement in `SearchModel`:
  - all lower -> lower
  - Initial Capital -> Initial Capital
  - ALL UPPER -> ALL UPPER
- Implemented in-selection search scope in `SearchModel` + `SearchController`.
- Wired live selection scope from `CodeEditor` via `setSelectionRangeSupplier(...)`.
- Added skip action (`Skip`) in replace row.

## Deferred

- Search/replace history dropdowns (E1) not implemented.
- Open/expand animation (D3) not implemented.

## Validation

```bash
mvn -pl papiflyfx-docking-code -Dtest=SearchModelTest,CodeEditorThemeMapperTest test
# Tests run: 38, Failures: 0, Errors: 0, Skipped: 0

mvn -pl papiflyfx-docking-code -Dtest=CodeEditorThemeIntegrationTest -Dtestfx.headless=true test
# Tests run: 8, Failures: 0, Errors: 0, Skipped: 0

mvn -pl papiflyfx-docking-code -Dtest=CodeEditorIntegrationTest -Dtestfx.headless=true test
# Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
```

## Files Changed

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchIcons.java`
- `papiflyfx-docking-code/src/main/resources/org/metalib/papifly/fx/code/search/search-overlay.css`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapper.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/search/SearchModelTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeMapperTest.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/theme/CodeEditorThemeIntegrationTest.java`
