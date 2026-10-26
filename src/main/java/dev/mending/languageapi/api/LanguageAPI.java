package dev.mending.languageapi.api;

import dev.mending.languageapi.LanguageAPIPlugin;
import dev.mending.languageapi.core.LanguageService;
import dev.mending.languageapi.core.Settings;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;

/**
 * Entry point of the LanguageAPI for other plugins.
 * <p>
 * Usage:
 * <pre>{@code
 * private Messages messages;
 *
 * public void onEnable() {
 *     messages = LanguageAPI.register(this);
 * }
 *
 * messages.send(player, "shop.bought", Placeholder.unparsed("item", "Apple"));
 * }</pre>
 * <p>
 * Registering creates {@code plugins/<YourPlugin>/lang/}, copies the language files that are bundled
 * in your jar under {@code lang/} and loads all languages.
 */
public final class LanguageAPI {

    private LanguageAPI() {
    }

    /**
     * Registers a plugin with {@code en_us} as its default language.
     * Calling this again for the same plugin returns the existing instance.
     *
     * @param plugin the plugin that wants to use translations
     * @return the message API of the plugin
     */
    public static Messages register(Plugin plugin) {
        return register(plugin, Settings.DEFAULT_LANGUAGE);
    }

    /**
     * Registers a plugin.
     * <p>
     * The default language is the last resort: it is used when neither the player's language nor the
     * fallback language of the LanguageAPI config contains a key. It should be the language
     * your bundled default file is written in.
     *
     * @param plugin          the plugin that wants to use translations
     * @param defaultLanguage the language code of your complete default file, e.g. "en_us"
     * @return the message API of the plugin
     */
    public static Messages register(Plugin plugin, String defaultLanguage) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(defaultLanguage, "defaultLanguage");
        return service().register(plugin, defaultLanguage);
    }

    /**
     * @param plugin a plugin
     * @return the message API of the plugin if it has been registered
     */
    public static Optional<Messages> get(Plugin plugin) {
        return service().get(plugin);
    }

    /**
     * @return the fallback language configured in the LanguageAPI config (e.g. "en_us")
     */
    public static String getFallbackLanguage() {
        return service().settings().fallbackLanguage();
    }

    private static LanguageService service() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LanguageAPI");
        if (!(plugin instanceof LanguageAPIPlugin languageApi) || !plugin.isEnabled()) {
            throw new IllegalStateException("LanguageAPI is not installed or not enabled. "
                    + "Add it as a dependency in your paper-plugin.yml.");
        }
        return languageApi.getService();
    }
}