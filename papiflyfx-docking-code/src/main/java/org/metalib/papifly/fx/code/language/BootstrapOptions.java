package org.metalib.papifly.fx.code.language;

public record BootstrapOptions(
    boolean includeBuiltIns,
    boolean loadServiceProviders,
    ConflictPolicy conflictPolicy
) {
    public static BootstrapOptions defaults() {
        return new BootstrapOptions(true, true, ConflictPolicy.REPLACE_EXISTING);
    }
}
