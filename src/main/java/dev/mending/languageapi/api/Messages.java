package dev.mending.languageapi.api;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Message access of one plugin. Obtain an instance with {@link LanguageAPI#register(org.bukkit.plugin.Plugin)}.
 * <p>
 * All texts are MiniMessage. Dynamic values are passed as {@link TagResolver}s, e.g.
 * {@code Placeholder.unparsed("player", name)} for {@code <player>} in the text.
 * Use {@code unparsed} for player-controlled input so nobody can inject MiniMessage tags.
 * <p>
 * If PlaceholderAPI is installed and your plugin declares it as a dependency, PlaceholderAPI
 * placeholders (e.g. {@code %player_name%}) in the texts are resolved automatically before the text is parsed.
 * <p>
 * The language of an {@link Audience} is its client language. Everything that is not a player
 * (console, ...) uses the fallback language. Objects that are shown to several players at once
 * (boss bars, scoreboards, holograms) show everyone the same text, so create one object per player
 * or per language. All methods are thread-safe.
 */
public interface Messages {

    /**
     * Sends a message in the audience's language. Texts that are empty in the language file are not
     * sent, so server owners can disable single messages.
     */
    void send(Audience audience, String key, TagResolver... resolvers);

    /**
     * Sends a message as action bar. Empty texts are not sent.
     */
    void sendActionBar(Audience audience, String key, TagResolver... resolvers);

    /**
     * Sends a title with the default display times. Nothing is sent if title and subtitle are both empty.
     *
     * @param titleKey    the key of the title
     * @param subtitleKey the key of the subtitle, or null for no subtitle
     */
    default void sendTitle(Audience audience, String titleKey, @Nullable String subtitleKey,
                           TagResolver... resolvers) {
        sendTitle(audience, titleKey, subtitleKey, Title.DEFAULT_TIMES, resolvers);
    }

    /**
     * Sends a title with custom display times. Nothing is sent if title and subtitle are both empty.
     *
     * @param titleKey    the key of the title
     * @param subtitleKey the key of the subtitle, or null for no subtitle
     * @param times       fade in, stay and fade out times
     */
    void sendTitle(Audience audience, String titleKey, @Nullable String subtitleKey,
                   Title.Times times, TagResolver... resolvers);

    /**
     * Creates a boss bar with the translated name, shows it to the audience and returns it.
     * Every call creates a new bar for this audience only. Keep the returned bar to update it
     * ({@code bar.name(messages.get(...))}, {@code bar.progress(...)}) or to hide it again.
     * <p>
     * Unlike {@link #send}, an empty text does not disable the bar: it is shown without a name.
     *
     * @param key      the key of the bar's name
     * @param progress the progress between 0.0 and 1.0
     */
    BossBar showBossBar(Audience audience, String key, float progress,
                        BossBar.Color color, BossBar.Overlay overlay, TagResolver... resolvers);

    /**
     * Sends a message to all online players (each in their own language) and to the console.
     */
    void broadcast(String key, TagResolver... resolvers);

    /**
     * Returns a message as component in the audience's language.
     */
    Component get(Audience audience, String key, TagResolver... resolvers);

    /**
     * Returns a message as list of components, one per line. Useful for YAML lists (e.g. item lore).
     */
    List<Component> getLines(Audience audience, String key, TagResolver... resolvers);

    /**
     * Returns the unparsed MiniMessage text in the audience's language
     * (PlaceholderAPI placeholders are already resolved, if enabled for your plugin).
     */
    String getRaw(Audience audience, String key);

    /**
     * @return true if any language of this plugin contains the key
     */
    boolean has(String key);

    /**
     * @return the language code that is actually used for the audience (after fallback handling)
     */
    String getLanguage(Audience audience);

    /**
     * @return true if PlaceholderAPI is installed, your plugin declares it as a dependency and
     * placeholders are therefore resolved in your texts
     */
    boolean isPlaceholderApiEnabled();

    /**
     * Reloads the language files of this plugin.
     */
    void reload();
}