package org.metalib.papifly.fx.code.state;

public record FoldRegionRef(
    int startLine,
    String kind,
    int endLine
) {
    public FoldRegionRef {
        startLine = Math.max(0, startLine);
        endLine = Math.max(startLine, endLine);
        kind = kind == null ? "" : kind.trim();
    }
}

