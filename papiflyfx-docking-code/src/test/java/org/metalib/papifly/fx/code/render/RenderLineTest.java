package org.metalib.papifly.fx.code.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderLineTest {

    @Test
    void recordFieldsAreAccessible() {
        RenderLine rl = new RenderLine(5, 2, 8, "hello world", 100.0, List.of());
        assertEquals(5, rl.lineIndex());
        assertEquals(2, rl.startColumn());
        assertEquals(8, rl.endColumn());
        assertEquals("hello world", rl.text());
        assertEquals(100.0, rl.y());
        assertEquals(List.of(), rl.tokens());
    }

    @Test
    void emptyLineText() {
        RenderLine rl = new RenderLine(0, 0, 0, "", 0.0, List.of());
        assertEquals(0, rl.lineIndex());
        assertEquals(0, rl.startColumn());
        assertEquals(0, rl.endColumn());
        assertEquals("", rl.text());
        assertEquals(0.0, rl.y());
        assertEquals(List.of(), rl.tokens());
    }

    @Test
    void equalityAndHashCode() {
        RenderLine a = new RenderLine(1, 0, 3, "abc", 20.0, List.of());
        RenderLine b = new RenderLine(1, 0, 3, "abc", 20.0, List.of());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
