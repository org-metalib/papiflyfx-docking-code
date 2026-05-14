# papiflyfx-docking-tree research

## 1) Scope and source material

This report captures learnings, findings, and implementation ideas for a new `papiflyfx-docking-tree` component, based on:

- `spec/papiflyfx-docking-tree/docking-tree-applications.md`
- `spec/papiflyfx-docking-tree/docking-tree-features.md`

The goal is to translate those notes into a practical architecture and delivery strategy suitable for the PapiflyFX ecosystem.

---

## 2) Primary learnings from the applications document

The applications document shows a very broad usage surface (30 examples across software tooling, content systems, business systems, design tooling, and niche domains). The key learnings are:

1. **A tree is a relationship viewer, not just a list.**  
   The component must model parent-child relationships as a first-class concept, including dynamic expansion and mixed node types.

2. **Categorization is the dominant usage pattern.**  
   Most examples (project explorer, DAM, inventory, org chart, taxonomy, library catalog) rely on grouping and drill-down. This strongly favors predictable expansion behavior, stable ordering, and clear visual hierarchy.

3. **Node-level controls matter in real products.**  
   The pro-tip in the source is important: practical tree UIs frequently require inline controls (visibility, lock, check state). That implies the cell rendering API must support persistent controls plus contextual actions.

4. **The component must be domain-agnostic.**  
   Use cases range from file systems to scene graphs to support tickets. Hardcoding semantics into the base tree would reduce reusability; the core should be generic and data-driven.

5. **Scalability is mandatory.**  
   Several listed applications naturally produce large hierarchies (dependency trees, schema browsers, scene graphs, taxonomy trees). Virtualization and incremental updates are not optional.

---

## 3) Primary learnings from the features document

The features document gives a solid architectural direction and practical UI guidance.

### 3.1 Architecture split is correct

The recommendation to separate:

- **Data model** (hierarchy),
- **Virtualization logic** (visible rows only),
- **Visual representation** (cells),

is the right baseline for long-term maintainability and performance.

### 3.2 MVP feature set is clear and sufficient

The MVP set is coherent:

- recursive hierarchy,
- virtualization,
- expand/collapse,
- modular cell factory,
- single selection/focus,
- depth-based indentation and scrolling behavior.

This is enough for an initial usable component without premature complexity.

### 3.3 Advanced features are realistic for enterprise use

The advanced set maps well to real-world needs:

- lazy loading and fixed-size optimizations,
- contextual cell controls (prefix and suffix),
- multi-select, DnD, in-place edit, context menus,
- filtering/search and richer visual polish.

### 3.4 The TreeCell "five-zone" strategy is a key UX finding

The five-zone anatomy is one of the strongest insights from the source:

1. disclosure,
2. prefix controls,
3. core data,
4. flexible spacer,
5. suffix info/actions.

This structure reduces clutter while preserving interaction density, and is likely the right default composition contract for PapiflyFX tree cells.

---

## 4) Consolidated findings for papiflyfx-docking-tree

From both source documents, the component should be positioned as:

1. **A reusable tree framework primitive**, not a domain widget.
2. **Performance-first by design**, with virtualization as a core invariant.
3. **Composable at the cell level**, enabling different products (docking navigator, scene graph, schema browser) without forking core code.
4. **Interaction-capable**, with clear extension points for selection models, DnD, editing, and hover actions.
5. **Safe for dense UIs**, where information and controls can coexist without visual chaos.

---

## 5) Recommended component architecture

Below is a proposed architecture direction aligned with the findings.

### 5.1 Proposed package surface

`org.metalib.papifly.fx.tree` (or equivalent under docking namespace), with clear layers:

- model,
- control/state,
- virtualization,
- cell rendering,
- interaction extensions.

### 5.2 Core model types (conceptual)

- `TreeNode<T>`: holds value, children, parent link (or parent resolver), and mutable node state.
- `TreeModel<T>`: root management, child access policy, mutation events.
- `TreeNodeState`: expanded, selected, focused, disabled, loading, editable.

Design preference: keep node state separate from business `T` to avoid polluting domain objects.

### 5.3 Virtualization and flattening

Use a flattened visible-row index:

- maintain list of currently visible nodes (`visibleRows`),
- recompute incrementally on expand/collapse and filtered changes,
- map row index -> node quickly,
- render only viewport rows.

This mirrors known high-performance control patterns and satisfies the source requirement for rendering only visible nodes.

### 5.4 Selection and focus model

Start with:

- single selection + focus (MVP),
- keyboard navigation (up/down/left/right, home/end),
- deterministic focus vs selection styling.

Then extend to multi-selection with range/toggle semantics.

### 5.5 Cell composition contract

Adopt the five-zone cell structure as a formal API concept:

