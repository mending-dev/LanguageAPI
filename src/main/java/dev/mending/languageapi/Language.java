package dev.mending.languageapi;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * An immutable, fully loaded language.
 *
 * @param code  the normalized language code (e.g. "de_de")
 * @param texts all texts by lower-cased key, already converted to the configured output format
 */
public record Language(String code, Map<String, String> texts) {

    public Language {
        texts = Map.copyOf(texts);
    }

    /**
     * @param key a lower-cased key
     * @return the text or null if this language does not contain the key
     */
    public @Nullable String get(String key) {
        return texts.get(key);
    }
}