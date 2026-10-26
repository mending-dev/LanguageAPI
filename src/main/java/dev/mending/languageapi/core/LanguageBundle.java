package dev.mending.languageapi.core;

import dev.mending.languageapi.api.Messages;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Manages the {@code lang/} folder of exactly one plugin: creates the files, loads all languages and
 * resolves texts. It is used for LanguageAPI itself and for every plugin that registers via the API.
 * <p>
 * All data lives in an immutable snapshot which is swapped atomically on reload, so every lookup
 * is thread-safe (PlaceholderAPI may request placeholders asynchronously).
 * <p>
 * Text sources per language, the later one wins:
 * <ol>
 *     <li>the file bundled in the plugin jar (only if {@link Options#embeddedDefaults()} is set)</li>
 *     <li>the file in the plugin's data folder</li>
 * </ol>
 */
public final class LanguageBundle implements Messages {

    /**
     * Settings of a bundle.
     *
     * @param defaultLanguage  last-resort language of the plugin
     * @param embeddedDefaults use the language files bundled in the jar as the lowest layer
     * @param reservedKeys     keys that must not be used in language files
     * @param textMapper       applied once to every text while loading
     * @param placeholderApi   resolve PlaceholderAPI placeholders in texts (the plugin declares PlaceholderAPI)
     */
    public record Options(
            String defaultLanguage,
            boolean embeddedDefaults,
            Set<String> reservedKeys,
            UnaryOperator<String> textMapper,
            boolean placeholderApi
    ) {

        /** Options for plugins that register through the API: raw MiniMessage, bundled defaults. */
        public static Options thirdParty(String defaultLanguage, boolean placeholderApi) {
            return new Options(LanguageCodes.normalize(defaultLanguage), true, Set.of(),
                    UnaryOperator.identity(), placeholderApi);
        }
    }

    private record Snapshot(
            Map<String, Language> languages,
            Map<String, String> prefixIndex,
            @Nullable Language fallback,
            @Nullable Language defaultLanguage
    ) {
        static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of(), null, null);
    }

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String LANG_FOLDER = "lang";

    private final Plugin owner;
    private final LanguageService service;
    private final Options options;
    private final File langFolder;
    private final List<String> bundledFiles;
    private final Set<String> warnedKeys = ConcurrentHashMap.newKeySet();
    private volatile Snapshot snapshot = Snapshot.EMPTY;

    public LanguageBundle(Plugin owner, LanguageService service, Options options) {
        this.owner = owner;
        this.service = service;
        this.options = options;
        this.langFolder = new File(owner.getDataFolder(), LANG_FOLDER);
        this.bundledFiles = scanBundledFiles();
    }

    // ------------------------------------------------------------------
    // Loading
    // ------------------------------------------------------------------

    /**
     * (Re)loads all language files of the plugin.
     */
    @Override
    public synchronized void reload() {
        reloadAndCount();
    }

    /**
     * Same as {@link #reload()}, but returns the number of loaded languages.
     */
    public synchronized int reloadAndCount() {
        Settings settings = service.settings();
        prepareFolder(settings.fallbackLanguage());

        Map<String, String> bundledByCode = new HashMap<>();
        for (String path : bundledFiles) {
            bundledByCode.putIfAbsent(codeOf(path), path);
        }

        Map<String, File> filesByCode = new HashMap<>();
        File[] files = langFolder.listFiles((dir, name) -> isYamlName(name));
        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                String code = codeOf(file.getName());
                if (code.isEmpty()) {
                    continue;
                }
                if (filesByCode.putIfAbsent(code, file) != null) {
                    owner.getLogger().warning("Duplicate language file '" + file.getName()
                            + "' (code '" + code + "' is already loaded), skipping it.");
                }
            }
        }

        Set<String> codes = new TreeSet<>(filesByCode.keySet());
        if (options.embeddedDefaults()) {
            codes.addAll(bundledByCode.keySet());
        }

        // TreeMap keeps the order deterministic, which matters for prefix matching
        Map<String, Language> languages = new TreeMap<>();
        for (String code : codes) {
            Map<String, String> texts = new HashMap<>();
            boolean loaded = false;

            String bundledPath = options.embeddedDefaults() ? bundledByCode.get(code) : null;
            if (bundledPath != null) {
                loaded |= readBundled(bundledPath, texts);
            }
            File file = filesByCode.get(code);
            if (file != null) {
                loaded |= readFile(file, texts);
            }
            if (!loaded) {
                continue;
            }

            // Convert once at load time, so a lookup is only a map access
            texts.replaceAll((key, value) -> options.textMapper().apply(value));
            languages.put(code, new Language(code, texts));
        }

        // Index "language part" -> language code, used for client locales without an own file
        Map<String, String> prefixIndex = new HashMap<>();
        for (String code : languages.keySet()) {
            String prefix = LanguageCodes.prefixOf(code);
            // Prefer the "main" variant (e.g. de_de for de), otherwise the first alphabetically
            if (!prefixIndex.containsKey(prefix) || code.equals(prefix + "_" + prefix)) {
                prefixIndex.put(prefix, code);
            }
        }

        Language fallback = languages.get(settings.fallbackLanguage());
        Language defaultLanguage = languages.get(options.defaultLanguage());
        if (fallback == null && defaultLanguage == null) {
            owner.getLogger().warning("Neither the fallback language '" + settings.fallbackLanguage()
                    + "' nor the default language '" + options.defaultLanguage()
                    + "' exists in " + LANG_FOLDER + "/. Missing texts cannot be resolved.");
        }

        this.snapshot = new Snapshot(Map.copyOf(languages), Map.copyOf(prefixIndex), fallback, defaultLanguage);
        this.warnedKeys.clear();

        owner.getLogger().info("Loaded " + languages.size() + " language(s): "
                + String.join(", ", languages.keySet()) + " (fallback: " + settings.fallbackLanguage() + ")");
        return languages.size();
    }

    /**
     * Creates the lang folder and copies the bundled language files.
     * On the first start (empty folder) all bundled files are copied. Later only the active fallback
     * language is restored if it was deleted, so intentionally removed languages stay removed.
     */
    private void prepareFolder(String fallbackCode) {
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            owner.getLogger().severe("Could not create the lang folder!");
            return;
        }

        String[] existing = langFolder.list();
        boolean empty = existing == null || existing.length == 0;

        for (String path : bundledFiles) {
            File target = new File(owner.getDataFolder(), path);
            if (target.exists()) {
                continue;
            }
            if (empty || codeOf(path).equals(fallbackCode)) {
                try {
                    owner.saveResource(path, false);
                } catch (IllegalArgumentException exception) {
                    owner.getLogger().warning("Could not copy bundled language file '" + path + "'.");
                }
            }
        }
    }

    private boolean readBundled(String path, Map<String, String> target) {
        try (InputStream stream = owner.getResource(path)) {
            if (stream == null) {
                return false;
            }
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            flatten(yaml, path, target);
            return true;
        } catch (IOException | InvalidConfigurationException exception) {
            owner.getLogger().warning("Could not load bundled language file '" + path
                    + "': " + exception.getMessage());
            return false;
        }
    }

    private boolean readFile(File file, Map<String, String> target) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            // One broken file must not prevent the other languages from loading
            owner.getLogger().warning("Could not load language file '" + file.getName()
                    + "': " + exception.getMessage());
            return false;
        }
        flatten(yaml, file.getName(), target);
        return true;
    }

    /**
     * Flattens nested sections to dotted keys. Lists are joined with line breaks.
     */
    private void flatten(YamlConfiguration yaml, String source, Map<String, String> target) {
        for (String path : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(path)) {
                continue;
            }
            Object value = yaml.get(path);
            if (value == null) {
                continue;
            }

            String key = path.toLowerCase(Locale.ROOT);
            if (options.reservedKeys().contains(key)) {
                owner.getLogger().warning("Key '" + path + "' in " + source + " is reserved and will be ignored.");
                continue;
            }

            if (value instanceof List<?> list) {
                List<String> lines = new ArrayList<>(list.size());
                for (Object element : list) {
                    lines.add(String.valueOf(element));
                }
                target.put(key, String.join("\n", lines));
            } else {
                target.put(key, String.valueOf(value));
            }
        }
    }

    /**
     * Finds the language files bundled in the plugin jar ({@code lang/*.yml}).
     */
    private List<String> scanBundledFiles() {
        List<String> found = new ArrayList<>();
        try {
            CodeSource source = owner.getClass().getProtectionDomain().getCodeSource();
            if (source != null) {
                Path path = Path.of(source.getLocation().toURI());
                if (Files.isRegularFile(path)) {
                    try (JarFile jar = new JarFile(path.toFile())) {
                        jar.stream()
                                .map(JarEntry::getName)
                                .filter(LanguageBundle::isLanguageResource)
                                .sorted()
                                .forEach(found::add);
                    }
                }
            }
        } catch (Exception exception) {
            owner.getLogger().fine("Could not scan the plugin jar for language files: " + exception.getMessage());
        }

        if (found.isEmpty()) {
            // Scanning is not possible in every environment, so at least look for the default language
            String probe = LANG_FOLDER + "/" + options.defaultLanguage() + ".yml";
            try (InputStream stream = owner.getResource(probe)) {
                if (stream != null) {
                    found.add(probe);
                }
            } catch (IOException ignored) {
                // Nothing to do, the plugin simply has no bundled file
            }
        }
        return List.copyOf(found);
    }

    private static boolean isLanguageResource(String name) {
        String prefix = LANG_FOLDER + "/";
        return name.startsWith(prefix)
                && name.indexOf('/', prefix.length()) == -1
                && isYamlName(name);
    }

    private static boolean isYamlName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yml") || lower.endsWith(".yaml");
    }

    /** Extracts the normalized language code from a path or file name. */
    private static String codeOf(String path) {
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return LanguageCodes.normalize(LanguageCodes.stripExtension(fileName));
    }

    // ------------------------------------------------------------------
    // Resolving
    // ------------------------------------------------------------------

    /**
     * Finds the text for a client language. Order: client language, fallback language, default language.
     * PlaceholderAPI placeholders are not resolved here.
     *
     * @param clientCode the normalized client language code, may be empty
     * @param key        the key (case-insensitive)
     * @return the text, or null if no language contains the key
     */
    public @Nullable String lookup(String clientCode, String key) {
        Snapshot current = snapshot;
        String lowerKey = key.toLowerCase(Locale.ROOT);

        Language primary = match(current, clientCode);
        if (primary != null) {
            String text = primary.get(lowerKey);
            if (text != null) {
                return text;
            }
        }

        Language fallback = current.fallback();
        if (fallback != null && fallback != primary) {
            String text = fallback.get(lowerKey);
            if (text != null) {
                return text;
            }
        }

        Language defaultLanguage = current.defaultLanguage();
        if (defaultLanguage != null && defaultLanguage != primary && defaultLanguage != fallback) {
            return defaultLanguage.get(lowerKey);
        }
        return null;
    }

    /**
     * @return the language code that is used for a client language (after fallback handling)
     */
    public String effectiveLanguage(String clientCode) {
        Snapshot current = snapshot;
        Language primary = match(current, clientCode);
        if (primary != null) {
            return primary.code();
        }
        if (current.fallback() != null) {
            return current.fallback().code();
        }
        if (current.defaultLanguage() != null) {
            return current.defaultLanguage().code();
        }
        return service.settings().fallbackLanguage();
    }

    /** Exact match first, then another file of the same language (if enabled). */
    private @Nullable Language match(Snapshot current, String clientCode) {
        if (clientCode.isEmpty()) {
            return null;
        }
        Language exact = current.languages().get(clientCode);
        if (exact != null) {
            return exact;
        }
        if (service.settings().prefixMatching()) {
            String matchedCode = current.prefixIndex().get(LanguageCodes.prefixOf(clientCode));
            if (matchedCode != null) {
                return current.languages().get(matchedCode);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Messages API
    // ------------------------------------------------------------------

    @Override
    public void send(Audience audience, String key, TagResolver... resolvers) {
        String raw = find(audience, key);
        if (raw == null) {
            audience.sendMessage(missingComponent(key));
            return;
        }
        if (raw.isEmpty()) {
            return;
        }
        audience.sendMessage(MINI_MESSAGE.deserialize(raw, resolvers));
    }

    @Override
    public void sendActionBar(Audience audience, String key, TagResolver... resolvers) {
        String raw = find(audience, key);
        if (raw == null) {
            audience.sendActionBar(missingComponent(key));
            return;
        }
        if (raw.isEmpty()) {
            return;
        }
        audience.sendActionBar(MINI_MESSAGE.deserialize(raw, resolvers));
    }

    @Override
    public void broadcast(String key, TagResolver... resolvers) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            send(player, key, resolvers);
        }
        send(Bukkit.getConsoleSender(), key, resolvers);
    }

    @Override
    public Component get(Audience audience, String key, TagResolver... resolvers) {
        String raw = find(audience, key);
        if (raw == null) {
            return missingComponent(key);
        }
        return MINI_MESSAGE.deserialize(raw, resolvers);
    }

    @Override
    public List<Component> getLines(Audience audience, String key, TagResolver... resolvers) {
        String raw = find(audience, key);
        if (raw == null) {
            return List.of(missingComponent(key));
        }
        if (raw.isEmpty()) {
            return List.of();
        }

        String[] lines = raw.split("\n", -1);
        List<Component> components = new ArrayList<>(lines.length);
        for (String line : lines) {
            components.add(MINI_MESSAGE.deserialize(line, resolvers));
        }
        return components;
    }

    @Override
    public String getRaw(Audience audience, String key) {
        String raw = find(audience, key);
        return raw == null ? service.settings().formatMissing(key) : raw;
    }

    @Override
    public boolean has(String key) {
        String lowerKey = key.toLowerCase(Locale.ROOT);
        return snapshot.languages().values().stream().anyMatch(language -> language.get(lowerKey) != null);
    }

    @Override
    public String getLanguage(Audience audience) {
        return effectiveLanguage(LanguageCodes.of(audience));
    }

    @Override
    public boolean isPlaceholderApiEnabled() {
        return options.placeholderApi() && service.hasPlaceholderApi();
    }

    /**
     * Looks up a text for an audience, resolves PlaceholderAPI placeholders (if enabled) and
     * reports unknown keys once.
     */
    private @Nullable String find(Audience audience, String key) {
        String raw = lookup(LanguageCodes.of(audience), key);
        if (raw == null) {
            if (warnedKeys.add(key)) {
                owner.getLogger().warning("Missing translation key '" + key + "' (not found in any language file).");
            }
            return null;
        }
        return resolvePlaceholders(audience, raw);
    }

    private String resolvePlaceholders(Audience audience, String raw) {
        if (raw.indexOf('%') == -1 || !isPlaceholderApiEnabled()) {
            return raw;
        }
        PlaceholderResolver resolver = service.placeholderResolver();
        if (resolver == null) {
            return raw;
        }
        // Everything that is not a player (console, ...) is resolved without player context
        OfflinePlayer player = audience instanceof Player online ? online : null;
        return resolver.resolve(player, raw, service.settings().escapePlaceholderValues());
    }

    private Component missingComponent(String key) {
        return Component.text(service.settings().formatMissing(key));
    }

    // ------------------------------------------------------------------
    // Info
    // ------------------------------------------------------------------

    public Plugin owner() {
        return owner;
    }

    public String ownerName() {
        return owner.getName();
    }

    /**
     * @return the codes of all loaded languages (sorted)
     */
    public List<String> languageCodes() {
        return snapshot.languages().keySet().stream().sorted().toList();
    }
}