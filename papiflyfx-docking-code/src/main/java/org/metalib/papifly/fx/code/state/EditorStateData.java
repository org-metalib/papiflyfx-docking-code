package org.metalib.papifly.fx.code.state;

import java.util.List;

/**
 * Serializable editor state payload &mdash; v3 persistence contract.
 *
 * <h2>V3 field invariants</h2>
 * <ul>
 *   <li>{@code filePath} &ndash; nullable input normalized to {@code ""}.</li>
 *   <li>{@code cursorLine} &ndash; {@code >= 0}.</li>
 *   <li>{@code cursorColumn} &ndash; {@code >= 0}.</li>
 *   <li>{@code anchorLine} &ndash; {@code >= 0}.</li>
 *   <li>{@code anchorColumn} &ndash; {@code >= 0}.</li>
 *   <li>{@code verticalScrollOffset} &ndash; {@code >= 0.0}.</li>
 *   <li>{@code horizontalScrollOffset} &ndash; {@code >= 0.0}.</li>
 *   <li>{@code wordWrap} &ndash; persisted wrap-mode flag.</li>
 *   <li>{@code languageId} &ndash; blank/null normalized to {@code "plain-text"}.</li>
 *   <li>{@code foldedLines} &ndash; non-null immutable list (empty in MVP).</li>
 *   <li>{@code secondaryCarets} &ndash; non-null immutable list.</li>
 * </ul>
 *
 * <p>The canonical map key set used by {@link EditorStateCodec} is:
 * {@code filePath, cursorLine, cursorColumn, anchorLine, anchorColumn, verticalScrollOffset,
 * horizontalScrollOffset, wordWrap, languageId, foldedLines, secondaryCarets}.
 * Unknown keys present in a deserialized map are
 * silently ignored; missing keys default to the invariants above. V1 payloads without anchor and
 * secondary caret fields are migrated by defaulting anchor to cursor and secondary carets to empty.
 * V2 payloads default horizontal scroll to {@code 0.0} and wrap to {@code false}.</p>
 *
 * @param filePath persisted file path, normalized to non-null
 * @param cursorLine zero-based primary caret line
 * @param cursorColumn zero-based primary caret column
 * @param verticalScrollOffset vertical scroll offset in pixels
 * @param horizontalScrollOffset horizontal scroll offset in pixels
 * @param wordWrap persisted wrap-mode flag
 * @param languageId persisted language id
 * @param foldedLines persisted folded line indices
 * @param anchorLine zero-based selection anchor line
 * @param anchorColumn zero-based selection anchor column
 * @param secondaryCarets persisted secondary caret snapshots
 */
public record EditorStateData(
    String filePath,
    int cursorLine,
    int cursorColumn,
    double verticalScrollOffset,
    double horizontalScrollOffset,
    boolean wordWrap,
    String languageId,
    List<Integer> foldedLines,
    List<FoldRegionRef> foldedRegions,
    int anchorLine,
    int anchorColumn,
    List<CaretStateData> secondaryCarets
) {
    /**
     * Creates normalized state defaults.
     */
    public EditorStateData {
        filePath = filePath == null ? "" : filePath;
        cursorLine = Math.max(0, cursorLine);
        cursorColumn = Math.max(0, cursorColumn);
        anchorLine = Math.max(0, anchorLine);
        anchorColumn = Math.max(0, anchorColumn);
        verticalScrollOffset = Math.max(0.0, verticalScrollOffset);
        horizontalScrollOffset = Math.max(0.0, horizontalScrollOffset);
        languageId = languageId == null || languageId.isBlank() ? "plain-text" : languageId;
        foldedLines = foldedLines == null ? List.of() : List.copyOf(foldedLines);
        foldedRegions = foldedRegions == null ? List.of() : List.copyOf(foldedRegions);
        secondaryCarets = secondaryCarets == null ? List.of() : List.copyOf(secondaryCarets);
    }

    /**
     * Backward-compatible constructor for callers still creating v1-shaped state.
     *
     * @param filePath persisted file path
     * @param cursorLine zero-based primary caret line
     * @param cursorColumn zero-based primary caret column
     * @param verticalScrollOffset vertical scroll offset in pixels
     * @param languageId persisted language id
     * @param foldedLines persisted folded line indices
     */
    public EditorStateData(
        String filePath,
        int cursorLine,
        int cursorColumn,
        double verticalScrollOffset,
        String languageId,
        List<Integer> foldedLines
    ) {
        this(
            filePath,
            cursorLine,
            cursorColumn,
            verticalScrollOffset,
            0.0,
            false,
            languageId,
            foldedLines,
            List.of(),
            cursorLine,
            cursorColumn,
            List.of()
        );
    }

    /**
     * Backward-compatible constructor for callers still creating v2-shaped state.
     *
     * @param filePath persisted file path
     * @param cursorLine zero-based primary caret line
     * @param cursorColumn zero-based primary caret column
     * @param verticalScrollOffset vertical scroll offset in pixels
     * @param languageId persisted language id
     * @param foldedLines persisted folded line indices
     * @param anchorLine zero-based selection anchor line
     * @param anchorColumn zero-based selection anchor column
     * @param secondaryCarets persisted secondary caret snapshots
     */
    public EditorStateData(
        String filePath,
        int cursorLine,
        int cursorColumn,
        double verticalScrollOffset,
        String languageId,
        List<Integer> foldedLines,
        int anchorLine,
        int anchorColumn,
        List<CaretStateData> secondaryCarets
    ) {
        this(
            filePath,
            cursorLine,
            cursorColumn,
            verticalScrollOffset,
            0.0,
            false,
            languageId,
            foldedLines,
            List.of(),
            anchorLine,
            anchorColumn,
            secondaryCarets
        );
    }

    public EditorStateData(
        String filePath,
        int cursorLine,
        int cursorColumn,
        double verticalScrollOffset,
        double horizontalScrollOffset,
        boolean wordWrap,
        String languageId,
        List<Integer> foldedLines,
        int anchorLine,
        int anchorColumn,
        List<CaretStateData> secondaryCarets
    ) {
        this(
            filePath,
            cursorLine,
            cursorColumn,
            verticalScrollOffset,
            horizontalScrollOffset,
            wordWrap,
            languageId,
            foldedLines,
            List.of(),
            anchorLine,
            anchorColumn,
            secondaryCarets
        );
    }

    /**
     * Returns an empty default state.
     *
     * @return immutable default state with zeroed offsets and empty caret collections
     */
    public static EditorStateData empty() {
        return new EditorStateData("", 0, 0, 0.0, 0.0, false, "plain-text", List.of(), List.of(), 0, 0, List.of());
    }
}
