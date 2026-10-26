package dev.mending.languageapi.placeholder;

import dev.mending.languageapi.core.LanguageBundle;
import dev.mending.languageapi.core.LanguageCodes;
import dev.mending.languageapi.core.LanguageService;
import dev.mending.languageapi.core.PlaceholderResolver;
import dev.mending.languageapi.core.Settings;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * PlaceholderAPI expansion with the identifier "language".
 * Only exposes the texts of LanguageAPI's own lang/ folder, never those of other plugins.
 * <ul>
 *     <li>%language_player% - the client's language code (e.g. de_de)</li>
 *     <li>%language_active% - the language file actually used for the player</li>
 *     <li>%language_fallback% - the configured fallback language</li>
 *     <li>%language_&lt;key&gt;% - any key from the language files</li>
 * </ul>
 */
public final class LanguageExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final LanguageService service;
    private final PlaceholderResolver resolver;

    public LanguageExpansion(Plugin plugin, LanguageService service, PlaceholderResolver resolver) {
        this.plugin = plugin;
        this.service = service;
        this.resolver = resolver;
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

        Settings settings = service.settings();
        LanguageBundle bundle = service.ownBundle();

        // Online players are Player instances; offline players resolve to the fallback language
        Player player = offlinePlayer instanceof Player online ? online : null;
        String clientCode = LanguageCodes.of(player);

        String lowerParams = params.toLowerCase(Locale.ROOT);
        switch (lowerParams) {
            case "player":
                return clientCode.isEmpty() ? settings.fallbackLanguage() : clientCode;
            case "active":
                return bundle.effectiveLanguage(clientCode);
            case "fallback":
                return settings.fallbackLanguage();
            default:
                break;
        }

        String text = bundle.lookup(clientCode, lowerParams);
        if (text == null) {
            return settings.formatMissing(params);
        }

        // The result is returned to the caller as-is, so values are not escaped here
        return settings.parseNestedPlaceholders() ? resolver.resolve(offlinePlayer, text, false) : text;
    }
}