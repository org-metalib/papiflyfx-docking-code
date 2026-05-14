# PapiflyFX Search UI Visual Review (Codex)

Compared snapshots:

- Current implementation: `spec/papiflyfx-docking-code-editor-search/code-search-implemented.png` (also user Image #1)
- IntelliJ reference: `spec/papiflyfx-docking-code-editor-search/intellij-idea-search-screenshot.png` (also user Image #2)

Reviewed code:

- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/SearchController.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/CodeEditorTheme.java`

## Findings (Visual/UX)

## 1. Overlay reads like a full-width toolbar, not a compact search panel

Severity: High

Current snapshot:

- Search UI spans almost the whole editor width.
- Bottom accent line runs full width, creating a "top bar" feel.

IntelliJ snapshot:

- Search appears as a compact floating panel with clear bounds and breathing room.

Recommendation:

- Cap overlay width and stop row stretching.
- Keep panel width stable in a practical range (for example `min 520`, `max 760`).
- Remove full-width underline treatment on the container and use a bounded border around the panel itself.

Code touch points:

- `SearchController.java` (`setMaxSize`, `HBox.setHgrow`, root container shape)
- `CodeEditor.java` (top-right anchoring is fine; width behavior needs tightening in controller)

## 2. Missing visual hierarchy between panel, fields, toggles, and actions

Severity: High

Current snapshot:

- Everything has nearly identical weight (same background, same border rhythm).
- Eyes do not get a clear "primary focus" target.

IntelliJ snapshot:

- Distinct hierarchy: search field is dominant, toggles are compact, actions are secondary, close is de-emphasized.

Recommendation:

- Make search field dominant via stronger contrast and fixed height.
- Make toggles and icon actions compact chips (not same visual weight as text inputs).
- Keep replace row visually secondary (slightly dimmer or tighter).

Code touch points:

- `SearchController.java` (`configureTextField`, `configureButton`, row spacing/padding)
- `CodeEditorTheme.java` (needs more than current 6 overlay tokens for stateful hierarchy)

## 3. Checkbox controls look heavy and dated vs IDE-style toggle chips

Severity: High

Current snapshot:

- `Aa`, `W`, `.*` are JavaFX `CheckBox` controls, so they render like form controls.

IntelliJ snapshot:

- The same options read as lightweight mode chips/buttons.

Recommendation:

- Replace `CheckBox` with `ToggleButton` (or custom button) for `Aa`, `W`, `.*`.
- Add clear selected/hover/pressed visuals.
- Keep these controls square and icon-like to reduce visual noise.

Code touch points:

- `SearchController.java` (`caseSensitiveToggle`, `wholeWordToggle`, `regexToggle`, `configureToggle`)

## 4. Iconography feels inconsistent (filled triangles + text glyphs)

Severity: Medium

Current snapshot:

- Uses glyph text (`▲`, `▼`, `✕`, `▶`) with varying stroke weight.

IntelliJ snapshot:

- Uses consistent line icons with coherent size/weight.

Recommendation:

- Switch to one icon set and unify icon metrics (16px icon, consistent padding).
- Keep navigation/close/filter/more icons on the same visual baseline.

Code touch points:

- `SearchController.java` button labels and icon wiring

## 5. Panel shape is flat; lacks depth and edge definition

Severity: Medium

Current snapshot:

- Sharp-corner rectangle with little separation from editor surface.

IntelliJ snapshot:

- Subtle floating surface (radius + border + depth cue).

Recommendation:

- Use corner radius (for example 6-8px), a subtle outer stroke, and light drop shadow.
- Use internal padding that reads as a composed card, not a strip.

Code touch points:

- `SearchController.java` container `Background`, `Border`, padding
- `CodeEditorTheme.java` add panel border/shadow tokens (or derive from existing theme)

## 6. Missing state styling (hover, focus, active, disabled) reduces perceived quality

Severity: Medium

Current snapshot:

- Controls look mostly static; disabled actions are not strongly differentiated.

IntelliJ snapshot:

- State transitions are clear and communicate affordance immediately.

Recommendation:

- Add explicit styles for `hover`, `focused`, `selected`, `disabled`.
- Make keyboard focus ring visible on search/replace fields and active chips.

Code touch points:

- `SearchController.java` currently paints direct backgrounds/borders with no pseudo-class styling
- Move search overlay styling into a dedicated CSS to leverage JavaFX pseudo-classes

## 7. Layout rhythm is cramped in expanded mode

Severity: Medium

Current snapshot:

- Replace row aligns functionally but feels crowded and uniform.

IntelliJ snapshot:

- Expanded mode has cleaner rhythm and stronger left/right grouping.

Recommendation:

- Increase horizontal spacing between logical groups.
- Keep left cluster for query/toggles, center for result count, right cluster for navigation/actions.
- Reserve optional advanced actions (`Exclude`, menu) without compressing core controls.

Code touch points:

- `SearchController.java` row composition in constructor

## Suggested Implementation Order

1. Structural pass: width cap, panel bounds, spacing, corner radius.
2. Control pass: switch checkboxes to toggle chips and normalize icon buttons.
3. Styling pass: move overlay styling to CSS with pseudo-classes.
4. Theme pass: extend overlay tokens for active/hover/disabled/focus states.
5. Polish pass: optional subtle expand/collapse animation for replace row.

## Minimal Acceptance Criteria for "Visually Compelling"

- Search panel no longer reads as a full-width bar.
- Clear hierarchy: query field first, toggles second, actions third.
- All compact controls share one icon language and size.
- Selected/hover/focus/disabled states are visually obvious.
- Expanded replace mode keeps rhythm and does not feel crowded.
