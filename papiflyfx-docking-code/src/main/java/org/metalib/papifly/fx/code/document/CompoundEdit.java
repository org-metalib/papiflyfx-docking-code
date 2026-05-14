package org.metalib.papifly.fx.code.document;

import java.util.List;

/**
 * Groups multiple {@link EditCommand}s into a single undoable unit.
 * <p>
 * Apply executes sub-edits in forward order; undo reverts them in reverse.
 */
final class CompoundEdit implements EditCommand {

    private final List<EditCommand> edits;

    CompoundEdit(List<EditCommand> edits) {
        this.edits = edits;
    }

    @Override
    public void apply(TextSource textSource) {
        for (EditCommand edit : edits) {
            edit.apply(textSource);
        }
    }

    @Override
    public void undo(TextSource textSource) {
        for (int i = edits.size() - 1; i >= 0; i--) {
            edits.get(i).undo(textSource);
        }
    }

    @Override
    public boolean applyLineIndex(LineIndex lineIndex) {
        for (EditCommand edit : edits) {
            if (!edit.applyLineIndex(lineIndex)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean undoLineIndex(LineIndex lineIndex) {
        for (int i = edits.size() - 1; i >= 0; i--) {
            if (!edits.get(i).undoLineIndex(lineIndex)) {
                return false;
            }
        }
        return true;
    }
}
