package org.metalib.papifly.fx.code.language;

import java.util.Optional;

public interface UserFileAssociationMapping {
    Optional<String> resolveLanguageId(String fileNameOrPath);
}