- disclosure slot,
- prefix slot,
- core content slot,
- spacer,
- suffix slot.

Expose a `TreeCellFactory<T>` that can provide per-node controls for prefix/suffix while the control manages layout consistency and hover policies.

### 5.6 Interaction extension points

Design dedicated hooks/interfaces for:

- drag source/target behavior,
- context menu provider,
- inline edit lifecycle,
- per-node action handling.

This avoids baking behavior into one monolithic cell class.

---

## 6) API ideas (high-level)

The source docs do not define API signatures, but these ideas fit the required behavior:

```java
public final class DockTreeView<T> extends Control {
    public void setModel(TreeModel<T> model);
    public TreeModel<T> getModel();

    public void setCellFactory(TreeCellFactory<T> factory);
    public void setSelectionModel(TreeSelectionModel<T> selectionModel);

    public void setFixedCellSize(double size);
    public void setLazyChildrenProvider(LazyChildrenProvider<T> provider);
}

public interface TreeCellFactory<T> {
    DockTreeCell<T> createCell();
}

public interface DockTreeCell<T> {
    Node disclosureNode();
    Node prefixNode();
    Node coreNode();
    Node suffixNode();
    void update(TreeCellContext<T> context);
}
```

The intent is not to lock this exact API, but to preserve the architectural split and five-zone cell strategy.

---

## 7) Integration ideas within PapiflyFX docking ecosystem

`papiflyfx-docking-tree` can serve as a foundational side-panel/navigation component across modules:

1. **Docking navigator** (tabs/groups/layout objects as hierarchy).
2. **Code editor outline** (file symbols/headings/regions).
3. **Workspace/project explorer** (folders/files/logical groups).
4. **Scene/layer inspector** for graphical or UI modules.
5. **Schema/object inspectors** for debugging and tooling surfaces.

If kept generic, one tree control can power many panels with custom cell factories.

---

## 8) Delivery roadmap (phased)

### Phase 1: MVP foundation

- hierarchy model/events,
- virtualization + visible-row flattening,
- expand/collapse,
- single selection/focus + keyboard navigation,
- basic five-zone-compatible cell layout API.

### Phase 2: Practical interaction

- context menus,
- inline editing hooks,
- drag and drop (internal tree reorder first),
- hover actions in suffix zone.

### Phase 3: Performance and scalability

- lazy child loading,
- fixed cell size optimization path,
- optimized large-tree mutation handling.

### Phase 4: Advanced UX

- multi-selection,
- filtering/search,
- optional tree lines and badge/counter conventions.

This sequencing keeps early value high while controlling implementation risk.

---

## 9) Risks and mitigations

### Risk 1: UI clutter in high-density nodes

Mitigation:

- enforce slot-based composition,
- reserve suffix actions for hover,
- keep prefix controls intentionally minimal.

### Risk 2: Performance degradation on large trees

Mitigation:

- make virtualization non-optional in core,
- flatten only visible nodes,
- add fixed cell size fast-path and incremental recalculation.

### Risk 3: Over-coupling to a single domain

Mitigation:

- generic `T` model payload,
- domain logic in adapters/cell factories, not in core control.

### Risk 4: Interaction complexity (DnD, editing, selection)

Mitigation:

- layered delivery,
- explicit extension interfaces,
- strong test coverage for event ordering and state transitions.

---

## 10) Testing strategy recommendations

1. **Model tests**
   - parent/child integrity,
   - expand/collapse state propagation,
   - mutation event correctness.

2. **Virtualization tests**
   - row-window correctness,
   - visible-row recalculation on expand/collapse,
   - large dataset behavior.

3. **Interaction tests**
   - keyboard navigation semantics,
   - selection/focus consistency,
   - hover action visibility rules.

4. **Cell layout tests**
   - slot presence/ordering,
   - spacer alignment behavior,
   - suffix visibility on hover.

5. **Integration tests**
   - representative scenarios from source applications (project explorer, scene graph style panel, schema browser pattern).

---

## 11) Open decisions to finalize before implementation

1. Should the first release include multi-selection or defer it to phase 2/3?
2. Which DnD behaviors are in-scope initially (reorder only vs external drops)?
3. How much built-in cell chrome should be provided vs delegated entirely to factories?
4. Should filtering be view-only (hide rows) or model-aware (stateful query mode)?

Resolving these early will reduce redesign churn.

---

## 12) Final recommendation

Build `papiflyfx-docking-tree` as a reusable, virtualization-first control with a strict separation of model, rendering, and interaction layers. Use the five-zone `TreeCell` composition strategy as the default contract, deliver a strong MVP first, and then layer advanced interactions (multi-select, DnD, editing, filtering) in controlled phases.

This approach directly reflects both source documents and creates a component that is broadly useful across the PapiflyFX docking ecosystem.
