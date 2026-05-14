# PapiflyFX Code Editor Search/Replace Review

Reference UI targets:

- `spec/papiflyfx-docking-code-editor-search/text-search.png` (collapsed find row)
- `spec/papiflyfx-docking-code-editor-search/text-search-replace.png` (expanded replace row)

Reviewed source:

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchModel.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/EditorCommand.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/command/KeymapTable.java`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/search/SearchModelTest.java`

## Findings (Ordered by Severity)

## 1. Search overlay can cover the whole editor area (text appears to disappear on Cmd+F)

Severity: Critical  
User-observed behavior: pressing `Cmd+F` shows search UI but editor text disappears behind it.

Evidence:

- Search UI is added as a `StackPane` child overlay and aligned top-center: `CodeEditor.java:137-139`.
- Search overlay is a resizable `VBox` with opaque background fill: `SearchController.java:33`, `SearchController.java:56-67`, `SearchController.java:180-187`.
- Overlay is toggled to managed+visible on open without bounding its size to preferred dimensions: `SearchController.java:224-229`.
- There is no `setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE)` / width clamp / clip policy on `SearchController`.

Impact:

- Search panel can occupy the full editor layer instead of just a compact top strip.
- Core editing visibility is blocked, making find/replace practically unusable.

Solution:

- Keep overlay as floating compact panel:
  - set `searchController.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE)`,
  - set `searchController.setPrefWidth(...)` and do not allow fill-to-parent expansion,
  - keep only panel background visible (not full-layer paint).
- Alternative: host search UI in a separate non-filling container (`BorderPane` top slot) instead of overlay `StackPane`.
- Add UI integration test asserting that opening search does not occlude viewport content area.

## 2. Missing whole-word search (`W`) support

Severity: High  
Target mismatch: screenshot shows `W` toggle.

Evidence:

- Only regex + case toggles are implemented in UI: `SearchController.java:92-104`.
- No whole-word state/logic in model: `SearchModel.java:18-79`, `SearchModel.java:226-276`.

Impact:

- Cannot match VS Code-like `Whole Word` behavior from target UI.

Solution:

- Add `wholeWord` flag to `SearchModel`.
- Add `W` toggle in `SearchController`.
- Enforce boundary check in both plain-text and regex modes before accepting a match.

## 3. Replace row is always visible; no collapsed/expanded mode

Severity: High  
Target mismatch: screenshot shows collapsed single-row find state and expandable replace state.

Evidence:

- Both rows are always added and visible together: `SearchController.java:106-127`.
- No expand/collapse control state exists.

Impact:

- Current UX cannot reproduce screenshot behavior.
- Consumes extra vertical space permanently.

Solution:

- Add an explicit `replaceMode` state.
- Add left chevron toggle button.
- Keep replace row `managed=false, visible=false` by default; show only in replace mode.

## 4. No shortcut for “Open Replace” mode (`Ctrl+H` / `Cmd+Option+F`)

Severity: High  
Target mismatch: replace mode should be directly accessible via keyboard.

Evidence:

- `EditorCommand` has only `OPEN_SEARCH` and `GO_TO_LINE` for search-related global commands: `EditorCommand.java:42-43`.
- Keymap maps only `Primary+F` and `Primary+G`: `KeymapTable.java:72-75`.
- No replace-open command in `CodeEditor` dispatch: `CodeEditor.java:397-400`.

Impact:

- Keyboard-only workflow is incomplete.

Solution:

- Add `OPEN_REPLACE` command.
- Map:
  - Windows/Linux: `Ctrl+H`
  - macOS: `Cmd+Option+F`
- Implement `CodeEditor.openReplace()` to open overlay in expanded replace mode and focus replace field.

## 5. Search results can become stale after document edits while overlay is open

Severity: High  

Evidence:

- `SearchController` recomputes only on query/toggle/replace actions: `SearchController.java:129-155`, `SearchController.java:273-315`.
- `CodeEditor` wires document listener for gutter width only, not search refresh: `CodeEditor.java:110-125`.
- `Viewport` renders cached `searchMatches` until explicitly replaced: `Viewport.java:148-152`, `Viewport.java:572-635`.

Impact:

- Highlight positions and match counts can diverge from current document content.

Solution:

- Re-run search on document change while search overlay is open.
- Preserve user context by selecting nearest match to current caret (`SearchModel.selectNearestMatch(...)`) instead of resetting to index 0 every time.
- Debounce refresh to avoid unnecessary churn while typing quickly.

## 6. Regex replace does not support capture-group substitution

Severity: Medium

Evidence:

- Replacement is inserted literally for both `replaceCurrent` and `replaceAll`: `SearchModel.java:187-214`.
- No `Matcher.appendReplacement`/group expansion logic exists.

Impact:

- In regex mode, common replace patterns like `$1_$2` will not behave as expected.

Solution:

- When `regexMode` is enabled, execute replacements using matcher-based substitution semantics.
- Keep literal replacement path for plain-text mode.

