# PapiflyFX Code Editor Actions Requirements

This directory contains source requirements and normalized planning/spec documents for keyboard and mouse behavior in `papiflyfx-docking-code`.

Current implementation status (2026-02-21): Phases 0–6 plus Addenda 0–3 complete (`Hardening and Performance`, `Page Navigation and Selection`, macOS `Cmd+Home/End` document-boundary aliases, caret blinking, and vertical caret-column preservation delivered).

## Documents

- `spec.md` — normalized command-level action specification for implementation/testing.
- `spec-add0.md` — implemented addendum for `Page Up` / `Page Down` behavior.
- `spec-add1.md` — implemented addendum for macOS `Cmd+Home` / `Cmd+End` document jumps.
- `spec-add2.md` — implemented addendum for caret blinking behavior and lifecycle.
- `spec-add3.md` — implemented addendum for vertical movement preferred-column preservation.
- `implementation.md` — phased implementation plan for missing actions.
- `PROGRESS.md` — current status of implemented vs pending actions.
- `userguide.md` — end-user reference for all supported keyboard and mouse commands.
- `README.md` — raw requirement reference (below).

## Raw Requirement Reference

Here is a list of typical key and mouse actions for a plain text editor, separated by macOS and Windows. These shortcuts reflect industry-standard conventions found in popular modern editors like Visual Studio Code and Sublime Text.

### Windows Key / Mouse Actions

**Basic Editing & Navigation**
*   **Undo / Redo:** `Ctrl + Z` to undo, `Ctrl + Y` to redo.
*   **Select Word:** `Ctrl + D` to select the current word (pressing it again selects the next occurrence).
*   **Select Line:** `Ctrl + L` to select the entire current line.
*   **Line Navigation:** `Home` to jump to the beginning of the line, and `End` to jump to the end of the line.
*   **Document Navigation:** `Ctrl + Home` to jump to the start of the file, and `Ctrl + End` to jump to the end of the file.

**Advanced Text Manipulation**
*   **Delete Line:** `Ctrl + Shift + K` to delete the entire current line without highlighting it first.
*   **Move Line:** `Alt + Up Arrow` or `Alt + Down Arrow` to move the current line (or selected block) up or down.
*   **Copy/Duplicate Line:** `Shift + Alt + Up Arrow` or `Shift + Alt + Down Arrow` to duplicate the current line directly above or below it.
*   **Join Lines:** `Ctrl + J` to append the line below to the current line, removing unnecessary whitespace.

**Multi-Cursor & Mouse Actions**
*   **Add Cursor (Mouse):** `Alt + Click` allows you to place multiple cursors anywhere in the document. *(Note: Some editors allow configuring this to `Ctrl + Click`).*
*   **Column / Box Selection (Mouse):** Hold `Shift + Alt` while dragging the mouse to select a rectangular block of text. Alternatively, dragging with the `Middle Mouse Button` achieves the same result.
*   **Add Cursor (Keyboard):** `Ctrl + Alt + Up Arrow` or `Ctrl + Alt + Down Arrow` adds a new cursor exactly one line above or below the current position.
*   **Select All Occurrences:** `Ctrl + Shift + L` selects all instances of the currently highlighted word simultaneously.

**Search & Replace**
*   **Find:** `Ctrl + F` to search within the current file.
*   **Replace:** `Ctrl + H` to open the replace interface.
*   **Find in Files:** `Ctrl + Shift + F` to search across all files in a directory or project.

---

### macOS Key / Mouse Actions

**Basic Editing & Navigation**
*   **Undo / Redo:** `Cmd + Z` to undo, `Shift + Cmd + Z` to redo.
*   **Select Word:** `Cmd + D` to select the current word (pressing it again selects the next matching occurrence).
*   **Select Line:** `Cmd + L` to select the entire current line.
*   **Line Navigation:** `Cmd + Left Arrow` to jump to the beginning of the line, and `Cmd + Right Arrow` to jump to the end.
*   **Document Navigation:** `Cmd + Up Arrow` to jump to the top of the file, and `Cmd + Down Arrow` to jump to the bottom. Full-size keyboards also commonly use `Cmd + Home` / `Cmd + End` for the same actions.

**Advanced Text Manipulation**
*   **Delete Line:** `Cmd + Shift + K` to delete the current line.
*   **Move Line:** `Option + Up Arrow` or `Option + Down Arrow` to swap the current line up or down.
*   **Copy/Duplicate Line:** `Option + Shift + Up Arrow` or `Option + Shift + Down Arrow` to copy the current line above or below.
*   **Join Lines:** `Cmd + J` to merge the current line with the line below it.

**Multi-Cursor & Mouse Actions**
*   **Add Cursor (Mouse):** `Option + Click` to manually spawn secondary cursors at your mouse pointer. *(Note: Often configurable to `Cmd + Click`).*
*   **Column / Box Selection (Mouse):** Hold `Shift + Option` while dragging the mouse, or use `Option + Left Mouse Button` drag to select a vertical rectangle of text.
*   **Add Cursor (Keyboard):** `Cmd + Option + Up Arrow` or `Cmd + Option + Down Arrow` to spawn cursors vertically in a straight line.
*   **Select All Occurrences:** `Cmd + Shift + L` places a cursor at every instance of the currently highlighted text.

**Search & Replace**
*   **Find:** `Cmd + F` to search the current file.
*   **Replace:** `Cmd + Option + F` to search and replace in the current document.
*   **Find in Files:** `Cmd + Shift + F` to search globally across the entire project directory.

