package dev.mending.languageapi.core;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Helpers for language codes like "de_de".
 */
public final class LanguageCodes {

    private LanguageCodes() {
    }

    /**
     * Normalizes a code: trimmed, lower case, "-" becomes "_" (de-DE becomes de_de).
     */
    public static String normalize(@Nullable String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * @return the language part of a code ("de_at" becomes "de")
     */
    public static String prefixOf(String code) {
        int index = code.indexOf('_');
        return index == -1 ? code : code.substring(0, index);
    }

    public static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index == -1 ? fileName : fileName.substring(0, index);
    }

    /**
     * Detects the client language of an audience.
     *
     * @return the normalized code, or an empty string if the audience has no language (console, ...)
     */
    public static String of(@Nullable Audience audience) {
        if (audience == null) {
            return "";
        }
        Locale locale;
        if (audience instanceof Player player) {
            locale = player.locale();
        } else {
            locale = audience.getOrDefault(Identity.LOCALE, null);
        }
        return locale == null ? "" : normalize(locale.toString());
    }
}