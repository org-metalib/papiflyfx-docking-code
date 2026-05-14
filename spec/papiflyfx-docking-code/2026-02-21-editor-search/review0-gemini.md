# Code Editor Search/Replace Visual Review

This review compares the current implementation (Image #1) with the IntelliJ IDEA reference (Image #2) and provides recommendations to improve the visual aesthetics and user experience.

## Visual Comparison & Analysis

| Feature | Current Implementation (Image #1) | IntelliJ IDEA Reference (Image #2) |
| :--- | :--- | :--- |
| **Compactness** | Tall and "blocky"; consumes significant vertical space. | Extremely slim; height is optimized to minimize occlusion. |
| **Iconography** | Uses Unicode characters (▲, ▼, ▶, ✕) which look dated. | Uses crisp, custom SVG icons for search, navigation, and closing. |
| **Layout** | Controls are loosely grouped; toggles are separate checkboxes. | Highly integrated; toggles (Cc, W, .*) are inside or adjacent to the field. |
| **Typography** | Standard font sizes; labels like "Replace" and "All" are text-heavy. | Smaller, specialized fonts; clear visual hierarchy for match counts. |
| **Depth/Elevation** | Flat background with a simple bottom border. | Subtle drop shadow and slightly different background color to provide "floating" depth. |
| **Search Field** | Basic TextField without internal icons or history. | Includes a search icon on the left and a history dropdown on the right. |

## Recommendations for Improvement

### 1. Compact & Integrated Layout
- **Reduce Padding:** Tighten vertical padding in `SearchController` and `HBox` rows to make the panel feel like a sleek toolbar rather than a blocky overlay.
- **In-Field Toggles:** Move the "Cc", "W", and ".*" toggles into the search field (on the right side) to save horizontal space and create a modern "integrated" look.

### 2. Modern Iconography
- **SVG Icons:** Replace Unicode symbols with high-quality SVG icons for:
  - Magnifying glass (Search)
  - Up/Down arrows (Navigation)
  - Close "X"
  - Chevron (Expand/Collapse)
  - History dropdown arrow
- **Icon Buttons:** Use icon-only buttons for navigation and closing, which reduces visual noise.

### 3. Enhanced "Replace" Row
- **Button Styling:** The "Replace" and "All" buttons should be smaller and styled as "Action" buttons with subtle borders that only appear on hover, or high-contrast buttons that match the theme's primary color.
- **Exclude Action:** Add the "Exclude" button as seen in IntelliJ to allow users to skip specific matches during a replace-all operation.

### 4. Search History
- **Dropdowns:** Add a history dropdown to both Search and Replace fields. This is a standard IDE feature that is currently missing and adds significant value for repetitive tasks.

### 5. Visual Feedback & Depth
- **Drop Shadow:** Add a subtle `DropShadow` effect to the `SearchController` to make it appear elevated above the code content.
- **Match Count:** Style the match count (e.g., "1 of 28") with a lighter, secondary color and position it consistently next to the navigation buttons.
- **Field Highlighting:** Use a more distinct border color (e.g., the theme's accent color) when a text field is focused or when search results are found.

### 6. Animation (Optional but Recommended)
- **Slide/Fade:** Implement a subtle slide-down or fade-in animation when opening the search panel to make the transition feel smoother and less jarring.

## Proposed Action Plan

1. **Refactor `SearchController` styling:** Update CSS/JavaFX properties to reduce heights and paddings.
2. **Introduce Icon Library:** Add a set of SVG icons to the project resources.
3. **Custom SearchField:** Create a custom component that allows placing icons and toggles inside the `TextField`.
4. **Implement History:** Update `SearchModel` to track recent queries and wire them to a dropdown in the UI.
5. **Add Elevation:** Apply a `DropShadow` and refine background/border colors in `CodeEditorTheme`.
