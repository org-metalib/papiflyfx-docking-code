package org.metalib.papifly.fx.code.state;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorStateCodecTest {

    @Test
    void roundTripPreservesValues() {
        EditorStateData state = new EditorStateData(
            "/tmp/demo.txt",
            12,
            8,
            120.5,
            "java",
            List.of(1, 4, 7)
        );

        Map<String, Object> map = EditorStateCodec.toMap(state);
        EditorStateData restored = EditorStateCodec.fromMap(map);

        assertEquals(state, restored);
    }

    @Test
    void roundTripPreservesSecondaryCaretsAndPrimarySelection() {
        EditorStateData state = new EditorStateData(
            "/tmp/demo.txt",
            10,
            12,
            32.0,
            "java",
            List.of(2),
            10,
            4,
            List.of(
                new CaretStateData(1, 1, 1, 3),
                new CaretStateData(3, 2, 3, 2)
            )
        );

        Map<String, Object> map = EditorStateCodec.toMap(state);
        EditorStateData restored = EditorStateCodec.fromMap(map);

        assertEquals(state, restored);
    }

    @Test
    void fromMapWithNullReturnsEmpty() {
        EditorStateData result = EditorStateCodec.fromMap(null);
        assertEquals(EditorStateData.empty(), result);
    }

    @Test
    void fromMapWithEmptyMapReturnsEmpty() {
        EditorStateData result = EditorStateCodec.fromMap(Map.of());
        assertEquals(EditorStateData.empty(), result);
    }

    @Test
    void fromMapWithMissingKeysReturnsFallbackValues() {
        Map<String, Object> map = new HashMap<>();
        map.put("filePath", "/some/path.txt");

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals("/some/path.txt", result.filePath());
        assertEquals(0, result.cursorLine());
        assertEquals(0, result.cursorColumn());
        assertEquals(0.0, result.verticalScrollOffset());
        assertEquals(0.0, result.horizontalScrollOffset());
        assertEquals(false, result.wordWrap());
        assertEquals("plain-text", result.languageId());
        assertEquals(List.of(), result.foldedLines());
        assertEquals(List.of(), result.foldedRegions());
        assertEquals(0, result.anchorLine());
        assertEquals(0, result.anchorColumn());
        assertEquals(List.of(), result.secondaryCarets());
    }

    @Test
    void fromMapWithTypeMismatchedValuesReturnsFallbacks() {
        Map<String, Object> map = new HashMap<>();
        map.put("filePath", 42);
        map.put("cursorLine", "not-a-number");
        map.put("cursorColumn", List.of());
        map.put("anchorLine", "bad");
        map.put("anchorColumn", false);
        map.put("verticalScrollOffset", "bad");
        map.put("languageId", 99);
        map.put("foldedLines", "not-a-list");
        map.put("secondaryCarets", "not-a-list");

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals("", result.filePath());
        assertEquals(0, result.cursorLine());
        assertEquals(0, result.cursorColumn());
        assertEquals(0, result.anchorLine());
        assertEquals(0, result.anchorColumn());
        assertEquals(0.0, result.verticalScrollOffset());
        assertEquals(0.0, result.horizontalScrollOffset());
        assertEquals(false, result.wordWrap());
        assertEquals("plain-text", result.languageId());
        assertEquals(List.of(), result.foldedLines());
        assertEquals(List.of(), result.foldedRegions());
        assertEquals(List.of(), result.secondaryCarets());
    }

    @Test
    void fromMapFoldedLinesDropsNonNumbers() {
        Map<String, Object> map = new HashMap<>();
        map.put("foldedLines", Arrays.asList(1, "bad", 3, null, 5, -2, 3, 1));

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals(List.of(1, 3, 5), result.foldedLines());
    }

    @Test
    void fromMapV1DefaultsAnchorToCursorAndSecondaryCaretsToEmpty() {
        Map<String, Object> map = new HashMap<>();
        map.put("filePath", "/legacy.txt");
        map.put("cursorLine", 6);
        map.put("cursorColumn", 9);
        map.put("verticalScrollOffset", 12.5);
        map.put("languageId", "json");
        map.put("foldedLines", List.of(1, 2));

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals(6, result.cursorLine());
        assertEquals(9, result.cursorColumn());
        assertEquals(6, result.anchorLine());
        assertEquals(9, result.anchorColumn());
        assertEquals(List.of(), result.secondaryCarets());
    }

    @Test
    void fromMapSecondaryCaretsDropsInvalidEntries() {
        Map<String, Object> map = new HashMap<>();
        map.put("secondaryCarets", List.of(
            Map.of("anchorLine", 1, "anchorColumn", 1, "caretLine", 1, "caretColumn", 3),
            Map.of("anchorLine", 2, "anchorColumn", 2, "caretLine", 2),
            Map.of("anchorLine", "bad", "anchorColumn", 0, "caretLine", 3, "caretColumn", 4),
            Map.of("caretLine", 5, "caretColumn", 1),
            "not-a-map"
        ));

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals(
            List.of(
                new CaretStateData(1, 1, 1, 3),
                new CaretStateData(3, 0, 3, 4),
                new CaretStateData(5, 1, 5, 1)
            ),
            result.secondaryCarets()
        );
    }

    @Test
    void fromMapSecondaryCaretsCapsAndDeduplicates() {
        List<Map<String, Object>> serializedCarets = new ArrayList<>();
        serializedCarets.add(Map.of("anchorLine", 0, "anchorColumn", 0, "caretLine", 0, "caretColumn", 1));
        serializedCarets.add(Map.of("anchorLine", 0, "anchorColumn", 0, "caretLine", 0, "caretColumn", 1));
        for (int i = 1; i <= 2600; i++) {
            serializedCarets.add(Map.of("anchorLine", i, "anchorColumn", 0, "caretLine", i, "caretColumn", 1));
        }
        Map<String, Object> map = new HashMap<>();
        map.put("secondaryCarets", serializedCarets);

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals(2048, result.secondaryCarets().size());
        assertEquals(new CaretStateData(0, 0, 0, 1), result.secondaryCarets().getFirst());
    }

    @Test
    void toMapWithNullStateProducesEmptyDefaults() {
        Map<String, Object> map = EditorStateCodec.toMap(null);
        assertNotNull(map);
        EditorStateData roundTrip = EditorStateCodec.fromMap(map);
        assertEquals(EditorStateData.empty(), roundTrip);
    }

    // --- Forward/backward tolerance tests ---

    @Test
    void fromMapIgnoresUnknownKeys() {
        Map<String, Object> map = new HashMap<>();
        map.put("filePath", "/test.txt");
        map.put("cursorLine", 5);
        map.put("cursorColumn", 3);
        map.put("anchorLine", 5);
        map.put("anchorColumn", 1);
        map.put("verticalScrollOffset", 42.5);
        map.put("languageId", "java");
        map.put("foldedLines", List.of(1, 2));
        map.put("secondaryCarets", List.of(Map.of("caretLine", 9, "caretColumn", 2)));
        // Unknown keys from a future version
        map.put("futureField", "value");
        map.put("anotherFutureField", 999);

        EditorStateData result = EditorStateCodec.fromMap(map);

        assertEquals("/test.txt", result.filePath());
        assertEquals(5, result.cursorLine());
        assertEquals(3, result.cursorColumn());
        assertEquals(5, result.anchorLine());
        assertEquals(1, result.anchorColumn());
        assertEquals(42.5, result.verticalScrollOffset());
        assertEquals("java", result.languageId());
        assertEquals(List.of(1, 2), result.foldedLines());
        assertEquals(List.of(new CaretStateData(9, 2, 9, 2)), result.secondaryCarets());
    }

    @Test
    void roundTripPreservesAllV3Fields() {
        EditorStateData state = new EditorStateData(
            "/home/user/project/Main.java",
            42,
            15,
            350.75,
            128.5,
            true,
            "java",
            List.of(10, 20, 30),
            42,
            8,
            List.of(new CaretStateData(4, 4, 4, 6))
        );

        Map<String, Object> map = EditorStateCodec.toMap(state);
        EditorStateData restored = EditorStateCodec.fromMap(map);

        assertEquals(state.filePath(), restored.filePath());
        assertEquals(state.cursorLine(), restored.cursorLine());
        assertEquals(state.cursorColumn(), restored.cursorColumn());
        assertEquals(state.anchorLine(), restored.anchorLine());
        assertEquals(state.anchorColumn(), restored.anchorColumn());
        assertEquals(state.verticalScrollOffset(), restored.verticalScrollOffset());
        assertEquals(state.horizontalScrollOffset(), restored.horizontalScrollOffset());
        assertEquals(state.wordWrap(), restored.wordWrap());
        assertEquals(state.languageId(), restored.languageId());
        assertEquals(state.foldedLines(), restored.foldedLines());
        assertEquals(state.secondaryCarets(), restored.secondaryCarets());
    }

    @Test
    void toMapContainsExactV4KeySet() {
        EditorStateData state = new EditorStateData(
            "/file.txt", 1, 2, 3.0, "json", List.of(5), 1, 0, List.of()
        );
        Map<String, Object> map = EditorStateCodec.toMap(state);

        assertEquals(12, map.size());
        assertTrue(map.containsKey("filePath"));
        assertTrue(map.containsKey("cursorLine"));
        assertTrue(map.containsKey("cursorColumn"));
        assertTrue(map.containsKey("anchorLine"));
        assertTrue(map.containsKey("anchorColumn"));
        assertTrue(map.containsKey("verticalScrollOffset"));
        assertTrue(map.containsKey("horizontalScrollOffset"));
        assertTrue(map.containsKey("wordWrap"));
        assertTrue(map.containsKey("languageId"));
        assertTrue(map.containsKey("foldedLines"));
        assertTrue(map.containsKey("foldedRegions"));
        assertTrue(map.containsKey("secondaryCarets"));
    }
}