**Word Navigation**
*   **Jump by Word:** Use **`Ctrl + Left/Right Arrow`** (Windows/Linux) or **`Alt/Option + Left/Right Arrow`** (macOS) to move the cursor by whole words instead of single characters.
*   **Subword Navigation:** Advanced editors allow jumping between "subwords" or camelCase/PascalCase boundaries (e.g., jumping between the distinct words in `thisIsACamelCaseExample`) using the same arrow shortcuts. In PyCharm, this is called "CamelHumps" navigation and can even be configured to select subwords with a standard mouse double-click.

**Word Selection**
*   **Highlight Word:** Hold **`Ctrl + Shift + Left/Right Arrow`** (Windows/Linux) or **`Alt + Shift + Left/Right Arrow`** (macOS) to highlight text expanding outward by entire words at a time.
*   **Select Current Word:** Press **`Ctrl + D`** (Windows/Linux) or **`Cmd + D`** (macOS) to instantly select the word currently under your cursor.
*   **Select Next Occurrence:** Pressing **`Ctrl + D`** (**`Cmd + D`** on Mac) repeatedly will spawn multiple cursors and select the next identical occurrences of your highlighted word down the document.
*   **Skip / Undo Selection:** If you accidentally grab a word you don't want while using the `Ctrl+D` multi-cursor feature, you can skip the current match by pressing **`Ctrl + K, Ctrl + D`** (**`Cmd + K, Cmd + D`** on Mac). Alternatively, pressing **`Ctrl + U`** will undo the last selected occurrence.
*   **Select All Occurrences:** Press **`Ctrl + Shift + L`** (**`Cmd + Shift + L`** on Mac) to instantly place a cursor at every instance of the currently highlighted word in the entire document. Another default shortcut for this in VS Code is **`Ctrl + F2`** (**`Cmd + F2`** on Mac).
*   **Expand / Shrink Selection:** Press **`Shift + Alt + Right Arrow`** (Windows/Linux) or **`Ctrl + Shift + Cmd + Right Arrow`** (macOS) to intelligently expand your selection from the current word to the surrounding semantic block (like a sentence, paragraph, or function). Use the **`Left Arrow`** equivalent to shrink the selection back down. In JetBrains editors like PyCharm, this semantic selection is done with **`Ctrl + W`** to expand and **`Ctrl + Shift + W`** to shrink.

**Word Deletion**
*   **Delete Word Backwards:** Press **`Ctrl + Backspace`** (Windows/Linux) or **`Option + Delete`** (macOS) to delete the entire word immediately to the left of your cursor.
*   **Delete Word Forwards:** Press **`Ctrl + Delete`** (Windows/Linux) or **`Option + Forward Delete`** (macOS) to delete the entire word immediately to the right of your cursor.

Here is a breakdown of the most common and powerful mouse and mouse+key combinations used in modern text editors, categorized by their function:

**Basic Selection & Navigation**
*   **Continuous Selection:** Standard clicking and dragging creates a range selection from where you press the mouse to where you release it.
*   **Extend Selection:** **`Shift` + Click** extends your text selection from your current caret position to the exact location you clicked.
*   **Select Whole Line:** **Triple-Clicking** anywhere on a line will highlight the entire wrapped line of text.
*   **Go to Definition / Open Link:** **`Ctrl` + Click** (Windows/Linux) or **`Cmd` + Click** (macOS) on a function, variable, or URL will jump to its definition or open the link in your default browser.

**Multiple Cursors (Multi-Select)**
*   **Add Arbitrary Cursors:** **`Alt` + Click** (Windows/Linux) or **`Option` + Click** (macOS) spawns secondary cursors wherever you click, allowing you to edit multiple different parts of the document simultaneously. *(Note: In VS Code, you can configure this to be `Ctrl/Cmd` + Click instead).*
*   **Add/Remove Specific Cursors:** In JetBrains IDEs, **`Alt` + `Shift` + Click** adds a new caret, and clicking on an existing caret with the same modifier removes it. Double-clicking a word while holding **`Alt` + `Shift`** will select multiple different words across the file.

**Column / Box Selection (Rectangular Blocks)**
These actions allow you to select a vertical slice of text, ignoring standard line wrapping:
*   **Standard Box Drag:** Hold **`Shift` + `Alt`** (Windows/Linux) or **`Shift` + `Option`** (macOS) while dragging the mouse to select a rectangular block of text.
*   **Middle Mouse Drag:** Clicking and dragging with the **Middle Mouse Button** (the scroll wheel click) achieves the same rectangular selection without needing to hold any keyboard keys.
*   **Alternative Editor Bindings:** In Sublime Text, you can also use **`Right Mouse Button` + `Shift`** (Windows/Linux) or **`Left Mouse Button` + `Option`** (macOS) to drag a column selection. In editors like jEdit, holding **`Ctrl` + Drag** creates a rectangular selection.

**Zooming and Display**
*   **Scroll Wheel Zoom:** Hold **`Ctrl`** (Windows/Linux) or **`Cmd`** (macOS) and scroll the **Mouse Wheel** up or down to quickly increase or decrease the editor's font size/zoom level.
*   **Trackpad Zoom:** On a Mac, while holding the **`Cmd`** key, sliding two fingers upward on the trackpad zooms out, and sliding downward zooms in.

**Code Folding**
*   **Fold/Unfold Recursively:** While you can normally click the folding icons (the arrows or +/- in the left gutter) to collapse a code block, holding **`Shift` + Click** on the folding icon will fold or unfold that region *and* all nested sub-regions inside of it.
