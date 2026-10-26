package dev.mending.languageapi.core;

import dev.mending.languageapi.api.Messages;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central service of LanguageAPI: holds the global settings and the bundles of all plugins
 * (LanguageAPI itself and every plugin that registered through the API).
 */
public final class LanguageService implements Listener {

    /** Keys handled directly by the PlaceholderAPI expansion. They must not be used in LanguageAPI's own files. */
    public static final Set<String> RESERVED_KEYS = Set.of("player", "active", "fallback");

    private static final String PLACEHOLDER_API = "PlaceholderAPI";

    private final JavaPlugin plugin;
    private final Map<String, LanguageBundle> bundles = new ConcurrentHashMap<>();
    private final LanguageBundle ownBundle;
    private volatile Settings settings;
    private volatile @Nullable PlaceholderResolver placeholderResolver;

    public LanguageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.settings = Settings.from(plugin.getConfig());

        // LanguageAPI's own texts (exposed via PlaceholderAPI): converted to the configured output format,
        // no bundled defaults, so keys that an admin deleted do not come back.
        // Placeholders inside these texts are handled by the expansion itself.
        this.ownBundle = new LanguageBundle(plugin, this, new LanguageBundle.Options(
                Settings.DEFAULT_LANGUAGE,
                false,
                RESERVED_KEYS,
                text -> this.settings.outputFormat().convert(text),
                false
        ));
        this.bundles.put(plugin.getName(), ownBundle);
    }

    /**
     * Loads the language files of LanguageAPI itself.
     */
    public void load() {
        ownBundle.reload();
    }

    /**
     * Reloads the config and the language files of all registered plugins.
     *
     * @return the number of reloaded plugins (including LanguageAPI)
     */
    public int reloadAll() {
        plugin.reloadConfig();
        settings = Settings.from(plugin.getConfig());

        int count = 0;
        for (LanguageBundle bundle : bundles.values()) {
            bundle.reload();
            count++;
        }
        return count;
    }

    /**
     * Registers a plugin. Registering the same plugin again returns the existing instance.
     * PlaceholderAPI support is enabled if the plugin declares PlaceholderAPI as a dependency.
     */
    public synchronized Messages register(Plugin owner, String defaultLanguage) {
        LanguageBundle existing = bundles.get(owner.getName());
        if (existing != null && existing.owner() == owner) {
            return existing;
        }

        LanguageBundle bundle = new LanguageBundle(owner, this,
                LanguageBundle.Options.thirdParty(defaultLanguage, declaresPlaceholderApi(owner)));
        bundle.reload();
        bundles.put(owner.getName(), bundle);
        return bundle;
    }

    public Optional<Messages> get(Plugin owner) {
        Objects.requireNonNull(owner, "plugin");
        LanguageBundle bundle = bundles.get(owner.getName());
        if (bundle != null && bundle.owner() == owner) {
            return Optional.of(bundle);
        }
        return Optional.empty();
    }

    /**
     * Removes the registration of a plugin when it is disabled, so no stale plugin instances are kept.
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        Plugin disabled = event.getPlugin();
        if (disabled == plugin) {
            return;
        }
        LanguageBundle bundle = bundles.get(disabled.getName());
        if (bundle != null && bundle.owner() == disabled) {
            bundles.remove(disabled.getName());
        }
    }

    /**
     * Checks whether a plugin declares PlaceholderAPI as a (required or optional) dependency.
     */
    private static boolean declaresPlaceholderApi(Plugin owner) {
        PluginMeta meta = owner.getPluginMeta();
        return containsIgnoreCase(meta.getPluginDependencies(), PLACEHOLDER_API)
                || containsIgnoreCase(meta.getPluginSoftDependencies(), PLACEHOLDER_API);
    }

    private static boolean containsIgnoreCase(Collection<String> names, String name) {
        return names.stream().anyMatch(candidate -> candidate.equalsIgnoreCase(name));
    }

    public Settings settings() {
        return settings;
    }

    /**
     * @return the bundle of LanguageAPI itself (the only one exposed via PlaceholderAPI)
     */
    public LanguageBundle ownBundle() {
        return ownBundle;
    }

    /**
     * @return the resolver for PlaceholderAPI placeholders, or null if PlaceholderAPI is not installed
     */
    public @Nullable PlaceholderResolver placeholderResolver() {
        return placeholderResolver;
    }

    public void setPlaceholderResolver(@Nullable PlaceholderResolver placeholderResolver) {
        this.placeholderResolver = placeholderResolver;
    }

    /**
     * @return true if PlaceholderAPI is installed and hooked
     */
    public boolean hasPlaceholderApi() {
        return placeholderResolver != null;
    }

    /**
     * @return all registered bundles, sorted by plugin name
     */
    public List<LanguageBundle> getBundles() {
        return bundles.values().stream()
                .sorted(Comparator.comparing(LanguageBundle::ownerName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}