package org.metalib.papifly.fx.code.gutter;

/**
 * Types of markers that can appear in the gutter marker lane.
 */
public enum MarkerType {
    /**
     * Error marker type.
     */
    ERROR(0),
    /**
     * Warning marker type.
     */
    WARNING(1),
    /**
     * Informational marker type.
     */
    INFO(2),
    /**
     * Breakpoint marker type.
     */
    BREAKPOINT(3),
    /**
     * Bookmark marker type.
     */
    BOOKMARK(4);

    private final int priority;

    MarkerType(int priority) {
        this.priority = priority;
    }

    /**
     * Lower values mean higher display priority.
     *
     * @return marker display priority
     */
    public int priority() {
        return priority;
    }
}
