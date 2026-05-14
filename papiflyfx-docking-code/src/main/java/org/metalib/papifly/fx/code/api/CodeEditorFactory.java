package org.metalib.papifly.fx.code.api;

import javafx.scene.Node;
import org.metalib.papifly.fx.docking.api.ContentFactory;
import org.metalib.papifly.fx.code.settings.EditorSettingsSupport;

/**
 * ContentFactory implementation for creating code editor content nodes.
 */
public class CodeEditorFactory implements ContentFactory {

    /**
     * Stable factory identifier for code editor content.
     */
    public static final String FACTORY_ID = "code-editor";

    /**
     * Creates a code-editor content factory.
     */
    public CodeEditorFactory() {
        // Default constructor for service wiring.
    }

    /**
     * Creates content for the provided factory id.
     *
     * @param factoryId requested factory id
     * @return code editor node when id matches, otherwise {@code null}
     */
    @Override
    public Node create(String factoryId) {
        if (!FACTORY_ID.equals(factoryId)) {
            return null;
        }
        CodeEditor editor = new CodeEditor();
        EditorSettingsSupport.applyDefaults(editor);
        return editor;
    }
}
