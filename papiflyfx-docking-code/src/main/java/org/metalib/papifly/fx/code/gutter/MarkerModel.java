package org.metalib.papifly.fx.code.gutter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Model holding line markers (errors, breakpoints, bookmarks, etc.).
 * Thread-safe for concurrent add/remove from background threads.
 */
public class MarkerModel {

    /**
     * Creates an empty marker model.
     */
    public MarkerModel() {
        // Default constructor for editor composition.
    }

    /**
     * Listener notified when markers change.
     */
    @FunctionalInterface
    public interface MarkerChangeListener {
        /**
         * Called when marker state has changed.
         */
        void markersChanged();
    }

    private final Map<Integer, List<Marker>> markersByLine = new ConcurrentHashMap<>();
    private final List<MarkerChangeListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a marker change listener.
     *
     * @param listener listener to register
     */
    public void addChangeListener(MarkerChangeListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a marker change listener.
     *
     * @param listener listener to remove
     */
    public void removeChangeListener(MarkerChangeListener listener) {
        listeners.remove(listener);
    }

    /**
     * Adds a marker.
     *
     * @param marker marker to add
     */
    public void addMarker(Marker marker) {
        markersByLine.computeIfAbsent(marker.line(), k -> new CopyOnWriteArrayList<>()).add(marker);
        fireChanged();
    }

    /**
     * Removes a specific marker.
     *
     * @param marker marker to remove
     */
    public void removeMarker(Marker marker) {
        List<Marker> lineMarkers = markersByLine.get(marker.line());
        if (lineMarkers != null && lineMarkers.remove(marker)) {
            if (lineMarkers.isEmpty()) {
                markersByLine.remove(marker.line());
            }
            fireChanged();
        }
    }

    /**
     * Removes all markers for a line.
     *
     * @param line zero-based line index
     */
    public void clearLine(int line) {
        if (markersByLine.remove(line) != null) {
            fireChanged();
        }
    }

    /**
     * Removes all markers.
     */
    public void clearAll() {
        if (!markersByLine.isEmpty()) {
            markersByLine.clear();
            fireChanged();
        }
    }

    /**
     * Returns markers for a given line (unmodifiable).
     *
     * @param line zero-based line index
     * @return unmodifiable markers for the line
     */
    public List<Marker> getMarkersForLine(int line) {
        List<Marker> markers = markersByLine.get(line);
        return markers == null ? List.of() : Collections.unmodifiableList(markers);
    }

    /**
     * Returns true if any marker exists for the given line.
     *
     * @param line zero-based line index
     * @return {@code true} when at least one marker exists on the line
     */
    public boolean hasMarkers(int line) {
        List<Marker> markers = markersByLine.get(line);
        return markers != null && !markers.isEmpty();
    }

    /**
     * Returns all markers across all lines.
     *
     * @return unmodifiable list of all markers
     */
    public List<Marker> getAllMarkers() {
        List<Marker> all = new ArrayList<>();
        for (List<Marker> lineMarkers : markersByLine.values()) {
            all.addAll(lineMarkers);
        }
        return Collections.unmodifiableList(all);
    }

    /**
     * Returns the highest-priority marker type for a line, or null if none.
     *
     * @param line zero-based line index
     * @return highest-priority marker type, or {@code null} when none exist
     */
    public MarkerType getHighestPriorityType(int line) {
        List<Marker> markers = markersByLine.get(line);
        if (markers == null || markers.isEmpty()) {
            return null;
        }
        MarkerType best = null;
        for (Marker m : markers) {
            if (best == null || m.type().priority() < best.priority()) {
                best = m.type();
            }
        }
        return best;
    }

    private void fireChanged() {
        for (MarkerChangeListener listener : listeners) {
            listener.markersChanged();
        }
    }
}
