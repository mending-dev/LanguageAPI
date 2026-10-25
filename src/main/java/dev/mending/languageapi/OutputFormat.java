package dev.mending.languageapi;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Locale;

/**
 * Defines how language texts are returned by the placeholders.
 */
public enum OutputFormat {

    /** Raw MiniMessage text, returned unchanged. */
    MINIMESSAGE,

    /** Legacy section-sign colors, including hex colors. */
    LEGACY,

    /** Plain text without any formatting. */
    PLAIN;

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    /**
     * Converts a raw MiniMessage text to this output format.
     *
     * @param raw the raw MiniMessage text
     * @return the converted text
     */
    public String convert(String raw) {
        if (this == MINIMESSAGE) {
            return raw;
        }
        try {
            Component component = MINI_MESSAGE.deserialize(raw);
            return switch (this) {
                case LEGACY -> LEGACY_SERIALIZER.serialize(component);
                case PLAIN -> PlainTextComponentSerializer.plainText().serialize(component);
                default -> raw;
            };
        } catch (RuntimeException exception) {
            // A broken text must never break the whole language, return it unchanged
            return raw;
        }
    }

    /**
     * Parses a config value, falling back to the given default for invalid input.
     */
    public static OutputFormat parse(String input, OutputFormat fallback) {
        if (input == null) {
            return fallback;
        }
        try {
            return valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}