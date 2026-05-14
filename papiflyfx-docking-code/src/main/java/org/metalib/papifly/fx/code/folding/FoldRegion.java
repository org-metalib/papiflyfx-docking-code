package org.metalib.papifly.fx.code.folding;

public record FoldRegion(
    int startLine,
    int endLine,
    FoldKind kind,
    int depth,
    boolean collapsed
) {
    public FoldRegion {
        if (startLine < 0) {
            throw new IllegalArgumentException("startLine must be >= 0");
        }
        if (endLine < startLine) {
            throw new IllegalArgumentException("endLine must be >= startLine");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        depth = Math.max(1, depth);
    }

    public boolean containsLine(int line) {
        return line >= startLine && line <= endLine;
    }
}

