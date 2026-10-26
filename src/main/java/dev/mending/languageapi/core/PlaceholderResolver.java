package dev.mending.languageapi.core;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves PlaceholderAPI placeholders in texts.
 * <p>
 * This abstraction keeps the core free of PlaceholderAPI classes, so LanguageAPI works
 * when PlaceholderAPI is not installed.
 */
public interface PlaceholderResolver {

    /**
     * Resolves all placeholders in a text.
     *
     * @param player     the player for the placeholders, or null for no player context
     * @param text       the text containing placeholders like %player_name%
     * @param escapeTags if true, values of placeholders from other expansions are escaped so that they
     *                   can be safely inserted into MiniMessage text
     * @return the text with resolved placeholders
     */
    String resolve(@Nullable OfflinePlayer player, String text, boolean escapeTags);
}