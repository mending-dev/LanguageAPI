package dev.mending.languageapi.placeholder;

import dev.mending.languageapi.core.LanguageService;
import org.bukkit.plugin.Plugin;

/**
 * Connects LanguageAPI to PlaceholderAPI: registers the "language" expansion and enables placeholders
 * in the texts of other plugins.
 * <p>
 * Only instantiate this class if PlaceholderAPI is installed, otherwise loading it fails.
 */
public final class PlaceholderHook {

    private final LanguageService service;
    private final PapiPlaceholderResolver resolver = new PapiPlaceholderResolver();
    private final LanguageExpansion expansion;

    public PlaceholderHook(Plugin plugin, LanguageService service) {
        this.service = service;
        this.expansion = new LanguageExpansion(plugin, service, resolver);
    }

    /**
     * Enables placeholders in texts of other plugins and registers the expansion.
     *
     * @return false if the expansion could not be registered
     */
    public boolean register() {
        // Placeholders in texts of other plugins work independently of the expansion
        service.setPlaceholderResolver(resolver);
        return expansion.register();
    }

    public void unregister() {
        service.setPlaceholderResolver(null);
        expansion.unregister();
    }
}