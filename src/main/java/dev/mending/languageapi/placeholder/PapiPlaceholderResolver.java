package dev.mending.languageapi.placeholder;

import dev.mending.languageapi.core.PlaceholderResolver;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves PlaceholderAPI placeholders in texts.
 * <p>
 * This class references PlaceholderAPI directly and must only be loaded if PlaceholderAPI is installed.
 */
public final class PapiPlaceholderResolver implements PlaceholderResolver {

    /** Maximum depth for placeholders inside placeholder values. Protects against self-references. */
    private static final int MAX_DEPTH = 3;

    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private static final Pattern PLACEHOLDER = Pattern.compile("%([^%\\s]+)%");

    /** Our own expansion: its values are MiniMessage texts and therefore trusted. */
    private static final String TRUSTED_IDENTIFIER = "language";

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public String resolve(@Nullable OfflinePlayer player, String text, boolean escapeTags) {
        if (text.indexOf('%') == -1) {
            return text;
        }

        int depth = DEPTH.get();
        if (depth >= MAX_DEPTH) {
            return text;
        }

        DEPTH.set(depth + 1);
        try {
            return escapeTags ? resolveEscaped(player, text) : PlaceholderAPI.setPlaceholders(player, text);
        } finally {
            if (depth == 0) {
                DEPTH.remove();
            } else {
                DEPTH.set(depth);
            }
        }
    }

    /**
     * Resolves every placeholder on its own and escapes MiniMessage tags in the values of foreign
     * expansions, so they are inserted as plain text.
     */
    private String resolveEscaped(@Nullable OfflinePlayer player, String text) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder result = new StringBuilder(text.length() + 16);

        while (matcher.find()) {
            String placeholder = matcher.group();
            String value = PlaceholderAPI.setPlaceholders(player, placeholder);

            // An unchanged value means the placeholder is unknown, keep it as it is
            if (!value.equals(placeholder) && !isTrusted(matcher.group(1))) {
                value = MINI_MESSAGE.escapeTags(value);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private boolean isTrusted(String placeholderContent) {
        int separator = placeholderContent.indexOf('_');
        String identifier = separator == -1 ? placeholderContent : placeholderContent.substring(0, separator);
        return identifier.toLowerCase(Locale.ROOT).equals(TRUSTED_IDENTIFIER);
    }
}