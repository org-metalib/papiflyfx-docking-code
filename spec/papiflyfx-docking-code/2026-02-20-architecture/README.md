# Text Editor Features

Here is a comprehensive feature list for a plain text editor, categorized by functionality:

**Core Editing & Navigation**
*   **Basic Text Manipulation:** Standard insert, delete, and replace operations.
*   **Caret Navigation:** Ability to move the cursor by character, word, subword (e.g., camelCase boundaries), logical line, and jump to the start or end of a file.
*   **Undo and Redo:** A robust history management system that supports unlimited undo/redo steps, coalesces sequential typing into single actions, and preserves cursor positions.
*   **Overtype Mode:** The ability to toggle between inserting characters and overwriting existing text.

**Advanced Text Manipulation**
*   **Multi-Cursor Editing:** Spawning multiple cursors to edit several parts of a document simultaneously, often placed via mouse clicks, keyboard shortcuts, or by selecting multiple occurrences of a word.
*   **Column/Box Selection:** Selecting a vertical rectangular slice of text across multiple lines for columnar editing.
*   **Line Operations:** Atomic actions to move lines up or down, duplicate lines, delete entire lines, and join lines together.
*   **Case Transformation:** Quickly converting selected text to uppercase, lowercase, title case, snake_case, or camelCase.
*   **Data Cleaning:** Sorting selected lines alphabetically or numerically, and deduplicating (removing) identical lines.
*   **Transposition:** Swapping adjacent characters or lines.

**Search & Replace**
*   **Incremental Search:** Real-time search highlighting matches within the viewport as the user types.
*   **Regex Support:** Advanced find and replace using regular expressions, including numbered/named capture groups, backreferences, and case-changing modifiers.
*   **Scope Options:** Toggles to match exact case, match whole words, or limit the search to a specific highlighted selection.
*   **Find in Files:** Global, multi-threaded searching across entire directories or projects, with the ability to filter results using glob patterns and include/exclude rules.

**Formatting & Display**
*   **Word Wrap:** Soft wrapping long lines to fit the viewport width without inserting hard return characters.
*   **Indentation Management:** Automatic and smart indentation (such as automatically indenting after an opening curly brace), converting tabs to spaces, and trimming trailing whitespace.
*   **Whitespace Visualization:** Rendering invisible characters like spaces, tabs, and line breaks as subtle graphical glyphs (e.g., center dots and arrows).
*   **Syntax Highlighting:** Coloring text based on language grammar, distinguishing keywords, strings, variables, and comments.
*   **Code Folding:** Expanding and collapsing hierarchical text blocks, such as functions or indented sections.
*   **Viewport Zoom:** Scaling the editor font size in and out for better readability.
*   **UI Guides:** Displaying line numbers in the margin, structure guide lines matching braces, and a right margin boundary.

**Accessibility**
*   **Keyboard Navigation:** Exhaustive command palettes and customizable keyboard shortcuts allowing use without a mouse.
*   **Focus Management:** Tab trapping toggles, ensuring that users can configure the `Tab` key to either insert spaces or move UI focus.
*   **Screen Reader Support:** Optimized text pagination and reading modes for compatibility with screen readers like NVDA, JAWS, or VoiceOver.
*   **High Contrast Themes:** Support for custom color themes, including specific high-contrast themes and palettes friendly to color vision deficiencies.

**Architecture & Performance**
*   **Massive File Support:** Under-the-hood implementation of performant data structures like Piece Tables, Gap Buffers, or Ropes to maintain speed and low memory usage during edits, regardless of file size.
*   **Virtualized Rendering:** Only calculating layout and rendering UI nodes for the text currently visible on the screen to prevent performance degradation.