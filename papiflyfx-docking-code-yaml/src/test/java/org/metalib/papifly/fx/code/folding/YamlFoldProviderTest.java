package org.metalib.papifly.fx.code.folding;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.code.lexer.TokenMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlFoldProviderTest {

    private final YamlFoldProvider provider = new YamlFoldProvider();

    @Test
    void detectsIndentedMappingRegion() {
        FoldMap foldMap = provider.recompute(
            List.of(
                "metadata:",
                "  name: demo",
                "  labels:",
                "    app: demo",
                "spec: {}"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().anyMatch(region ->
            region.kind() == FoldKind.YAML_MAPPING && region.startLine() == 0 && region.endLine() == 3));
        assertTrue(foldMap.regions().stream().anyMatch(region ->
            region.kind() == FoldKind.YAML_MAPPING && region.startLine() == 2 && region.endLine() == 3));
    }

    @Test
    void detectsSequenceRegionWithIndentedChildren() {
        FoldMap foldMap = provider.recompute(
            List.of(
                "containers:",
                "  - name: app",
                "    image: demo:latest",
                "    ports:",
                "      - containerPort: 8080"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().anyMatch(region ->
            region.kind() == FoldKind.YAML_MAPPING && region.startLine() == 1 && region.endLine() == 4));
    }

    @Test
    void detectsBlockScalarRegionIncludingBlankBodyLines() {
        FoldMap foldMap = provider.recompute(
            List.of(
                "script: |",
                "  echo one",
                "",
                "  echo two",
                "next: true"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        FoldRegion blockScalar = foldMap.regions().stream()
            .filter(region -> region.kind() == FoldKind.YAML_BLOCK_SCALAR)
            .findFirst()
            .orElseThrow();
        assertEquals(0, blockScalar.startLine());
        assertEquals(3, blockScalar.endLine());
    }

    @Test
    void preservesCollapsedHeaderLinesAcrossRecompute() {
        FoldMap initial = provider.recompute(
            List.of("root:", "  child: value"),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        ).withCollapsedHeaders(List.of(0));

        FoldMap recomputed = provider.recompute(
            List.of("root:", "  child: updated"),
            TokenMap.empty(),
            initial,
            0,
            () -> false
        );

        assertTrue(recomputed.isCollapsedHeader(0));
    }

    @Test
    void detectsMultiLineFlowMappingRegion() {
        FoldMap foldMap = provider.recompute(
            List.of(
                "metadata: {",
                "  name: demo,",
                "  labels: { app: demo }",
                "}"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().anyMatch(region ->
            region.kind() == FoldKind.YAML_FLOW && region.startLine() == 0 && region.endLine() == 3));
    }

    @Test
    void detectsNestedMultiLineFlowSequenceRegionAndIgnoresQuotedDelimiters() {
        FoldMap foldMap = provider.recompute(
            List.of(
                "items: [",
                "  \"not a ] close\",",
                "  { name: demo }",
                "] # close"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().anyMatch(region ->
            region.kind() == FoldKind.YAML_FLOW && region.startLine() == 0 && region.endLine() == 3));
    }

    @Test
    void ignoresUnbalancedFlowDelimiters() {
        FoldMap foldMap = provider.recompute(
            List.of(
                "items: [",
                "  one",
                "next: true"
            ),
            TokenMap.empty(),
            FoldMap.empty(),
            0,
            () -> false
        );

        assertTrue(foldMap.regions().stream().noneMatch(region -> region.kind() == FoldKind.YAML_FLOW));
    }
}