## 7. Replace controls are not disabled when no matches

Severity: Medium  
Target mismatch: screenshot shows disabled Replace/Replace All when no results.

Evidence:

- Buttons are always active; no disable binding: `SearchController.java:116-123`, `SearchController.java:304-315`.

Impact:

- UI allows no-op actions and does not communicate state clearly.

Solution:

- Disable `Replace` and `Replace All` when `matchCount == 0` or query is empty.
- Keep visual state synced with results count.

## 8. Missing “preserve case” option in replace row (`Aa`) and missing scope/filter controls

Severity: Medium  
Target mismatch: screenshot shows extra options in expanded mode.

Evidence:

- No preserve-case toggle in replace row (`SearchController.java:109-127`).
- Model has no preserve-case flag (`SearchModel.java:18-79`).
- No include/exclude scope/filter actions in controller/model.

Impact:

- Cannot match advanced behavior indicated by target UI.

Solution:

- Add optional `preserveCase` toggle and transform replacement casing based on matched token shape.
- If required by product scope, add scope controls (document/selection and include/exclude filters).

## 9. Replace-field Enter key has no replace behavior

Severity: Medium

Evidence:

- `replaceField` key handler only supports Escape: `SearchController.java:150-155`.

Impact:

- Slower keyboard workflow than expected for a text editor find/replace panel.

Solution:

- Map `Enter` in replace field to `replaceCurrent()`.
- Optionally map `Shift+Enter` to replace + previous navigation, or to `replaceAll()`.

## 10. Limited automated coverage for search UI integration

Severity: Low

Evidence:

- Only model tests exist in `search` package: `SearchModelTest.java` (no `SearchController` FX tests).
- No dedicated tests for search/replace keybinding behavior (`Ctrl+H`, expanded mode, button disable states).

Impact:

- Regression risk for UI and keyboard behavior.

Solution:

- Add `SearchControllerFxTest` for:
  - collapsed/expanded states,
  - toggle behavior (`Cc`, `W`, `.*`, optional `Aa`),
  - Enter/Escape semantics,
  - disabled button states.
- Extend `KeymapTableTest`/integration tests for replace-open shortcuts.

## Recommended Implementation Shape (To Match Screenshots)

1. Overlay state model:
   - `findMode` (single row)
   - `replaceMode` (two rows)
2. Find row controls:
   - query field
   - toggles: case (`Cc`), whole word (`W`), regex (`.*`)
   - count + next/previous + close
3. Replace row controls:
   - replacement field
   - optional preserve case (`Aa`)
   - `Replace`, `Replace All` (disabled when no matches)
4. Keyboard:
   - `Primary+F` -> open find
   - `Ctrl+H` (win/linux), `Cmd+Option+F` (mac) -> open replace
   - `Enter`/`Shift+Enter` for next/previous in find field
   - `Enter` in replace field -> replace current
5. Model:
   - add `wholeWord` (required)
   - add regex group-aware replacement (required for regex mode)
   - optional `preserveCase`
6. Live sync:
   - refresh search results on document changes while overlay is open.
7. Layout safety:
   - keep search panel non-filling and limited to compact preferred size.

## Implementation Progress

| # | Finding | Severity | Status | Commit/Branch |
|---|---------|----------|--------|---------------|
| 1 | Overlay covers whole editor area | Critical | Done | `code` branch |
| 2 | Missing whole-word search (`W`) | High | Done | `code` branch |
| 3 | Replace row always visible; no collapsed/expanded mode | High | Done | `code` branch |
| 4 | No shortcut for "Open Replace" mode | High | Done | `code` branch |
| 5 | Search results stale after document edits | High | Done | `code` branch |
| 6 | Regex replace lacks capture-group substitution | Medium | Done | `code` branch |
| 7 | Replace controls not disabled when no matches | Medium | Done | `code` branch |
| 8 | Missing "preserve case" toggle and scope controls | Medium | Deferred | — |
| 9 | Replace-field Enter key has no replace behavior | Medium | Done | `code` branch |
| 10 | Limited automated coverage for search UI | Low | Partial | `code` branch |

### Implementation Summary

All changes were made on the `code` branch. Build verified: 318 tests pass (`./mvnw test -pl papiflyfx-docking-code -am -Dtestfx.headless=true`).

#### Phase 1: Fix overlay layout (Finding #1)

**`SearchController.java`**
- Added `setMaxSize(Double.MAX_VALUE, Region.USE_PREF_SIZE)` so the VBox only takes its preferred height, not the full StackPane layer.

**`CodeEditor.java`**
- Changed alignment from `TOP_CENTER` to `TOP_RIGHT` to match VS Code style.
- Added 16px right margin: `StackPane.setMargin(searchController, new Insets(0, 16, 0, 0))`.

#### Phase 2: Collapsed/expanded replace mode (Finding #3)

