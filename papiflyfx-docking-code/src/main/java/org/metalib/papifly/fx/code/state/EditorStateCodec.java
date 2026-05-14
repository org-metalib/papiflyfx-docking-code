package org.metalib.papifly.fx.code.state;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts editor state between DTO and generic map payload.
 */
public final class EditorStateCodec {

    private static final int MAX_SECONDARY_CARETS = 2_048;

    private static final String KEY_FILE_PATH = "filePath";
    private static final String KEY_CURSOR_LINE = "cursorLine";
    private static final String KEY_CURSOR_COLUMN = "cursorColumn";
    private static final String KEY_ANCHOR_LINE = "anchorLine";
    private static final String KEY_ANCHOR_COLUMN = "anchorColumn";
    private static final String KEY_VERTICAL_SCROLL_OFFSET = "verticalScrollOffset";
    private static final String KEY_HORIZONTAL_SCROLL_OFFSET = "horizontalScrollOffset";
    private static final String KEY_WORD_WRAP = "wordWrap";
    private static final String KEY_LANGUAGE_ID = "languageId";
    private static final String KEY_FOLDED_LINES = "foldedLines";
    private static final String KEY_FOLDED_REGIONS = "foldedRegions";
    private static final String KEY_SECONDARY_CARETS = "secondaryCarets";
    private static final String KEY_CARET_LINE = "caretLine";
    private static final String KEY_CARET_COLUMN = "caretColumn";
    private static final String KEY_FOLD_START_LINE = "startLine";
    private static final String KEY_FOLD_END_LINE = "endLine";
    private static final String KEY_FOLD_KIND = "kind";

    private EditorStateCodec() {
    }

    /**
     * Converts EditorStateData to map payload.
     *
     * @param state editor state snapshot
     * @return serializable map payload
     */
    public static Map<String, Object> toMap(EditorStateData state) {
        EditorStateData safe = state == null ? EditorStateData.empty() : state;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(KEY_FILE_PATH, safe.filePath());
        map.put(KEY_CURSOR_LINE, safe.cursorLine());
        map.put(KEY_CURSOR_COLUMN, safe.cursorColumn());
        map.put(KEY_ANCHOR_LINE, safe.anchorLine());
        map.put(KEY_ANCHOR_COLUMN, safe.anchorColumn());
        map.put(KEY_VERTICAL_SCROLL_OFFSET, safe.verticalScrollOffset());
        map.put(KEY_HORIZONTAL_SCROLL_OFFSET, safe.horizontalScrollOffset());
        map.put(KEY_WORD_WRAP, safe.wordWrap());
        map.put(KEY_LANGUAGE_ID, safe.languageId());
        map.put(KEY_FOLDED_LINES, safe.foldedLines());
        map.put(KEY_FOLDED_REGIONS, toFoldRegionMapList(safe.foldedRegions()));
        map.put(KEY_SECONDARY_CARETS, toCaretMapList(safe.secondaryCarets()));
        return map;
    }

    /**
     * Converts map payload to EditorStateData.
     *
     * @param state serialized map payload
     * @return decoded editor state snapshot
     */
    public static EditorStateData fromMap(Map<String, Object> state) {
        if (state == null || state.isEmpty()) {
            return EditorStateData.empty();
        }
        int cursorLine = asInt(state.get(KEY_CURSOR_LINE), 0);
        int cursorColumn = asInt(state.get(KEY_CURSOR_COLUMN), 0);
        return new EditorStateData(
            asString(state.get(KEY_FILE_PATH), ""),
            cursorLine,
            cursorColumn,
            asDouble(state.get(KEY_VERTICAL_SCROLL_OFFSET), 0.0),
            asDouble(state.get(KEY_HORIZONTAL_SCROLL_OFFSET), 0.0),
            asBoolean(state.get(KEY_WORD_WRAP), false),
            asString(state.get(KEY_LANGUAGE_ID), "plain-text"),
            asIntList(state.get(KEY_FOLDED_LINES)),
            asFoldRegionRefList(state.get(KEY_FOLDED_REGIONS)),
            asInt(state.get(KEY_ANCHOR_LINE), cursorLine),
            asInt(state.get(KEY_ANCHOR_COLUMN), cursorColumn),
            asCaretStateList(state.get(KEY_SECONDARY_CARETS))
        );
    }

    private static String asString(Object value, String fallback) {
        if (value instanceof String text) {
            return text;
        }
        return fallback;
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return fallback;
    }

    private static double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return fallback;
    }

    private static List<Integer> asIntList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        Set<Integer> result = new LinkedHashSet<>(list.size());
        for (Object item : list) {
            if (item instanceof Number number) {
                int line = number.intValue();
                if (line >= 0) {
                    result.add(line);
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<CaretStateData> asCaretStateList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        Set<CaretStateData> result = new LinkedHashSet<>(Math.min(list.size(), MAX_SECONDARY_CARETS));
        for (Object item : list) {
            if (result.size() >= MAX_SECONDARY_CARETS) {
                break;
            }
            if (!(item instanceof Map<?, ?> caretMap)) {
                continue;
            }
            Integer caretLine = asNullableInt(caretMap.get(KEY_CARET_LINE));
            Integer caretColumn = asNullableInt(caretMap.get(KEY_CARET_COLUMN));
            if (caretLine == null || caretColumn == null) {
                continue;
            }
            int anchorLine = asInt(caretMap.get(KEY_ANCHOR_LINE), caretLine);
            int anchorColumn = asInt(caretMap.get(KEY_ANCHOR_COLUMN), caretColumn);
            result.add(new CaretStateData(anchorLine, anchorColumn, caretLine, caretColumn));
        }
        return List.copyOf(result);
    }

    private static List<FoldRegionRef> asFoldRegionRefList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<FoldRegionRef> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> foldMap)) {
                continue;
            }
            Integer startLine = asNullableInt(foldMap.get(KEY_FOLD_START_LINE));
            Integer endLine = asNullableInt(foldMap.get(KEY_FOLD_END_LINE));
            if (startLine == null || endLine == null) {
                continue;
            }
            String kind = asString(foldMap.get(KEY_FOLD_KIND), "");
            result.add(new FoldRegionRef(startLine, kind, endLine));
        }
        return List.copyOf(result);
    }

    private static Integer asNullableInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static List<Map<String, Object>> toCaretMapList(List<CaretStateData> carets) {
        if (carets == null || carets.isEmpty()) {
            return List.of();
        }
        int maxCount = Math.min(carets.size(), MAX_SECONDARY_CARETS);
        List<Map<String, Object>> result = new ArrayList<>(maxCount);
        for (CaretStateData caret : carets) {
            if (result.size() >= maxCount) {
                break;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_ANCHOR_LINE, caret.anchorLine());
            map.put(KEY_ANCHOR_COLUMN, caret.anchorColumn());
            map.put(KEY_CARET_LINE, caret.caretLine());
            map.put(KEY_CARET_COLUMN, caret.caretColumn());
            result.add(map);
        }
        return List.copyOf(result);
    }

    private static List<Map<String, Object>> toFoldRegionMapList(List<FoldRegionRef> foldRegions) {
        if (foldRegions == null || foldRegions.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(foldRegions.size());
        for (FoldRegionRef foldRegion : foldRegions) {
            if (foldRegion == null) {
                continue;
            }
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(KEY_FOLD_START_LINE, foldRegion.startLine());
            map.put(KEY_FOLD_END_LINE, foldRegion.endLine());
            map.put(KEY_FOLD_KIND, foldRegion.kind());
            result.add(map);
        }
        return List.copyOf(result);
    }
}
