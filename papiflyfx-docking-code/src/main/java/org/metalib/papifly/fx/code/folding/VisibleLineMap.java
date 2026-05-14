package org.metalib.papifly.fx.code.folding;

import java.util.Arrays;

public final class VisibleLineMap {

    private int[] visibleToLogical = new int[0];
    private int[] logicalToVisible = new int[0];

    public void rebuild(int logicalLineCount, FoldMap foldMap) {
        int safeCount = Math.max(0, logicalLineCount);
        if (safeCount == 0) {
            visibleToLogical = new int[0];
            logicalToVisible = new int[0];
            return;
        }
        FoldMap safeFoldMap = foldMap == null ? FoldMap.empty() : foldMap;
        logicalToVisible = new int[safeCount];
        Arrays.fill(logicalToVisible, -1);
        int visibleCount = 0;
        for (int logicalLine = 0; logicalLine < safeCount; logicalLine++) {
            if (!safeFoldMap.isHiddenLine(logicalLine)) {
                visibleCount++;
            }
        }
        if (visibleCount == 0) {
            visibleToLogical = new int[]{0};
            logicalToVisible[0] = 0;
            return;
        }
        visibleToLogical = new int[visibleCount];
        int visibleLine = 0;
        for (int logicalLine = 0; logicalLine < safeCount; logicalLine++) {
            if (safeFoldMap.isHiddenLine(logicalLine)) {
                continue;
            }
            visibleToLogical[visibleLine] = logicalLine;
            logicalToVisible[logicalLine] = visibleLine;
            visibleLine++;
        }
    }

    public int visibleCount() {
        return visibleToLogical.length;
    }

    public int logicalCount() {
        return logicalToVisible.length;
    }

    public int visibleToLogical(int visibleLine) {
        if (visibleToLogical.length == 0) {
            return 0;
        }
        int safe = clamp(visibleLine, 0, visibleToLogical.length - 1);
        return visibleToLogical[safe];
    }

    public int logicalToVisible(int logicalLine) {
        if (logicalToVisible.length == 0) {
            return -1;
        }
        int safe = clamp(logicalLine, 0, logicalToVisible.length - 1);
        return logicalToVisible[safe];
    }

    public boolean isHiddenLogicalLine(int logicalLine) {
        return logicalToVisible(logicalLine) < 0;
    }

    public int nearestVisibleIndexForLogical(int logicalLine) {
        if (visibleToLogical.length == 0 || logicalToVisible.length == 0) {
            return -1;
        }
        int safe = clamp(logicalLine, 0, logicalToVisible.length - 1);
        int direct = logicalToVisible[safe];
        if (direct >= 0) {
            return direct;
        }
        for (int offset = 1; offset < logicalToVisible.length; offset++) {
            int previous = safe - offset;
            if (previous >= 0 && logicalToVisible[previous] >= 0) {
                return logicalToVisible[previous];
            }
            int next = safe + offset;
            if (next < logicalToVisible.length && logicalToVisible[next] >= 0) {
                return logicalToVisible[next];
            }
        }
        return visibleToLogical.length > 0 ? 0 : -1;
    }

    public int nearestVisibleLogicalLine(int logicalLine) {
        int visible = nearestVisibleIndexForLogical(logicalLine);
        return visible < 0 ? 0 : visibleToLogical[visible];
    }

    public int previousVisibleLogicalLine(int logicalLine) {
        if (visibleToLogical.length == 0 || logicalToVisible.length == 0) {
            return 0;
        }
        int safe = clamp(logicalLine, 0, logicalToVisible.length - 1);
        for (int line = safe - 1; line >= 0; line--) {
            if (logicalToVisible[line] >= 0) {
                return line;
            }
        }
        return nearestVisibleLogicalLine(safe);
    }

    public int nextVisibleLogicalLine(int logicalLine) {
        if (visibleToLogical.length == 0 || logicalToVisible.length == 0) {
            return 0;
        }
        int safe = clamp(logicalLine, 0, logicalToVisible.length - 1);
        for (int line = safe + 1; line < logicalToVisible.length; line++) {
            if (logicalToVisible[line] >= 0) {
                return line;
            }
        }
        return nearestVisibleLogicalLine(safe);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}

