# Review 2 (Codex): `papiflyfx-docking-code` vs Specs

Date: 2026-02-16  
Scope reviewed:
- `spec/papiflyfx-docking-code/spec.md`
- `spec/papiflyfx-docking-code/implementation.md`
- `spec/papiflyfx-docking-code/additions.md`
- `spec/papiflyfx-docking-code/additions2.md`
- `spec/papiflyfx-docking-code/PROGRESS.md`
- `papiflyfx-docking-code` module source/tests

## Fixed In This Pass

### 1. FIXED: Restored cursor state now updates runtime caret model
Spec references:
- `spec/papiflyfx-docking-code/spec.md:95`
- `spec/papiflyfx-docking-code/spec.md:145`

Code changes:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:372`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:398`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:415`

Notes:
- `applyState(...)`, `setCursorLine(...)`, and `setCursorColumn(...)` now route through caret-application logic that drives `SelectionModel`.

---

### 2. FIXED: Undo/redo no longer teleports caret to `0:0`
Spec references:
- `spec/papiflyfx-docking-code/spec.md:15`

Code changes:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:260`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:270`

Notes:
- Undo/redo now preserve a caret position near the edited location using offset delta, then ensure visibility.

---

### 3. FIXED: Captured scroll state is now aligned with actual viewport offset
Spec references:
- `spec/papiflyfx-docking-code/spec.md:145`

Code changes:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:82`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:370`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:432`

Notes:
- Added scroll synchronization between editor property and viewport clamp result.
- `captureState()` now serializes the runtime viewport offset.

---

### 4. FIXED: State adapter now has explicit version-aware restore path
Spec references:
- `spec/papiflyfx-docking-code/spec.md:135`
- `spec/papiflyfx-docking-code/spec.md:136`
- `spec/papiflyfx-docking-code/spec.md:137`

Code changes:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java:41`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditorStateAdapter.java:49`

Notes:
- `restore(...)` now branches by `LeafContentData.version()`.
- Added migration hook for version `0` and safe fallback to `EditorStateData.empty()` for unknown versions.

---

### 5. FIXED: Added disposal lifecycle hooks to editor and viewport
Spec references:
- `spec/papiflyfx-docking-code/spec.md:121`
- `spec/papiflyfx-docking-code/spec.md:123`
- `spec/papiflyfx-docking-code/spec.md:124`
- `spec/papiflyfx-docking-code/spec.md:125`

Code changes:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/api/CodeEditor.java:500`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:349`

Notes:
- `dispose()` unbinds handlers/listeners and detaches document listeners to reduce leak risk.

---

### 6. FIXED: Added targeted tests for restored-caret, undo/redo caret, scroll capture, migration, disposal
Spec references:
- `spec/papiflyfx-docking-code/spec.md:149`
- `spec/papiflyfx-docking-code/spec.md:150`

Test changes:
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:141`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:163`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:183`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:204`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:220`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:228`
- `papiflyfx-docking-code/src/test/java/org/metalib/papifly/fx/code/api/CodeEditorIntegrationTest.java:247`

## Remaining Open Findings

### A. MEDIUM: Theming contract is still not implemented
Spec references:
- `spec/papiflyfx-docking-code/spec.md:66`
- `spec/papiflyfx-docking-code/spec.md:69`

Code references:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:25`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/theme/package-info.java:1`

Status:
- Still hardcoded colors and no `DockManager.themeProperty()` mapping yet.

---

### B. MEDIUM: Incremental dirty-region redraw is still not implemented
Spec references:
- `spec/papiflyfx-docking-code/spec.md:56`
- `spec/papiflyfx-docking-code/implementation.md:106`

Code references:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:142`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/render/Viewport.java:239`

Status:
- Redraw path still repaints full visible area.

---

### C. MEDIUM: MVP features pending by design phase scope (lexer/gutter/search)
Spec references:
- `spec/papiflyfx-docking-code/spec.md:17`
- `spec/papiflyfx-docking-code/spec.md:18`
- `spec/papiflyfx-docking-code/spec.md:19`

Code references:
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/lexer/package-info.java:1`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/gutter/package-info.java:1`
- `papiflyfx-docking-code/src/main/java/org/metalib/papifly/fx/code/search/package-info.java:1`

Status:
- Not implemented yet (Phase 3+ scope).

## Validation Notes
- Local verification command executed:
  - `mvn -pl papiflyfx-docking-code -am -Dtestfx.headless=true test`
- Result observed: build success, 99 tests passing.