**`SearchController.java`**
- Added `replaceMode` field and chevron toggle button (`▶` collapsed, `▼` expanded).
- Replace row starts hidden: `replaceRow.setManaged(false); replaceRow.setVisible(false)`.
- Chevron click toggles replace row visibility.
- Added `openInReplaceMode(String)` method for direct replace-mode opening.
- Restructured layout: chevron on left, search/replace rows in a nested VBox on right.

#### Phase 3: OPEN_REPLACE shortcut (Finding #4)

**`EditorCommand.java`**
- Added `OPEN_REPLACE` enum value after `OPEN_SEARCH`.

**`KeymapTable.java`**
- Mac: `Cmd+Option+F` → `OPEN_REPLACE`
- Non-mac: `Ctrl+H` → `OPEN_REPLACE`

**`CodeEditor.java`**
- Added `openReplace()` method calling `searchController.openInReplaceMode(selectedText)`.
- Added `OPEN_REPLACE` to the "always-on" guard in `handleKeyPressed`.
- Added `OPEN_REPLACE` case in `executeCommand` dispatch.

#### Phase 4: Whole-word search (Finding #2)

**`SearchModel.java`**
- Added `boolean wholeWord` field with getter/setter.
- `searchPlainText()`: after finding a match, checks word boundaries via `isWordBoundary(text, start, end)` — character before start and after end must be non-word chars (or start/end of text).
- `searchRegex()`: when `wholeWord` is enabled, wraps the compiled pattern with `\b...\b` word boundary anchors.
- Added `isWordBoundary()` and `isWordChar()` helpers.

**`SearchController.java`**
- Added `CheckBox wholeWordToggle = new CheckBox("W")` between `caseSensitiveToggle` and `regexToggle`.
- Wired to `searchModel.setWholeWord(...)` + `executeSearch()`.
- Toggle order in search row matches screenshot: `Aa`, `W`, `.*`.

#### Phase 5: Replace button disable states (Finding #7)

**`SearchController.java`**
- `replaceButton` and `replaceAllButton` stored as fields, initialized with `setDisable(true)`.
- `updateMatchLabel()` now also sets `replaceButton.setDisable(count == 0)` and `replaceAllButton.setDisable(count == 0)`.

#### Phase 6: Enter key in replace field (Finding #9)

**`SearchController.java`**
- Added `Enter` handler to `replaceField.setOnKeyPressed` that calls `replaceCurrent()` and consumes the event.

#### Phase 7: Regex capture-group replacement (Finding #6)

**`SearchModel.java`**
- `replaceCurrent()`: when `regexMode`, compiles the pattern, matches it against the matched substring, and uses `matcher.replaceFirst(replacement)` to expand `$1`, `$2`, etc.
- `replaceAll()`: when `regexMode`, uses `Matcher.appendReplacement`/`appendTail` in a single pass over the full text for correct group expansion, skipping zero-length and multi-line matches.

#### Phase 8: Live search refresh on document edits (Finding #5)

**`CodeEditor.java`**
- Added `DocumentChangeListener searchRefreshListener` that triggers refresh when search overlay is open.
- Added `PauseTransition searchRefreshDebounce` (150ms) to avoid churn during fast typing.
- On refresh: re-runs `searchModel.search(document)`, selects nearest match to caret position, updates highlights and match label.
- Added `refreshMatchDisplay()` public method to `SearchController`.
- Cleanup in `dispose()`: stops debounce timer and removes listener.

#### Phase 9: Preserve case toggle (Finding #8)

Deferred to a follow-up. This is an advanced feature (`Aa` toggle in replace row, `Exclude` button) that is lower priority.

#### Phase 10: Tests (Finding #10)

**`SearchModelTest.java`** — 5 new tests added (19 → 24 total):
- `searchPlainTextWholeWord()` — "hello" matches whole word only, not "helloworld"
- `searchPlainTextWholeWordCaseInsensitive()` — case-insensitive whole-word
- `searchRegexWholeWord()` — regex with `\b` wrapping
- `regexReplaceCaptureGroups()` — pattern `(\w+)-(\w+)`, replacement `$2_$1`
- `replaceAllRegexCaptureGroups()` — multi-match capture-group replace

No `SearchControllerFxTest` added in this phase — requires TestFX infrastructure for overlay testing.

## Conclusion

The current implementation provides a functional baseline find/replace overlay, but it does not yet match the expected UX and behavior shown in the reference screenshots.
The highest-priority gaps are:

1. ~~overlay currently hiding editor content on search open~~ — **Fixed**
2. ~~whole-word support~~ — **Fixed**
3. ~~collapsible replace mode~~ — **Fixed**
4. ~~replace-open shortcut~~ — **Fixed**
5. ~~live resync after document edits~~ — **Fixed**

**Remaining:** Finding #8 (preserve case toggle / scope controls) is deferred. Finding #10 (SearchController FX integration tests) is partially addressed with model-level tests.
