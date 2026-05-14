# Summary of Specification Changes (additions2.md)

Based on the latest version of `spec.md`, the following major changes and requirements have been identified:

## 1. Major Scope Refinement (The "MVP" Focus)
- **Aggressive Scoping**: Multi-caret, block selection, and "Language Islands" (Markdown/HTML embedding) have been moved to **Post-MVP**.
- **MVP Languages**: Limited to Java, JSON, and JavaScript.
- **Canvas-Only Rendering**: Reconfirmed as the primary rendering strategy via a single `Canvas` and `GraphicsContext`.

## 2. New Architectural Requirements
- **Revision Tracking**: Added requirement for "revision checks" (Section 5.1). Token updates must be discarded if the document revision has changed since the lexing job started.
- **Disposal Contract**: Explicit lifecycle management (Section 5.2) requiring background worker termination and listener unbinding to prevent memory leaks in the docking environment.
- **State Migration**: `ContentStateAdapter` must now handle versioned state migration (Section 7), ensuring older session data doesn't crash the editor on restore.

## 3. Performance & Quality Benchmarks (Section 8)
- **100k Line Performance**: Clear targets for file opening (<= 2.0s) and memory usage (<= 350MB).
- **Latency Targets**: p95 <= 16ms (60 FPS) for typing and scrolling, which is a high bar for JavaFX Canvas.

## 4. Docking-Specific Logic
- **Theme Composition**: Since `Theme` is a `record`, the editor must use `CodeEditorTheme` as a separate POJO/Record that reacts to `DockManager.themeProperty()`.
- **Restore Fallback**: A specific order for content restoration was defined (Adapter -> Factory -> Placeholder) to ensure the UI never stays empty if a file is missing.

## 5. Implementation Findings & Risks
- **LineIndex**: A new requirement for an efficient mapping structure between offsets and line numbers, critical for fast navigation in large files.
- **TextSource**: Recommendation to start with `StringBuilder` but design for `Piece-Table` or `Rope` to meet the 350MB memory target for 100k lines.
- **Debounced Lexing**: Requirement to implement cancellation/debouncing for lexer jobs to avoid UI lag during rapid typing.
