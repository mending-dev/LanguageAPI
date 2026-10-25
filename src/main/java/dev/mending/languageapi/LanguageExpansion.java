package dev.mending.languageapi;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * PlaceholderAPI expansion with the identifier "language".
 * <ul>
 *     <li>%language_player% - the client's language code (e.g. de_de)</li>
 *     <li>%language_active% - the language file actually used for the player</li>
 *     <li>%language_fallback% - the configured fallback language</li>
 *     <li>%language_&lt;key&gt;% - any key from the language files</li>
 * </ul>
 */
public final class LanguageExpansion extends PlaceholderExpansion {

    /** Maximum depth for placeholders inside language texts. Protects against self-references. */
    private static final int MAX_NESTING_DEPTH = 3;

    private static final ThreadLocal<Integer> NESTING_DEPTH = ThreadLocal.withInitial(() -> 0);

    private final LanguageAPIPlugin plugin;
    private final LanguageManager languageManager;

    public LanguageExpansion(LanguageAPIPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "language";
    }

    @Override
    public @NotNull String getAuthor() {
        List<String> authors = plugin.getPluginMeta().getAuthors();
        return authors.isEmpty() ? "unknown" : String.join(", ", authors);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    /**
     * Keep the expansion registered when PlaceholderAPI is reloaded via /papi reload.
     */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (params.isEmpty()) {
            return null;
        }

        // Online players are Player instances; offline players resolve to the fallback language
        Player player = offlinePlayer instanceof Player online ? online : null;

        String lowerParams = params.toLowerCase(Locale.ROOT);
        switch (lowerParams) {
            case "player":
                return languageManager.getClientLanguageCode(player);
            case "active":
                return languageManager.resolveLanguage(player).code();
            case "fallback":
                return languageManager.getFallbackCode();
            default:
                break;
        }

        String text = languageManager.translate(player, lowerParams);
        if (text == null) {
            return languageManager.formatMissing(params);
        }

        return resolveNested(offlinePlayer, text);
    }

    /**
     * Resolves placeholders inside a language text (e.g. %player_name%) with a depth guard.
     */
    private String resolveNested(OfflinePlayer player, String text) {
        if (!languageManager.isParseNestedPlaceholders() || text.indexOf('%') == -1) {
            return text;
        }

        int depth = NESTING_DEPTH.get();
        if (depth >= MAX_NESTING_DEPTH) {
            return text;
        }

        NESTING_DEPTH.set(depth + 1);
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } finally {
            if (depth == 0) {
                NESTING_DEPTH.remove();
            } else {
                NESTING_DEPTH.set(depth);
            }
        }
    }
}