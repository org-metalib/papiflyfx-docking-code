package org.metalib.papifly.fx.code.language;

public record RegistryDiagnostic(
    String languageId,
    String sourceProvider,
    String message,
    Throwable cause
) {}
