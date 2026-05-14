## 1. The Core Architecture: Unidirectional Flow

To prevent race conditions between the user typing and the schema validating, the system must follow a strict, unidirectional data flow.

* **State:** The current raw YAML string, the JSON Schema definition, and a collection of active validation errors.
* **Action:** The user types in the `papiflyfx-docking-code` editor.
* **Reducer (Background):** A parser builds the AST, runs validation, and calculates exact text offsets.
* **Effect (UI Thread):** The editor applies error styling (squiggles), and the `papiflyfx-docking-tree` component populates a "Problems" panel.

---

## 2. Solving the "Line-Mapping" Problem

JSON Schema validators return errors using **JSON Pointers** (e.g., `$.database.password`). However, `papiflyfx-docking-code` needs **line and column numbers** to draw red squiggles. Standard JSON/YAML parsers (like Jackson) drop line numbers during parsing.

To bridge this gap, you must implement a dual-pass strategy:

* **Pass 1 (The Coordinate Map):** Parse the raw text using **SnakeYAML**. SnakeYAML generates an AST where every `Node` contains a `Mark` object (holding the exact line and column). Traverse this AST to build a dictionary mapping JSON Pointers to Line Numbers (e.g., `"/database/password" -> Line 14`).
* **Pass 2 (The Validation):** Convert the text into a standard `JsonNode` (via Jackson) and run it through the `networknt/json-schema-validator`.
* **The Join:** When the validator outputs an error at `$.database.password`, look up that path in your Coordinate Map to get "Line 14", and send that line number to the editor UI.

---

## 3. The Reactive Execution Pipeline

To keep the UI snappy, validation must never block the JavaFX Application Thread. Here is the lifecycle of a single keystroke:

| Pipeline Stage | Thread | Primary Action | Technology/Component |
| :--- | :--- | :--- | :--- |
| **Input** | UI Thread | Capture keystroke and update text model. | `papiflyfx-docking-code` |
| **Debounce** | Background | Wait ~300ms for typing to pause before proceeding. | RxJava or `ScheduledExecutorService` |
| **AST Mapping** | Background | Build the JSON Pointer-to-Line map. | `SnakeYAML` |
| **Validation** | Background | Evaluate the parsed data against the Schema. | `networknt/json-schema-validator` |
| **UI Update** | UI Thread | Draw red squiggles and populate error docks. | `Platform.runLater` |

---

## 4. PapiflyFX Component Integration

Leverage the specific modules within the `org.metalib.papifly.docking` framework to make the experience feel cohesive:

* **The Editor Dock (`papiflyfx-docking-code`):** Use its styling APIs to apply custom CSS classes (e.g., `.yaml-error-squiggle`) to the specific line ranges resolved in the pipeline above.
* **The Problems Dock (`papiflyfx-docking-tree`):** Create a separate dockable panel at the bottom of the screen. Bind the results of your Validation pipeline to a virtualized tree component here. Clicking an error in this tree should programmatically scroll the `papiflyfx-docking-code` editor to the corresponding line.
* **The Docking Manager:** Intercept the state of the editor. If the text has changed but hasn't been saved, append an asterisk (`*`) to the dock's tab title. If the document is currently in an invalid schema state, you can optionally disable the "Save" action via your application's command bindings.

---

Are you planning to support dynamic schema switching (e.g., detecting the required schema based on the file name or an inline `# yaml-language-server` comment), or will this editor be tied to a single, static configuration schema?