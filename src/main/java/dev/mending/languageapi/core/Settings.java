package dev.mending.languageapi.core;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Immutable values of the LanguageAPI config.yml.
 *
 * @param fallbackLanguage         global fallback language (applies to all plugins)
 * @param prefixMatching           map "de_at" to another "de_*" file if there is no exact file
 * @param outputFormat             output format of the PlaceholderAPI placeholders
 * @param parseNestedPlaceholders  resolve placeholders inside texts of the PlaceholderAPI placeholders
 * @param missingKeyFormat         text for missing keys, {key} is replaced by the key
 * @param escapePlaceholderValues  escape MiniMessage tags in placeholder values inside texts of other plugins
 */
public record Settings(
        String fallbackLanguage,
        boolean prefixMatching,
        OutputFormat outputFormat,
        boolean parseNestedPlaceholders,
        String missingKeyFormat,
        boolean escapePlaceholderValues
) {

    public static final String DEFAULT_LANGUAGE = "en_us";

    public static Settings from(FileConfiguration config) {
        String fallback = LanguageCodes.normalize(config.getString("fallback-language", DEFAULT_LANGUAGE));
        if (fallback.isEmpty()) {
            fallback = DEFAULT_LANGUAGE;
        }

        String missingFormat = config.getString("missing-key-format", "[{key}]");

        return new Settings(
                fallback,
                config.getBoolean("language-prefix-matching", true),
                OutputFormat.parse(config.getString("output-format"), OutputFormat.MINIMESSAGE),
                config.getBoolean("parse-nested-placeholders", true),
                missingFormat == null ? "[{key}]" : missingFormat,
                config.getBoolean("escape-placeholder-values", true)
        );
    }

    public String formatMissing(String key) {
        return missingKeyFormat.replace("{key}", key);
    }
}