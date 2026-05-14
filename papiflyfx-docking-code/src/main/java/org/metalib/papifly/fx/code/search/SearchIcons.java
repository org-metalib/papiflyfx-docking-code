package org.metalib.papifly.fx.code.search;

import javafx.scene.shape.SVGPath;
import org.metalib.papifly.fx.searchui.SearchIconPaths;

/**
 * Centralized SVG icon definitions for the search overlay.
 */
public final class SearchIcons {

    /**
     * Magnifier icon path.
     */
    public static final String SEARCH = SearchIconPaths.SEARCH;
    /**
     * Chevron-right icon path.
     */
    public static final String CHEVRON_RIGHT = SearchIconPaths.CHEVRON_RIGHT;
    /**
     * Chevron-down icon path.
     */
    public static final String CHEVRON_DOWN = SearchIconPaths.CHEVRON_DOWN;
    /**
     * Up arrow icon path.
     */
    public static final String ARROW_UP = SearchIconPaths.ARROW_UP;
    /**
     * Down arrow icon path.
     */
    public static final String ARROW_DOWN = SearchIconPaths.ARROW_DOWN;
    /**
     * Close icon path.
     */
    public static final String CLOSE = SearchIconPaths.CLOSE;
    /**
     * Filter icon path.
     */
    public static final String FILTER = SearchIconPaths.FILTER;

    private SearchIcons() {
    }

    /**
     * Creates a sized icon node from SVG path data.
     *
     * @param svgPath SVG path content
     * @param size target icon size in pixels
     * @return configured SVG path node
     */
    public static SVGPath createIcon(String svgPath, double size) {
        return SearchIconPaths.createIcon(svgPath, size);
    }
}
