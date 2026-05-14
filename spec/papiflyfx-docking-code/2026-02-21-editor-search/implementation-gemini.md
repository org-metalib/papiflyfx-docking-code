# Code Editor Search/Replace Implementation Plan

This plan consolidates the findings from `review0-codex.md` (Functional Gaps) and `review0-gemini.md` (Visual Polish) to deliver a professional, IntelliJ-like search and replace experience.

## Objectives

1.  **Visual Parity**: Match the compact, modern aesthetic of IntelliJ IDEA (less padding, SVG icons, depth).
2.  **Functional Completeness**: Implement deferred features like "Preserve Case", "Search in Selection", and "Exclude" action.
3.  **UX Enhancement**: Add search history and integrated field controls.

## Phase 1: Visual Foundation & Layout

**Goal:** Transform the current "blocky" overlay into a sleek, floating toolbar.

### 1.1 Layout Compactness
- **Target:** `SearchController.java`
- **Action:**
    - Reduce internal padding of `SearchController` from `(4, 8, 4, 8)` to `(2, 4, 2, 4)`.
    - Reduce `HBox` spacing between elements from `4` to `2` or `0` (using borders/separators instead).
    - Enforce a fixed, smaller height for text fields (e.g., 22px-24px) to match standard IDE toolbars.
    - Use `Font.font(11)` or `12` for all text inputs to reduce vertical footprint.

### 1.2 Depth & Elevation
- **Target:** `SearchController.java`, `CodeEditorTheme.java`
- **Action:**
    - Add a `DropShadow` effect to `SearchController`:
        ```java
        setEffect(new DropShadow(10, 0, 4, Color.rgb(0, 0, 0, 0.2)));
        ```
    - Define new theme keys in `CodeEditorTheme`:
        - `searchOverlayShadowColor`
    - Update background color to be slightly lighter/distinct from the editor background if needed for contrast.

### 1.3 Border & Focus Styling
- **Target:** `SearchController` CSS/Style
- **Action:**
    - Remove default JavaFX focus rings.
    - Implement custom focus highlighting:
        - When `searchField` is focused, highlight the *entire* search row or the field border with `accentColor`.
        - When "No results" found, change the field border/background to a warning/error color (e.g., soft red).

## Phase 2: Modern Iconography

**Goal:** Replace amateur Unicode characters with crisp SVG icons.

### 2.1 Icon Infrastructure
- **Target:** New class `SearchIcons.java` (or internal constants).
- **Action:**
    - Define SVG path strings for:
        - `SEARCH_ICON` (Magnifying glass)
        - `NEXT_ARROW` (Down)
        - `PREV_ARROW` (Up)
        - `CLOSE_ICON` (X)
        - `EXPAND_ICON` (Chevron Right)
        - `COLLAPSE_ICON` (Chevron Down)
        - `FILTER_ICON` (Funnel/Filter - optional for selection scope)
    - Create a helper method `createIcon(String svgPath)` returning a sized `SVGPath` or `Region`.

### 2.2 Button Replacement
- **Target:** `SearchController.java`
- **Action:**
    - Replace `Button("\u25b2")` etc. with icon-only buttons.
    - Style buttons to be transparent (no background) by default, showing background only on hover (`-fx-background-color: transparent;`).
    - Ensure hit targets remain usable (min-width/height) even if the icon is small.

## Phase 3: Advanced Controls & History

**Goal:** Integrate powerful features without cluttering the UI.

### 3.1 Integrated Field Controls
- **Target:** Custom `SearchField` component (extending `CustomTextField` or `StackPane` wrapping `TextField`).
- **Action:**
    - **Left:** Display the Magnifying Glass icon *inside* the text field area.
    - **Right:** Move toggles (`Cc`, `W`, `.*`) *inside* the text field area (right-aligned).
    - This requires utilizing `ControlsFX` `CustomTextField` or implementing a similar layout manually (StackPane with padding on the TextField).

### 3.2 Search History
- **Target:** `SearchModel.java`, `SearchController.java`
- **Action:**
    - **Model:** Add `List<String> searchHistory` and `List<String> replaceHistory`. Limit to last 10-20 items.
    - **UI:** Add a small "History" dropdown arrow (icon) next to the search/replace fields.
    - **Behavior:** Clicking the arrow shows a `ContextMenu` or `ComboBox` dropdown with recent queries.

### 3.3 "Preserve Case" (Deferred Item)
- **Target:** `SearchModel.java`, `SearchController.java`
- **Action:**
    - Add `boolean preserveCase` to model.
    - Add `Aa` toggle to the **Replace Row**.
    - **Logic:**
        - If `preserveCase` is ON:
        - "foo" -> "bar"
        - "Foo" -> "Bar"
        - "FOO" -> "BAR"
        - Detect casing of the *matched* text and apply it to the replacement string before insertion.

### 3.4 Scope: "In Selection" (Deferred Item)
- **Target:** `SearchModel.java`, `SearchController.java`
- **Action:**
    - Add `boolean searchInSelection` to model.
    - Add toggle button (Icon: Selection/Box) to Search Row.
    - **Logic:**
        - If ON, `search()` only considers matches where `match.start >= selection.start && match.end <= selection.end`.
        - Alternatively, searching restricts the loop to the selection range.

### 3.5 "Exclude" Action (Gemini Review)
- **Target:** `SearchController.java`
- **Action:**
    - Add an "Exclude" button (or "Skip") next to "Replace".
    - **Behavior:**
        - Skips the current match (moves to next) *without* replacing it.
        - Useful for manual "Replace All" workflows where the user spams "Replace" but wants to skip one instance.

## Phase 4: Testing & Polish

### 4.1 UI Animation
- **Action:** Add a `TranslateTransition` or `FadeTransition` when `open()` is called to slide the panel down from the top edge.

### 4.2 Automated Tests
- **Target:** `SearchControllerTest.java` (TestFX)
- **Action:**
    - Verify toggles update model state.
    - Verify "Preserve Case" logic (unit test in `SearchModelTest`).
    - Verify "History" persistence (if applicable) or simple add/select behavior.

## Phase 5: Theme Integration

**Goal:** Ensure all new visual elements are fully theme-aware and consistent.

### 5.1 Extended Theme Keys
- **Target:** `CodeEditorTheme.java`
- **Action:**
    - Add `Paint searchOverlayShadowColor`.
    - Add `Paint searchOverlayIntegratedToggleActive` (color for ON state of Cc/W/.*).
    - Add `Paint searchOverlayErrorBackground` (for "No results" state).
    - Update `dark()` and `light()` static constructors with appropriate values.

### 5.2 Dynamic Palette Updates
- **Target:** `SearchController.java`
- **Action:**
    - Update `applyThemeColors()` to handle the new `DropShadow` color.
    - Ensure SVG icons use `fillProperty().bind()` or are re-painted when the theme changes.
    - Update integrated toggle styling (casing, background) based on the current theme.

### 5.3 Mapper Synchronization
- **Target:** `CodeEditorThemeMapper.java`
- **Action:**
    - Ensure the mapper correctly derives the new search-specific colors from the base docking theme if they are not explicitly provided.

## Execution Order

1.  **Refactor Layout & Icons** (Phases 1 & 2) - High Impact, Immediate Visual Win.
2.  **Integrated Controls** (Phase 3.1) - Complex UI work, essential for "Compact" look.
3.  **Deferred Logic** (Phases 3.3, 3.4) - Functional parity.
4.  **History & Polish** (Phases 3.2, 3.5, 4) - Quality of Life.
