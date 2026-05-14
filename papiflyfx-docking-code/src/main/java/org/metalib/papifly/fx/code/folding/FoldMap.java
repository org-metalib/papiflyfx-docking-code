package org.metalib.papifly.fx.code.folding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FoldMap {

    private static final FoldMap EMPTY = new FoldMap(List.of(), Set.of());

    private final List<FoldRegion> regions;
    private final Map<Integer, List<FoldRegion>> regionsByStartLine;
    private final Set<Integer> collapsedHeaderLines;
    private final List<LineRange> hiddenRanges;

    public FoldMap(List<FoldRegion> regions) {
        this(regions, extractCollapsedHeaders(regions));
    }

    private FoldMap(List<FoldRegion> regions, Set<Integer> requestedCollapsedHeaders) {
        List<FoldRegion> normalized = normalizeRegions(regions);
        this.regionsByStartLine = buildRegionsByStartLine(normalized);
        this.collapsedHeaderLines = Collections.unmodifiableSet(filterCollapsedHeaders(
            requestedCollapsedHeaders,
            regionsByStartLine.keySet()
        ));
        this.regions = applyCollapsedFlags(normalized, collapsedHeaderLines);
        this.hiddenRanges = buildHiddenRanges(this.regions, this.collapsedHeaderLines);
    }

    public static FoldMap empty() {
        return EMPTY;
    }

    public List<FoldRegion> regions() {
        return regions;
    }

    public boolean isEmpty() {
        return regions.isEmpty();
    }

    public boolean hasRegionStartingAt(int line) {
        List<FoldRegion> found = regionsByStartLine.get(line);
        return found != null && !found.isEmpty();
    }

    public List<FoldRegion> regionsStartingAt(int line) {
        List<FoldRegion> found = regionsByStartLine.get(line);
        return found == null ? List.of() : found;
    }

    public FoldRegion innermostRegionAt(int line) {
        FoldRegion best = null;
        for (FoldRegion region : regions) {
            if (!region.containsLine(line)) {
                continue;
            }
            if (best == null) {
                best = region;
                continue;
            }
            if (region.depth() > best.depth()) {
                best = region;
                continue;
            }
            if (region.depth() == best.depth()) {
                int bestSpan = best.endLine() - best.startLine();
                int candidateSpan = region.endLine() - region.startLine();
                if (candidateSpan < bestSpan) {
                    best = region;
                }
            }
        }
        return best;
    }

    public FoldRegion headerRegionAt(int line) {
        List<FoldRegion> startRegions = regionsByStartLine.get(line);
        if (startRegions == null || startRegions.isEmpty()) {
            return null;
        }
        FoldRegion best = startRegions.getFirst();
        for (int i = 1; i < startRegions.size(); i++) {
            FoldRegion candidate = startRegions.get(i);
            int bestSpan = best.endLine() - best.startLine();
            int candidateSpan = candidate.endLine() - candidate.startLine();
            if (candidateSpan < bestSpan) {
                best = candidate;
            }
        }
        return best;
    }

    public boolean isCollapsedHeader(int line) {
        return collapsedHeaderLines.contains(line);
    }

    public Set<Integer> collapsedHeaderLines() {
        return collapsedHeaderLines;
    }

    public boolean hasCollapsedRegions() {
        return !collapsedHeaderLines.isEmpty();
    }

    public List<FoldRegion> collapsedRegions() {
        if (collapsedHeaderLines.isEmpty()) {
            return List.of();
        }
        List<FoldRegion> collapsed = new ArrayList<>();
        for (FoldRegion region : regions) {
            if (collapsedHeaderLines.contains(region.startLine())) {
                collapsed.add(region);
            }
        }
        return List.copyOf(collapsed);
    }

    public boolean isHiddenLine(int line) {
        if (line < 0 || hiddenRanges.isEmpty()) {
            return false;
        }
        int low = 0;
        int high = hiddenRanges.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            LineRange range = hiddenRanges.get(mid);
            if (line < range.startInclusive()) {
                high = mid - 1;
                continue;
            }
            if (line > range.endInclusive()) {
                low = mid + 1;
                continue;
            }
            return true;
        }
        return false;
    }

    public FoldMap withCollapsedHeaders(Collection<Integer> headers) {
        if (regions.isEmpty()) {
            return empty();
        }
        Set<Integer> requested = new LinkedHashSet<>();
        if (headers != null) {
            for (Integer value : headers) {
                if (value != null && value >= 0) {
                    requested.add(value);
                }
            }
        }
        Set<Integer> filtered = filterCollapsedHeaders(requested, regionsByStartLine.keySet());
        if (filtered.equals(collapsedHeaderLines)) {
            return this;
        }
        return new FoldMap(regions, filtered);
    }

    public FoldMap toggleAtHeaderLine(int line) {
        if (!hasRegionStartingAt(line)) {
            return this;
        }
        Set<Integer> next = new LinkedHashSet<>(collapsedHeaderLines);
        if (!next.add(line)) {
            next.remove(line);
        }
        return withCollapsedHeaders(next);
    }

    public FoldMap collapseAll() {
        if (regions.isEmpty()) {
            return this;
        }
        return withCollapsedHeaders(regionsByStartLine.keySet());
    }

    public FoldMap expandAll() {
        if (collapsedHeaderLines.isEmpty()) {
            return this;
        }
        return withCollapsedHeaders(Set.of());
    }

    private static List<FoldRegion> normalizeRegions(List<FoldRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return List.of();
        }
        List<FoldRegion> sorted = new ArrayList<>(regions.size());
        for (FoldRegion region : regions) {
            if (region != null && region.endLine() > region.startLine()) {
                sorted.add(new FoldRegion(
                    region.startLine(),
                    region.endLine(),
                    region.kind(),
                    region.depth(),
                    region.collapsed()
                ));
            }
        }
        sorted.sort(
            Comparator.comparingInt(FoldRegion::startLine)
                .thenComparingInt(FoldRegion::endLine)
                .thenComparingInt(region -> region.kind().ordinal())
        );
        List<FoldRegion> deduplicated = new ArrayList<>(sorted.size());
        FoldRegion previous = null;
        for (FoldRegion region : sorted) {
            if (previous != null
                && previous.startLine() == region.startLine()
                && previous.endLine() == region.endLine()
                && previous.kind() == region.kind()) {
                continue;
            }
            deduplicated.add(region);
            previous = region;
        }
        return List.copyOf(deduplicated);
    }

    private static List<FoldRegion> applyCollapsedFlags(List<FoldRegion> regions, Set<Integer> collapsedHeaders) {
        if (regions.isEmpty()) {
            return List.of();
        }
        List<FoldRegion> updated = new ArrayList<>(regions.size());
        for (FoldRegion region : regions) {
            updated.add(new FoldRegion(
                region.startLine(),
                region.endLine(),
                region.kind(),
                region.depth(),
                collapsedHeaders.contains(region.startLine())
            ));
        }
        return List.copyOf(updated);
    }

    private static Map<Integer, List<FoldRegion>> buildRegionsByStartLine(List<FoldRegion> regions) {
        if (regions.isEmpty()) {
            return Map.of();
        }
        Map<Integer, List<FoldRegion>> grouped = new HashMap<>();
        for (FoldRegion region : regions) {
            grouped.computeIfAbsent(region.startLine(), ignored -> new ArrayList<>()).add(region);
        }
        Map<Integer, List<FoldRegion>> frozen = new HashMap<>(grouped.size());
        for (Map.Entry<Integer, List<FoldRegion>> entry : grouped.entrySet()) {
            List<FoldRegion> byLine = entry.getValue();
            byLine.sort(
                Comparator.comparingInt((FoldRegion region) -> region.endLine() - region.startLine())
                    .thenComparingInt(FoldRegion::depth)
            );
            frozen.put(entry.getKey(), List.copyOf(byLine));
        }
        return Collections.unmodifiableMap(frozen);
    }

    private static Set<Integer> extractCollapsedHeaders(List<FoldRegion> regions) {
        if (regions == null || regions.isEmpty()) {
            return Set.of();
        }
        Set<Integer> headers = new HashSet<>();
        for (FoldRegion region : regions) {
            if (region != null && region.collapsed()) {
                headers.add(region.startLine());
            }
        }
        return headers;
    }

    private static Set<Integer> filterCollapsedHeaders(Collection<Integer> requested, Collection<Integer> availableHeaders) {
        if (requested == null || requested.isEmpty() || availableHeaders == null || availableHeaders.isEmpty()) {
            return Set.of();
        }
        Set<Integer> available = availableHeaders instanceof Set<Integer> set
            ? set
            : new HashSet<>(availableHeaders);
        Set<Integer> filtered = new LinkedHashSet<>();
        for (Integer line : requested) {
            if (line != null && available.contains(line)) {
                filtered.add(line);
            }
        }
        return filtered;
    }

    private static List<LineRange> buildHiddenRanges(List<FoldRegion> regions, Set<Integer> collapsedHeaders) {
        if (regions.isEmpty() || collapsedHeaders.isEmpty()) {
            return List.of();
        }
        List<LineRange> raw = new ArrayList<>();
        for (FoldRegion region : regions) {
            if (!collapsedHeaders.contains(region.startLine())) {
                continue;
            }
            int start = region.startLine() + 1;
            int end = region.endLine();
            if (end >= start) {
                raw.add(new LineRange(start, end));
            }
        }
        if (raw.isEmpty()) {
            return List.of();
        }
        raw.sort(Comparator.comparingInt(LineRange::startInclusive).thenComparingInt(LineRange::endInclusive));
        List<LineRange> merged = new ArrayList<>();
        LineRange current = raw.getFirst();
        for (int i = 1; i < raw.size(); i++) {
            LineRange next = raw.get(i);
            if (next.startInclusive() <= current.endInclusive() + 1) {
                current = new LineRange(current.startInclusive(), Math.max(current.endInclusive(), next.endInclusive()));
                continue;
            }
            merged.add(current);
            current = next;
        }
        merged.add(current);
        return List.copyOf(merged);
    }

    private record LineRange(int startInclusive, int endInclusive) {
    }
}

