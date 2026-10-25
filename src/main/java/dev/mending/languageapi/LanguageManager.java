package dev.mending.languageapi;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

/**
 * Loads all language files and resolves texts for players.
 * <p>
 * All data lives in an immutable snapshot which is swapped atomically on reload.
 * This makes every lookup thread-safe, which matters because PlaceholderAPI
 * placeholders may be requested from async threads.
 */
public final class LanguageManager {

    /** Keys handled directly by the expansion. User keys with these names are ignored. */
    public static final Set<String> RESERVED_KEYS = Set.of("player", "active", "fallback");

    private static final String DEFAULT_FALLBACK = "en_us";
    private static final String DEFAULT_RESOURCE = "lang/en_us.yml";

    private final LanguageAPIPlugin plugin;
    private volatile Snapshot snapshot = new Snapshot(
            Map.of(), Map.of(), new Language(DEFAULT_FALLBACK, Map.of()),
            OutputFormat.MINIMESSAGE, true, true, "[{key}]"
    );

    public LanguageManager(LanguageAPIPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * (Re)loads the config and all language files.
     *
     * @return the number of successfully loaded languages
     */
    public int reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        String fallbackCode = normalize(config.getString("fallback-language", DEFAULT_FALLBACK));
        if (fallbackCode.isEmpty()) {
            fallbackCode = DEFAULT_FALLBACK;
        }
        boolean prefixMatching = config.getBoolean("language-prefix-matching", true);
        boolean parseNested = config.getBoolean("parse-nested-placeholders", true);
        String missingFormat = config.getString("missing-key-format", "[{key}]");
        OutputFormat format = OutputFormat.parse(config.getString("output-format"), OutputFormat.MINIMESSAGE);

        File langFolder = new File(plugin.getDataFolder(), "lang");
        ensureDefaults(langFolder, fallbackCode);

        // TreeMap keeps the language order deterministic (important for prefix matching)
        Map<String, Language> languages = new TreeMap<>();
        File[] files = langFolder.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".yml") || lower.endsWith(".yaml");
        });

        if (files != null) {
            for (File file : files) {
                String code = normalize(stripExtension(file.getName()));
                if (code.isEmpty()) {
                    continue;
                }
                if (languages.containsKey(code)) {
                    plugin.getLogger().warning("Duplicate language file '" + file.getName()
                            + "' (code '" + code + "' is already loaded), skipping it.");
                    continue;
                }
                Language language = loadLanguage(file, code, format);
                if (language != null) {
                    languages.put(code, language);
                }
            }
        }

        Language fallback = languages.get(fallbackCode);
        if (fallback == null) {
            plugin.getLogger().severe("Fallback language '" + fallbackCode + "' could not be loaded! "
                    + "Missing texts can not be resolved until you fix lang/" + fallbackCode + ".yml.");
            fallback = new Language(fallbackCode, Map.of());
        }

        // Index "language part" -> language code, used for client locales without an own file
        Map<String, String> prefixIndex = new HashMap<>();
        for (String code : languages.keySet()) {
            String prefix = prefixOf(code);
            // Prefer the "main" variant (e.g. de_de for de), otherwise the first alphabetically
            if (!prefixIndex.containsKey(prefix) || code.equals(prefix + "_" + prefix)) {
                prefixIndex.put(prefix, code);
            }
        }

        this.snapshot = new Snapshot(
                Map.copyOf(languages), Map.copyOf(prefixIndex), fallback,
                format, prefixMatching, parseNested, missingFormat == null ? "[{key}]" : missingFormat
        );

        plugin.getLogger().info("Loaded " + languages.size() + " language(s): "
                + String.join(", ", languages.keySet()) + " (fallback: " + fallbackCode + ")");
        return languages.size();
    }

    private @Nullable Language loadLanguage(File file, String code, OutputFormat format) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            // One broken file must not prevent the other languages from loading
            plugin.getLogger().log(Level.WARNING,
                    "Could not load language file '" + file.getName() + "': " + exception.getMessage());
            return null;
        }

        Map<String, String> texts = new HashMap<>();
        for (String path : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(path)) {
                continue;
            }
            Object value = yaml.get(path);
            if (value == null) {
                continue;
            }

            String key = path.toLowerCase(Locale.ROOT);
            if (RESERVED_KEYS.contains(key)) {
                plugin.getLogger().warning("Key '" + path + "' in " + file.getName()
                        + " is reserved and will be ignored.");
                continue;
            }

            String raw;
            if (value instanceof List<?> list) {
                List<String> lines = new ArrayList<>(list.size());
                for (Object element : list) {
                    lines.add(String.valueOf(element));
                }
                raw = String.join("\n", lines);
            } else {
                raw = String.valueOf(value);
            }

            // Convert once at load time, so a placeholder request is only a map lookup
            texts.put(key, format.convert(raw));
        }
        return new Language(code, texts);
    }

    /**
     * Creates the lang folder and the default en_us.yml if needed.
     */
    private void ensureDefaults(File langFolder, String fallbackCode) {
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().severe("Could not create the lang folder!");
            return;
        }

        File defaultFile = new File(langFolder, "en_us.yml");
        String[] existing = langFolder.list();
        boolean folderEmpty = existing == null || existing.length == 0;

        // Only restore en_us.yml if the folder is empty or it is the active fallback.
        // If an admin intentionally removed it and uses another fallback, respect that.
        if (!defaultFile.exists() && (folderEmpty || fallbackCode.equals(DEFAULT_FALLBACK))) {
            try {
                plugin.saveResource(DEFAULT_RESOURCE, false);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Default language resource is missing in the plugin jar.");
            }
        }
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Returns the raw client language code of a player (e.g. "de_de").
     * Falls back to the fallback language code if the player is unknown or reports no locale.
     */
    public String getClientLanguageCode(@Nullable Player player) {
        if (player != null) {
            String code = normalize(player.locale().toString());
            if (!code.isEmpty()) {
                return code;
            }
        }
        return snapshot.fallback.code();
    }

    /**
     * Resolves the language that is used for a player.
     * Order: exact match, same-language match (optional), fallback language.
     */
    public Language resolveLanguage(@Nullable Player player) {
        Snapshot current = snapshot;
        if (player == null) {
            return current.fallback;
        }

        String clientCode = normalize(player.locale().toString());
        if (clientCode.isEmpty()) {
            return current.fallback;
        }

        Language exact = current.languages.get(clientCode);
        if (exact != null) {
            return exact;
        }

        if (current.prefixMatching) {
            String matchedCode = current.prefixIndex.get(prefixOf(clientCode));
            if (matchedCode != null) {
                Language matched = current.languages.get(matchedCode);
                if (matched != null) {
                    return matched;
                }
            }
        }
        return current.fallback;
    }

    /**
     * Translates a key for a player. Falls back to the fallback language if the key is missing
     * in the player's language.
     *
     * @param player the player, or null to use the fallback language
     * @param key    the key (case-insensitive)
     * @return the text in the configured output format, or null if no language contains the key
     */
    public @Nullable String translate(@Nullable Player player, String key) {
        Snapshot current = snapshot;
        String lowerKey = key.toLowerCase(Locale.ROOT);

        Language language = resolveLanguage(player);
        String text = language.get(lowerKey);
        if (text == null && language != current.fallback) {
            text = current.fallback.get(lowerKey);
        }
        return text;
    }

    /**
     * Builds the text that is returned for a key that exists in no language.
     */
    public String formatMissing(String key) {
        return snapshot.missingFormat.replace("{key}", key);
    }

    public String getFallbackCode() {
        return snapshot.fallback.code();
    }

    public boolean isParseNestedPlaceholders() {
        return snapshot.parseNested;
    }

    public OutputFormat getOutputFormat() {
        return snapshot.format;
    }

    /**
     * @return the codes of all loaded languages (sorted)
     */
    public List<String> getLoadedLanguages() {
        return new ArrayList<>(snapshot.languages.keySet());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Normalizes codes: lower case, "-" becomes "_" (de-DE -> de_de). */
    static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String prefixOf(String code) {
        int index = code.indexOf('_');
        return index == -1 ? code : code.substring(0, index);
    }

    private static String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index == -1 ? fileName : fileName.substring(0, index);
    }

    /**
     * Immutable state of the manager, replaced as a whole on reload.
     */
    private record Snapshot(
            Map<String, Language> languages,
            Map<String, String> prefixIndex,
            Language fallback,
            OutputFormat format,
            boolean prefixMatching,
            boolean parseNested,
            String missingFormat
    ) {
    }
}